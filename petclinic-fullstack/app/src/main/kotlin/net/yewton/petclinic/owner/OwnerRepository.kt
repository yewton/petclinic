package net.yewton.petclinic.owner

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import net.yewton.petclinic.jooq.tables.references.OWNERS
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.selectFrom
import org.jooq.kotlin.intoList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Component
class OwnerRepository(private val create: DSLContext) {
  @Transactional(readOnly = true)
  suspend fun findByLastName(
    lastName: String,
    pageable: Pageable,
  ): Page<Owner> {
    val query =
      create.select(
        OWNERS.ID,
        OWNERS.FIRST_NAME,
        OWNERS.LAST_NAME,
        OWNERS.ADDRESS,
        OWNERS.CITY,
        OWNERS.TELEPHONE,
        multiset(
          selectFrom(OWNERS.pets),
        ).intoList { Pet(it.name) },
        count().over(),
      ).from(OWNERS)
        .where(OWNERS.LAST_NAME.startsWith(lastName))
        .orderBy(OWNERS.ID)
        .limit(pageable.pageSize)
        .offset(pageable.offset)
    val result =
      Flux.from(query)
        .asFlow().map {
          val owner =
            Owner(
              it[OWNERS.ID],
              it[OWNERS.FIRST_NAME],
              it[OWNERS.LAST_NAME],
              it[OWNERS.ADDRESS],
              it[OWNERS.CITY],
              it[OWNERS.TELEPHONE],
              it.value7(),
            )
          val totalCount = it.value8()
          owner to totalCount
        }.toList().toMap()
    return PageImpl(result.keys.toList(), pageable, result.values.first().toLong())
  }
}

package net.yewton.petclinic.vet

import io.micrometer.tracing.annotation.NewSpan
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import net.yewton.petclinic.jooq.tables.references.VETS
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.selectFrom
import org.jooq.kotlin.intoList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class VetRepository(private val create: DSLContext) {
  companion object {
    val logger = LoggerFactory.getLogger(VetRepository::class.java)
  }

  @NewSpan("findAllVetsCoroutine")
  @Transactional(readOnly = true)
  suspend fun findAll(pageable: Pageable): Page<Vet> {
    val query = findAllQuery(pageable)
    val result =
      Flux.from(query).asFlow().map {
        logger.info("Coroutine 版")
        val vet = Vet(it.get(VETS.ID), it.get(VETS.FIRST_NAME), it.get(VETS.LAST_NAME), it.value4())
        val totalCount = it.value5()
        vet to totalCount
      }.toList().toMap()
    return PageImpl(result.keys.toList(), pageable, result.values.first().toLong())
  }

  @NewSpan("findAllVetsReactor")
  @Transactional(readOnly = true)
  fun findAllR(pageable: Pageable): Mono<Page<Vet>> {
    val query = findAllQuery(pageable)
    return Flux.from(query).map {
      logger.info("Reactor 版")
      val vet = Vet(it.get(VETS.ID), it.get(VETS.FIRST_NAME), it.get(VETS.LAST_NAME), it.value4())
      val totalCount = it.value5()
      vet to totalCount
    }.collectList().map { it.toMap() }.map {
      PageImpl(it.keys.toList(), pageable, it.values.first().toLong())
    }
  }

  private fun findAllQuery(pageable: Pageable) =
    create.select(
      VETS.ID,
      VETS.FIRST_NAME,
      VETS.LAST_NAME,
      multiset(
        selectFrom(VETS.specialties),
      ).intoList { Specialty(it.name) },
      count().over(),
    ).from(VETS)
      .orderBy(VETS.ID.asc())
      .limit(pageable.pageSize)
      .offset(pageable.offset)
}

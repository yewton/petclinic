package net.yewton.petclinic.owner

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import net.yewton.petclinic.jooq.tables.references.OWNERS
import net.yewton.petclinic.jooq.tables.references.PETS
import net.yewton.petclinic.pet.Pet
import net.yewton.petclinic.pet.PetType
import org.jooq.DSLContext
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.multiset
import org.jooq.impl.DSL.row
import org.jooq.impl.DSL.select
import org.jooq.kotlin.intoList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OwnerRepository(private val create: DSLContext) {
  private fun pets() =
    multiset(
      select(
        OWNERS.pets.ID,
        OWNERS.pets.NAME,
        OWNERS.pets.BIRTH_DATE,
        row(OWNERS.pets.types_.NAME).mapping { PetType(it) },
      ).from(OWNERS.pets),
    ).intoList { Pet(it[PETS.ID], it[PETS.NAME], it[PETS.BIRTH_DATE], it.value4(), hashSetOf()) }

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
        pets(),
        count().over(),
      ).from(OWNERS)
        .where(OWNERS.LAST_NAME.startsWith(lastName))
        .orderBy(OWNERS.ID)
        .limit(pageable.pageSize)
        .offset(pageable.offset)
    val result =
      query.asFlow().map {
        val owner =
          Owner(
            it[OWNERS.ID],
            it[OWNERS.FIRST_NAME],
            it[OWNERS.LAST_NAME],
            it[OWNERS.ADDRESS],
            it[OWNERS.CITY],
            it[OWNERS.TELEPHONE],
            it.value7().toSet(),
          )
        val totalCount = it.value8()
        owner to totalCount
      }.toList().toMap()
    if (result.isEmpty()) {
      return Page.empty(pageable)
    }
    return PageImpl(result.keys.toList(), pageable, result.values.first().toLong())
  }

  @Transactional(readOnly = true)
  suspend fun findById(id: Int): Owner? {
    val query =
      create.select(
        OWNERS.ID,
        OWNERS.FIRST_NAME,
        OWNERS.LAST_NAME,
        OWNERS.ADDRESS,
        OWNERS.CITY,
        OWNERS.TELEPHONE,
        pets(),
      ).from(OWNERS)
        .where(OWNERS.ID.eq(id))
    return query.awaitFirstOrNull()?.let {
      Owner(
        it[OWNERS.ID],
        it[OWNERS.FIRST_NAME],
        it[OWNERS.LAST_NAME],
        it[OWNERS.ADDRESS],
        it[OWNERS.CITY],
        it[OWNERS.TELEPHONE],
        it.value7().toSet(),
      )
    }
  }

  @Transactional
  suspend fun save(owner: Owner): Owner {
    if (owner.isNew()) {
      val newId =
        create.insertInto(OWNERS)
          .columns(OWNERS.FIRST_NAME, OWNERS.LAST_NAME, OWNERS.ADDRESS, OWNERS.CITY, OWNERS.TELEPHONE)
          .values(owner.firstName, owner.lastName, owner.address, owner.city, owner.telephone)
          .returningResult(OWNERS.ID)
          .awaitSingle()
          .let { it.value1() }
      return owner.copy(id = newId)
    } else {
      create.update(OWNERS)
        .set(OWNERS.FIRST_NAME, owner.firstName)
        .set(OWNERS.LAST_NAME, owner.lastName)
        .set(OWNERS.ADDRESS, owner.address)
        .set(OWNERS.CITY, owner.city)
        .set(OWNERS.TELEPHONE, owner.telephone)
        .where(OWNERS.ID.eq(owner.id))
        .awaitSingle()
      return owner
    }
  }
}

package net.yewton.petclinic.pet

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import net.yewton.petclinic.jooq.tables.references.PETS
import net.yewton.petclinic.jooq.tables.references.TYPES
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PetTypeRepository(
  private val create: DSLContext,
) {
  @Transactional(readOnly = true)
  suspend fun findAll(): List<PetType> =
    create
      .select(TYPES.ID, TYPES.NAME)
      .from(TYPES)
      .orderBy(TYPES.NAME)
      .asFlow()
      .map { PetType(it[TYPES.ID], it[TYPES.NAME]) }
      .toList()

  @Transactional(readOnly = true)
  suspend fun findById(id: Int): PetType? =
    create
      .select(TYPES.ID, TYPES.NAME)
      .from(TYPES)
      .where(TYPES.ID.eq(id))
      .awaitFirstOrNull()
      ?.let { PetType(it[TYPES.ID], it[TYPES.NAME]) }

  @Transactional(readOnly = true)
  suspend fun isInUse(id: Int): Boolean =
    create
      .selectCount()
      .from(PETS)
      .where(PETS.TYPE_ID.eq(id))
      .awaitSingle()
      .value1() > 0

  @Transactional
  suspend fun save(petType: PetType): PetType {
    if (petType.isNew()) {
      val newId =
        create
          .insertInto(TYPES)
          .columns(TYPES.NAME)
          .values(petType.name)
          .returningResult(TYPES.ID)
          .awaitSingle()
          .value1()
      return petType.copy(id = newId)
    } else {
      create
        .update(TYPES)
        .set(TYPES.NAME, petType.name)
        .where(TYPES.ID.eq(petType.id))
        .awaitSingle()
      return petType
    }
  }

  @Transactional
  suspend fun delete(id: Int) {
    create
      .deleteFrom(TYPES)
      .where(TYPES.ID.eq(id))
      .awaitSingle()
  }
}

package net.yewton.petclinic.vet

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import net.yewton.petclinic.jooq.tables.references.SPECIALTIES
import net.yewton.petclinic.jooq.tables.references.VET_SPECIALTIES
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SpecialtyRepository(
  private val create: DSLContext,
) {
  @Transactional(readOnly = true)
  suspend fun findAll(): List<Specialty> =
    create
      .select(SPECIALTIES.ID, SPECIALTIES.NAME)
      .from(SPECIALTIES)
      .orderBy(SPECIALTIES.NAME)
      .asFlow()
      .map { Specialty(it[SPECIALTIES.ID], it[SPECIALTIES.NAME]) }
      .toList()

  @Transactional(readOnly = true)
  suspend fun findById(id: Int): Specialty? =
    create
      .select(SPECIALTIES.ID, SPECIALTIES.NAME)
      .from(SPECIALTIES)
      .where(SPECIALTIES.ID.eq(id))
      .awaitFirstOrNull()
      ?.let { Specialty(it[SPECIALTIES.ID], it[SPECIALTIES.NAME]) }

  @Transactional(readOnly = true)
  suspend fun isInUse(id: Int): Boolean =
    create
      .selectCount()
      .from(VET_SPECIALTIES)
      .where(VET_SPECIALTIES.SPECIALTY_ID.eq(id))
      .awaitSingle()
      .value1() > 0

  @Transactional
  suspend fun save(specialty: Specialty): Specialty {
    if (specialty.isNew()) {
      val newId =
        create
          .insertInto(SPECIALTIES)
          .columns(SPECIALTIES.NAME)
          .values(specialty.name)
          .returningResult(SPECIALTIES.ID)
          .awaitSingle()
          .value1()
      return specialty.copy(id = newId)
    } else {
      create
        .update(SPECIALTIES)
        .set(SPECIALTIES.NAME, specialty.name)
        .where(SPECIALTIES.ID.eq(specialty.id))
        .awaitSingle()
      return specialty
    }
  }

  @Transactional
  suspend fun delete(id: Int) {
    create
      .deleteFrom(SPECIALTIES)
      .where(SPECIALTIES.ID.eq(id))
      .awaitSingle()
  }
}

package net.yewton.petclinic.pet

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import net.yewton.petclinic.jooq.tables.references.PETS
import net.yewton.petclinic.jooq.tables.references.TYPES
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PetRepository(
  private val create: DSLContext,
) {
  @Transactional
  suspend fun save(
    pet: Pet,
    ownerId: Int,
  ): Pet {
    val typeId =
      create
        .select(TYPES.ID)
        .from(TYPES)
        .where(TYPES.NAME.eq(pet.type.name))
        .awaitFirstOrNull()
        ?.value1()
        ?: throw IllegalArgumentException("Pet type not found: ${pet.type.name}")

    if (pet.isNew()) {
      val newId =
        create
          .insertInto(PETS)
          .columns(PETS.NAME, PETS.BIRTH_DATE, PETS.TYPE_ID, PETS.OWNER_ID)
          .values(pet.name, pet.birthDate, typeId, ownerId)
          .returningResult(PETS.ID)
          .awaitSingle()
          .value1()

      return pet.copy(id = newId)
    } else {
      create
        .update(PETS)
        .set(PETS.NAME, pet.name)
        .set(PETS.BIRTH_DATE, pet.birthDate)
        .set(PETS.TYPE_ID, typeId)
        .set(PETS.OWNER_ID, ownerId)
        .where(PETS.ID.eq(pet.id))
        .awaitFirstOrNull()
      return pet
    }
  }
}

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
  suspend fun findById(petId: Int): Pet? {
    return create
      .select(PETS.ID, PETS.NAME, PETS.BIRTH_DATE, TYPES.NAME)
      .from(PETS)
      .join(TYPES).on(PETS.TYPE_ID.eq(TYPES.ID))
      .where(PETS.ID.eq(petId))
      .awaitFirstOrNull()
      ?.let { record ->
        Pet(
          id = record.value1(),
          name = record.value2(),
          birthDate = record.value3(),
          type = PetType(record.value4()),
          visits = hashSetOf()
        )
      }
  }

  @Transactional
  suspend fun save(
    pet: Pet,
    ownerId: Int,
  ): Pet {
    if (pet.isNew()) {
      val typeId =
        create
          .select(TYPES.ID)
          .from(TYPES)
          .where(TYPES.NAME.eq(pet.type.name))
          .awaitFirstOrNull()
          ?.value1()
          ?: throw IllegalArgumentException("Pet type not found: ${pet.type.name}")

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
      val typeId =
        create
          .select(TYPES.ID)
          .from(TYPES)
          .where(TYPES.NAME.eq(pet.type.name))
          .awaitFirstOrNull()
          ?.value1()
          ?: throw IllegalArgumentException("Pet type not found: ${pet.type.name}")

      create
        .update(PETS)
        .set(PETS.NAME, pet.name)
        .set(PETS.BIRTH_DATE, pet.birthDate)
        .set(PETS.TYPE_ID, typeId)
        .where(PETS.ID.eq(pet.id))
        .awaitSingle()

      return pet
    }
  }
}

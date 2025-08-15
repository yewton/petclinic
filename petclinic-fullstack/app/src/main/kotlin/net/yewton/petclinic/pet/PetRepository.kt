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
  suspend fun findById(id: Int): Pet? {
    return create.select(PETS.ID, PETS.NAME, PETS.BIRTH_DATE, TYPES.NAME)
      .from(PETS)
      .join(TYPES)
      .on(PETS.TYPE_ID.eq(TYPES.ID))
      .where(PETS.ID.eq(id))
      .awaitFirstOrNull()
      ?.let {
        Pet(
          it.value1(),
          it.value2(),
          it.value3(),
          PetType(it.value4()!!),
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

      pet.id = newId
      return pet
    } else {
      val typeId =
        create
          .select(TYPES.ID)
          .from(TYPES)
          .where(TYPES.NAME.eq(pet.type.name))
          .awaitFirstOrNull()
          ?.value1()
          ?: throw IllegalArgumentException("Pet type not found: ${pet.type.name}")
      create.update(PETS)
        .set(PETS.NAME, pet.name)
        .set(PETS.BIRTH_DATE, pet.birthDate)
        .set(PETS.TYPE_ID, typeId)
        .where(PETS.ID.eq(pet.id))
        .awaitSingle()
      return pet
    }
  }
}

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
    if (pet.isNew()) {
      val typeId =
        create
          .select(TYPES.ID)
          .from(TYPES)
          .where(TYPES.NAME.eq(pet.type.name))
          .awaitFirstOrNull()?.value1()
          ?: throw IllegalArgumentException("Pet type not found: ${pet.type.name}")

      val newId =
        create
          .insertInto(PETS)
          .columns(PETS.NAME, PETS.BIRTH_DATE, PETS.TYPE_ID, PETS.OWNER_ID)
          .values(pet.name, pet.birthDate, typeId, ownerId)
          .returningResult(PETS.ID)
          .awaitSingle()
          .let { it.value1() }

      return pet.copy(id = newId)
    } else {
      TODO("Update functionality not implemented")
    }
  }
}

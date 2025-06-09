package net.yewton.petclinic.pet

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
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
      .select(TYPES.NAME)
      .from(TYPES)
      .orderBy(TYPES.NAME)
      .asFlow()
      .map { PetType(it[TYPES.NAME]) }
      .toList()
}

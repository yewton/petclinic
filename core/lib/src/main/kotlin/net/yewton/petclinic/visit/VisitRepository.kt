package net.yewton.petclinic.visit

import kotlinx.coroutines.reactive.awaitSingle
import net.yewton.petclinic.jooq.tables.references.VISITS
import org.jooq.DSLContext
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class VisitRepository(
  private val create: DSLContext,
) {
  @Transactional
  suspend fun save(
    visit: Visit,
    petId: Int,
  ): Visit {
    if (visit.id == null) {
      val newId =
        create
          .insertInto(VISITS)
          .columns(VISITS.VISIT_DATE, VISITS.DESCRIPTION, VISITS.PET_ID)
          .values(visit.date, visit.description, petId)
          .returningResult(VISITS.ID)
          .awaitSingle()
          .value1()
      return visit.copy(id = newId)
    } else {
      create
        .update(VISITS)
        .set(VISITS.VISIT_DATE, visit.date)
        .set(VISITS.DESCRIPTION, visit.description)
        .where(VISITS.ID.eq(visit.id))
        .awaitSingle()
      return visit
    }
  }
}

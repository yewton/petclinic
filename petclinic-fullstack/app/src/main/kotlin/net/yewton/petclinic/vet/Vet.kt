package net.yewton.petclinic.vet

import net.yewton.petclinic.model.Person

data class Vet(
  val id: Int?,
  override val firstName: String?,
  override val lastName: String?,
  val specialties: List<Specialty>?,
) : Person {
  val nrOfSpecialties = specialties?.size
}

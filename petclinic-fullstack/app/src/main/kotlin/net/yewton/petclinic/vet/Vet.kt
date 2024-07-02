package net.yewton.petclinic.vet

data class Vet(val id: Int?, val firstName: String?, val lastName: String?, val specialties: List<Specialty>?) {
  val nrOfSpecialties = specialties?.size
}

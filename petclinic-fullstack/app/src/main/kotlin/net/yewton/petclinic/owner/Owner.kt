package net.yewton.petclinic.owner

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotEmpty
import net.yewton.petclinic.model.Person
import net.yewton.petclinic.pet.Pet

data class Owner(
  val id: Int?,
  @field:NotEmpty
  override val firstName: String?,
  @field:NotEmpty
  override val lastName: String?,
  @field:NotEmpty
  val address: String?,
  @field:NotEmpty
  val city: String?,
  @field:NotEmpty
  @field:Digits(fraction = 0, integer = 10)
  val telephone: String?,
  val pets: Set<Pet> = emptySet(),
) : Person {
  fun isNew(): Boolean = id == null
}

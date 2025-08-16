package net.yewton.petclinic.owner

import jakarta.validation.constraints.Digits
import jakarta.validation.constraints.NotEmpty
import net.yewton.petclinic.model.Person
import net.yewton.petclinic.pet.Pet

data class Owner(
  var id: Int?,
  @field:NotEmpty
  override var firstName: String?,
  @field:NotEmpty
  override var lastName: String?,
  @field:NotEmpty
  var address: String?,
  @field:NotEmpty
  var city: String?,
  @field:NotEmpty
  @field:Digits(fraction = 0, integer = 10)
  var telephone: String?,
  val pets: MutableSet<Pet> = mutableSetOf(),
) : Person {
  fun isNew(): Boolean = id == null

  fun addPet(pet: Pet) {
    if (pet.isNew()) {
      pets.add(pet)
    }
  }
}

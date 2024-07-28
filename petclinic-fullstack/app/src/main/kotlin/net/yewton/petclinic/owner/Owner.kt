package net.yewton.petclinic.owner

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import net.yewton.petclinic.model.Person
import net.yewton.petclinic.pet.Pet

data class Owner(
  val id: Int?,
  override val firstName: String?,
  override val lastName: String?,
  @field:NotBlank
  val address: String?,
  @field:NotBlank
  val city: String?,
  @field:NotBlank
  @field:Pattern(regexp = "\\d{10}", message = "Telephone must be a 10-digit number")
  val telephone: String?,
  val pets: List<Pet>,
) : Person

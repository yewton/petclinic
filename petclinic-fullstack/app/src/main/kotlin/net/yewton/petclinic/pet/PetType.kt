package net.yewton.petclinic.pet

import jakarta.validation.constraints.NotBlank

data class PetType(
  val id: Int? = null,
  @field:NotBlank
  val name: String? = null,
) {
  override fun toString(): String = name ?: ""

  fun isNew(): Boolean = id == null
}

package net.yewton.petclinic.pet

import jakarta.validation.constraints.NotBlank
import net.yewton.petclinic.model.Persistable

data class PetType(
  override val id: Int? = null,
  @field:NotBlank
  val name: String? = null,
) : Persistable<Int> {
  override fun toString(): String = name ?: ""
}

package net.yewton.petclinic.vet

import jakarta.validation.constraints.NotBlank
import net.yewton.petclinic.model.Persistable

data class Specialty(
  override val id: Int? = null,
  @field:NotBlank
  val name: String? = null,
) : Persistable<Int> {
  override fun toString(): String = name ?: ""
}

package net.yewton.petclinic.pet

import jakarta.validation.constraints.NotEmpty
import net.yewton.petclinic.model.Persistable
import net.yewton.petclinic.visit.Visit
import java.time.LocalDate

data class Pet(
  override val id: Int?,
  @field:NotEmpty
  val name: String?,
  val birthDate: LocalDate?,
  val type: PetType,
  val visits: Set<Visit> = hashSetOf(),
) : Persistable<Int> {
  override fun toString(): String = name ?: ""
}

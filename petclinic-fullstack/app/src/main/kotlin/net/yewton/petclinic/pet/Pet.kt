package net.yewton.petclinic.pet

import jakarta.validation.constraints.NotEmpty
import net.yewton.petclinic.visit.Visit
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class Pet(
  val id: Int?,
  @field:NotEmpty
  val name: String?,
  @field:DateTimeFormat(pattern = "yyyy-MM-dd") val birthDate: LocalDate?,
  val type: PetType,
  val visits: Set<Visit> = hashSetOf(),
) {
  override fun toString(): String = name ?: ""

  fun isNew(): Boolean = id == null
}

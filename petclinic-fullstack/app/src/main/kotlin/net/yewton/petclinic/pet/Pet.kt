package net.yewton.petclinic.pet

import net.yewton.petclinic.visit.Visit
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class Pet(
  val id: Int?,
  val name: String?,
  @field:DateTimeFormat(pattern = "yyyy-MM-dd") val birthDate: LocalDate?,
  val type: PetType,
  val visits: Set<Visit> = hashSetOf(),
) {
  override fun toString(): String {
    return name ?: ""
  }
}

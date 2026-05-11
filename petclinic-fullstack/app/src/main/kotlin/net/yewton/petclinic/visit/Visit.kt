package net.yewton.petclinic.visit

import jakarta.validation.constraints.NotBlank
import net.yewton.petclinic.model.Persistable
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class Visit(
  override val id: Int? = null,
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate?,
  @field:NotBlank
  val description: String?,
) : Persistable<Int>

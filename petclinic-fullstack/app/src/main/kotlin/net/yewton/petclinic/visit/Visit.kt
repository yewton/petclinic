package net.yewton.petclinic.visit

import jakarta.validation.constraints.NotBlank
import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate

data class Visit(
  @field:DateTimeFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate?,
  @field:NotBlank
  val description: String?,
)

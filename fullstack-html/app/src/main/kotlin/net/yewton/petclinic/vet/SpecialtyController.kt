package net.yewton.petclinic.vet

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("/specialties")
class SpecialtyController(
  private val specialties: SpecialtyRepository,
) {
  @GetMapping
  suspend fun list(model: Model): String {
    model.addAttribute("specialties", specialties.findAll())
    return "specialties/specialtyList"
  }

  @GetMapping("/new")
  fun initCreationForm(model: Model): String {
    model.addAttribute("specialty", Specialty())
    return "specialties/createOrUpdateSpecialtyForm"
  }

  @PostMapping("/new")
  suspend fun processCreationForm(
    @Valid @ModelAttribute specialty: Specialty,
    result: BindingResult,
  ): String {
    if (result.hasErrors()) {
      return "specialties/createOrUpdateSpecialtyForm"
    }
    specialties.save(specialty)
    return "redirect:/specialties"
  }

  @GetMapping("/{specialtyId}/edit")
  suspend fun initUpdateForm(
    @PathVariable specialtyId: Int,
    model: Model,
  ): String {
    val specialty = specialties.findById(specialtyId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Specialty not found")
    model.addAttribute("specialty", specialty)
    return "specialties/createOrUpdateSpecialtyForm"
  }

  @PostMapping("/{specialtyId}/edit")
  suspend fun processUpdateForm(
    @PathVariable specialtyId: Int,
    @Valid @ModelAttribute specialty: Specialty,
    result: BindingResult,
  ): String {
    if (result.hasErrors()) {
      return "specialties/createOrUpdateSpecialtyForm"
    }
    specialties.save(specialty.copy(id = specialtyId))
    return "redirect:/specialties"
  }

  @PostMapping("/{specialtyId}/delete")
  suspend fun delete(
    @PathVariable specialtyId: Int,
  ): String {
    specialties.findById(specialtyId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Specialty not found")
    if (specialties.isInUse(specialtyId)) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "Specialty is in use and cannot be deleted")
    }
    specialties.delete(specialtyId)
    return "redirect:/specialties"
  }
}

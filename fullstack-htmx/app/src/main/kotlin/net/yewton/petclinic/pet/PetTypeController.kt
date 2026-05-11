package net.yewton.petclinic.pet

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
@RequestMapping("/pettypes")
class PetTypeController(
  private val petTypes: PetTypeRepository,
) {
  @GetMapping
  suspend fun list(model: Model): String {
    model.addAttribute("petTypes", petTypes.findAll())
    return "pettypes/petTypeList"
  }

  @GetMapping("/new")
  fun initCreationForm(model: Model): String {
    model.addAttribute("petType", PetType())
    return "pettypes/createOrUpdatePetTypeForm"
  }

  @PostMapping("/new")
  suspend fun processCreationForm(
    @Valid @ModelAttribute petType: PetType,
    result: BindingResult,
  ): String {
    if (result.hasErrors()) {
      return "pettypes/createOrUpdatePetTypeForm"
    }
    petTypes.save(petType)
    return "redirect:/pettypes"
  }

  @GetMapping("/{petTypeId}/edit")
  suspend fun initUpdateForm(
    @PathVariable petTypeId: Int,
    model: Model,
  ): String {
    val petType = petTypes.findById(petTypeId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pet type not found")
    model.addAttribute("petType", petType)
    return "pettypes/createOrUpdatePetTypeForm"
  }

  @PostMapping("/{petTypeId}/edit")
  suspend fun processUpdateForm(
    @PathVariable petTypeId: Int,
    @Valid @ModelAttribute petType: PetType,
    result: BindingResult,
  ): String {
    if (result.hasErrors()) {
      return "pettypes/createOrUpdatePetTypeForm"
    }
    petTypes.save(petType.copy(id = petTypeId))
    return "redirect:/pettypes"
  }

  @PostMapping("/{petTypeId}/delete")
  suspend fun delete(
    @PathVariable petTypeId: Int,
  ): String {
    petTypes.findById(petTypeId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pet type not found")
    if (petTypes.isInUse(petTypeId)) {
      throw ResponseStatusException(HttpStatus.CONFLICT, "Pet type is in use and cannot be deleted")
    }
    petTypes.delete(petTypeId)
    return "redirect:/pettypes"
  }
}

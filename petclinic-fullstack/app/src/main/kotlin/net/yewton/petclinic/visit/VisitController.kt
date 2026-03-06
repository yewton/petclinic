package net.yewton.petclinic.visit

import jakarta.validation.Valid
import net.yewton.petclinic.owner.OwnerRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/owners/{ownerId}")
class VisitController(
  private val visits: VisitRepository,
  private val owners: OwnerRepository,
) {
  @GetMapping("/pets/{petId}/visits/new")
  suspend fun initNewVisitForm(
    @PathVariable ownerId: Int,
    @PathVariable petId: Int,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)
    val pet = owner?.pets?.find { it.id == petId } ?: throw IllegalArgumentException("Pet not found")
    model.addAttribute("owner", owner)
    model.addAttribute("pet", pet)
    model.addAttribute("visit", Visit(null, null, null))
    return "pets/createOrUpdateVisitForm"
  }

  @PostMapping("/pets/{petId}/visits/new")
  suspend fun processNewVisitForm(
    @PathVariable ownerId: Int,
    @PathVariable petId: Int,
    @Valid @ModelAttribute visit: Visit,
    result: BindingResult,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)
    val pet = owner?.pets?.find { it.id == petId } ?: throw IllegalArgumentException("Pet not found")

    if (result.hasErrors()) {
      model.addAttribute("owner", owner)
      model.addAttribute("pet", pet)
      return "pets/createOrUpdateVisitForm"
    }

    visits.save(visit, petId)
    return "redirect:/owners/$ownerId"
  }
}

package net.yewton.petclinic.pet

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
import java.time.LocalDate

@Controller
@RequestMapping("/owners/{ownerId}")
class PetController(
  private val owners: OwnerRepository,
  private val pets: PetRepository,
  private val petTypes: PetTypeRepository,
) {
  @GetMapping("/pets/new")
  suspend fun initCreationForm(
    @PathVariable ownerId: Int,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)
    val types = petTypes.findAll()
    model.addAttribute("owner", owner)
    model.addAttribute("pet", Pet(null, "", null, PetType("cat"), hashSetOf()))
    model.addAttribute("types", types)
    return "pets/createOrUpdatePetForm"
  }

  @PostMapping("/pets/new")
  suspend fun processCreationForm(
    @PathVariable ownerId: Int,
    @ModelAttribute pet: Pet,
    result: BindingResult,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)

    if (!pet.name.isNullOrBlank() && pet.isNew() && owner?.pets?.any { it.name == pet.name } == true) {
      result.rejectValue("name", "duplicate", "already exists")
    }

    val currentDate = LocalDate.now()
    if (pet.birthDate != null && pet.birthDate!!.isAfter(currentDate)) {
      result.rejectValue("birthDate", "typeMismatch.birthDate")
    }

    if (result.hasErrors()) {
      val types = petTypes.findAll()
      model.addAttribute("owner", owner)
      model.addAttribute("types", types)
      return "pets/createOrUpdatePetForm"
    }
    pets.save(pet, ownerId)
    return "redirect:/owners/$ownerId"
  }
}

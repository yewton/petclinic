package net.yewton.petclinic.pet

import net.yewton.petclinic.owner.OwnerRepository
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
    model.addAttribute("pet", Pet(null, "", null, PetType(name = "cat"), hashSetOf()))
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

  @GetMapping("/pets/{petId}/edit")
  suspend fun initUpdateForm(
    @PathVariable ownerId: Int,
    @PathVariable petId: Int,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)
    val pet = owner?.pets?.find { it.id == petId } ?: throw IllegalArgumentException("Pet not found")
    val types = petTypes.findAll()
    model.addAttribute("owner", owner)
    model.addAttribute("pet", pet)
    model.addAttribute("types", types)
    return "pets/createOrUpdatePetForm"
  }

  @PostMapping("/pets/{petId}/edit")
  suspend fun processUpdateForm(
    @PathVariable ownerId: Int,
    @PathVariable petId: Int,
    @ModelAttribute pet: Pet,
    result: BindingResult,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)
    val petName = pet.name

    if (!petName.isNullOrBlank()) {
      val existingPet = owner?.pets?.find { it.name == petName }
      if (existingPet != null && existingPet.id != petId) {
        result.rejectValue("name", "duplicate", "already exists")
      }
    }

    val currentDate = LocalDate.now()
    if (pet.birthDate != null && pet.birthDate.isAfter(currentDate)) {
      result.rejectValue("birthDate", "typeMismatch.birthDate")
    }

    if (result.hasErrors()) {
      val types = petTypes.findAll()
      model.addAttribute("owner", owner)
      model.addAttribute("types", types)
      return "pets/createOrUpdatePetForm"
    }

    pets.save(pet.copy(id = petId), ownerId)
    return "redirect:/owners/$ownerId"
  }

  @PostMapping("/pets/{petId}/delete")
  suspend fun deletePet(
    @PathVariable ownerId: Int,
    @PathVariable petId: Int,
  ): String {
    val owner = owners.findById(ownerId) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found")
    owner.pets.find { it.id == petId } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Pet not found")
    pets.delete(petId)
    return "redirect:/owners/$ownerId"
  }
}

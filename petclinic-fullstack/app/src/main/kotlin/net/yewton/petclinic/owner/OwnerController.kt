package net.yewton.petclinic.owner

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import jakarta.validation.Valid
import net.yewton.petclinic.pet.Pet // Added for emptyList<Pet>()
// kotlinx.coroutines.reactor.awaitSingle will be removed as it's no longer needed

@Controller
@RequestMapping("/owners")
class OwnerController(val owners: OwnerRepository) {
  @GetMapping("find")
  fun initFindForm(
    model: Model,
    @RequestHeader("HX-Request", required = false, defaultValue = "false") isHxRequest: Boolean,
  ): String {
    model.addAttribute("owner", OwnerCriteria())
    if (isHxRequest) {
      return "owners/findOwners :: #search-owner-form"
    }
    return "owners/findOwners"
  }

  @PostMapping(headers = ["HX-Request"])
  suspend fun processFindFormHtmx(
    @ModelAttribute("owner") owner: OwnerCriteria,
    result: BindingResult,
    model: Model,
    response: ServerHttpResponse,
  ): String {
    model.addAttribute("owner", owner)
    val ownersResults = findPaginatedForOwnersLastName(1, owner.lastName)
    if (ownersResults.isEmpty) {
      result.rejectValue("lastName", "notFound", "not found")
      response.statusCode = HttpStatus.UNPROCESSABLE_ENTITY
      return "owners/findOwners :: #search-owner-form"
    }
    return addPaginationModel(1, model, ownersResults, true)
  }

  @GetMapping
  suspend fun processFindForm(
    @RequestParam(defaultValue = "1") page: Int,
    @ModelAttribute("owner") owner: OwnerCriteria,
    result: BindingResult,
    model: Model,
  ): String {
    model.addAttribute("owner", owner)
    val ownersResults = findPaginatedForOwnersLastName(page, owner.lastName)
    if (ownersResults.isEmpty) {
      result.rejectValue("lastName", "notFound", "not found")
      return "owners/findOwners"
    }
    if (ownersResults.totalElements == 1L) {
      return "redirect:/owners/${ownersResults.first().id}"
    }
    return addPaginationModel(page, model, ownersResults, false)
  }

  private fun addPaginationModel(
    page: Int,
    model: Model,
    paginated: Page<Owner>,
    isHxRequest: Boolean,
  ): String {
    val listOwners = paginated.content
    model.addAttribute("currentPage", page)
    model.addAttribute("totalPages", paginated.totalPages)
    model.addAttribute("totalItems", paginated.totalElements)
    model.addAttribute("listOwners", listOwners)
    if (isHxRequest) {
      return "owners/ownersList :: #owners"
    }
    return "owners/ownersList"
  }

  @GetMapping("/new")
  fun initCreationForm(model: Model): String {
    val owner = Owner(null, "", "", "", "", "", emptyList<Pet>()) // Changed HashSet to emptyList<Pet>
    model.addAttribute("owner", owner)
    return "owners/createOrUpdateOwnerForm"
  }

  @PostMapping("/new")
  suspend fun processCreationForm(
    @Valid @ModelAttribute("owner") owner: Owner,
    result: BindingResult
  ): String {
    if (result.hasErrors()) {
      return "owners/createOrUpdateOwnerForm"
    }
    // Assume owners.save(owner) saves the owner and returns the saved instance with an ID.
    // Also assume Owner has a non-nullable id after being saved.
    // If Owner.id is nullable, then savedOwner.id might need a null check or ?.
    val savedOwner = owners.save(owner) // Removed .awaitSingle() as OwnerRepository.save is now a suspend fun returning Owner
    return "redirect:/owners/${savedOwner.id!!}" // Using !! as id should be non-null after save
  }

  private suspend fun findPaginatedForOwnersLastName(
    page: Int,
    lastName: String,
  ): Page<Owner> {
    val pageSize = 5
    val pageable = PageRequest.of(page - 1, pageSize)
    return owners.findByLastName(lastName, pageable)
  }

  @GetMapping("/{ownerId}")
  suspend fun showOwner(
    @PathVariable("ownerId") ownerId: Long,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId.toInt()) // Changed ownerId to ownerId.toInt()
    // TODO 見つからない場合の処理 (参考実装にもないけど)
    model.addAttribute("owner", owner) // Changed owner.orElse(null) to owner, assuming findById returns Owner?
    return "owners/ownerDetails"
  }
}

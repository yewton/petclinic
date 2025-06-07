package net.yewton.petclinic.owner

import jakarta.validation.Valid
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
    @PathVariable("ownerId") ownerId: Int,
    model: Model,
  ): String {
    val owner = owners.findById(ownerId)
    // TODO 見つからない場合の処理 (参考実装にもないけど)
    model.addAttribute("owner", owner)
    return "owners/ownerDetails"
  }

  @GetMapping("/new")
  fun initCreationForm(model: Model): String {
    model.addAttribute("owner", Owner(null, "", "", "", "", "", emptySet()))
    return "owners/createOrUpdateOwnerForm"
  }

  @PostMapping
  suspend fun processCreationForm(
    @Valid owner: Owner,
    result: BindingResult,
  ): String {
    if (result.hasErrors()) {
      return "owners/createOrUpdateOwnerForm"
    }
    val savedOwner = this.owners.save(owner)
    return "redirect:/owners/${savedOwner.id}"
  }

  @GetMapping("/{ownerId}/edit")
  suspend fun initUpdateOwnerForm(
    @PathVariable ownerId: Int,
    model: Model,
  ): String {
    val owner = this.owners.findById(ownerId)
    model.addAttribute("owner", owner)
    return "owners/createOrUpdateOwnerForm"
  }

  @PostMapping("/{ownerId}/edit")
  suspend fun processUpdateForm(
    @Valid owner: Owner,
    result: BindingResult,
    @PathVariable ownerId: Int,
  ): String {
    if (result.hasErrors()) {
      return "owners/createOrUpdateOwnerForm"
    }
    val savedOwner = this.owners.save(owner.copy(id = ownerId))
    return "redirect:/owners/${savedOwner.id}"
  }
}

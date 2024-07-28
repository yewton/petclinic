package net.yewton.petclinic.owner

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/owners")
class OwnerController(val owners: OwnerRepository) {
  @GetMapping("find")
  fun initFindForm(model: Model): String {
    model.addAttribute("owner", OwnerCriteria())
    return "owners/findOwners"
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
    // TODO 結果が一件だけの場合の処理
    return addPaginationModel(page, model, ownersResults)
  }

  private fun addPaginationModel(
    page: Int,
    model: Model,
    paginated: Page<Owner>,
  ): String {
    val listOwners = paginated.content
    model.addAttribute("currentPage", page)
    model.addAttribute("totalPages", paginated.totalPages)
    model.addAttribute("totalItems", paginated.totalElements)
    model.addAttribute("listOwners", listOwners)
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
}

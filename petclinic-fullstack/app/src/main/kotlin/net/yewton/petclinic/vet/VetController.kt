package net.yewton.petclinic.vet

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Controller
class VetController(private val vetRepository: VetRepository) {
  @GetMapping("/vets.html")
  suspend fun showVetList(
    @RequestParam(defaultValue = "1") page: Int,
    model: Model,
  ): String {
    val paginated = findPaginated(page)
    return addPaginationModel(page, paginated, model)
  }

  @GetMapping("/vetsR.html")
  fun showVetListR(
    @RequestParam(defaultValue = "1") page: Int,
    model: Model,
  ): Mono<String> =
    findPaginatedR(page).map {
      addPaginationModel(page, it, model)
    }

  private fun addPaginationModel(
    page: Int,
    paginated: Page<Vet>,
    model: Model,
  ): String {
    val listVets = paginated.content
    model.addAttribute("currentPage", page)
    model.addAttribute("totalPages", paginated.totalPages)
    model.addAttribute("totalItems", paginated.totalElements)
    model.addAttribute("listVets", listVets)
    return "vets/vetList"
  }

  private suspend fun findPaginated(page: Int): Page<Vet> {
    val pageSize = 5
    val pageable: Pageable = PageRequest.of(page - 1, pageSize)
    return vetRepository.findAll(pageable)
  }

  private fun findPaginatedR(page: Int): Mono<Page<Vet>> {
    val pageSize = 5
    val pageable: Pageable = PageRequest.of(page - 1, pageSize)
    return vetRepository.findAllR(pageable)
  }
}

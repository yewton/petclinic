package net.yewton.petclinic.vet

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestParam
import reactor.core.publisher.Mono

@Controller
class VetController(
  private val vetRepository: VetRepository,
) {
  enum class DBClient {
    JOOQ,
    SPRING,
  }

  data class VetParam(
    val page: Int,
    val dbClient: DBClient,
    val reactor: Boolean,
  )

  @ModelAttribute
  fun param(
    @RequestParam(defaultValue = "1") page: Int,
    @RequestParam(defaultValue = "JOOQ") dbClient: DBClient,
    @RequestParam(defaultValue = "false") reactor: Boolean,
  ) = VetParam(page, dbClient, reactor)

  @GetMapping("/vets.html", params = ["!reactor"])
  suspend fun showVetListCoroutine(
    @ModelAttribute param: VetParam,
    model: Model,
  ): String {
    val paginated =
      when (param.dbClient) {
        DBClient.JOOQ -> vetRepository.findAllCoroutineJooq(pageable(param.page))
        DBClient.SPRING -> vetRepository.findAllCoroutineSpring(pageable(param.page))
      }
    return addPaginationModel(param.page, paginated, model)
  }

  @GetMapping("/vets.html", params = ["reactor"])
  fun showVetListReactor(
    @ModelAttribute param: VetParam,
    model: Model,
  ): Mono<String> =
    when (param.dbClient) {
      DBClient.JOOQ -> vetRepository.findAllReactorJooq(pageable(param.page))
      DBClient.SPRING -> vetRepository.findAllReactorSpring(pageable(param.page))
    }.map {
      addPaginationModel(param.page, it, model)
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

  private fun pageable(page: Int) = PageRequest.of(page - 1, 5)
}

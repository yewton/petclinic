package net.yewton.petclinic.owner

import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/owners")
class OwnerController {
  @GetMapping("find")
  fun initFindForm(model: Model): String {
    model.addAttribute("owner", OwnerCriteria())
    return "owners/findOwners"
  }

  @GetMapping
  fun processFindForm(
    @RequestParam(defaultValue = "1") page: Int,
    @ModelAttribute("owner") owner: OwnerCriteria,
    result: BindingResult,
    model: Model,
  ): String {
    model.addAttribute("owner", owner)
    // 一旦常に失敗を返しておく
    result.rejectValue("lastName", "notFound", "not found")
    return "owners/findOwners"
  }
}

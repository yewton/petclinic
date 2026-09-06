package net.yewton.petclinic.about

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class AboutController {
  @GetMapping("/about")
  fun about() = "about"
}

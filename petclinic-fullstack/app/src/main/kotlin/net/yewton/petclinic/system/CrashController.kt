package net.yewton.petclinic.system

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class CrashController {
  @GetMapping("/oups")
  fun triggerException(): String {
    throw RuntimeException(
      "Expected: 例外発生時の挙動確認用コントローラー",
    )
  }
}

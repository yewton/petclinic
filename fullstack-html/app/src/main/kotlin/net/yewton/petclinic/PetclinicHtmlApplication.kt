package net.yewton.petclinic

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["net.yewton.petclinic"])
class PetclinicHtmlApplication

fun main(args: Array<String>) {
  runApplication<PetclinicHtmlApplication>(*args)
}

apply(from = file("../gradle/repositories.settings.gradle.kts"))

pluginManagement {
  includeBuild("../lint-logic")
  includeBuild("../build-logic")
  includeBuild("../build-logic-settings")
}

plugins {
  id("net.yewton.petclinic.foojay-resolver")
}

includeBuild("../platforms")

rootProject.name = "core"
include("lib")

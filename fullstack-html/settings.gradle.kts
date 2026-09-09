pluginManagement {
  includeBuild("../lint-logic")
  includeBuild("../build-logic")
  includeBuild("../build-logic-settings")
}

plugins {
  id("net.yewton.petclinic.foojay-resolver")
}

apply(from = file("../gradle/repositories.settings.gradle.kts"))

includeBuild("../platforms")
includeBuild("../core")

rootProject.name = "fullstack-html"
include("app")

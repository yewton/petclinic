pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("lint-logic")
  includeBuild("build-logic")
}

plugins {
  id("net.yewton.petclinic.foojay-resolver")
}

includeBuild("lint-logic")
includeBuild("platforms")
includeBuild("build-logic")

includeBuild("core")
includeBuild("fullstack-html")
includeBuild("fullstack-htmx")

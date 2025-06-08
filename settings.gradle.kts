pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("lint-logic")
  includeBuild("build-logic")
}

includeBuild("lint-logic")
includeBuild("platforms")
includeBuild("build-logic")

includeBuild("petclinic-fullstack")

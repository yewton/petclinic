dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
  }
}
includeBuild("../platforms")

rootProject.name = "lint-logic"
include("spotless")

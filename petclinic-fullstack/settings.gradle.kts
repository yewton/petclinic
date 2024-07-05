pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("../lint-logic")
  includeBuild("../build-logic")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}
includeBuild("../platforms")

rootProject.name = "petclinic-fullstack"
include("app")

pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("../lint-logic")
  includeBuild("../build-logic")
}

plugins {
  id("net.yewton.petclinic.foojay-resolver")
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}
includeBuild("../platforms")
includeBuild("../core")

rootProject.name = "fullstack-htmx"
include("app")

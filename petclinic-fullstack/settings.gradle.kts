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
includeBuild("../libs/r2dbc-postgresql")

rootProject.name = "petclinic-fullstack"
include("app")

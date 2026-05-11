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
includeBuild("../core")

rootProject.name = "fullstack-html"
include("app")

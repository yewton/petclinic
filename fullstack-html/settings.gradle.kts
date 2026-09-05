pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("../lint-logic")
  includeBuild("../build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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

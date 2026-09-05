pluginManagement {
  repositories {
    gradlePluginPortal()
  }
  includeBuild("lint-logic")
  includeBuild("build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("lint-logic")
includeBuild("platforms")
includeBuild("build-logic")

includeBuild("core")
includeBuild("fullstack-html")
includeBuild("fullstack-htmx")

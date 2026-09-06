pluginManagement {
  includeBuild("../lint-logic")
}

dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
  }
}

includeBuild("../platforms")

rootProject.name = "build-logic-settings"

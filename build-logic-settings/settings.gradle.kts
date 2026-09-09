pluginManagement {
  includeBuild("../lint-logic")
}

apply(from = file("../gradle/repositories.settings.gradle.kts"))

includeBuild("../platforms")

rootProject.name = "build-logic-settings"

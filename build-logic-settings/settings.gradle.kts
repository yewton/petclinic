apply(from = file("../gradle/repositories.settings.gradle.kts"))

pluginManagement {
  includeBuild("../lint-logic")
}

includeBuild("../platforms")

rootProject.name = "build-logic-settings"

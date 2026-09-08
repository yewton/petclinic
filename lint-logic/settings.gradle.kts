apply(from = file("../gradle/repositories.settings.gradle.kts"))

includeBuild("../platforms")

rootProject.name = "lint-logic"
include("spotless")

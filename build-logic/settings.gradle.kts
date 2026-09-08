pluginManagement {
  includeBuild("../lint-logic")
}

apply(from = file("../gradle/repositories.settings.gradle.kts"))

includeBuild("../platforms")

rootProject.name = "build-logic"
include("commons")
include("dependency-management")
include("spring-boot")
include("jooq-codegen")

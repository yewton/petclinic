dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
  }
  versionCatalogs {
    create("libs") {
      from(files("../platforms/gradle/libs.versions.toml"))
    }
  }
}
includeBuild("../platforms")

rootProject.name = "lint-logic"
include("spotless")

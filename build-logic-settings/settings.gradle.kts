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

rootProject.name = "build-logic-settings"

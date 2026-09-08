pluginManagement {
  includeBuild("../lint-logic")
}

dependencyResolutionManagement {
  repositories {
    // Maven Central mirror first — see root settings.gradle.kts.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    gradlePluginPortal()
  }
}

includeBuild("../platforms")

rootProject.name = "build-logic-settings"

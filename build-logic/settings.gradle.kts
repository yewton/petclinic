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

pluginManagement {
  repositories {
    // Maven Central mirror first — see root settings.gradle.kts.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    gradlePluginPortal()
  }
  includeBuild("../lint-logic")
}

includeBuild("../platforms")

rootProject.name = "build-logic"
include("commons")
include("dependency-management")
include("spring-boot")
include("jooq-codegen")

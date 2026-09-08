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
  includeBuild("../build-logic")
  includeBuild("../build-logic-settings")
}

plugins {
  id("net.yewton.petclinic.foojay-resolver")
}

dependencyResolutionManagement {
  repositories {
    // Maven Central mirror first — see root settings.gradle.kts.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    mavenCentral()
  }
}
includeBuild("../platforms")
includeBuild("../core")

rootProject.name = "fullstack-htmx"
include("app")

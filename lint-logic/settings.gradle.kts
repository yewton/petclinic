pluginManagement {
  repositories {
    // The `kotlin-dsl` plugin pulls kotlin-gradle-plugin-api and kotlin-stdlib onto the
    // plugin classpath; without the mirror here those resolve through gradlePluginPortal()
    // to repo.maven.apache.org and hit shared-IP HTTP 429s (see root settings.gradle.kts).
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    gradlePluginPortal()
  }
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

rootProject.name = "lint-logic"
include("spotless")

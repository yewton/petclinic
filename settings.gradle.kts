pluginManagement {
  repositories {
    // Google-hosted Maven Central mirror, consulted first to avoid shared-IP HTTP 429s
    // from repo.maven.apache.org during CI and Renovate metadata regeneration.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    gradlePluginPortal()
  }
  includeBuild("lint-logic")
  includeBuild("build-logic")
  includeBuild("build-logic-settings")
}

plugins {
  id("net.yewton.petclinic.foojay-resolver")
}

includeBuild("lint-logic")
includeBuild("platforms")
includeBuild("build-logic")
includeBuild("build-logic-settings")

includeBuild("core")
includeBuild("fullstack-html")
includeBuild("fullstack-htmx")

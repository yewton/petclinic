pluginManagement {
  repositories {
    // Google-hosted Maven Central mirror, consulted first: it avoids the shared-IP HTTP 429s
    // that CI and Renovate hit on repo.maven.apache.org, and it pins plugin marker POMs to the
    // Maven Central copy whose checksum is recorded in gradle/verification-metadata.xml (the
    // Gradle Plugin Portal serves a different-byte copy of generated marker POMs).
    // This release-only mirror lets future SNAPSHOT dependencies bypass it for Maven Central or the Plugin Portal.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

// Fail loudly if anything (an inline repository, or an external settings plugin resolved before
// this script runs) put a repository ahead of the mirror: plugin marker POMs from the Gradle
// Plugin Portal have a different checksum than the Maven Central copy recorded in
// gradle/verification-metadata.xml, and that failure is otherwise only visible on CI.
gradle.settingsEvaluated {
  val firstPluginRepo = settings.pluginManagement.repositories.firstOrNull()
  check(
    firstPluginRepo is org.gradle.api.artifacts.repositories.MavenArtifactRepository &&
      firstPluginRepo.url.toString().startsWith("https://maven-central.storage-download.googleapis.com/"),
  ) {
    "pluginManagement.repositories must start with the Maven Central mirror, but starts with " +
      "'${firstPluginRepo?.name ?: "no repository"}'. Something declared a repository before " +
      "gradle/repositories.settings.gradle.kts ran."
  }
}

dependencyResolutionManagement {
  repositories {
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

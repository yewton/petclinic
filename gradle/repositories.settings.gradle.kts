import org.gradle.api.artifacts.repositories.MavenArtifactRepository

// Google-hosted Maven Central endpoint shared by repository declarations and the ordering guard.
val mavenCentralMirrorUrl = "https://maven-central.storage-download.googleapis.com/maven2/"

pluginManagement {
  repositories {
    // Google-hosted Maven Central mirror, consulted first: it avoids the shared-IP HTTP 429s
    // that CI and Renovate hit on repo.maven.apache.org, and it pins plugin marker POMs to the
    // Maven Central copy whose checksum is recorded in gradle/verification-metadata.xml (the
    // Gradle Plugin Portal serves a different-byte copy of generated marker POMs).
    // Keep this aligned with mavenCentralMirrorUrl: pluginManagement is extracted before the script body.
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
  val firstPluginRepoUrl = (firstPluginRepo as? MavenArtifactRepository)?.url
  check(
    firstPluginRepo is MavenArtifactRepository &&
      firstPluginRepo.url.toString().trimEnd('/') == mavenCentralMirrorUrl.trimEnd('/'),
  ) {
    "pluginManagement.repositories must start with the Maven Central mirror, but starts with " +
      "'${firstPluginRepo?.name ?: "no repository"}' at '${firstPluginRepoUrl ?: "no URL"}'; expected " +
      "'$mavenCentralMirrorUrl'. Something declared a repository before gradle/repositories.settings.gradle.kts ran."
  }
}

dependencyResolutionManagement {
  repositories {
    // Keep this aligned with mavenCentralMirrorUrl: pluginManagement is extracted before the script body.
    // This release-only mirror lets future SNAPSHOT dependencies bypass it for Maven Central or the Plugin Portal.
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

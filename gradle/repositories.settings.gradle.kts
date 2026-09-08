pluginManagement {
  repositories {
    // Google-hosted Maven Central mirror, consulted first: it avoids the shared-IP HTTP 429s
    // that CI and Renovate hit on repo.maven.apache.org, and it pins plugin marker POMs to the
    // Maven Central copy whose checksum is recorded in gradle/verification-metadata.xml (the
    // Gradle Plugin Portal serves a different-byte copy of generated marker POMs).
    maven("https://maven-central.storage-download.googleapis.com/maven2/") {
      name = "Maven Central Mirror"
      mavenContent { releasesOnly() }
    }
    mavenCentral()
    gradlePluginPortal()
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

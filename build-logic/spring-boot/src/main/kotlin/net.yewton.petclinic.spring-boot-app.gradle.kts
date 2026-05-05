import org.apache.tools.ant.filters.ReplaceTokens

plugins {
  java
  id("org.springframework.boot")
  id("org.jetbrains.kotlin.plugin.spring")
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun>().configureEach {
  val envFile = rootProject.file(".env")
  if (envFile.exists()) {
    envFile
      .readLines()
      .filter { it.isNotBlank() && !it.startsWith("#") && "=" in it }
      .forEach { line ->
        val idx = line.indexOf('=')
        environment(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
      }
  }
}

// https://www.baeldung.com/spring-boot-auto-property-expansion
tasks.processResources {
  val rootDirPath = rootDir.path
  filesMatching("**/application*.yml") {
    filter<ReplaceTokens>(
      "tokens" to
        mapOf("rootDir" to rootDirPath),
    )
  }
}

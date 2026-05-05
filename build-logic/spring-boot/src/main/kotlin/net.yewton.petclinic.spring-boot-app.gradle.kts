import org.apache.tools.ant.filters.ReplaceTokens

plugins {
  java
  id("org.springframework.boot")
  id("org.jetbrains.kotlin.plugin.spring")
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

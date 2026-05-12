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

// https://docs.spring.io/spring-boot/reference/packaging/container-images/efficient-images.html
tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootBuildImage>().configureEach {
  imageName = "net.yewton.petclinic/${project.parent?.name ?: project.name}"
  environment.put("BP_JVM_VERSION", "21")
  environment.put("BP_JVM_CDS_ENABLED", "true")
}

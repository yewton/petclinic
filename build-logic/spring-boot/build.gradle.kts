plugins {
  `kotlin-dsl`
  id("net.yewton.petclinic.spotless")
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  implementation(project(":commons"))
  implementation("org.springframework.boot:spring-boot-gradle-plugin")
  implementation("io.spring.gradle:dependency-management-plugin")
  implementation("org.jetbrains.kotlin:kotlin-allopen")
}

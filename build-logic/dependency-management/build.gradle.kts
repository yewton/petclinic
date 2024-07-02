plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  implementation(project(":commons"))
  implementation("org.springframework.boot:spring-boot-gradle-plugin")
  implementation("io.spring.gradle:dependency-management-plugin")
}

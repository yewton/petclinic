plugins {
  id("net.yewton.petclinic.commons")
  id("net.yewton.petclinic.spotless")
  id("net.yewton.petclinic.dependency-management")
  id("net.yewton.petclinic.spring-boot-app")
}

group = "$group.apps"

dependencies {
  implementation("net.yewton.petclinic:lib")

  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.springframework.boot:spring-boot-starter-webflux")

  runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
  runtimeOnly("org.webjars.npm:bootstrap")
  runtimeOnly("org.webjars.npm:font-awesome")

  developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
  testImplementation("org.springframework.boot:spring-boot-micrometer-metrics-test")
  testImplementation("org.springframework.boot:spring-boot-micrometer-tracing-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.testcontainers:testcontainers-junit-jupiter")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.testcontainers:testcontainers-r2dbc")
  testImplementation("org.testcontainers:testcontainers-postgresql")
}

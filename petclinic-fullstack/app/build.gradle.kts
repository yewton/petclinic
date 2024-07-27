plugins {
  id("net.yewton.petclinic.commons")
  id("net.yewton.petclinic.spotless")
  id("net.yewton.petclinic.dependency-management")
  id("net.yewton.petclinic.spring-boot-app")
  id("net.yewton.petclinic.jooq-codegen")
}

group = "$group.apps"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-jooq")
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  // https://docs.spring.io/spring-data/relational/reference/kotlin/coroutines.html#kotlin.coroutines.dependencies
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jooq:jooq")
  implementation("org.jooq:jooq-kotlin")
  implementation("org.jooq:jooq-kotlin-coroutines")
  runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.postgresql:r2dbc-postgresql")
  runtimeOnly("org.webjars.npm:bootstrap")
  runtimeOnly("org.webjars.npm:font-awesome")
  developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.testcontainers:junit-jupiter")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testImplementation("org.testcontainers:r2dbc")
  testImplementation("org.testcontainers:postgresql")
}

jooq {
  configuration {
    generator {
      name = "org.jooq.codegen.KotlinGenerator"

      generate {
        isKotlinNotNullPojoAttributes = true
        isKotlinNotNullRecordAttributes = true
        isKotlinNotNullInterfaceAttributes = true
      }

      database {
        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
        properties {
          property {
            key = "dialect"
            value = "POSTGRES"
          }
          property {
            key = "scripts"
            value = "src/main/resources/db/schema.sql"
          }
          property {
            key = "unqualifiedSchema"
            value = "petclinic"
          }
        }
      }
      target {
        packageName = "net.yewton.petclinic.jooq"
        directory = "src/main/jooq"
      }
    }
  }
}

sourceSets {
  main {
    kotlin {
      srcDir("src/main/jooq")
    }
  }
}

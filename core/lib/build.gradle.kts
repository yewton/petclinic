plugins {
  id("net.yewton.petclinic.commons")
  id("net.yewton.petclinic.spotless")
  id("net.yewton.petclinic.dependency-management")
  id("net.yewton.petclinic.spring-boot-library")
  id("net.yewton.petclinic.jooq-codegen")
}

dependencies {
  api("org.springframework.boot:spring-boot-starter-data-r2dbc")
  api("org.springframework.boot:spring-boot-starter-validation")
  api("org.springframework.boot:spring-boot-starter-aspectj") {
    because("@NewSpan などを使えるようにするため")
  }

  api("com.fasterxml.jackson.module:jackson-module-kotlin")
  api("io.projectreactor.kotlin:reactor-kotlin-extensions")
  api("io.projectreactor:reactor-core-micrometer")
  api("org.jetbrains.kotlin:kotlin-reflect")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
  api("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  api("org.jooq:jooq")
  api("org.jooq:jooq-kotlin")
  api("org.jooq:jooq-kotlin-coroutines")

  api("org.springframework.boot:spring-boot-starter-opentelemetry")
  api("io.micrometer:micrometer-core")
  api("io.r2dbc:r2dbc-proxy")
  api("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")

  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.postgresql:r2dbc-postgresql")

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

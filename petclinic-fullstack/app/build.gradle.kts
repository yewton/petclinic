plugins {
  id("net.yewton.petclinic.commons")
  id("net.yewton.petclinic.spotless")
  id("net.yewton.petclinic.dependency-management")
  id("net.yewton.petclinic.spring-boot-app")
  id("net.yewton.petclinic.jooq-codegen")
}

group = "$group.apps"

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
  implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-aspectj") {
    because("@NewSpan などを使えるようにするため")
  }

  implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("io.projectreactor:reactor-core-micrometer")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  // https://docs.spring.io/spring-data/relational/reference/kotlin/coroutines.html#kotlin.coroutines.dependencies
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("org.jooq:jooq")
  implementation("org.jooq:jooq-kotlin")
  implementation("org.jooq:jooq-kotlin-coroutines")

  // OpenTelemetry (metrics / traces / logs export, OTel SDK auto-configuration)
  // Spring Boot 4.0 でアクチュエータ用の自動設定が `spring-boot-opentelemetry` 等の
  // 専用モジュールへ分離されたため、まとめて取り込む `spring-boot-starter-opentelemetry`
  // を利用する。これにより以下が同梱される:
  //   - spring-boot-opentelemetry (OpenTelemetry SDK の auto-config)
  //   - spring-boot-micrometer-tracing-opentelemetry (Micrometer ↔ OTel トレース)
  //   - spring-boot-starter-micrometer-metrics (メトリクス auto-config)
  //   - micrometer-registry-otlp / micrometer-tracing-bridge-otel / opentelemetry-exporter-otlp
  implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
  implementation("io.micrometer:micrometer-core")
  implementation("io.r2dbc:r2dbc-proxy")

  // Logs
  implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0")

  runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
  runtimeOnly("org.postgresql:postgresql")
  runtimeOnly("org.postgresql:r2dbc-postgresql")
  runtimeOnly("org.webjars.npm:bootstrap")
  runtimeOnly("org.webjars.npm:font-awesome")
  runtimeOnly("org.webjars.npm:htmx.org")

  developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.boot:spring-boot-testcontainers")
  // Spring Boot 4.0 では `@SpringBootTest` だけでは `WebTestClient` が auto-configure されなくなり、
  // 専用の `@AutoConfigureWebTestClient` (新パッケージ) と auto-config モジュールの追加が必要になった。
  testImplementation("org.springframework.boot:spring-boot-webtestclient")
  // Spring Boot 4.0 で `@AutoConfigureObservability` は metrics/tracing 個別に分割されたため
  // 観測可能性 E2E テスト用に専用テストモジュールを追加する。
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

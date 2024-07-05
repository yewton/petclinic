plugins {
  id("java-platform")
}

group = "net.yewton.petclinic.platform"

dependencies {
  constraints {
    api(libs.spring.boot.plugin)
    api(libs.dependency.management.plugin)
    api(libs.kotlin.gradle.plugin)
    api(libs.kotlin.allopen)
    api(libs.jooq.codegen.core)
    api(libs.jooq.meta.core)
    api(libs.jooq.meta.extensions)
    api(libs.jooq.codegen.gradle)
    api(libs.jooq.postgres.extensions)
    api(libs.spotless.plugin.gradle)
  }
}

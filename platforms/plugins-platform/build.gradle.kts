plugins {
  id("java-platform")
}

group = "net.yewton.petclinic.platform"

dependencies {
  constraints {
    api(libs.spring.boot.plugin)
    api(libs.dependency.management.plugin)
    api(libs.kotlin.jvm.plugin)
    api(libs.kotlin.spring.plugin)
    api(libs.jooq.codegen.core)
    api(libs.jooq.meta.core)
    api(libs.jooq.meta.extensions)
    api(libs.jooq.codegen.gradle)
    api(libs.jooq.postgres.extensions)
    api(libs.spotless.plugin.gradle)
    api(libs.foojay.resolver.convention.plugin)
  }
}

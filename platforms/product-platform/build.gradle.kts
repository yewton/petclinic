plugins {
  id("java-platform")
}

group = "net.yewton.petclinic.platform"

dependencies {
  constraints {
    api(libs.webjars.bootstrap)
    api(libs.webjars.font.awesome)
    api(libs.webjars.htmx)
    api(libs.jooq.kotlin.coroutines)
    api(libs.opentelemetry.logback.appender)
    api(libs.opentelemetry.logback.mdc)
  }
}

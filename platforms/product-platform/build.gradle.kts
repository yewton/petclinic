plugins {
  id("java-platform")
}

group = "net.yewton.petclinic.platform"

javaPlatform.allowDependencies()

dependencies {
  api(platform(libs.spring.modulith.bom))
  constraints {
    api(libs.webjars.bootstrap)
    api(libs.webjars.font.awesome)
    api(libs.webjars.htmx)
    api(libs.jooq.kotlin.coroutines)
    api(libs.opentelemetry.logback.appender)
    api(libs.opentelemetry.logback.mdc)
  }
}

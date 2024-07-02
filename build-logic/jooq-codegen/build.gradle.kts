plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  implementation("org.jooq:jooq-codegen")
  implementation("org.jooq:jooq-meta")
  implementation("org.jooq:jooq-meta-extensions")
  implementation("org.jooq:jooq-codegen-gradle")
  implementation("org.jooq:jooq-postgres-extensions")
}

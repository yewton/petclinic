plugins {
  `kotlin-dsl`
  id("net.yewton.petclinic.spotless")
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
  implementation(
    "org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin",
  )
}

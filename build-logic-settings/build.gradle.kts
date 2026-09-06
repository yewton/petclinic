plugins {
  `kotlin-dsl`
  id("net.yewton.petclinic.spotless")
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  // The marker maps the plugin ID to its implementation on this precompiled settings script's classpath.
  implementation("org.gradle.toolchains.foojay-resolver-convention:org.gradle.toolchains.foojay-resolver-convention.gradle.plugin")
}

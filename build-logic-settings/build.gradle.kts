plugins {
  `kotlin-dsl`
  id("com.diffplug.spotless") version "8.4.0"
}

dependencies {
  // The marker maps the plugin ID to its implementation on this precompiled settings script's classpath.
  implementation(libs.foojay.resolver.convention.plugin)
}

plugins {
  `kotlin-dsl`
}

dependencies {
  // The marker maps the plugin ID to its implementation on this precompiled settings script's classpath.
  implementation(libs.foojay.resolver.convention.plugin)
}

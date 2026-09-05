plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
}

spotless {
  kotlinGradle {
    target(
      "build.gradle.kts",
      "settings.gradle.kts",
      "src/main/kotlin/**/*.gradle.kts",
    )
    ktlint().setEditorConfigPath(file("../.editorconfig"))
  }
}

dependencies {
  // The marker maps the plugin ID to its implementation on this precompiled settings script's classpath.
  implementation(libs.foojay.resolver.convention.plugin)
}

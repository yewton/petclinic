import com.diffplug.gradle.spotless.SpotlessTask

plugins {
  id("com.diffplug.spotless")
}

val editorConfigPath = listOf(
  rootDir.parentFile,
  rootDir,
  projectDir
).map { it.toPath().resolve(".editorconfig") }
  .firstOrNull { it.toFile().exists() }

spotless {
  pluginManager.withPlugin("kotlin") {
    kotlin {
      targetExclude("**/build/**/*.kt", "**/jooq/**/*.kt")
      ktlint().setEditorConfigPath(editorConfigPath)
    }
  }
  kotlinGradle {
    targetExclude("**/build/**/*.kts")
    ktlint().setEditorConfigPath(editorConfigPath)
  }
}

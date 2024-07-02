plugins {
  id("com.diffplug.spotless") version "6.25.0"
}

// IDE でソースダウンロードする用
repositories {
  mavenCentral()
}

spotless {
  kotlin {
    target("**/*.kt")
    targetExclude("**/build/**/*.kt", "**/jooq/**/*.kt")
    ktlint().setEditorConfigPath("$rootDir/.editorconfig")
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    ktlint()
  }
}

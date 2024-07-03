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
    targetExclude("**/build/**/*.kts")
    ktlint()
  }
}

listOf(
  LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
  LifecycleBasePlugin.BUILD_TASK_NAME,
  LifecycleBasePlugin.CHECK_TASK_NAME,
  LifecycleBasePlugin.CLEAN_TASK_NAME,
).forEach { taskName ->
  tasks.named(taskName) {
    dependsOn(gradle.includedBuild("petclinic-fullstack").task(":$taskName"))
  }
}

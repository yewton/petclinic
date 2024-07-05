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

tasks.withType<SpotlessTask> {
  doFirst {
    logger.warn("editorconfig = ${editorConfigPath?.toFile()?.absolutePath}")
  }
}

// net.yewton.petclinic.root-project.gradle.kts と同様に、
// ルートからサブプロジェクトの spotlessApply を呼び出せるようにする。
tasks.named("spotlessApply") {
  dependsOn(subprojects.flatMap { it.tasks.named { taskName -> taskName == "spotlessApply" } })
}

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

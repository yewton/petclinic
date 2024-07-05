plugins {
  base
  id("net.yewton.petclinic.spotless")
}

// IDE でソースダウンロードする用
repositories {
  mavenCentral()
}

listOf(
  LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
  LifecycleBasePlugin.BUILD_TASK_NAME,
  LifecycleBasePlugin.CHECK_TASK_NAME,
  LifecycleBasePlugin.CLEAN_TASK_NAME,
).forEach { taskName ->
  tasks.named(taskName) {
    dependsOn(
      gradle.includedBuilds.map {
        it.task(":$taskName")
      },
    )
  }
}

tasks.named("spotlessApply") {
  dependsOn(
    listOf("build-logic", "petclinic-fullstack")
      .map { gradle.includedBuild(it) }
      .map { it.task(":$name") },
  )
}

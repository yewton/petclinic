plugins {
  base
}

listOf(
  LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
  LifecycleBasePlugin.BUILD_TASK_NAME,
  LifecycleBasePlugin.CHECK_TASK_NAME,
  LifecycleBasePlugin.CLEAN_TASK_NAME,
).forEach { taskName ->
  tasks.named(taskName) {
    dependsOn(subprojects.map { it.tasks.named(taskName) })
  }
}

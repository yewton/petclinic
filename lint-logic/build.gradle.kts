plugins {
  base
}

listOf(
  LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
  LifecycleBasePlugin.BUILD_TASK_NAME,
  LifecycleBasePlugin.CHECK_TASK_NAME,
  LifecycleBasePlugin.CLEAN_TASK_NAME,
  // Renovate invokes this non-lifecycle task to regenerate dependency verification metadata.
  "dependencies",
).forEach { taskName ->
  tasks.named(taskName) {
    dependsOn(subprojects.map { it.tasks.named(taskName) })
  }
}

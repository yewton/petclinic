/*
 * Composite Build だとサブプロジェクトのタスクを
 * ルートから暗黙的に実行することが出来ないので明示的に依存関係を設定する。
 * https://github.com/gradle/gradle/issues/20863
 */
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

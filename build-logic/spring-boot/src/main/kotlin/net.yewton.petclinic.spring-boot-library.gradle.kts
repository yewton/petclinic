/*
 * Spring Boot アプリにはしない（bootJar/bootRun を持たない）が、
 * Spring の Bean (@Component, @Configuration 等) を含むライブラリ用の
 * convention plugin。
 *
 * Spring Boot Gradle plugin は Kotlin 互換性を含む各種設定を行うため、
 * `org.springframework.boot` を適用したうえで bootJar / bootRun を無効化し、
 * 通常の jar を成果物とする。
 * 同時に Spring AOP プロキシ生成 (@Transactional, @NewSpan 等) のために
 * Kotlin Spring プラグイン (allopen) も適用する。
 */
plugins {
  java
  id("org.springframework.boot")
  id("org.jetbrains.kotlin.plugin.spring")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  enabled = false
}

tasks.named("jar") {
  enabled = true
}

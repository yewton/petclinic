package net.yewton.petclinic

import org.springframework.boot.SpringApplication
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource
import java.io.File
import java.util.Properties

class DotEnvEnvironmentPostProcessor : EnvironmentPostProcessor {
  override fun postProcessEnvironment(
    environment: ConfigurableEnvironment,
    application: SpringApplication,
  ) {
    val envFile = findEnvFile() ?: return
    val properties = Properties()
    envFile
      .readLines()
      .filter { it.isNotBlank() && !it.startsWith("#") && "=" in it }
      .forEach { line ->
        val idx = line.indexOf('=')
        properties[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
      }
    // addLast = lowest priority; existing env vars and Spring properties take precedence
    environment.propertySources.addLast(PropertiesPropertySource(".env", properties))
  }

  private fun findEnvFile(): File? {
    for (dir in generateSequence(File("").absoluteFile) { it.parentFile }) {
      val candidate = File(dir, ".env")
      if (candidate.isFile) return candidate
      if (File(dir, ".git").exists()) return null
    }
    return null
  }
}

plugins {
  id("java")
  kotlin("jvm")
}

group = "net.yewton.petclinic"

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjsr305=strict")
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:product-platform"))

  testImplementation(platform("net.yewton.petclinic.platform:test-platform"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("-Xshare:off", "-Duser.language=ja", "-Duser.country=JP")
}

tasks.withType<JavaCompile>().configureEach {
  with(options) {
    isFork = true
    isIncremental = true
    compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked", "-Xlint:varargs"))
  }
}

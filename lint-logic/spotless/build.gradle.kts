plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
}

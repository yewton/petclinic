plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(platform("net.yewton.petclinic.platform:plugins-platform"))
  implementation("com.diffplug.spotless:spotless-plugin-gradle:7.2.1")
}

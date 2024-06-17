plugins {
    id("java")
 }

group = "net.yewton.petclinic"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform("net.yewton.petclinic.platform:product-platform"))

    testImplementation(platform("net.yewton.petclinic.platform:test-platform"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

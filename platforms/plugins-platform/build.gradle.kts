plugins {
    id("java-platform")
}

group = "net.yewton.petclinic.platform"

dependencies {
    constraints {
        api(libs.spring.boot.plugin)
        api(libs.dependency.management.plugin)
        api(libs.kotlin.gradle.plugin)
        api(libs.kotlin.allopen)
    }
}

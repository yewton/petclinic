plugins {
    id("java-platform")
}

group = "net.yewton.petclinic.platform"

dependencies {
    constraints {
        api(libs.webjars.bootstrap)
        api(libs.webjars.font.awesome)
    }
}

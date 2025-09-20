package net.yewton.petclinic

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.test.ApplicationModuleTest

@ApplicationModuleTest(allowedDependencies = ["jooq"])
class ModulithIntegrationTest {
  @Test
  fun verifyModules() {
    ApplicationModules
      .of(PetclinicApplication::class.java)
      .withAllowedDependencies { source, target ->
        if (source.basePackage == "owner" && target.basePackage == "pet") {
          return@withAllowedDependencies true
        }
        false
      }.verify()
  }
}

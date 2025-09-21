package net.yewton.petclinic

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.test.ApplicationModuleTest

@ApplicationModuleTest
class ModulithIntegrationTest {
  @Test
  fun verifyModules() {
    ApplicationModules
      .of(
        PetclinicApplication::class.java,
      ).verify()
  }
}

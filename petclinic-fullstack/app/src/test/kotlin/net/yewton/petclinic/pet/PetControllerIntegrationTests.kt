package net.yewton.petclinic.pet

import kotlinx.coroutines.test.runTest
import net.yewton.petclinic.owner.OwnerRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.util.LinkedMultiValueMap

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PetControllerIntegrationTests(
  @param:Autowired private val webTestClient: WebTestClient,
  @param:Autowired private val ownerRepository: OwnerRepository,
) {
  @Test
  fun `should show update pet form`() {
    webTestClient
      .get()
      .uri("/owners/1/pets/1/edit")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<String>()
      .value { body ->
        assertThat(body).contains("Update Pet")
      }
  }

  @Test
  fun `should process update pet form`() =
    runTest {
      val petData = LinkedMultiValueMap<String, String>()
      petData.add("name", "Leo-Updated")
      petData.add("birthDate", "2000-09-07")
      petData.add("type", "cat")

      webTestClient
        .post()
        .uri("/owners/1/pets/1/edit")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(petData)
        .exchange()
        .expectStatus()
        .is3xxRedirection
        .expectHeader()
        .valueEquals("Location", "/owners/1")

      val owner = ownerRepository.findById(1)
      val updatedPet = owner?.pets?.find { it.id == 1 }

      assertThat(updatedPet).isNotNull
      assertThat(updatedPet!!.name).isEqualTo("Leo-Updated")
    }
}

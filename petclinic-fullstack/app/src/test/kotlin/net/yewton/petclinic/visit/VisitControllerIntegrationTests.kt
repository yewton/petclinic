package net.yewton.petclinic.visit

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
class VisitControllerIntegrationTests(
  @param:Autowired private val webTestClient: WebTestClient,
  @param:Autowired private val ownerRepository: OwnerRepository,
) {
  @Test
  fun `should show new visit form`() {
    webTestClient
      .get()
      .uri("/owners/1/pets/1/visits/new")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<String>()
      .value { body ->
        assertThat(body).contains("Visit")
        assertThat(body).contains("Add Visit")
      }
  }

  @Test
  fun `should process new visit form`() =
    runTest {
      val visitData = LinkedMultiValueMap<String, String>()
      visitData.add("date", "2024-01-01")
      visitData.add("description", "Rabies shot")

      webTestClient
        .post()
        .uri("/owners/1/pets/1/visits/new")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(visitData)
        .exchange()
        .expectStatus()
        .is3xxRedirection
        .expectHeader()
        .valueEquals("Location", "/owners/1")

      val owner = ownerRepository.findById(1)
      val pet = owner?.pets?.find { it.id == 1 }
      val visit = pet?.visits?.find { it.description == "Rabies shot" }

      assertThat(visit).isNotNull
      assertThat(visit!!.date.toString()).isEqualTo("2024-01-01")
    }

  @Test
  fun `should show edit visit form`() {
    // visit id=1 belongs to pet id=7, owner id=6
    webTestClient
      .get()
      .uri("/owners/6/pets/7/visits/1/edit")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<String>()
      .value { body ->
        assertThat(body).contains("Visit")
        assertThat(body).contains("rabies shot")
      }
  }

  @Test
  fun `should process edit visit form`() =
    runTest {
      val visitData = LinkedMultiValueMap<String, String>()
      visitData.add("date", "2010-03-04")
      visitData.add("description", "Updated rabies shot")

      webTestClient
        .post()
        .uri("/owners/6/pets/7/visits/1/edit")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(visitData)
        .exchange()
        .expectStatus()
        .is3xxRedirection
        .expectHeader()
        .valueEquals("Location", "/owners/6")

      val owner = ownerRepository.findById(6)
      val pet = owner?.pets?.find { it.id == 7 }
      val visit = pet?.visits?.find { it.id == 1 }

      assertThat(visit).isNotNull
      assertThat(visit!!.description).isEqualTo("Updated rabies shot")
    }
}

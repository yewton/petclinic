package net.yewton.petclinic.owner

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.returnResult
import org.springframework.util.LinkedMultiValueMap

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OwnerControllerIntegrationTests(
  @Autowired private val webTestClient: WebTestClient,
  @Autowired private val ownerRepository: OwnerRepository,
) {
  @Test
  fun `should show new owner form`() {
    webTestClient.get().uri("/owners/new")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus().isOk
      .expectBody<String>()
      .value { body ->
        assertThat(body).contains("<h2>New Owner</h2>")
      }
  }

  @Test
  fun `should process new owner form`() =
    runTest {
      val ownerData = LinkedMultiValueMap<String, String>()
      ownerData.add("firstName", "Joe")
      ownerData.add("lastName", "Bloggs")
      ownerData.add("address", "123 Caramel Street")
      ownerData.add("city", "London")
      ownerData.add("telephone", "0123456789")

      val result =
        webTestClient.post().uri("/owners")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .bodyValue(ownerData)
          .exchange()
          .expectStatus().is3xxRedirection
          .expectHeader().valueMatches("Location", "/owners/\\d+")
          .returnResult<String>()

      val location = result.responseHeaders.location!!
      val newOwnerId = location.path.substringAfterLast("/").toInt()

      val newOwner = ownerRepository.findById(newOwnerId)

      assertThat(newOwner).isNotNull
      assertThat(newOwner!!.firstName).isEqualTo("Joe")
      assertThat(newOwner.lastName).isEqualTo("Bloggs")
      assertThat(newOwner.address).isEqualTo("123 Caramel Street")
      assertThat(newOwner.city).isEqualTo("London")
      assertThat(newOwner.telephone).isEqualTo("0123456789")
    }

  @Test
  fun `should show update owner form`() {
    webTestClient.get().uri("/owners/1/edit")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus().isOk
      .expectBody<String>()
      .value { body ->
        assertThat(body).contains("<h2>Owner</h2>")
        assertThat(body).contains("""value="George"""")
        assertThat(body).contains("""value="Franklin"""")
      }
  }

  @Test
  fun `should process update owner form`() =
    runTest {
      val ownerData = LinkedMultiValueMap<String, String>()
      ownerData.add("firstName", "George")
      ownerData.add("lastName", "Franklin-Updated")
      ownerData.add("address", "110 W. Liberty St.-Updated")
      ownerData.add("city", "Madison-Updated")
      ownerData.add("telephone", "6085551023")

      webTestClient.post().uri("/owners/1/edit")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(ownerData)
        .exchange()
        .expectStatus().is3xxRedirection
        .expectHeader().valueEquals("Location", "/owners/1")

      val updatedOwner = ownerRepository.findById(1)
      assertThat(updatedOwner).isNotNull
      assertThat(updatedOwner!!.lastName).isEqualTo("Franklin-Updated")
      assertThat(updatedOwner.address).isEqualTo("110 W. Liberty St.-Updated")
      assertThat(updatedOwner.city).isEqualTo("Madison-Updated")
    }
}

package net.yewton.petclinic.owner

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
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
    suspend fun `should process new owner form`() {
        val ownerData = LinkedMultiValueMap<String, String>()
        ownerData.add("firstName", "Joe")
        ownerData.add("lastName", "Bloggs")
        ownerData.add("address", "123 Caramel Street")
        ownerData.add("city", "London")
        ownerData.add("telephone", "0123456789")

        val result = webTestClient.post().uri("/owners")
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
}

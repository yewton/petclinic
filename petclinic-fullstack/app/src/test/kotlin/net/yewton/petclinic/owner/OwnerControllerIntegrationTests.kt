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
class OwnerControllerIntegrationTests(@Autowired private val webTestClient: WebTestClient) {
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
    fun `should process new owner form`() {
        val owner = LinkedMultiValueMap<String, String>()
        owner.add("firstName", "Joe")
        owner.add("lastName", "Bloggs")
        owner.add("address", "123 Caramel Street")
        owner.add("city", "London")
        owner.add("telephone", "0123456789")

        webTestClient.post().uri("/owners")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(owner)
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader().valueMatches("Location", "/owners/\\d+")
    }
}

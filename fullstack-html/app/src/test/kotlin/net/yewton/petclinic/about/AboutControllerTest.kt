package net.yewton.petclinic.about

import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class AboutControllerTest(
  @param:Autowired private val webTestClient: WebTestClient,
) : WithAssertions {
  @Test
  fun `about page is rendered`() {
    webTestClient
      .get()
      .uri("/about")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<String>()
      .value { body ->
        assertThat(body)
          .containsSubsequence(
            "<h2>",
            "このプロジェクトについて",
            "</h2>",
          ).contains("Spring PetClinic サンプルを基にした Spring Boot デモアプリケーションです。")
      }
  }
}

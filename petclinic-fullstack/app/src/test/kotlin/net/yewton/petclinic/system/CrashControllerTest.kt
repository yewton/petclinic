package net.yewton.petclinic.system

import org.assertj.core.api.WithAssertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = ["server.error.include-message=ALWAYS", "management.endpoints.enabled-by-default=false"],
)
class CrashControllerTest : WithAssertions {
  @Test
  fun testTriggerExceptionHtml(
    @Autowired webClient: WebTestClient,
  ) {
    webClient
      .get()
      .uri("/oups")
      .accept(MediaType.TEXT_HTML)
      .exchange()
      .expectStatus()
      .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
      .expectBody(String::class.java)
      .consumeWith {
        val body = it.responseBody
        assertThat(body)
          .isNotNull()
          .containsSubsequence(
            "<body>",
            "<h2>",
            "問題が発生しました...",
            "</h2>",
            "<p>",
            "Expected:",
            "例外発生時の挙動確認用コントローラー",
            "</p>",
            "</body>",
          ).doesNotContain("Whitelabel Error Page", "This application has no explicit mapping for")
      }
  }
}

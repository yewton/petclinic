package net.yewton.petclinic.observability

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.WithAssertions
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.ComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

/**
 * docker-compose の可観測性スタック (alloy → mimir/loki/tempo) を Testcontainers で起動し、
 * PetClinic アプリへの実 HTTP リクエストの結果として trace / metric / log が
 * 各バックエンドに到達することを E2E で検証する。
 *
 * `docker-compose.yml` の image pin と各設定ファイル (config 配下) をそのまま利用するため、
 * 依存イメージや設定のバージョンアップ時にこのテストが失敗すれば早期に互換性破壊を検出できる。
 *
 * 他テストとシグナルが混ざらないよう、本テストでは `spring.application.name` を独自値に上書きし、
 * バックエンド側のクエリでもその値で絞り込む。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  properties = [
    "spring.application.name=" + PetclinicObservabilityEndToEndTests.SERVICE_NAME,
    "management.tracing.sampling.probability=1.0",
    "management.otlp.metrics.export.step=1s",
    // application-local.yml に合わせて gRPC で送る (Spring Boot のデフォルトは http)
    "management.otlp.tracing.transport=grpc",
    "management.otlp.logging.transport=grpc",
  ],
)
// @SpringBootTest は既定で `management.defaults.metrics.export.enabled=false` 相当を適用し、
// メトリクス/トレース/ログのエクスポーターを無効化する。本テストではこの抑止を解除して
// 実エクスポート経路を検証するため `@AutoConfigureObservability` を付与する。
@AutoConfigureObservability
class PetclinicObservabilityEndToEndTests(
  @Autowired private val webTestClient: WebTestClient,
) : WithAssertions {
  @Test
  fun `application requests produce traces metrics and logs in the observability stack`() {
    repeat(REQUESTS) {
      webTestClient
        .get()
        .uri("/owners?lastName=")
        .accept(MediaType.TEXT_HTML)
        .exchange()
        .expectStatus()
        .isOk
    }

    await()
      .atMost(POLL_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        // Tempo の TraceQL: resource.service.name で絞り込んでスパンを検索
        val q = URLEncoder.encode("""{resource.service.name="$SERVICE_NAME"}""", StandardCharsets.UTF_8)
        val response = httpGet("${tempoUrl()}/api/search?q=$q&limit=20")
        assertThat(response.statusCode()).isEqualTo(HTTP_OK)
        val traces = mapper.readTree(response.body()).path("traces")
        assertThat(traces.isArray).isTrue()
        assertThat(traces.size()).isGreaterThan(0)
      }

    await()
      .atMost(POLL_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        // Mimir: OTLP→Prometheus 変換で `service.name` リソース属性は `job` ラベルへマップされる
        val q = URLEncoder.encode("""target_info{job="$SERVICE_NAME"}""", StandardCharsets.UTF_8)
        val response = httpGet("${mimirUrl()}/prometheus/api/v1/query?query=$q")
        assertThat(response.statusCode()).isEqualTo(HTTP_OK)
        val resultArr = mapper.readTree(response.body()).path("data").path("result")
        assertThat(resultArr.isArray).isTrue()
        assertThat(resultArr.size()).isGreaterThan(0)
      }

    await()
      .atMost(POLL_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        val now = System.currentTimeMillis()
        val startNs = (now - Duration.ofMinutes(10).toMillis()) * NS_PER_MS
        val endNs = (now + Duration.ofMinutes(1).toMillis()) * NS_PER_MS
        val q = URLEncoder.encode("""{service_name="$SERVICE_NAME"}""", StandardCharsets.UTF_8)
        val response =
          httpGet(
            "${lokiUrl()}/loki/api/v1/query_range?query=$q&start=$startNs&end=$endNs&limit=1000",
          )
        assertThat(response.statusCode()).isEqualTo(HTTP_OK)
        val streams = mapper.readTree(response.body()).path("data").path("result")
        assertThat(streams.isArray).isTrue()
        assertThat(streams.size()).isGreaterThan(0)
      }
  }

  companion object {
    const val SERVICE_NAME = "petclinic-obs-e2e-test"

    private const val NS_PER_MS = 1_000_000L
    private const val HTTP_OK = 200
    private const val ALLOY_OTLP_GRPC = 4317
    private const val ALLOY_OTLP_HTTP = 4318
    private const val LOKI_HTTP = 3100
    private const val MIMIR_HTTP = 9009
    private const val TEMPO_HTTP = 3200
    private const val REQUESTS = 3

    private val POLL_TIMEOUT: Duration = Duration.ofSeconds(90)
    private val POLL_INTERVAL: Duration = Duration.ofSeconds(2)

    private val mapper = ObjectMapper()
    private val httpClient: HttpClient =
      HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    @JvmStatic
    @TempDir
    lateinit var tempDir: Path

    @JvmStatic
    private lateinit var compose: ComposeContainer

    @JvmStatic
    @BeforeAll
    fun startStack() {
      val readyWait =
        Wait
          .forHttp("/ready")
          .forStatusCodeMatching { it in 200..399 }
          .withStartupTimeout(Duration.ofMinutes(3))
      compose =
        ComposeContainer(prepareComposeFile(locateComposeFile(), tempDir))
          .withServices("alloy", "mimir", "loki", "tempo")
          .withExposedService(
            "alloy",
            ALLOY_OTLP_GRPC,
            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)),
          ).withExposedService(
            "alloy",
            ALLOY_OTLP_HTTP,
            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)),
          ).withExposedService("loki", LOKI_HTTP, readyWait)
          .withExposedService("mimir", MIMIR_HTTP, readyWait)
          .withExposedService("tempo", TEMPO_HTTP, readyWait)
      compose.start()
    }

    @JvmStatic
    @AfterAll
    fun stopStack() {
      if (::compose.isInitialized) {
        compose.stop()
      }
    }

    @JvmStatic
    @DynamicPropertySource
    fun overrideOtlpEndpoints(registry: DynamicPropertyRegistry) {
      registry.add("management.otlp.tracing.endpoint") {
        "http://${compose.getServiceHost("alloy", ALLOY_OTLP_GRPC)}:${compose.getServicePort("alloy", ALLOY_OTLP_GRPC)}"
      }
      registry.add("management.otlp.logging.endpoint") {
        "http://${compose.getServiceHost("alloy", ALLOY_OTLP_GRPC)}:${compose.getServicePort("alloy", ALLOY_OTLP_GRPC)}"
      }
      registry.add("management.otlp.metrics.export.url") {
        "http://${compose.getServiceHost("alloy", ALLOY_OTLP_HTTP)}:${compose.getServicePort("alloy", ALLOY_OTLP_HTTP)}/v1/metrics"
      }
    }

    private fun lokiUrl(): String = serviceUrl("loki", LOKI_HTTP)

    private fun mimirUrl(): String = serviceUrl("mimir", MIMIR_HTTP)

    private fun tempoUrl(): String = serviceUrl("tempo", TEMPO_HTTP)

    private fun serviceUrl(
      service: String,
      internalPort: Int,
    ): String = "http://${compose.getServiceHost(service, internalPort)}:${compose.getServicePort(service, internalPort)}"

    private fun httpGet(url: String): HttpResponse<String> =
      httpClient.send(
        HttpRequest
          .newBuilder(URI.create(url))
          .GET()
          .timeout(Duration.ofSeconds(10))
          .build(),
        HttpResponse.BodyHandlers.ofString(),
      )

    private fun locateComposeFile(): File {
      var dir: File? = File(System.getProperty("user.dir")).absoluteFile
      while (dir != null && !File(dir, "docker-compose.yml").exists()) {
        dir = dir.parentFile
      }
      requireNotNull(dir) {
        "docker-compose.yml not found from ${System.getProperty("user.dir")}"
      }
      return File(dir, "docker-compose.yml")
    }

    /**
     * 開発者がローカルで `bootRun` を実行して compose スタックを上げているケースとの
     * ホストポート衝突を避けるため、`ports:` を取り除いた compose ファイルを一時生成する。
     * Testcontainers の ambassador 経由でランダムに割り当てられる動的ポートでアクセスする。
     * volumes の相対パスは元ファイルからの解決ができないため絶対パスへ書き換える。
     */
    @Suppress("UNCHECKED_CAST")
    private fun prepareComposeFile(
      original: File,
      tempDir: Path,
    ): File {
      val rootDir = original.parentFile.absoluteFile
      val yaml = Yaml()
      val tree =
        original.inputStream().use { input ->
          (yaml.load(input) as Map<String, Any?>).toMutableMap()
        }
      val services = (tree["services"] as Map<String, Any?>).toMutableMap()
      services.replaceAll { _, svcAny ->
        val svc = (svcAny as Map<String, Any?>).toMutableMap()
        svc.remove("ports")
        val volumes = svc["volumes"] as? List<Any?>
        if (!volumes.isNullOrEmpty()) {
          svc["volumes"] = volumes.map { entry -> rewriteVolumeEntry(entry, rootDir) }
        }
        svc
      }
      tree["services"] = services
      val temp = Files.createTempFile(tempDir, "petclinic-observability-compose-", ".yml").toFile()
      temp.writeText(yaml.dump(tree))
      return temp
    }

    private fun rewriteVolumeEntry(
      entry: Any?,
      rootDir: File,
    ): Any? {
      if (entry !is String) return entry
      val sepIdx = entry.indexOf(':')
      if (sepIdx <= 0) return entry
      val source = entry.substring(0, sepIdx)
      val rest = entry.substring(sepIdx)
      return if (source.startsWith("./") || source.startsWith("../") || source == ".") {
        File(rootDir, source).canonicalPath + rest
      } else {
        entry
      }
    }
  }
}

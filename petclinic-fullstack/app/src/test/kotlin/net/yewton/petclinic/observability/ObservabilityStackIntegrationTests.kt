package net.yewton.petclinic.observability

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.logs.Severity
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import org.assertj.core.api.WithAssertions
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
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
import java.util.Base64
import java.util.HexFormat
import java.util.concurrent.TimeUnit

/**
 * docker-compose の可観測性スタック (alloy → mimir/loki/tempo) を Testcontainers で起動し、
 * OTLP で送信したシグナル (trace/metric/log) が各バックエンドまで到達することを E2E で検証する。
 *
 * `docker-compose.yml` の image pin と各設定ファイル (config 配下) をそのまま利用するため、
 * 依存イメージや設定のバージョンアップ時にこのテストが失敗すれば早期に互換性破壊を検出できる。
 */
class ObservabilityStackIntegrationTests : WithAssertions {
  @Test
  fun `OTLP traces are forwarded by Alloy and ingested by Tempo`() {
    val tracerProvider =
      SdkTracerProvider
        .builder()
        .setResource(testResource())
        .addSpanProcessor(
          SimpleSpanProcessor.create(
            OtlpHttpSpanExporter
              .builder()
              .setEndpoint("${alloyHttpUrl()}/v1/traces")
              .build(),
          ),
        ).build()

    val traceId =
      try {
        val span = tracerProvider.get("obs-stack-test").spanBuilder(SPAN_NAME).startSpan()
        try {
          span.spanContext.traceId
        } finally {
          span.end()
        }
      } finally {
        tracerProvider.shutdown().join(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
      }

    val traceIdBase64 = Base64.getEncoder().encodeToString(HexFormat.of().parseHex(traceId))
    await()
      .atMost(POLL_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        val response = httpGet("${tempoUrl()}/api/traces/$traceId")
        assertThat(response.statusCode()).isEqualTo(HTTP_OK)
        // Tempo は OTLP/proto を JSON 化したレスポンスを返すため、traceId は base64 エンコードされて含まれる
        assertThat(response.body())
          .contains(traceIdBase64)
          .contains(SPAN_NAME)
      }
  }

  @Test
  fun `OTLP metrics are forwarded by Alloy and ingested by Mimir`() {
    val meterProvider =
      SdkMeterProvider
        .builder()
        .setResource(testResource())
        .registerMetricReader(
          PeriodicMetricReader
            .builder(
              OtlpHttpMetricExporter
                .builder()
                .setEndpoint("${alloyHttpUrl()}/v1/metrics")
                .build(),
            ).setInterval(Duration.ofSeconds(1))
            .build(),
        ).build()

    try {
      meterProvider
        .get("obs-stack-test")
        .counterBuilder(METRIC_NAME)
        .build()
        .add(7L)
      meterProvider.forceFlush().join(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } finally {
      meterProvider.shutdown().join(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    await()
      .atMost(POLL_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        val query = URLEncoder.encode("${METRIC_NAME}_total", StandardCharsets.UTF_8)
        val response = httpGet("${mimirUrl()}/prometheus/api/v1/query?query=$query")
        assertThat(response.statusCode()).isEqualTo(HTTP_OK)
        val resultArr = mapper.readTree(response.body()).path("data").path("result")
        assertThat(resultArr.isArray).isTrue()
        assertThat(resultArr.size()).isGreaterThan(0)
      }
  }

  @Test
  fun `OTLP logs are forwarded by Alloy and ingested by Loki`() {
    val message = "$LOG_BODY_PREFIX ${System.nanoTime()}"
    val loggerProvider =
      SdkLoggerProvider
        .builder()
        .setResource(loggingResource())
        .addLogRecordProcessor(
          SimpleLogRecordProcessor.create(
            OtlpHttpLogRecordExporter
              .builder()
              .setEndpoint("${alloyHttpUrl()}/v1/logs")
              .build(),
          ),
        ).build()

    try {
      loggerProvider
        .get("obs-stack-test")
        .logRecordBuilder()
        .setBody(message)
        .setSeverity(Severity.INFO)
        .setAllAttributes(
          Attributes.of(AttributeKey.stringKey("kind"), "test"),
        ).emit()
      loggerProvider.forceFlush().join(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    } finally {
      loggerProvider.shutdown().join(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    await()
      .atMost(POLL_TIMEOUT)
      .pollInterval(POLL_INTERVAL)
      .untilAsserted {
        val now = System.currentTimeMillis()
        val startNs = (now - Duration.ofMinutes(10).toMillis()) * NS_PER_MS
        val endNs = (now + Duration.ofMinutes(1).toMillis()) * NS_PER_MS
        val query = URLEncoder.encode("""{service_name="$SERVICE_NAME"}""", StandardCharsets.UTF_8)
        val response =
          httpGet(
            "${lokiUrl()}/loki/api/v1/query_range?query=$query&start=$startNs&end=$endNs&limit=1000",
          )
        assertThat(response.statusCode()).isEqualTo(HTTP_OK)
        assertThat(response.body()).contains(message)
      }
  }

  private fun testResource(): Resource =
    Resource
      .builder()
      .put(ServiceAttributes.SERVICE_NAME, SERVICE_NAME)
      .build()

  // Loki エクスポータにリソース属性 service.name を Loki ラベルへ昇格するよう指示するヒント。
  // trace/metric の resource にも混ざらないようログ用にだけ追加する。
  private fun loggingResource(): Resource =
    testResource()
      .merge(
        Resource
          .builder()
          .put(AttributeKey.stringKey("loki.resource.labels"), "service.name")
          .build(),
      )

  private fun httpGet(url: String): HttpResponse<String> =
    httpClient.send(
      HttpRequest
        .newBuilder(URI.create(url))
        .GET()
        .timeout(Duration.ofSeconds(10))
        .build(),
      HttpResponse.BodyHandlers.ofString(),
    )

  companion object {
    private const val SERVICE_NAME = "obs-stack-test"
    private const val SPAN_NAME = "petclinic-observability-pipeline-test"
    private const val METRIC_NAME = "obs_stack_test_counter"
    private const val LOG_BODY_PREFIX = "petclinic-observability-pipeline-test"
    private const val NS_PER_MS = 1_000_000L
    private const val SHUTDOWN_TIMEOUT_SECONDS = 10L
    private const val HTTP_OK = 200
    private const val ALLOY_OTLP_HTTP = 4318
    private const val LOKI_HTTP = 3100
    private const val MIMIR_HTTP = 9009
    private const val TEMPO_HTTP = 3200

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
      compose.stop()
    }

    private fun alloyHttpUrl(): String = serviceUrl("alloy", ALLOY_OTLP_HTTP)

    private fun lokiUrl(): String = serviceUrl("loki", LOKI_HTTP)

    private fun mimirUrl(): String = serviceUrl("mimir", MIMIR_HTTP)

    private fun tempoUrl(): String = serviceUrl("tempo", TEMPO_HTTP)

    private fun serviceUrl(
      service: String,
      internalPort: Int,
    ): String = "http://${compose.getServiceHost(service, internalPort)}:${compose.getServicePort(service, internalPort)}"

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

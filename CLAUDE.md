# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

- **Build & verify**: `./gradlew check` — must pass before committing
- **Format**: `./gradlew spotlessApply` — enforces ktlint via Spotless
- **JOOQ codegen**: `./gradlew :petclinic-fullstack:app:jooqCodegen` — regenerate after schema changes
- **Run app**: `./gradlew :petclinic-fullstack:app:bootRun`
- **Single test class**: `./gradlew :petclinic-fullstack:app:test --tests "net.yewton.petclinic.owner.OwnerControllerIntegrationTests"`

CI runs: `./gradlew check --parallel --warning-mode all --build-cache --configuration-cache`

## Workflow

1. Before implementing, check `references/spring-petclinic` for the reference implementation of any missing feature.
2. After changes, run `./gradlew spotlessApply` then `./gradlew check`.

## Pull Request の作成

PR を作成するときは `.github/PULL_REQUEST_TEMPLATE.md` の構造に従う。

### 必須要素

- **`closes #<issue番号>`** — 冒頭に記載する
- **変更ファイル一覧と読む順序** — テーブル形式で、レビュアーが読む順番を明示する
- **クラス図（Mermaid）** — 新規クラスや既存クラスの変更を伴う場合
- **シーケンス図（Mermaid）** — ユーザー操作ごとに1図。HTTPメソッド・パス・分岐を示す
- **レビュー観点と私の原案** — 下記ルールを守る

### レビュー観点の書き方ルール

「確認してください」で終わらせない。各観点には必ず次の2点を記載する：

1. **原案（結論）**: 「問題なし」「〇〇の方が適切」など、自分の判断を一言で示す
2. **根拠**: 調査結果・コード grep 結果・他実装との比較・トレードオフなど

例：
```
### isInUse() による削除ガード — 409 Conflict の妥当性

**原案：現状の設計で問題ないが、TOCTOU の懸念は将来課題として記録する。**

- 409 は RFC 9110 的に正しく、PetTypeController と一貫している。
- isInUse() と delete() が別トランザクションのため TOCTOU は存在するが、
  DB の FK 制約が最終ガードとなるため実害は限定的。
```

### 影響範囲の調査

コンストラクタや型シグネチャを変更した場合は、PR 説明に調査結果を記載する：

```bash
grep -rn "ClassName" src/main/kotlin/ --include="*.kt"
grep -rn "ClassName" src/test/ --include="*.kt"
```

## Architecture

This is a Spring Boot application using **Spring WebFlux + Kotlin Coroutines** (no blocking I/O) with **Thymeleaf** server-side rendering and **HTMX** for partial page updates.

### Stack
- **Database**: PostgreSQL via R2DBC (reactive); queries written with **jOOQ** DSL (not Spring Data repositories for most queries)
- **Async**: All controller methods and repository methods are `suspend fun`; Reactor `Mono`/`Flux` are used in some places and interop via `kotlinx-coroutines-reactive`
- **Observability**: OpenTelemetry traces/metrics/logs exported via OTLP; local stack (Grafana/Tempo/Loki/Mimir) configured in `docker-compose.yml`

### Module layout
```
petclinic-fullstack/app/   — main application
  src/main/kotlin/         — domain packages: owner/, pet/, visit/, vet/, system/, welcome/
  src/main/jooq/           — generated JOOQ classes (do not edit manually)
  src/main/resources/db/   — schema.sql (DDL source for JOOQ codegen) + data.sql (seed)
  src/test/                — integration tests using Testcontainers + WebTestClient
build-logic/               — custom Gradle convention plugins
lint-logic/                — Spotless/ktlint configuration
platforms/                 — version catalog (libs.versions.toml) and platform BOMs
references/                — git submodules; spring-petclinic is the reference implementation
```

### Domain packages
Each domain package (`owner`, `pet`, `visit`, `vet`) follows the same pattern:
- **Entity** — data class (e.g. `Owner.kt`)
- **Repository** — `@Component` using `DSLContext` directly; all methods are `suspend fun`
- **Controller** — `@Controller` with `suspend fun` handlers; returns Thymeleaf view names

### JOOQ usage
Generated classes live in `src/main/jooq/net/yewton/petclinic/jooq/`. Repositories use `multiset` + `intoList` for nested object mapping (e.g. fetching pets with their visits in a single query). Run `jooqCodegen` whenever `schema.sql` changes.

### HTMX partial rendering
Controllers detect the `HX-Request` header and return Thymeleaf fragment selectors (e.g. `"owners/findOwners :: #search-owner-form"`) instead of full pages. For validation errors on HTMX requests, set `response.statusCode = HttpStatus.UNPROCESSABLE_ENTITY`.

### Testing
Tests use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `WebTestClient` + Testcontainers (PostgreSQL 16.3 via R2DBC TC URL). The test profile (`application-test.yml`) initialises the schema and seed data on startup. Use `assertThat` from AssertJ (`WithAssertions`) for assertions.

**Kotlin annotation targets**: When injecting dependencies via primary constructor parameters that are also properties (`val`/`var`), always use explicit `@param:` target to avoid the annotation being applied to the backing field in future Kotlin versions ([KT-73255](https://youtrack.jetbrains.com/issue/KT-73255)):
```kotlin
// correct
class MyTest(@param:Autowired private val client: WebTestClient)

// wrong — generates compiler warning
class MyTest(@Autowired private val client: WebTestClient)
```

### Local development
The `local` Spring profile (default) uses `spring-boot-docker-compose` to start PostgreSQL automatically from `docker-compose.yml`. No manual Docker commands needed for `bootRun`.

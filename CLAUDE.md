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

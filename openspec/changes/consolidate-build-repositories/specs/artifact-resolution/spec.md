## ADDED Requirements

### Requirement: Cold resolution from a shared-IP environment

The build MUST resolve every dependency and plugin artifact it needs when run with an empty artifact cache from an environment whose outbound IP is shared with other consumers (CI runners, the Renovate cloud), without the build failing because a canonical public repository returned HTTP 429.

#### Scenario: CI runs the build with no cached artifacts

- **WHEN** `./gradlew check` runs on a fresh CI runner with no Gradle module cache
- **THEN** all dependency and plugin artifacts resolve
- **AND** the build does not fail with an HTTP 429 from any repository

#### Scenario: Renovate regenerates dependency verification metadata

- **WHEN** Renovate runs `./gradlew --dependency-verification lenient -q --write-verification-metadata sha256 dependencies` from the root build
- **THEN** the walk configures every included build, including the `kotlin-dsl` plugin classpath of the infrastructure plugin builds
- **AND** the artifacts resolve without an HTTP 429

### Requirement: Rate-limited repository is never consulted first

For any resolution context (a build's plugin classpath and its dependency configurations), a repository known to rate-limit shared IPs MUST NOT be the first repository in the list, because Gradle disables a repository on its first HTTP 429 for the rest of the build instead of falling through to the next one.

#### Scenario: A repository list that includes the canonical Maven Central

- **WHEN** a `repositories` block lists both the Google-hosted Maven Central mirror and `mavenCentral()` (or `gradlePluginPortal()`, which proxies non-plugin artifacts to Maven Central)
- **THEN** the mirror is listed first
- **AND** the canonical repository follows as a fallback for artifacts the mirror does not carry

### Requirement: One declaration site per group of builds

Repository configuration MUST NOT be hand-copied per build. The build tree has exactly two declaration sites: `gradle/repositories.settings.gradle.kts` for the infrastructure plugin builds (`lint-logic`, `build-logic`, `build-logic-settings`), applied to each via `apply(from = ...)`; and `net.yewton.petclinic.commons`' project-level `repositories {}` for the application projects. The application builds' `settings.gradle.kts` files MUST NOT declare `pluginManagement.repositories` or `dependencyResolutionManagement.repositories` (every plugin they use is an `includeBuild` substitution; every project dependency resolves through `commons`).

#### Scenario: Changing the Maven Central mirror endpoint

- **WHEN** the mirror endpoint must change
- **THEN** the edit is made in `gradle/repositories.settings.gradle.kts` and `commons.gradle.kts` — two files
- **AND** no `settings.gradle.kts` needs editing

#### Scenario: An application build's settings file is reviewed

- **WHEN** a contributor opens `settings.gradle.kts`, `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, or `fullstack-htmx/settings.gradle.kts`
- **THEN** it contains no `repositories { }` block in `pluginManagement` or `dependencyResolutionManagement`

### Requirement: The infrastructure repository config is shared by file, not by plugin

The infrastructure plugin builds resolve their plugin classpath from `pluginManagement.repositories`. A precompiled settings convention plugin cannot configure that block (it runs after the `plugins {}` block), and `lint-logic` / `build-logic-settings` could not apply such a plugin without a composite-build cycle. The shared configuration MUST therefore be a settings script applied with `apply(from = ...)`, matching the pattern used by `gradle/gradle` itself.

#### Scenario: An infrastructure build resolves its plugin classpath

- **WHEN** `lint-logic`, `build-logic`, or `build-logic-settings` is configured
- **THEN** its `pluginManagement.repositories` and `dependencyResolutionManagement.repositories` come from `gradle/repositories.settings.gradle.kts` via `apply(from = file("../gradle/repositories.settings.gradle.kts"))`
- **AND** its inline `pluginManagement {}` block contains only `includeBuild(...)` lines

#### Scenario: A contributor plans a repository change

- **WHEN** a contributor needs to change repository configuration
- **THEN** project documentation states which two files are load-bearing, which resolution context each serves, and that the application builds intentionally declare nothing

## ADDED Requirements

### Requirement: Cold resolution from a shared-IP environment

The build MUST resolve every dependency and plugin artifact it needs when run with an empty artifact cache from an environment whose outbound IP is shared with other consumers (CI runners, the Renovate cloud), without failing because a canonical public repository returned HTTP 429 and without failing dependency verification on a plugin marker POM.

#### Scenario: CI runs the full build with no cached artifacts

- **WHEN** `./gradlew check --parallel --warning-mode all --build-cache --configuration-cache` runs on a fresh runner with no Gradle module cache
- **THEN** all plugin classpaths, all project dependency configurations, and Spotless's ktlint detached configuration resolve
- **AND** the build does not fail with an HTTP 429 from any repository
- **AND** the build does not fail dependency verification for `org.jetbrains.kotlin.jvm.gradle.plugin` or any other plugin marker

#### Scenario: Renovate regenerates dependency verification metadata

- **WHEN** Renovate runs Gradle with `--write-verification-metadata` from the root build
- **THEN** the walk configures every included build, including the `kotlin-dsl` plugin classpath of the infrastructure plugin builds and the Kotlin plugin marker resolution triggered by `net.yewton.petclinic.commons`
- **AND** the artifacts resolve without an HTTP 429

### Requirement: The mirror is consulted first, and the build fails if it is not

In every resolution context — a build's plugin classpath (`pluginManagement.repositories`) and its dependency configurations (`dependencyResolutionManagement.repositories` and project-level `repositories`) — the Google-hosted Maven Central mirror MUST be the first repository, ahead of `mavenCentral()` and `gradlePluginPortal()`. If anything places another repository first, the build MUST fail immediately with a diagnostic, not silently fall through to a lower repository.

#### Scenario: Repository order in the shared settings script

- **WHEN** `gradle/repositories.settings.gradle.kts` declares the repository list
- **THEN** the mirror `maven("https://maven-central.storage-download.googleapis.com/maven2/")` is the first repository in both `pluginManagement.repositories` and `dependencyResolutionManagement.repositories`
- **AND** `mavenCentral()` and `gradlePluginPortal()` follow as fallbacks (`mavenCentral()` for artifacts the mirror lags; `gradlePluginPortal()` for portal-only artifacts — portal-generated plugin markers, `gradle-kotlin-dsl-plugins`, the foojay marker)

#### Scenario: An artifact whose POM checksum differs by repository is resolved

- **WHEN** an artifact whose `.pom` or `.module` bytes differ between the Gradle Plugin Portal and Maven Central (e.g. a portal-generated plugin marker like `org.jetbrains.kotlin.jvm.gradle.plugin`) is resolved under `verify-metadata=true`
- **THEN** it is fetched from the mirror (the Maven Central copy)
- **AND** its checksum matches the entry recorded in `gradle/verification-metadata.xml`

#### Scenario: The shared script is not applied, or a repository is declared before it

- **WHEN** a settings file omits the `apply(from = ...)` line, or a `plugins { id("some.settings.plugin") }` in a settings file causes a repository to be added to `pluginManagement.repositories` before the shared script runs
- **THEN** the build fails during settings evaluation with a message naming the first repository found
- **AND** it does NOT proceed to resolve plugins from the Gradle Plugin Portal default

### Requirement: One declaration site for the shared repository list

The shared repository list that the seven `settings.gradle.kts` files need MUST be declared once, in `gradle/repositories.settings.gradle.kts`, and pulled into each settings file with `apply(from = file("<relative>/gradle/repositories.settings.gradle.kts"))`. A `settings.gradle.kts` MUST NOT re-declare that shared list inline. A build MAY still add a build-specific repository (for example a milestone repository needed by a Spring Boot upgrade) — but only *after* the `apply(from = ...)` line, so the shared list, and the mirror at its head, come first.

#### Scenario: Changing the mirror endpoint

- **WHEN** the mirror endpoint must change
- **THEN** the edit is made in `gradle/repositories.settings.gradle.kts` and `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts` — two files
- **AND** no `settings.gradle.kts` needs editing

#### Scenario: A settings file is reviewed

- **WHEN** a contributor opens any of the seven `settings.gradle.kts` files
- **THEN** its `pluginManagement` block contains only `includeBuild(...)` lines
- **AND** its repository configuration comes from an `apply(from = ...)` line placed after the `pluginManagement {}` and `plugins {}` blocks

#### Scenario: An infrastructure build resolves its plugin classpath

- **WHEN** `lint-logic`, `build-logic`, or `build-logic-settings` is configured
- **THEN** its `pluginManagement.repositories` and `dependencyResolutionManagement.repositories` come from `gradle/repositories.settings.gradle.kts`

### Requirement: The load-bearing topology is documented

`CLAUDE.md` and this spec MUST record which repository declaration sites are load-bearing, which resolution context each serves, and why the mirror must be ordered first (the marker POM checksum under `verify-metadata=true`), so that a future change to the plugin dependency graph has a contract to check against.

#### Scenario: A contributor changes the plugin dependency graph

- **WHEN** a change alters which plugins a `net.yewton.petclinic.*` convention plugin declares, or the Kotlin/Spotless plugin coordinates
- **THEN** project documentation tells the contributor that consumer builds re-resolve leaked plugin markers through `pluginManagement.repositories` and that the mirror must stay first for verification to pass

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

### Requirement: Single declared source per resolution context

Each resolution context that needs repositories MUST declare them in exactly one place. Application project dependencies resolve from the settings `dependencyResolutionManagement` of their build; no application project declares its own `repositories` block.

#### Scenario: An application project declares its own repositories

- **WHEN** a project in the `core`, `fullstack-html`, or `fullstack-htmx` build adds a `repositories {}` block
- **THEN** the build fails at configuration time
- **AND** the failure names the offending project and points to the settings-level repository declaration

#### Scenario: Changing the mirror endpoint for application builds

- **WHEN** the Maven Central mirror endpoint used by the application builds must change
- **THEN** the change is made in one settings convention plugin
- **AND** no application build's `settings.gradle.kts` and no project convention plugin needs editing

### Requirement: Repository configuration that cannot be centralized is documented

The infrastructure plugin builds (`lint-logic`, `build-logic`, `build-logic-settings`) resolve their plugin classpath from `pluginManagement.repositories`, which a settings convention plugin cannot supply. Where a repository block is repeated because it cannot be centralized, each copy MUST carry a comment stating why, and the topology MUST be recorded in project documentation.

#### Scenario: A contributor reads an infrastructure build's settings file

- **WHEN** a contributor opens `lint-logic/settings.gradle.kts` or `build-logic-settings/settings.gradle.kts`
- **THEN** the `pluginManagement.repositories` block explains that it is an intentional inline copy and where the canonical rationale lives

#### Scenario: A contributor plans a repository change

- **WHEN** a contributor needs to change repository configuration
- **THEN** project documentation tells them which blocks are load-bearing, which resolution context each serves, and which cannot be centralized

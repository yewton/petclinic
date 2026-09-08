## Why

The HTTP 429 mitigation (#186, #189) hand-copied a Google Maven Central mirror `repositories` block into fourteen places — `pluginManagement.repositories` and `dependencyResolutionManagement.repositories` across seven `settings.gradle.kts` files, plus the project-level `repositories {}` in `net.yewton.petclinic.commons`. The copies have already drifted into two fallback variants (`gradlePluginPortal()` vs `mavenCentral()` as the second entry). Issue #183 asked for a settings convention plugin. An audit was supposed to find deletable ("vestigial") blocks first; it found none — every block is load-bearing — but the survey of how the wider Gradle ecosystem handles this pointed at a clean, low-risk consolidation.

## What Changes

- **No blocks are deleted.** Two spikes concluded the application-build blocks were vestigial; both were falsified (see `design.md` Decision 1):
  - `core` / `fullstack-*` `dependencyResolutionManagement.repositories` is used by Spotless's ktlint detached configuration on each build's root project (which applies `net.yewton.petclinic.spotless` but not `net.yewton.petclinic.commons`).
  - Every build's `pluginManagement.repositories` is used, after #195, to resolve the Kotlin plugin marker POM (`org.jetbrains.kotlin.jvm.gradle.plugin`) — and the mirror must be **first**, because `verify-metadata=true` records the Maven Central copy of that POM and the Gradle Plugin Portal serves a different one.
- **Centralize, don't delete.** Add `gradle/repositories.settings.gradle.kts` declaring both `pluginManagement.repositories` and `dependencyResolutionManagement.repositories` as the order-preserving union of the two drifted variants — mirror, then `mavenCentral()`, then `gradlePluginPortal()` — plus a `gradle.settingsEvaluated {}` guard that fails the build if `pluginManagement.repositories` does not start with the mirror. Replace the inline blocks in `settings.gradle.kts` (root), `core/`, `fullstack-html/`, `fullstack-htmx/`, `lint-logic/`, `build-logic/`, `build-logic-settings/` with `apply(from = file("<rel>/gradle/repositories.settings.gradle.kts"))`, placed after the `pluginManagement {}` / `plugins {}` blocks. This is the packaging `gradle/gradle` uses for its own `gradle/shared-with-buildSrc/mirrors.settings.gradle.kts`; the guard exists because PetClinic's script *declares* the repositories (load-bearing) rather than *rewriting* URLs (fail-safe).
- **`net.yewton.petclinic.commons` is unchanged.** Its project-level `repositories {}` is already a single declaration. The mirror endpoint then lives in exactly two files, down from fourteen.
- No settings convention plugin, no `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, no change to `verify-metadata`, no consolidation of the infra builds — see `design.md` Non-Goals.
- The fallback-list unification is proven safe by regenerating `gradle/verification-metadata.xml` (`--write-verification-metadata sha256`) and confirming an empty diff; `./gradlew check` does not write that file.
- Record the load-bearing topology in a new `artifact-resolution` capability spec and trim the `CLAUDE.md` note.

## Capabilities

### New Capabilities

- `artifact-resolution`: which repositories each resolution context (plugin classpath, project dependencies, Spotless's ktlint detached config) consults, in what order, from how many declaration sites, and the guarantee that cold resolution from a shared-IP environment (CI, Renovate) neither hits HTTP 429 nor fails dependency verification on a plugin marker POM.

### Modified Capabilities

<!-- none -->

## Impact

- `gradle/repositories.settings.gradle.kts`: new shared settings script (`pluginManagement.repositories` + `dependencyResolutionManagement.repositories`, mirror first then `gradlePluginPortal()`).
- `settings.gradle.kts` (root), `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`, `lint-logic/settings.gradle.kts`, `build-logic/settings.gradle.kts`, `build-logic-settings/settings.gradle.kts`: inline repository blocks replaced with `apply(from = file("<rel>/gradle/repositories.settings.gradle.kts"))`; `includeBuild(...)` lines kept.
- `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts`: unchanged.
- Spotless config (`lint-logic/spotless/...`): extend the `kotlinGradle` target to cover `gradle/*.settings.gradle.kts` if it does not already.
- No new convention plugin; `net.yewton.petclinic.foojay-resolver` unchanged.
- No dependency versions change; `gradle/verification-metadata.xml` is unaffected (verification is by artifact checksum, and the mirror-first order that produces the recorded checksums is preserved).
- Renovate's `renovate.json` `hostRules` throttling stays as-is (independent defense).
- `libs/**` is out of scope (not part of the composite build; Renovate-ignored).

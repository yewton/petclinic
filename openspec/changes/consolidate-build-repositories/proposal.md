## Why

Every `repositories` block across the eight composite builds now carries the same hand-copied Google Maven Central mirror snippet — roughly 13 occurrences after the HTTP 429 mitigation (PRs #186, #189). The copies already drift in intent (some sites were added defensively without knowing whether they are load-bearing), and the next repository change — a second mirror, credentials, a repository manager, or dropping the mirror — is a 13-file sweep where missing one silently reintroduces the 429 failures. Issue #183 asked for a settings convention plugin; the mitigation work showed the real problem is that the configuration was never audited down to what actually resolves artifacts.

## What Changes

- Audit each `repositories` block and classify it as load-bearing (an artifact resolves through it) or vestigial (shadowed by another block or never consulted). Spikes required:
  - Whether `pluginManagement.repositories` in the root / `core` / `fullstack-html` / `fullstack-htmx` settings is consulted at all, given those builds resolve every plugin through `includeBuild` substitution.
  - Whether `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx` settings is consulted, given all three application projects declare project-level repositories through the `net.yewton.petclinic.commons` convention plugin (`RepositoriesMode.PREFER_PROJECT` shadows settings-level repositories per project).
- Remove the seven blocks the spikes proved vestigial: `pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx`, and `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx`.
- Establish a single source of truth for the repositories that remain:
  - Infrastructure plugin builds (`lint-logic`, `build-logic`, `build-logic-settings`) — which compile `kotlin-dsl` and need both `pluginManagement.repositories` and `dependencyResolutionManagement.repositories`: add `gradle/repositories.settings.gradle.kts` declaring both blocks, and `apply(from = file("../gradle/repositories.settings.gradle.kts"))` from each of the three, replacing their inline blocks. This is the pattern `gradle/gradle` itself uses (`gradle/shared-with-buildSrc/mirrors.settings.gradle.kts`). A settings convention plugin is *not* used: it cannot configure `pluginManagement.repositories`, and `lint-logic` / `build-logic-settings` could not apply one without a composite-build cycle.
  - Application projects: unchanged — `net.yewton.petclinic.commons` already declares their `repositories {}` in one place.
- The Maven Central mirror endpoint then lives in exactly two files (`gradle/repositories.settings.gradle.kts`, `commons.gradle.kts`), down from ~14.
- Record the resolved repository topology in a new `artifact-resolution` capability spec so future changes have a contract to check against, and trim the `CLAUDE.md` note.

## Capabilities

### New Capabilities

- `artifact-resolution`: how the build obtains dependency and plugin artifacts — which repositories are consulted, in what order, from which resolution context, and the guarantee that cold resolution from a shared-IP environment (CI, Renovate) does not fail on canonical-repository rate limiting.

### Modified Capabilities

<!-- none: build-environment stays scoped to the daemon JVM; repository behavior is a distinct capability -->

## Impact

- `settings.gradle.kts` (root), `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`: remove `pluginManagement.repositories` (all four) and `dependencyResolutionManagement.repositories` (`core` / `fullstack-*`). Net: root ends with only `pluginManagement { includeBuild(...) }`; the apps keep `pluginManagement { includeBuild(...) }` and their `plugins { id("net.yewton.petclinic.foojay-resolver") }`.
- `gradle/repositories.settings.gradle.kts`: new shared settings script (`pluginManagement.repositories` + `dependencyResolutionManagement.repositories`, mirror first then `gradlePluginPortal()`).
- `lint-logic/settings.gradle.kts`, `build-logic/settings.gradle.kts`, `build-logic-settings/settings.gradle.kts`: replace inline repository blocks with `apply(from = file("../gradle/repositories.settings.gradle.kts"))`; keep `includeBuild(...)`.
- `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts`: unchanged.
- No new convention plugin; `net.yewton.petclinic.foojay-resolver` unchanged.
- No dependency versions change; `gradle/verification-metadata.xml` is unaffected (verification is by artifact checksum, independent of source repository).
- Renovate's `renovate.json` `hostRules` throttling stays as-is (independent defense).
- `libs/**` is out of scope (not part of the composite build; Renovate-ignored).

## Why

Every `repositories` block across the eight composite builds now carries the same hand-copied Google Maven Central mirror snippet — roughly 13 occurrences after the HTTP 429 mitigation (PRs #186, #189). The copies already drift in intent (some sites were added defensively without knowing whether they are load-bearing), and the next repository change — a second mirror, credentials, a repository manager, or dropping the mirror — is a 13-file sweep where missing one silently reintroduces the 429 failures. Issue #183 asked for a settings convention plugin; the mitigation work showed the real problem is that the configuration was never audited down to what actually resolves artifacts.

## What Changes

- Audit each `repositories` block and classify it as load-bearing (an artifact resolves through it) or vestigial (shadowed by another block or never consulted). Spikes required:
  - Whether `pluginManagement.repositories` in the root / `core` / `fullstack-html` / `fullstack-htmx` settings is consulted at all, given those builds resolve every plugin through `includeBuild` substitution.
  - Whether `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx` settings is consulted, given all three application projects declare project-level repositories through the `net.yewton.petclinic.commons` convention plugin (`RepositoriesMode.PREFER_PROJECT` shadows settings-level repositories per project).
- Remove the blocks the audit proves vestigial.
- Establish a single source of truth for the repositories that remain:
  - Application projects: resolve from settings `dependencyResolutionManagement` via a new `net.yewton.petclinic.repositories` settings convention plugin hosted in `build-logic-settings`, applied to root / `core` / `fullstack-html` / `fullstack-htmx`. Set `RepositoriesMode.FAIL_ON_PROJECT_REPOS` so the single source is enforced, and delete the `repositories` block from `net.yewton.petclinic.commons`.
  - Infrastructure plugin builds (`lint-logic`, `build-logic`, `build-logic-settings`): these compile `kotlin-dsl` and resolve their plugin classpath through `pluginManagement.repositories`, which a settings convention plugin cannot configure (evaluated before plugins apply) and which `lint-logic` and `build-logic-settings` cannot receive from a plugin anyway (`build-logic-settings` depends on `lint-logic`, so the dependency would cycle). Their `pluginManagement.repositories` stays inline. Reduce it to a shared `apply(from = ...)` snippet or accept three commented copies — decided in design.
- Record the resolved repository topology in the `build-environment` capability spec so future changes have a contract to check against, and in `CLAUDE.md`.

## Capabilities

### New Capabilities

- `artifact-resolution`: how the build obtains dependency and plugin artifacts — which repositories are consulted, in what order, from which resolution context, and the guarantee that cold resolution from a shared-IP environment (CI, Renovate) does not fail on canonical-repository rate limiting.

### Modified Capabilities

<!-- none: build-environment stays scoped to the daemon JVM; repository behavior is a distinct capability -->

## Impact

- `build-logic-settings/`: new `net.yewton.petclinic.repositories.settings.gradle.kts` precompiled settings script.
- `settings.gradle.kts` (root), `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`: apply the new plugin; remove blocks the audit proves vestigial.
- `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts`: remove the `repositories` block.
- `lint-logic/settings.gradle.kts`, `build-logic/settings.gradle.kts`, `build-logic-settings/settings.gradle.kts`: `pluginManagement.repositories` reduced to a shared snippet or left as commented copies.
- No dependency versions change; `gradle/verification-metadata.xml` is unaffected (verification is by artifact checksum, independent of source repository).
- Renovate's `renovate.json` `hostRules` throttling stays as-is (independent defense).
- `libs/**` is out of scope (not part of the composite build; Renovate-ignored).

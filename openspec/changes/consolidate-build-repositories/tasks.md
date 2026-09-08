## 1. Confirm the load-bearing set (prerequisite)

- [x] 1.1 Wait until PR #189 is merged and a Renovate PR rebased onto it has passed CI and the `renovate/artifacts` check
- [x] 1.2 Spike A: remove `pluginManagement { repositories { … } }` from `settings.gradle.kts`, `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`; `--refresh-dependencies` compile + `buildEnvironment` — PASS, vestigial (see design.md Spike Results)
- [x] 1.3 Spike B: remove `dependencyResolutionManagement { repositories { … } }` from `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`; `--refresh-dependencies` resolution incl. `:core:lib:jooqCodegen` — PASS, vestigial; two pre-existing failures reproduced on clean `main` (see design.md Spike Results)
- [x] 1.4 Resolve the remaining design Open Questions — DONE: shared `apply(from = "../gradle/repositories.settings.gradle.kts")` for the three infra builds (no `net.yewton.petclinic.repositories` plugin, no `FAIL_ON_PROJECT_REPOS`); `commons.gradle.kts` unchanged; `foojay-resolver` unchanged (see design.md Decision 2 / Open Questions)

## 2. Delete vestigial blocks (PR 1)

Spikes proved all seven blocks vestigial: `pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx`, and `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx`.

- [ ] 2.1 Remove `pluginManagement { repositories { … } }` from `settings.gradle.kts`, `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts` (keep the `includeBuild` lines)
- [ ] 2.2 Remove `dependencyResolutionManagement { repositories { … } }` from `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`
- [ ] 2.3 `./gradlew spotlessApply` then `./gradlew check --parallel --build-cache --configuration-cache`
- [ ] 2.4 Open PR; let a Renovate PR rebase onto it and confirm green before merge; the diff must be a clean revert if CI or Renovate 429s

## 3. Shared settings script for the infra builds (PR 2)

- [ ] 3.1 Add `gradle/repositories.settings.gradle.kts` with `pluginManagement { repositories { <mirror>; gradlePluginPortal() } }` and `dependencyResolutionManagement { repositories { <mirror>; gradlePluginPortal() } }` (mirror = the GCS Maven Central mirror with `mavenContent { releasesOnly() }`, matching the current inline blocks)
- [ ] 3.2 In `lint-logic/settings.gradle.kts`, `build-logic/settings.gradle.kts`, `build-logic-settings/settings.gradle.kts`: add `apply(from = file("../gradle/repositories.settings.gradle.kts"))`; delete the inline `pluginManagement { repositories { … } }` / `dependencyResolutionManagement { repositories { … } }` blocks, keeping the `includeBuild(...)` lines
- [ ] 3.3 Confirm repository order: `./gradlew --info help` (or a resolve) shows the mirror consulted before `gradlePluginPortal()` for the infra plugin classpaths
- [ ] 3.4 Emptied-cache / `--refresh-dependencies` `./gradlew check` to confirm the infra `kotlin-dsl` classpaths and `implementation` deps still resolve
- [ ] 3.5 `./gradlew spotlessApply` then `./gradlew check --parallel --build-cache --configuration-cache`
- [ ] 3.6 Check whether `gradle/repositories.settings.gradle.kts` is picked up by Spotless (it is under `gradle/`, not a source set) — add it to the Spotless target or the root aggregation if not

## 4. Documentation and spec (PR 3)

- [ ] 4.1 Add `openspec/specs/artifact-resolution/spec.md` (via `openspec archive` of this change, or hand-written)
- [ ] 4.2 Trim `CLAUDE.md`'s "Maven リポジトリ" note to: mirror-first rule, the load-bearing set (`gradle/repositories.settings.gradle.kts` for the infra builds, `commons.gradle.kts` for the app projects), pointer to the spec
- [ ] 4.3 Confirm `gradle/verification-metadata.xml` is unchanged by the whole series

## 5. Close out

- [ ] 5.1 Link the PRs on issue #183 and note the reframed scope (audit + shared `apply(from = ...)` file for the infra builds; app-build settings end with no repository config; `commons` unchanged)
- [ ] 5.2 `openspec archive consolidate-build-repositories` once all PRs are merged

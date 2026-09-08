## 1. Audit (done — falsified)

- [x] 1.1 #186, #187, #189, #190 merged; Renovate re-ran past the 429 and past dependency verification (remaining #174 failure is an unrelated Spring Boot 4.1 test break)
- [x] 1.2 Spike A (`pluginManagement.repositories` emptied in root / core / html / htmx) — concluded VESTIGIAL; **falsified** on current `main`: deleting it fails `org.jetbrains.kotlin.jvm.gradle.plugin-2.2.21.pom` verification (portal copy `5c3bde6e…` ≠ recorded `8d3b2cba…`). See design.md Decision 1
- [x] 1.3 Spike B (`dependencyResolutionManagement.repositories` removed in core / html / htmx) — concluded VESTIGIAL; **falsified** on current `main`: deleting it fails `:core:spotlessKotlinGradle` (`ktlint-cli:1.8.0`, "no repositories are defined"). See design.md Decision 1
- [x] 1.4 Open questions resolved — no block is deletable; ecosystem survey (design.md Decision 2) points at `gradle/gradle`'s `apply(from = ...)` shared-settings-script pattern
- [x] 1.5 `kotlin-build-tools-impl:2.4.20` (surfaced by Spike B) handled separately — #192 / #193 / #195

## 2. Reflect findings into the OpenSpec artifacts (PR A — this change)

- [x] 2.1 Rewrite `design.md` — corrected load-bearing map, the two mechanisms (ktlint detached config; marker POM checksum), Spike falsification, ecosystem survey, Decision 3 (shared `apply(from = ...)` file), alternatives A/C/D/E/F
- [x] 2.2 Rewrite `proposal.md` — centralize-don't-delete
- [x] 2.3 Rewrite `specs/artifact-resolution/spec.md` — mirror-first, single declaration site, `apply(from = ...)` not a plugin, documented topology
- [x] 2.4 Rewrite this `tasks.md`
- [ ] 2.5 `openspec validate consolidate-build-repositories`; open PR A

## 3. Implement the shared settings script (PR B, stacked on PR A)

- [ ] 3.1 Add `gradle/repositories.settings.gradle.kts`:
  - `pluginManagement { repositories { <mirror>; gradlePluginPortal() } }`
  - `dependencyResolutionManagement { repositories { <mirror>; gradlePluginPortal() } }`
  - `<mirror>` = `maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "Maven Central Mirror"; mavenContent { releasesOnly() } }`, byte-identical to the current inline snippets
- [ ] 3.2 In `settings.gradle.kts` (root): replace the `pluginManagement { repositories { … } }` block with `apply(from = file("gradle/repositories.settings.gradle.kts"))` as the first statement; keep `pluginManagement { includeBuild(...) }`
- [ ] 3.3 In `core/`, `fullstack-html/`, `fullstack-htmx/settings.gradle.kts`: replace the `pluginManagement.repositories` block and the whole `dependencyResolutionManagement { }` block with `apply(from = file("../gradle/repositories.settings.gradle.kts"))`; keep `pluginManagement { includeBuild(...) }`
- [ ] 3.4 In `lint-logic/`, `build-logic/`, `build-logic-settings/settings.gradle.kts`: replace the `pluginManagement.repositories` block and the `dependencyResolutionManagement { }` block with `apply(from = file("../gradle/repositories.settings.gradle.kts"))`; keep `includeBuild(...)`
- [ ] 3.5 `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts` — do NOT touch
- [ ] 3.6 Extend the Spotless `kotlinGradle` target to cover `gradle/*.settings.gradle.kts` if it is not already linted; `./gradlew spotlessApply`
- [ ] 3.7 `./gradlew check --parallel --warning-mode all --build-cache --configuration-cache` — full `check`, not targeted tasks — must be BUILD SUCCESSFUL
- [ ] 3.8 Emptied Gradle module cache (`rm -rf ~/.gradle/caches/modules-2`) + `./gradlew check` — confirm cold resolution: no 429, no marker-POM verification failure, ktlint resolves
- [ ] 3.9 `git diff gradle/verification-metadata.xml` — must be empty
- [ ] 3.10 Open PR B; let a Renovate PR rebase onto it and confirm green before merge; keep the diff a one-commit revert

## 4. Documentation (PR C, stacked on PR B)

- [ ] 4.1 Add `openspec/specs/artifact-resolution/spec.md` (via `openspec archive`, or hand-written)
- [ ] 4.2 Trim `CLAUDE.md`'s "Maven リポジトリ" note to: mirror-first rule; the two load-bearing files (`gradle/repositories.settings.gradle.kts`, `commons.gradle.kts`); why the marker POM checksum makes `pluginManagement.repositories` load-bearing; pointer to the spec

## 5. Close out

- [ ] 5.1 Link PRs A/B/C on issue #183, note the reframed scope (audit found nothing deletable; pure centralization via `apply(from = ...)`, matching `gradle/gradle`)
- [ ] 5.2 `openspec archive consolidate-build-repositories` once all PRs are merged

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
- [x] 2.5 `openspec validate`; open PR #197
- [x] 2.6 Advisor review (Opus 5 on design, Sonnet 5 on implementation); agreed corrections folded back into #197/#198 — two-variant framing, three-entry unified list, `settingsEvaluated` guard, `apply` placement after `pluginManagement`/`plugins`, `--write-verification-metadata` as the real safety proof, spec fail-safe scenario, OSS-survey precision

## 3. Implement the shared settings script (PR #198, stacked on #197)

- [ ] 3.1 Add `gradle/repositories.settings.gradle.kts`:
  - `pluginManagement { repositories { <mirror>; mavenCentral(); gradlePluginPortal() } }`
  - `dependencyResolutionManagement { repositories { <mirror>; mavenCentral(); gradlePluginPortal() } }`
  - `<mirror>` = `maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "Maven Central Mirror"; mavenContent { releasesOnly() } }` — mirror line byte-identical to the current inline snippets; the fallback list is the order-preserving union of the two drifted variants; a comment notes that `releasesOnly()` means SNAPSHOT deps bypass the mirror
  - a `gradle.settingsEvaluated {}` guard: `check` that `settings.pluginManagement.repositories.firstOrNull()` is a `MavenArtifactRepository` whose URL starts with the mirror host; fail with a message naming the actual first repository
- [ ] 3.2 In each of the seven settings files, replace the inline repository blocks with `apply(from = file("<rel>/gradle/repositories.settings.gradle.kts"))` — `gradle/…` from root, `../gradle/…` from the six nested builds — placed **after** the `pluginManagement {}` and `plugins {}` blocks (matching `gradle/gradle`). Keep every `includeBuild(...)`, `plugins {}`, `rootProject.name`, `include(...)` line. `lint-logic` loses its `pluginManagement {}` block entirely (it held only `repositories`)
- [ ] 3.3 `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts` — do NOT touch
- [ ] 3.4 Prove Spotless coverage: add a deliberate formatting violation to `gradle/repositories.settings.gradle.kts`, run root `./gradlew :spotlessKotlinGradleCheck`. If it does NOT fail, add an explicit `target(...)` to the `kotlinGradle {}` block in `lint-logic/spotless/src/main/kotlin/net.yewton.petclinic.spotless.gradle.kts` (re-listing the default patterns and `targetExclude`). Revert; `./gradlew spotlessApply`
- [ ] 3.5 `./gradlew check --parallel --warning-mode all --build-cache --configuration-cache` — full `check`, not targeted tasks — BUILD SUCCESSFUL
- [ ] 3.6 Emptied module cache (`rm -rf ~/.gradle/caches/modules-2`) + full `./gradlew check` — no 429, no marker-POM verification failure, ktlint resolves
- [ ] 3.7 `./gradlew --dependency-verification lenient -q --write-verification-metadata sha256 check --no-configuration-cache`, then `git diff --stat gradle/verification-metadata.xml` — MUST be empty (proves the fallback-list unification changed no resolved artifact or checksum). Do not commit a metadata change
- [ ] 3.8 IntelliJ IDEA reimport once before merge
- [ ] 3.9 Push #198; let a Renovate PR rebase onto the stack and confirm green before merge; keep the diff a one-commit revert

## 4. Documentation (PR #200, stacked on #198)

- [ ] 4.1 Trim `CLAUDE.md`'s "Maven リポジトリ" note: mirror-first rule + the `settingsEvaluated` guard; the two load-bearing files (`gradle/repositories.settings.gradle.kts`, `commons.gradle.kts`); why the marker POM checksum makes `pluginManagement.repositories` load-bearing; pointer to the spec
- [ ] 4.2 `openspec/specs/artifact-resolution/spec.md` is promoted by `openspec archive` after all three PRs merge (not in #200)

## 5. Close out

- [ ] 5.1 Link #197/#198/#200 on issue #183, note the reframed scope (audit found nothing deletable; pure centralization via `apply(from = ...)` + guard)
- [ ] 5.2 `openspec archive consolidate-build-repositories` once all PRs are merged

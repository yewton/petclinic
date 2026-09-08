## Context

The project is eight Gradle builds wired with `includeBuild`:

```
Layer 0  platforms                (version catalog + java-platform BOMs; no artifact resolution)
Layer 1  lint-logic               → platforms                         (spotless/ktlint convention)
Layer 2  build-logic              → lint-logic, platforms             (JVM/Spring/jOOQ conventions)
         build-logic-settings     → lint-logic, platforms             (foojay-resolver settings plugin)
Layer 3  core                     → build-logic, build-logic-settings, lint-logic, platforms
Layer 4  fullstack-html           → core, ...
         fullstack-htmx           → core, ...
Layer 5  root aggregator          → all of the above
```

`build-logic-settings` was split out from `build-logic` in #177/#179 because Gradle recommends a dedicated, minimal included build for settings plugins (a settings plugin in a general `build-logic` degrades build-cacheability). `lint-logic` is separate because `build-logic` and `build-logic-settings` both depend on it for Spotless formatting.

The HTTP 429 mitigation (#186, #189) copied a Google-hosted Maven Central mirror `repositories` block into fourteen places — `pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx` / `lint-logic` / `build-logic` / `build-logic-settings`, `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx` / `lint-logic` / `build-logic` / `build-logic-settings`, and the project-level `repositories {}` in `net.yewton.petclinic.commons`. The blocks are not byte-identical: they have **two fallback variants**. The mirror line is the same everywhere, but the second repository is `gradlePluginPortal()` in all seven `pluginManagement` blocks and in the three infra `dependencyResolutionManagement` blocks, and `mavenCentral()` in the `core` / `fullstack-*` `dependencyResolutionManagement` blocks and in `commons`. So the copies have already drifted.

### How artifacts actually resolve (verified)

| Resolution context | Repository source | Load-bearing? |
|---|---|---|
| Application project deps (`core:lib`, `fullstack-*:app`) | project-level `repositories {}` in `net.yewton.petclinic.commons` (`RepositoriesMode.PREFER_PROJECT`, the default, makes a project's own repos win) | **yes** |
| `:core` / `:fullstack-*` **root project** Spotless `ktlint()` | Spotless resolves `com.pinterest.ktlint:ktlint-cli` in a **detached configuration**. These root projects apply `net.yewton.petclinic.spotless` but not `net.yewton.petclinic.commons`, so they have no project-level repos and the detached config falls back to the build's settings `dependencyResolutionManagement.repositories` | **yes** — `core` / `fullstack-*` settings `dependencyResolutionManagement.repositories` |
| Any project that applies `net.yewton.petclinic.commons` (root project, `core:lib`, `fullstack-*:app`) | Applying that precompiled script plugin re-resolves the Kotlin plugin **marker** `org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin` (and `...plugin.spring...`) in the consuming build's `pluginManagement.repositories` — the precompiled plugin's `plugins { kotlin("jvm") }` request leaks to consumers | **yes** — every build's `pluginManagement.repositories`, and the mirror must be **first** (see below) |
| Infra plugin builds (`lint-logic`, `build-logic`, `build-logic-settings`) plugin classpath | `pluginManagement.repositories` — `kotlin-dsl` and its transitive `kotlin-gradle-plugin-api` / `kotlin-stdlib` / `gradle-kotlin-dsl-plugins` | **yes** |
| Infra plugin builds `implementation` deps | `dependencyResolutionManagement.repositories` — `spotless-plugin-gradle`, `spring-boot-gradle-plugin`, jOOQ, the foojay marker | **yes** |
| root settings `dependencyResolutionManagement` | not declared; the root project has repos via `commons`, so it is not needed | n/a |

**Every repository block that exists is load-bearing.** Nothing is deletable.

### Why the mirror must be first, not just present

`gradle/verification-metadata.xml` runs in strict mode with `verify-metadata=true` — it records a SHA-256 for **every** artifact including `.pom` and `.module` files. #195 switched the Kotlin plugin dependency to marker artifacts, so consumers now resolve `org.jetbrains.kotlin.jvm.gradle.plugin-2.2.21.pom`. The Gradle Plugin Portal *generates* its own copy of that marker POM; Maven Central serves the *published* one. They are different bytes:

```
verification-metadata.xml records:  sha256 8d3b2cba…   (Maven Central / GCS mirror copy)
gradlePluginPortal() serves:         sha256 5c3bde6e…   (portal-generated copy)
```

`#195` regenerated the metadata with the mirror as the first repository, so the recorded checksum is the Central copy. If the mirror is removed (default `gradlePluginPortal()`) or ordered after the portal, verification fails with "dependency has been compromised". So the mirror is load-bearing for a reason beyond 429 avoidance: it pins the marker POM source.

`repo.maven.apache.org` also returns HTTP 429 to shared CI / Renovate IPs, and Gradle disables a repository on its first 429 rather than falling through — so the rate-limited canonical repository cannot be listed first either. Both constraints point the same way: mirror first, `mavenCentral()` / `gradlePluginPortal()` as fallback.

## Goals / Non-Goals

**Goals:**

- One declaration site for the repository list that the seven settings files currently hand-copy.
- No regression of the 429 fix or of dependency verification; no dependency version changes.
- A written contract (`artifact-resolution` spec) plus a `CLAUDE.md` note, so the next repository change and the next `#195`-style plugin-graph change have something to check against.

**Non-Goals:**

- **Deleting any repository block.** The audit (Decision 1) falsified the earlier assumption that the application-build blocks were vestigial.
- **A settings convention plugin for repositories** or `RepositoriesMode.FAIL_ON_PROJECT_REPOS`. A precompiled settings plugin runs after the `plugins {}` block, so it cannot configure `pluginManagement.repositories` — the block the marker checksum and the 429 both depend on. It could only centralize `dependencyResolutionManagement.repositories`, and `lint-logic` / `build-logic-settings` could not apply it without a `build-logic-settings → lint-logic` cycle. `apply(from = ...)` of a shared settings script *can* carry `pluginManagement.repositories` (a `/tmp` spike resolved a plugin marker from repos declared by an applied script; PR B confirms it in the real build), so that is the mechanism (Decision 3).
- **`verify-metadata=false` + PGP signatures** (the model `gradle/gradle` itself uses, which would remove the marker-POM checksum sensitivity). Removing strict checksum verification was proposed in #168 and rejected; it is a supply-chain posture decision, out of scope here.
- **Consolidating `lint-logic` / `build-logic` / `build-logic-settings` into one build.** The canonical large-build structure (`jjohannes/gradle-project-setup-howto`) uses one `gradle/plugins` build, which would remove two infra `settings.gradle.kts` — but `build-logic-settings` was deliberately split for build-cacheability in #177/#179, and `gradle/gradle` keeps the same split for the same reason.
- Touching `libs/**` (outside the composite build, Renovate-ignored) or `renovate.json` `hostRules` (independent throttling, already in place).
- A Gradle init script `beforeSettings {}` — could configure `pluginManagement` for the whole tree, but needs `--init-script` on every invocation, which Renovate's fixed Gradle command cannot pass.

## Decisions

### Decision 1: audit — the spikes were falsified

Two spikes were run (PR #191). Both concluded the application-build blocks were vestigial. **Both conclusions were wrong**, for two independent reasons:

1. **Methodology.** The spikes ran targeted tasks (`compileKotlin`, `dependencies`, `buildEnvironment`, `jooqCodegen`) and never `spotless*` or a full `check`. `SECURITY.md` already documents that "Spotless resolves the ktlint runtime in a detached configuration at execution time, and no `dependencies` task reports it" — the exact path the spikes skipped.
2. **Timing.** The spikes ran before #195. #195 switched Kotlin to plugin marker artifacts, which made `pluginManagement.repositories` load-bearing for the marker POM checksum (see Context) where Spike A had found it vestigial.

Re-verified on current `main` (`758a1fec`):

- Delete `core`'s `dependencyResolutionManagement.repositories` → `:core:spotlessKotlinGradle` fails: `Cannot resolve external dependency com.pinterest.ktlint:ktlint-cli:1.8.0 because no repositories are defined. Required by: project ':core'`.
- Delete any of root / `core` / `fullstack-*`'s `pluginManagement.repositories` → configuring the build fails: `Dependency verification failed for configuration 'classpath'. One artifact failed verification: org.jetbrains.kotlin.jvm.gradle.plugin-2.2.21.pom … from repository Gradle Central Plugin Repository. Expected a sha256 checksum of 8d3b2cba… but was 5c3bde6e…`.

So: no block is vestigial.

### Decision 2: how the ecosystem handles this

Surveyed the settings layout of seven notable Gradle projects:

| Project | Structure | `pluginManagement.repositories` | `dependencyResolutionManagement.repositories` | Shared mechanism | Verification |
|---|---|---|---|---|---|
| **gradle/gradle** | `build-logic`, `build-logic-commons`, `build-logic-settings` + buildSrc | inline `gradlePluginPortal()` per settings file | inline `mavenCentral()` + `gradlePluginPortal()` per settings file | `gradlebuild.default-settings-plugins` (foojay + develocity + a `content`-filtered RC repo); `gradle/shared-with-buildSrc/mirrors.settings.gradle.kts` via `apply(from = ...)` for CI mirror **URL rewriting** | `verify-metadata=false`, PGP signatures |
| **spring-boot** | root + buildSrc + `gradle/plugins` composite | inline `mavenCentral()` + `gradlePluginPortal()` per settings file; Spring repos via `spring.mavenRepositories()` | via `gradle.allprojects { }` in the same shared Groovy script | `buildSrc/SpringRepositorySupport.groovy` via `evaluate(new File(...)).apply(this)` (only the Spring snapshot/commercial repos) | none |
| **detekt** | `build-logic`, `detekt-gradle-plugin` composite (closest analog) | minimal `includeBuild` only, default portal | **inline `FAIL_ON_PROJECT_REPOS` + repositories in every settings file**, with `// keep in sync` comments | none for repositories — inline duplication is accepted; the `buildCache {}` block is likewise duplicated with a sync comment | none |
| **jjohannes/gradle-project-setup-howto** (Gradle team, canonical "structuring large builds" sample) | one `gradle/plugins` included build for **all** convention plugins (settings + project) | none — `pluginManagement { includeBuild("gradle/plugins") }` only; every plugin incl. third-party resolves via the included build | settings convention plugin `org.example.gradle.feature.repositories`, whose entire body is `dependencyResolutionManagement { repositories.mavenCentral() }` | settings convention plugin | none |
| **androidx/androidx** | huge, buildSrc + settings plugins | `apply(from: "buildSrc/repos.gradle")` inside `pluginManagement {}`, then `repos.addMavenRepositories(repositories)` | same `repos.addMavenRepositories(...)` called in `gradle.beforeProject { }` | shared Groovy script exposing an `addMavenRepositories(RepositoryHandler)` function, called explicitly per resolution context | none |
| **JetBrains/kotlin**, **micronaut-core** | large / medium | inline | inline (`FAIL_ON_PROJECT_REPOS` in Kotlin) | — | — |

Conclusions:

- **No project fully centralizes the base `mavenCentral()` / `gradlePluginPortal()` declarations.** Every one inlines them per settings file. The structurally closest analog, **detekt**, inline-duplicates the whole `dependencyResolutionManagement { FAIL_ON_PROJECT_REPOS; repositories { … } }` block across three settings files with "keep in sync" comments and considers that acceptable.
- The shared-script mechanisms (`apply(from = ...)`, `evaluate().apply(this)`, a shared function) are used only for the **non-base layer**: CI mirror rewriting (Gradle), Spring snapshot repos (Spring), prebuilts (AndroidX). Those layers are additive and fail-safe — a missed application means the extra repos are absent, not that resolution has no repositories. PetClinic's mirror *is* the base layer and is load-bearing, so its shared file borrows only the *packaging* (a settings script pulled in with `apply(from = ...)` and per-location relative paths), not the fail-safe property. That gap is what the `settingsEvaluated` guard covers.
- **`gradle/gradle` is the one project with PetClinic's exact structure** (dedicated `build-logic-settings` split for the same build-cache reason) and the same class of concern. It still inlines `mavenCentral()` / `gradlePluginPortal()` in every settings file and shares only the mirror-URL-rewriting layer via `apply(from = "…/mirrors.settings.gradle.kts")` — and its rewriter is CI-gated and fail-safe. So `gradle/gradle` is precedent for the *packaging*, not for making the base repository list load-bearing on a shared script.
- PetClinic feels the marker-POM checksum problem that the others do not, because it runs `verify-metadata=true` (stricter than `gradle/gradle`'s own build) with no PGP, plus marker-based Kotlin plugins (#195), plus the composite-build split.

### Decision 3: shared `gradle/repositories.settings.gradle.kts` applied with `apply(from = ...)`

Add one file, `gradle/repositories.settings.gradle.kts`, declaring both blocks with the mirror first. The fallback list is unified to the order-preserving union of the two drifted variants — mirror, then `mavenCentral()`, then `gradlePluginPortal()`:

```
pluginManagement {
  repositories {
    <mirror>            // GCS Maven Central mirror; mavenContent { releasesOnly() }
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositories {
    <mirror>
    mavenCentral()
    gradlePluginPortal()
  }
}
```

Adding `mavenCentral()` before `gradlePluginPortal()` in `pluginManagement` is a safety improvement: if the mirror ever lags a marker POM, the next repository consulted is the Maven Central copy (matching the recorded checksum) rather than the portal-generated copy (different bytes → verification failure). Adding `gradlePluginPortal()` to the `core` / `fullstack-*` `dependencyResolutionManagement` is inert — everything that resolves there (`ktlint-cli` and its transitives) is a Maven Central artifact served by the mirror. The unification MUST be proven safe by regenerating `gradle/verification-metadata.xml` (`--write-verification-metadata sha256`) and confirming an empty diff — `./gradlew check` never writes that file, so a `git diff` on it after `check` proves nothing.

Replace the inline repository blocks in all seven settings files with `apply(from = file("<rel>/gradle/repositories.settings.gradle.kts"))` — `gradle/…` from the root, `../gradle/…` from the six nested builds. Place the `apply` line **after** the inline `pluginManagement {}` and `plugins {}` blocks, matching `gradle/gradle` (whose `settings.gradle.kts` applies `mirrors.settings.gradle.kts` after those blocks). The `apply` position does not affect the result — the shared file's `pluginManagement.repositories` is consumed at project-configuration time, and the inline `pluginManagement {}` blocks declare no repositories — but "after" avoids relying on Gradle's syntax rule that only `buildscript` / `pluginManagement` / `plugins` may precede a `plugins {}` block, which a future Gradle could tighten.

The shared file ends with a `gradle.settingsEvaluated {}` guard that fails the build if `pluginManagement.repositories` does not start with the mirror. This turns the one remaining failure mode — a future `plugins { id("some.settings.plugin") }` in a settings file resolving from an empty `pluginManagement.repositories` and silently falling back to the plugin portal — from a CI-only surprise into an immediate, local, named error. That silent portal fallback is the exact shape of every failure this issue has produced.

`net.yewton.petclinic.commons`' project-level `repositories {}` is left unchanged: it is a *project* convention (applied to `core:lib` / `fullstack-*:app` / the root project) and already a single declaration. The mirror endpoint then lives in exactly two files (`gradle/repositories.settings.gradle.kts`, `commons.gradle.kts`), down from fourteen.

This is `gradle/gradle`'s `gradle/shared-with-buildSrc/mirrors.settings.gradle.kts` packaging: a settings script pulled into each settings file with `apply(from = ...)` and per-location relative paths. The mechanisms differ in an important way, though — `gradle/gradle`'s script *rewrites* the URLs of already-declared repositories via a `settingsEvaluated {}` hook, is gated on the `CI` environment variable, and fails safe (a missed application just means no mirror). PetClinic's *declares* the repositories, is unconditional, and is load-bearing (a missed application means an empty `pluginManagement.repositories`). Hence the guard.

**To be confirmed in PR B (the implementation):** that `apply(from = ...)` carries `pluginManagement.repositories` such that the Kotlin marker POM resolves from the mirror with the recorded checksum, and that `:core:spotlessKotlinGradle`'s ktlint detached configuration resolves from the shared `dependencyResolutionManagement.repositories`. This must be checked with a full `./gradlew check` and an emptied-module-cache run — not the targeted tasks that made the spikes wrong.

**Alternatives considered:**

- **Accept the inline duplication** (detekt's choice — it duplicates the whole `dependencyResolutionManagement { … }` block across three settings files with `// keep in sync` comments). Fully defensible: it is the ecosystem norm, and the blocks are consistent after #189. Chosen against because the block is ~8 lines and the two-variant drift shows the "keep in sync" discipline already failed once.
- **AndroidX's shared function** (`gradle/repos.gradle` exposing `addRepositories(RepositoryHandler)`, called *inside* `pluginManagement {}` and again in `gradle.beforeProject {}`). Because the call sits inside `pluginManagement {}` it runs in settings-script stage 1, so it does not have the timing exposure that the `settingsEvaluated` guard exists to cover, and one function could also replace `commons`' block (one declaration site instead of two). Rejected because an imperative `addRepositories(handler)` call reads poorly in Kotlin DSL and the robustness gap it closes is small once the guard is in place — but this is a genuine trade-off, not a clear loss.
- **A settings convention plugin for `dependencyResolutionManagement.repositories`** (jjohannes's model). jjohannes's real move is putting *every* plugin, third-party included, in one `gradle/plugins` included build so that `pluginManagement.repositories` is not needed at all; the settings plugin only carries the trivial `dependencyResolutionManagement { repositories.mavenCentral() }`. PetClinic cannot adopt the first part without merging the infra builds (see Non-Goals), and a settings plugin cannot touch `pluginManagement.repositories`, so the shared file is needed regardless.
- **`verify-metadata=false` + PGP** — removes the marker-POM sensitivity entirely. Out of scope (see Non-Goals).

### Decision 4: record the topology in the `artifact-resolution` spec and `CLAUDE.md`

Code comments drift, and the load-bearing map here is non-obvious (it took two failed spikes to establish). The `artifact-resolution` capability spec states the invariants — cold resolution must not 429, mirror first, single declaration site, the two resolution contexts and what each serves — as scenarios. `CLAUDE.md`'s "Maven リポジトリ" note is trimmed to: the mirror-first rule, the two load-bearing files, why the marker POM checksum makes `pluginManagement.repositories` matter, and a pointer to the spec.

## Risks / Trade-offs

- **A future `plugins { id("some.settings.plugin") }` in a settings file resolves from an empty `pluginManagement.repositories` and silently uses the plugin portal** → this is the failure shape this whole issue keeps producing (invisible locally, breaks on CI/Renovate). Covered by the `gradle.settingsEvaluated {}` guard in the shared file: the build fails immediately, locally, with a message naming the offending first repository.
- **The fallback-list unification changed a resolved artifact's source or checksum** → prove empty with `./gradlew --dependency-verification lenient -q --write-verification-metadata sha256 check --no-configuration-cache` then `git diff gradle/verification-metadata.xml`. `./gradlew check` alone does not write that file, so a `git diff` after it is not a test.
- **A `#195`-style change later alters which plugins leak to consumers** → the spec's scenarios and the `CLAUDE.md` note name the mechanism explicitly, so the next person has a place to check. The shared file makes any needed repository change a one-file edit.
- **`apply(from = ...)` with a wrong relative path** → six of seven files use the identical `../gradle/…`; only the root differs. A smoke `./gradlew help` catches a typo immediately.
- **The GCS mirror lags a brand-new release** → `mavenCentral()` / `gradlePluginPortal()` fallback covers it; Renovate's `minimumReleaseAge: "3 days"` makes the window irrelevant here.
- **Spotless does not lint `gradle/repositories.settings.gradle.kts`** — the `kotlinGradle` default target is the project directory's `*.gradle.kts`, non-recursive, and the new file is in a subdirectory. PR B must prove coverage by introducing a deliberate formatting violation and confirming `./gradlew :spotlessKotlinGradleCheck` fails; if it does not, add an explicit `target(...)` to the `kotlinGradle` block (which replaces the default, so the default patterns must be re-listed).
- **`mavenContent { releasesOnly() }` on the mirror** → a future SNAPSHOT dependency bypasses the mirror and goes straight to `mavenCentral()` / `gradlePluginPortal()`. Same behaviour as the current inline blocks; noted with a comment in the shared file.

## Migration Plan

1. ~~#186, #187, #189, #190 merged; Renovate re-ran past the 429 and past dependency verification.~~ Done.
2. ~~Spike A and Spike B.~~ Done — and **falsified**; see Spike Results and Decision 1. #191 merged.
3. **PR #197 (this change)**: rewrite `design.md` / `proposal.md` / `specs` / `tasks.md` to the corrected findings and the centralize-don't-delete approach.
4. **PR #198**: add `gradle/repositories.settings.gradle.kts` (three-entry unified list + `settingsEvaluated` guard); replace the inline repository blocks in the seven settings files with `apply(from = ...)` placed after the `pluginManagement {}` / `plugins {}` blocks; leave `commons.gradle.kts` alone; prove and fix the Spotless target. Verified with a full `./gradlew check`, an emptied-cache run, and a `--write-verification-metadata` regeneration with an empty diff — **not** targeted tasks.
5. **PR #200**: `CLAUDE.md` note. The `openspec archive` (promoting `specs/artifact-resolution/spec.md`) happens after all three merge.
6. Rollback: PR #198 is a single revert; #197 and #200 are docs-only.

## Spike Results (FALSIFIED — retained for the record)

Run against `main` at `f1e4fa71e8` (before #195), Gradle 9.7.1, `--refresh-dependencies`. Both spikes concluded the application-build blocks were vestigial. **Both conclusions are wrong** — see Decision 1. What the spikes missed:

- **Spike A** (`pluginManagement.repositories` emptied in root / `core` / `fullstack-*`): the run compiled and passed because it ran `compileKotlin` / `buildEnvironment`, and pre-#195 the Kotlin plugin resolved without a marker POM. After #195, applying `net.yewton.petclinic.commons` re-resolves `org.jetbrains.kotlin.jvm.gradle.plugin-2.2.21.pom` in the consumer's `pluginManagement.repositories`, and the portal's copy fails the recorded checksum.
- **Spike B** (`dependencyResolutionManagement.repositories` removed in `core` / `fullstack-*`): the run passed because it never ran a `spotless*` task or `check`. `:core:spotlessKotlinGradle` resolves `com.pinterest.ktlint:ktlint-cli` in a detached configuration that falls back to the settings block, which the spike deleted.
- The `kotlin-build-tools-impl:2.4.20` verification failure the spike surfaced was a genuine pre-existing issue, handled separately in #192 / #193 / #195.

## Open Questions

None. The approach is Decision 3; implementation is PR B.

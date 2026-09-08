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

The HTTP 429 mitigation (#186, #189) copied a Google-hosted Maven Central mirror snippet into every `repositories` block: `pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx` / `lint-logic` / `build-logic` / `build-logic-settings`, `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx` / `lint-logic` / `build-logic` / `build-logic-settings`, and the project-level `repositories {}` in `net.yewton.petclinic.commons`. Fourteen occurrences of the same ~8-line block.

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
- **A settings convention plugin for repositories** or `RepositoriesMode.FAIL_ON_PROJECT_REPOS`. A precompiled settings plugin runs after the `plugins {}` block, so it cannot configure `pluginManagement.repositories` — the block the marker checksum and the 429 both depend on. It could only centralize `dependencyResolutionManagement.repositories`, and `lint-logic` / `build-logic-settings` could not apply it without a `build-logic-settings → lint-logic` cycle. `apply(from = ...)` of a shared settings script *can* carry `pluginManagement.repositories` (verified on Gradle 9.7.1), so that is the mechanism (Decision 3).
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
- The shared-script mechanisms (`apply(from = ...)`, `evaluate().apply(this)`, a shared function) are used only for the **non-base layer**: CI mirror rewriting (Gradle), Spring snapshot repos (Spring), prebuilts (AndroidX). PetClinic's mirror *is* the base layer, so its shared file will carry more than those precedents do — but the packaging pattern is identical.
- **`gradle/gradle` is the one project with PetClinic's exact structure** (dedicated `build-logic-settings` split for the same build-cache reason) and the same class of concern. It accepts inline base-repo duplication and shares only the mirror layer via `apply(from = "…/mirrors.settings.gradle.kts")`.
- PetClinic feels the marker-POM checksum problem that the others do not, because it runs `verify-metadata=true` (stricter than `gradle/gradle`'s own build) with no PGP, plus marker-based Kotlin plugins (#195), plus the composite-build split.

### Decision 3: shared `gradle/repositories.settings.gradle.kts` applied with `apply(from = ...)`

Add one file, `gradle/repositories.settings.gradle.kts`, declaring both blocks with the mirror first:

```
pluginManagement {
  repositories {
    <mirror>            // GCS Maven Central mirror, mavenContent { releasesOnly() }
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositories {
    <mirror>
    gradlePluginPortal()
  }
}
```

Replace the inline repository blocks in all seven settings files with `apply(from = file("<rel>/gradle/repositories.settings.gradle.kts"))` — `gradle/…` from the root, `../gradle/…` from the six nested builds. The inline `pluginManagement {}` blocks keep only their `includeBuild(...)` lines, so the shared file is the sole source of repositories and the mirror is unambiguously first regardless of where `apply` sits.

`net.yewton.petclinic.commons`' project-level `repositories {}` is left unchanged: it is a *project* convention (applied to `core:lib` / `fullstack-*:app` / the root project) and already a single declaration. The mirror endpoint then lives in exactly two files (`gradle/repositories.settings.gradle.kts`, `commons.gradle.kts`), down from fourteen.

This is `gradle/gradle`'s `mirrors.settings.gradle.kts` pattern. PetClinic *declares* a mirror repository rather than *rewriting* URLs the way Gradle does, because the GCS mirror is Central-only (portal artifacts need the `gradlePluginPortal()` fallback), which reads more clearly as a direct `repositories {}` block.

Verified on Gradle 9.7.1 with strict verification active: `apply(from = ...)` before the inline `pluginManagement {}` block carries `pluginManagement.repositories`; `:spotlessKotlinGradle`, `:core:spotlessKotlinGradle`, and `help` all pass (the Kotlin marker resolves from the mirror with the recorded checksum; `:core`'s ktlint detached config resolves from the shared `dependencyResolutionManagement.repositories`).

**Alternatives considered:**

- **Accept the inline duplication** (detekt's choice), improve the `// keep in sync` comments, close #183. Fully defensible — it is the ecosystem norm and the blocks are already correct and consistent after #189. Chosen against only because PetClinic's block is ~8 lines (vs. `mavenCentral()`'s one) so the duplication is more visible, and `apply(from = ...)` is a low-risk, Gradle-blessed way to remove it.
- **AndroidX's shared function** (`gradle/repos.gradle` exposing `addRepositories(RepositoryHandler)`, called in `pluginManagement {}` and `gradle.beforeProject {}`). More explicit than `apply(from = ...)` and could also replace `commons`' block. Rejected as more machinery than a declarative settings script for the same result.
- **A settings convention plugin for `dependencyResolutionManagement.repositories`** (jjohannes's model). Rejected — it cannot touch `pluginManagement.repositories`, so the shared file is needed anyway; adding a plugin on top removes nothing.
- **`verify-metadata=false` + PGP** — removes the marker-POM sensitivity entirely. Out of scope (see Non-Goals).

### Decision 4: record the topology in the `artifact-resolution` spec and `CLAUDE.md`

Code comments drift, and the load-bearing map here is non-obvious (it took two failed spikes to establish). The `artifact-resolution` capability spec states the invariants — cold resolution must not 429, mirror first, single declaration site, the two resolution contexts and what each serves — as scenarios. `CLAUDE.md`'s "Maven リポジトリ" note is trimmed to: the mirror-first rule, the two load-bearing files, why the marker POM checksum makes `pluginManagement.repositories` matter, and a pointer to the spec.

## Risks / Trade-offs

- **A `#195`-style change later alters which plugins leak to consumers** → the spec's scenarios and the `CLAUDE.md` note name the mechanism explicitly, so the next person has a place to check. The shared file makes any needed repository change a one-file edit.
- **`apply(from = file("<rel>/gradle/repositories.settings.gradle.kts"))` with a wrong relative path** → six of seven files use the identical `../gradle/…`; only the root differs. A smoke `./gradlew help` catches a typo immediately.
- **`apply(from = ...)` interaction with `pluginManagement`** → verified on Gradle 9.7.1 (the two `pluginManagement {}` invocations merge; the inline one declares no repositories, so order is unambiguous).
- **The GCS mirror lags a brand-new release** → `mavenCentral()` / `gradlePluginPortal()` fallback covers it; Renovate's `minimumReleaseAge: "3 days"` makes the window irrelevant here.
- **Spotless does not lint `gradle/repositories.settings.gradle.kts`** (it is under `gradle/`, not a source set) → check the Spotless `kotlinGradle` target and add `gradle/*.settings.gradle.kts` if missing, as `jjohannes` does with an explicit `target(...)`.

## Migration Plan

1. ~~#186, #187, #189, #190 merged; Renovate re-ran past the 429 and past dependency verification.~~ Done.
2. ~~Spike A and Spike B.~~ Done — and **falsified**; see Spike Results and Decision 1. #191 merged.
3. **PR A (this change)**: rewrite `design.md` / `proposal.md` / `specs` / `tasks.md` to the corrected findings and the centralize-don't-delete approach.
4. **PR B**: add `gradle/repositories.settings.gradle.kts`; replace the inline repository blocks in `settings.gradle.kts`, `core/`, `fullstack-html/`, `fullstack-htmx/`, `lint-logic/`, `build-logic/`, `build-logic-settings/` with `apply(from = ...)`; leave `commons.gradle.kts` alone; extend the Spotless target if needed. Verified with a full `./gradlew check` and an emptied-cache run — **not** targeted tasks.
5. **PR C**: `artifact-resolution` spec + `CLAUDE.md` trim.
6. Rollback: PR B is a single revert; PRs A and C are docs-only.

## Spike Results (FALSIFIED — retained for the record)

Run against `main` at `f1e4fa71e8` (before #195), Gradle 9.7.1, `--refresh-dependencies`. Both spikes concluded the application-build blocks were vestigial. **Both conclusions are wrong** — see Decision 1. What the spikes missed:

- **Spike A** (`pluginManagement.repositories` emptied in root / `core` / `fullstack-*`): the run compiled and passed because it ran `compileKotlin` / `buildEnvironment`, and pre-#195 the Kotlin plugin resolved without a marker POM. After #195, applying `net.yewton.petclinic.commons` re-resolves `org.jetbrains.kotlin.jvm.gradle.plugin-2.2.21.pom` in the consumer's `pluginManagement.repositories`, and the portal's copy fails the recorded checksum.
- **Spike B** (`dependencyResolutionManagement.repositories` removed in `core` / `fullstack-*`): the run passed because it never ran a `spotless*` task or `check`. `:core:spotlessKotlinGradle` resolves `com.pinterest.ktlint:ktlint-cli` in a detached configuration that falls back to the settings block, which the spike deleted.
- The `kotlin-build-tools-impl:2.4.20` verification failure the spike surfaced was a genuine pre-existing issue, handled separately in #192 / #193 / #195.

## Open Questions

None. The approach is Decision 3; implementation is PR B.

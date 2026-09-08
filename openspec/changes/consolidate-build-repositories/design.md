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

`build-logic-settings` depends on `lint-logic` because it applies `net.yewton.petclinic.spotless` to format its own `.settings.gradle.kts` files.

### How artifacts resolve today

| Resolution context | Repository list source |
|---|---|
| Application project dependencies (`core:lib`, `fullstack-*:app`) | project-level `repositories {}` in `net.yewton.petclinic.commons` — **authoritative**, because `RepositoriesMode.PREFER_PROJECT` (the default) makes a project's own repositories win and the settings `dependencyResolutionManagement` repositories be ignored for that project |
| `core` / `fullstack-*` settings `dependencyResolutionManagement.repositories` | consulted only for a project in that build that declares no repositories — there is none, since all three application projects apply `commons`. **Shadowed / vestigial (to be confirmed by spike).** |
| Infra plugin build compile + plugin classpath (`lint-logic:spotless`, `build-logic:*`, `build-logic-settings`) | that build's `pluginManagement.repositories` for the plugin/buildscript classpath (`kotlin-dsl` and its transitive `kotlin-gradle-plugin-api`, `kotlin-stdlib`, `gradle-kotlin-dsl-plugins`), and `dependencyResolutionManagement.repositories` for `implementation` dependencies — **both load-bearing** |
| root / `core` / `fullstack-*` settings `pluginManagement.repositories` | the `plugins {}` block resolves only `net.yewton.petclinic.foojay-resolver`, which comes from `includeBuild("build-logic-settings")` substitution and needs no repository. **Vestigial (to be confirmed by spike).** `lint-logic` already proves a build with no `pluginManagement` block works. |

### Why the HTTP 429 mitigation needed two rounds

`repo.maven.apache.org` returns HTTP 429 to shared CI and Renovate IPs; Gradle disables a repository on the first 429 rather than falling through, so a rate-limited repository cannot be listed first. PR #186 prepended the Google-hosted Maven Central mirror (`https://maven-central.storage-download.googleapis.com/maven2/`) to every `repositories` block it could see. It missed that `lint-logic` and `build-logic-settings` resolve their plugin classpath from `pluginManagement.repositories`, which those two builds never declared (defaulting to `gradlePluginPortal()` alone). Renovate's `--write-verification-metadata` walk from the root reaches `:lint-logic:spotless`'s plugin classpath and still hit 429 on `kotlin-gradle-plugin-api:2.4.0` (proxied by the portal to Maven Central). PR #189 added `pluginManagement.repositories` to those two builds.

The load-bearing repository configuration is therefore four places:

1. `net.yewton.petclinic.commons` — application project dependencies
2. `lint-logic` `pluginManagement.repositories` + `dependencyResolutionManagement.repositories`
3. `build-logic` `pluginManagement.repositories` + `dependencyResolutionManagement.repositories`
4. `build-logic-settings` `pluginManagement.repositories` + `dependencyResolutionManagement.repositories`

The other ~7 occurrences are candidates for deletion.

## Goals / Non-Goals

**Goals:**

- Prove which repository blocks are load-bearing and delete the rest.
- One edit site for the mirror endpoint used by application projects.
- A written contract (`artifact-resolution` spec) that a future repository change can be checked against.
- No regression of the 429 fix; no dependency version changes.

**Non-Goals:**

- A settings *convention plugin* for repositories (`net.yewton.petclinic.repositories`) or `RepositoriesMode.FAIL_ON_PROJECT_REPOS` enforcement. A precompiled settings plugin runs after the `plugins {}` block, so it cannot configure `pluginManagement.repositories` (the block that actually 429'd); and `lint-logic` / `build-logic-settings` could not apply it anyway (`build-logic-settings → lint-logic` would cycle). It would centralize only `dependencyResolutionManagement.repositories` for the application builds — which the spikes proved vestigial — so it removes nothing real while adding a plugin. `apply(from = ...)` of a shared settings script, by contrast, *can* supply `pluginManagement.repositories` (verified on Gradle 9.7.1: a plugin marker resolved from repos declared by the applied script), so that is the mechanism used instead — see Decision 2.
- Restructuring `build-logic-settings` to sit below `lint-logic`.
- Touching `libs/**` (outside the composite build, Renovate-ignored).
- Changing `renovate.json` `hostRules` (independent throttling, already in place).
- A Gradle init script `beforeSettings {}` — could configure `pluginManagement` for the whole tree, but needs `--init-script` on every invocation, which Renovate's fixed Gradle command cannot pass.

## Decisions

### Decision 1: Audit before centralizing

Run two spikes on a throwaway branch:

- **Spike A** — remove `pluginManagement { repositories { … } }` from root / `core` / `fullstack-html` / `fullstack-htmx` settings. Run `./gradlew help` and `./gradlew check --refresh-dependencies` (or at least a clean plugin-classpath resolution). Expectation: passes, because the only plugin resolved is `net.yewton.petclinic.foojay-resolver` via `includeBuild`.
- **Spike B** — remove `dependencyResolutionManagement { repositories { … } }` from `core` / `fullstack-html` / `fullstack-htmx` settings. Run `./gradlew check`. Expectation: passes, because all projects resolve through `commons`.

If a spike fails, that block is load-bearing and stays (documented). If it passes, the block is deleted.

**Alternative considered:** skip the audit, mechanically add a settings plugin over the existing blocks. Rejected — it leaves the vestigial blocks in place as permanent confusion and only removes 3–5 of ~13 sites. (Spike A + Spike B both passed — see Spike Results — so all seven vestigial blocks are deleted.)

### Decision 2: one shared `apply(from = ...)` settings script for the three infrastructure builds

After the spikes, only two contexts still need a repository declaration:

- the **infrastructure plugin builds** (`lint-logic`, `build-logic`, `build-logic-settings`) — both their `pluginManagement.repositories` (the `kotlin-dsl` plugin classpath) and their `dependencyResolutionManagement.repositories` (`spotless-plugin-gradle`, `spring-boot-gradle-plugin`, jOOQ, the foojay marker)
- the **application projects** — served by `net.yewton.petclinic.commons`' project-level `repositories {}`, already a single declaration

For the first, add `gradle/repositories.settings.gradle.kts` declaring both blocks directly:

```
pluginManagement { repositories { <mirror>; gradlePluginPortal() } }
dependencyResolutionManagement { repositories { <mirror>; gradlePluginPortal() } }
```

and, in each of the three infra `settings.gradle.kts`, replace the inline repository blocks with:

```
apply(from = file("../gradle/repositories.settings.gradle.kts"))
```

All three infra builds are one directory below the root, so the relative path is identical (`../gradle/...`) for all of them. The inline `pluginManagement {}` blocks keep only their `includeBuild(...)` lines, so the shared script is the sole source of repositories and the mirror stays first regardless of `apply` position.

`net.yewton.petclinic.commons` is left unchanged — the mirror endpoint then lives in exactly two files (`gradle/repositories.settings.gradle.kts` and `commons.gradle.kts`), down from ~14.

**Precedent:** this is Gradle's own pattern. `gradle/gradle` has `gradle/shared-with-buildSrc/mirrors.settings.gradle.kts`, applied via `apply(from = "gradle/shared-with-buildSrc/mirrors.settings.gradle.kts")` from `settings.gradle.kts` and `apply(from = "../gradle/shared-with-buildSrc/mirrors.settings.gradle.kts")` from `build-logic/settings.gradle.kts`. It configures `settings.pluginManagement.repositories`, project `repositories`, and `buildscript.repositories` from one file. The only difference: Gradle *rewrites* existing repository URLs to an internal Artifactory (a full transparent proxy) via a `gradle.settingsEvaluated {}` hook; PetClinic *prepends* a mirror repository because the GCS mirror is partial (plugin-portal-only artifacts still need the `gradlePluginPortal()` fallback), which reads more clearly as a direct `repositories {}` declaration.

**Alternative considered:** three inline copies of the block, each with a pointer comment. Rejected once `apply(from = ...)` was confirmed to work for `pluginManagement.repositories` — the shared file removes the duplication with the same relative path everywhere and matches Gradle's own build.

**Alternative considered:** `net.yewton.petclinic.repositories` settings convention plugin + `FAIL_ON_PROJECT_REPOS`, applied to the application builds, deleting `commons`' `repositories` block. Rejected — see Non-Goals. It cannot touch `pluginManagement.repositories`, so the infra builds would need the shared-file solution anyway; it would only centralize the application `dependencyResolutionManagement.repositories`, which the spikes proved vestigial; and moving the application repositories out of `commons` is an unverified change (Spike B only tested removing the settings block, not removing the `commons` block). The `FAIL_ON_PROJECT_REPOS` guard can be added later as a one-line follow-up if project-level repository drift ever becomes real.

### Decision 3: record the topology in the `artifact-resolution` spec, not inline in code

Code comments drift. The `artifact-resolution` capability spec states the invariants (mirror-first, both resolution contexts, cold resolution must not 429) as scenarios. `CLAUDE.md`'s existing "Maven リポジトリ" note is trimmed to point at the spec plus the load-bearing-vs-vestigial summary.

## Risks / Trade-offs

- **A deleted block turns out to be needed on a cold CI/Renovate runner** → the spikes ran with `--refresh-dependencies` (forces repository consultation) and reproduced no failure beyond two pre-existing ones. Land PR 1 (deletions) as a pure removal so a Renovate rebase exercises it before merge, and keep it a one-commit revert.
- **`apply(from = file("../gradle/repositories.settings.gradle.kts"))` with a wrong relative path** → all three infra builds are the same depth so the path is identical; a smoke `./gradlew help` catches a typo immediately.
- **Two `pluginManagement {}` blocks (shared file + inline `includeBuild`) do not merge as expected** → verified on Gradle 9.7.1 that they merge and that repository order follows declaration order; the inline blocks declare no repositories so the shared file is unambiguously first.
- **The GCS mirror lags a brand-new release** → `mavenCentral()` / `gradlePluginPortal()` fallback covers it; Renovate's `minimumReleaseAge: "3 days"` makes the window irrelevant here.
- **Mirror endpoint disappears / needs auth later** → after this change it lives in two files (`gradle/repositories.settings.gradle.kts`, `commons.gradle.kts`), down from ~14.

## Migration Plan

1. ~~Land nothing until PR #189 is merged and a Renovate PR has gone green.~~ Done — #186, #187, #189, #190 merged; Renovate re-ran and got past the 429 and past dependency verification (the remaining #174 failure is an unrelated Spring Boot 4.1 test break).
2. ~~Spike A and Spike B.~~ Done — see Spike Results; PR #191.
3. **PR 1**: delete the seven vestigial blocks (`pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx`; `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx`). Pure removal.
4. **PR 2**: add `gradle/repositories.settings.gradle.kts`; `apply(from = ...)` it from `lint-logic` / `build-logic` / `build-logic-settings`; delete their inline repository blocks (keep `includeBuild`). `commons.gradle.kts` untouched.
5. **PR 3**: `artifact-resolution` spec + `CLAUDE.md` trim.
6. Rollback: each PR is independently revertible; PR 1 is the only one with 429-regression risk and is a one-commit revert.

## Spike Results

Run locally against `main` at `f1e4fa71e8` (after #186, #187, #189, #190 merged), Gradle 9.7.1, with `--refresh-dependencies` to force repository consultation.

### Spike A — `pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx`: **VESTIGIAL**

Emptied the `repositories { }` block (kept `includeBuild` lines) in all four settings files. An empty block means zero repositories, not the `gradlePluginPortal()` default — any plugin needing download would fail hard.

- `./gradlew help` — pass (root `plugins { id("net.yewton.petclinic.foojay-resolver") }` resolves via `includeBuild`).
- `./gradlew :core:lib:compileKotlin :fullstack-html:app:compileKotlin :fullstack-htmx:app:compileKotlin :core:lib:buildEnvironment :fullstack-htmx:app:buildEnvironment --refresh-dependencies` — pass. `buildEnvironment` showed the plugin/buildscript classpaths (`kotlin-gradle-plugin-api`, `kotlin-allopen`, jOOQ) resolving entirely through `project ':build-logic:*'` / `project ':build-logic-settings'` substitutions, never a repository.

Conclusion: every plugin these four builds use arrives via `includeBuild` substitution. Their `pluginManagement.repositories` blocks can be deleted outright.

### Spike B — `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx`: **VESTIGIAL**

Removed the whole `dependencyResolutionManagement { repositories { … } }` block from the three build settings.

- `./gradlew :core:lib:jooqCodegen :core:lib:dependencies :core:lib:compileTestKotlin :fullstack-htmx:app:dependencies :fullstack-htmx:app:compileTestKotlin :fullstack-html:app:dependencies --refresh-dependencies` — the resolution paths all succeeded (`jooqCodegen` classpath, `runtimeClasspath`, test classpath, and Kotlin's detached build-tools configs all downloaded `from repository Maven Central Mirror` / `MavenRepo`, i.e. the repositories declared by `net.yewton.petclinic.commons` at project level).
- The run reported two failures, both **reproduced identically on unmodified `main`** with the same command:
  1. `java.nio.file.FileAlreadyExistsException` under `build/reports/dependency-verification/at-*/` — a Gradle race when several `dependencies` (`software-reporting-tasks`) tasks run in parallel and share the verification-report directory. Disappears when the `dependencies` tasks are run one at a time.
  2. `kotlin-build-tools-impl:2.4.20` fails dependency verification — the Kotlin plugin resolves the build-tools implementation through a detached configuration with a floating selector; `2.4.20` final was released after the `2.4.20-RC3` recorded in `gradle/verification-metadata.xml`, and `--refresh-dependencies` picks it up. A normal build uses the cached RC3 and passes. **This is a pre-existing latent issue on `main`, independent of this change** (see Open Questions).

Conclusion: `RepositoriesMode.PREFER_PROJECT` (the default) makes `commons`' project-level `repositories` authoritative for every project in these builds; the settings-level block is never consulted and can be deleted.

### Combined

Spike A + Spike B applied together (all seven blocks removed at once) produced no failure beyond the two pre-existing ones above. PR 1 can delete all seven in one change.

## Open Questions

- **Pre-existing, surfaced by Spike B — handed off, out of scope here**: `kotlin-build-tools-impl:2.4.20` is not in `gradle/verification-metadata.xml` (only `2.4.20-RC3` is). Any `--refresh-dependencies` run on `main` fails verification today. This is a dependency-verification / Kotlin-version concern, not a repository-declaration one, so it is being handled on its own branch (`fix/kotlin-build-tools-verification`) and is not part of `consolidate-build-repositories`. No action needed in this change.
- **Resolved** (`net.yewton.petclinic.repositories` plugin, `build-logic` as fifth consumer, merge with `foojay-resolver`): all moot. Decision 2 uses a shared `apply(from = ...)` settings script instead of a plugin. `build-logic` is one of the three infra builds that apply that script; there is no repositories plugin to merge with `foojay-resolver`, which stays as-is.
- **Resolved** (shared file vs. inline copies for the infra `pluginManagement.repositories`): shared file — `apply(from = "../gradle/repositories.settings.gradle.kts")` — after confirming on Gradle 9.7.1 that `apply(from = ...)` carries `pluginManagement.repositories`, and matching `gradle/gradle`'s own `mirrors.settings.gradle.kts` pattern.
- **Resolved** (`commons` repositories block): left in place. The mirror ends up in two files. Deleting it and making settings the single source would need its own verification and buys only `FAIL_ON_PROJECT_REPOS` enforcement, deferrable to a later one-line follow-up.

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

- Centralizing `pluginManagement.repositories`. It is evaluated before the `plugins {}` block, so no settings convention plugin and no `apply(from = ...)` can supply it. Only a Gradle init script's `beforeSettings {}` could, and that requires `--init-script` on every invocation, which Renovate's fixed Gradle command cannot pass. Ruled out.
- Restructuring `build-logic-settings` to sit below `lint-logic` (drop its self-formatting) so `lint-logic` could apply the settings plugin. The only gain would be `dependencyResolutionManagement` centralization for one more build; `pluginManagement` still could not be shared. Not worth the loss of formatting on that build.
- Touching `libs/**` (outside the composite build, Renovate-ignored).
- Changing `renovate.json` `hostRules` (independent throttling, already in place).

## Decisions

### Decision 1: Audit before centralizing

Run two spikes on a throwaway branch:

- **Spike A** — remove `pluginManagement { repositories { … } }` from root / `core` / `fullstack-html` / `fullstack-htmx` settings. Run `./gradlew help` and `./gradlew check --refresh-dependencies` (or at least a clean plugin-classpath resolution). Expectation: passes, because the only plugin resolved is `net.yewton.petclinic.foojay-resolver` via `includeBuild`.
- **Spike B** — remove `dependencyResolutionManagement { repositories { … } }` from `core` / `fullstack-html` / `fullstack-htmx` settings. Run `./gradlew check`. Expectation: passes, because all projects resolve through `commons`.

If a spike fails, that block is load-bearing and stays (documented). If it passes, the block is deleted.

**Alternative considered:** skip the audit, mechanically add a settings plugin over the existing blocks. Rejected — it leaves the vestigial blocks in place as permanent confusion and only removes 3–5 of ~13 sites.

### Decision 2: `net.yewton.petclinic.repositories` settings convention plugin for application builds

Host it in `build-logic-settings` beside `net.yewton.petclinic.foojay-resolver`. It configures:

```
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        <mirror>
        mavenCentral()
    }
}
```

Applied to root / `core` / `fullstack-html` / `fullstack-htmx` (the builds that already `includeBuild("build-logic-settings")` in `pluginManagement`, and that host application projects). `FAIL_ON_PROJECT_REPOS` makes the settings block the enforced single source; the `repositories {}` block is then removed from `net.yewton.petclinic.commons`.

**Alternative considered:** keep `commons` as the source and delete the settings blocks. Rejected — `commons` is a *project* convention plugin, so this concern would live at the wrong layer (repositories are a build-wide input, not a per-project one), and there would be no enforcement against a future project re-adding its own repositories.

**Alternative considered:** add `build-logic` as a fifth consumer of the plugin. Possible (no cycle: `build-logic → build-logic-settings → {lint-logic, platforms}`), but `build-logic` needs `gradlePluginPortal()` in `dependencyResolutionManagement` for portal-only plugin markers and the plugin would have to add it for everyone. Deferred to Open Questions.

### Decision 3: infra `pluginManagement.repositories` — shared snippet vs. copies

`lint-logic`, `build-logic`, `build-logic-settings` each need `pluginManagement { repositories { <mirror>; gradlePluginPortal() } }` and cannot get it from a plugin. Two options, decided during implementation after Spike A:

- **3a**: extract the block to `build-logic-settings/../gradle/plugin-repositories.settings.gradle.kts` (or similar) and `apply(from = file("<relative>/…"))` at the top of each infra settings file. One definition; relative paths differ per file.
- **3b**: three inline copies, each with a one-line comment pointing at the canonical explanation. Rarely changes; no relative-path fragility.

Leaning **3b** — three short copies of a block that changes once a year beat a fragile cross-build file include. Revisit if the block grows.

### Decision 4: record the topology in `build-environment` spec's neighbour, not inline in code

Code comments drift. The `artifact-resolution` capability spec states the invariants (mirror-first, both resolution contexts, cold resolution must not 429) as scenarios. `CLAUDE.md`'s existing "Maven リポジトリ" note is trimmed to point at the spec plus the load-bearing-vs-vestigial summary.

## Risks / Trade-offs

- **Spike A/B pass locally but a cold CI/Renovate run still needs a deleted block** → run the spikes with `--refresh-dependencies` and an emptied Gradle module cache; land the deletions in a single PR that a Renovate rebase will exercise before merge; keep the diff a pure revert if CI/Renovate 429s.
- **`FAIL_ON_PROJECT_REPOS` breaks a future contributor who adds `repositories {}` to a build file** → that is the intent; the failure message names the offending project and points to the settings plugin.
- **The GCS mirror lags a brand-new release** → `mavenCentral()` fallback covers it; Renovate's `minimumReleaseAge: "3 days"` makes the window irrelevant here.
- **`apply(from = ...)` (option 3a) with wrong `../` depth** → only a risk if 3a is chosen; a smoke `./gradlew help` catches it immediately.
- **Mirror endpoint disappears / needs auth later** → after this change the application-project edit is one file; infra builds are three files (or one with 3a). Down from ~13.

## Migration Plan

1. Land nothing until PR #189 (the second 429 fix) is merged and a Renovate PR has been rebased onto it and gone green — that confirms the load-bearing set.
2. Spike A and Spike B on a throwaway branch; record results in this document.
3. PR 1: delete the blocks the spikes proved vestigial. Small, pure removal.
4. PR 2: add `net.yewton.petclinic.repositories`, apply it to the four builds, set `FAIL_ON_PROJECT_REPOS`, delete `commons`' `repositories` block.
5. PR 3: infra `pluginManagement.repositories` — option 3a or 3b.
6. PR 4: `artifact-resolution` spec + `CLAUDE.md` trim.
7. Rollback: each PR is independently revertible; PR 1 is the only one with 429-regression risk and is a one-commit revert.

## Open Questions

- Does Spike A pass, or is `pluginManagement.repositories` in the application/root settings actually consulted for something (e.g. a detached configuration, `buildSrc`-style path)?
- Does Spike B pass, or does some configuration in `core`/`fullstack-*` (jOOQ codegen classpath, test fixtures) resolve through settings `dependencyResolutionManagement` rather than `commons`?
- Include `build-logic` as a fifth consumer of `net.yewton.petclinic.repositories` for its `dependencyResolutionManagement`, or leave it inline with `lint-logic`/`build-logic-settings`?
- Option 3a or 3b for the infra `pluginManagement.repositories`?
- Should `net.yewton.petclinic.foojay-resolver` and `net.yewton.petclinic.repositories` be merged into one `net.yewton.petclinic.settings` plugin (the name #183 proposed), or kept separate for single responsibility?

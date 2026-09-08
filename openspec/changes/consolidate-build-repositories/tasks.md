## 1. Confirm the load-bearing set (prerequisite)

- [x] 1.1 Wait until PR #189 is merged and a Renovate PR rebased onto it has passed CI and the `renovate/artifacts` check
- [x] 1.2 Spike A: remove `pluginManagement { repositories { … } }` from `settings.gradle.kts`, `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`; `--refresh-dependencies` compile + `buildEnvironment` — PASS, vestigial (see design.md Spike Results)
- [x] 1.3 Spike B: remove `dependencyResolutionManagement { repositories { … } }` from `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`; `--refresh-dependencies` resolution incl. `:core:lib:jooqCodegen` — PASS, vestigial; two pre-existing failures reproduced on clean `main` (see design.md Spike Results)
- [ ] 1.4 Resolve the remaining design Open Questions (spikes answered the first two): `build-logic` as a fifth consumer (yes/no), infra `pluginManagement.repositories` option 3a vs 3b, merge with `foojay-resolver` plugin (yes/no)

## 2. Delete vestigial blocks (PR 1)

Spikes proved all seven blocks vestigial: `pluginManagement.repositories` in root / `core` / `fullstack-html` / `fullstack-htmx`, and `dependencyResolutionManagement.repositories` in `core` / `fullstack-html` / `fullstack-htmx`.

- [ ] 2.1 Remove `pluginManagement { repositories { … } }` from `settings.gradle.kts`, `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts` (keep the `includeBuild` lines)
- [ ] 2.2 Remove `dependencyResolutionManagement { repositories { … } }` from `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts`
- [ ] 2.3 `./gradlew spotlessApply` then `./gradlew check --parallel --build-cache --configuration-cache`
- [ ] 2.4 Open PR; let a Renovate PR rebase onto it and confirm green before merge; the diff must be a clean revert if CI or Renovate 429s

## 3. Centralize application-build repositories (PR 2)

- [ ] 3.1 Add `build-logic-settings/src/main/kotlin/net.yewton.petclinic.repositories.settings.gradle.kts` configuring `dependencyResolutionManagement` with `repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS` and repositories `<mirror>` then `mavenCentral()`
- [ ] 3.2 Apply `id("net.yewton.petclinic.repositories")` in `settings.gradle.kts`, `core/settings.gradle.kts`, `fullstack-html/settings.gradle.kts`, `fullstack-htmx/settings.gradle.kts` (and `build-logic/settings.gradle.kts` if decided in 1.4)
- [ ] 3.3 Remove the inline `dependencyResolutionManagement { repositories { … } }` blocks now supplied by the plugin
- [ ] 3.4 Remove the `repositories {}` block from `build-logic/commons/src/main/kotlin/net.yewton.petclinic.commons.gradle.kts`
- [ ] 3.5 Verify `FAIL_ON_PROJECT_REPOS` triggers: temporarily add a `repositories {}` to an app build, confirm the configuration-time failure names the project, then revert
- [ ] 3.6 `./gradlew spotlessApply` then `./gradlew check`; confirm `:core:lib:jooqCodegen` still resolves

## 4. Infra plugin-classpath repositories (PR 3)

- [ ] 4.1 Apply the option chosen in 1.4 (3a: shared `apply(from = …)` snippet for `lint-logic`, `build-logic`, `build-logic-settings`; or 3b: three inline copies each with a pointer comment)
- [ ] 4.2 Emptied-cache `./gradlew check` to confirm the infra plugin classpaths still resolve mirror-first
- [ ] 4.3 `./gradlew spotlessApply` then `./gradlew check`

## 5. Documentation and spec (PR 4)

- [ ] 5.1 Add `openspec/specs/artifact-resolution/spec.md` (via `openspec` archive of this change, or hand-written if archiving later)
- [ ] 5.2 Trim `CLAUDE.md`'s "Maven リポジトリ" note to: mirror-first rule, load-bearing-vs-vestigial summary, pointer to the spec
- [ ] 5.3 Update `.github/CODEOWNERS` if `net.yewton.petclinic.repositories` needs an owner entry alongside `build-logic-settings/`
- [ ] 5.4 Confirm `gradle/verification-metadata.xml` is unchanged by the whole series

## 6. Close out

- [ ] 6.1 Link the PRs on issue #183 and note the reframed scope (audit + partial centralization; `pluginManagement` stays inline for three infra builds)
- [ ] 6.2 `openspec archive consolidate-build-repositories` once all PRs are merged

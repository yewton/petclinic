# Security Policy

## Supported Versions

This is a learning / demonstration project. Only the latest commit on the default branch is supported.

## Reporting a Vulnerability

**Please do not report security vulnerabilities via public GitHub issues.**

If you discover a security vulnerability, please open a [GitHub Security Advisory](https://github.com/yewton/petclinic/security/advisories/new) (private disclosure). Include:

- A description of the vulnerability and its potential impact
- Steps to reproduce or a minimal proof-of-concept
- Any suggested mitigations you have in mind

You can expect an acknowledgement within 7 days. We will keep you updated as we work on a fix.

## Supply Chain Security

This project applies the following measures to reduce supply chain risk:

| Measure | Implementation |
|---------|---------------|
| Dependency checksum verification | `gradle/verification-metadata.xml` — SHA-256 for every artifact used as a build input, in strict mode. See below for the IDE-only exceptions |
| Gradle distribution pinned to a version | `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` names an exact version. `validateDistributionUrl=true` makes the `wrapper` task check that a newly written URL is reachable; neither setting verifies the contents of the archive |
| Gradle wrapper JAR verified in CI | `gradle/actions/wrapper-validation` checks the wrapper JAR against known-good checksums |
| GitHub Actions pinned to commit SHA | Enforced by the repository's SHA pinning requirement; Renovate keeps the digests current |
| GitHub Actions restricted to an allow list | GitHub-owned actions, plus `gradle/*` and `mikepenz/action-junit-report@*` |
| Minimal CI token permissions | `permissions: {}` at workflow level; jobs grant only what they need |
| Automated dependency updates | Renovate bot with `config:best-practices` preset |
| Cooldown on dependency updates | `minimumReleaseAge` in `renovate.json` normally holds patch, minor and digest updates for three days from the release timestamp available to Renovate. Security updates bypass the hold; GitHub Actions digest updates are aged from the matching version's commit timestamp, not from when the digest changed |
| Vulnerability alerting | Renovate OSV alerts, and GitHub Dependabot alerts fed by the `dependency-graph` job in `ci.yml` together with the `submit` job in `dependency-graph-submit.yml` |
| Secret scanning | GitHub secret scanning with push protection |
| Private vulnerability reporting | Enabled, so the advisory link above works for reporters outside the project |

### Dependency verification metadata

`gradle/verification-metadata.xml` records a SHA-256 for every artifact the build
resolves. Verification runs in strict mode, so an artifact with no recorded
checksum fails the build. That makes the file an allow list as much as a
tamper check: a new version, a new transitive artifact or a new plugin cannot
enter the build until its checksum is recorded.

Renovate maintains the file. When it changes a version it re-runs Gradle with
`--write-verification-metadata` and commits the regenerated file alongside the
version change, so an update lands as one reviewable commit. This requires
Gradle to be able to resolve the project's Java 21 toolchain in Renovate's
environment, which is why `org.gradle.toolchains.foojay-resolver-convention` is
applied in the settings files — see below.

Renovate regenerates the file by running the `dependencies` task at the root.
That task only reports the configurations of the project it runs in, so the root
build and each convention plugin that aggregates a build have to forward it to
included builds and subprojects the same way they forward `check`. Without that
forwarding, Renovate's run succeeds while recording nothing, and the failure
only surfaces later as a verification error in CI.

One gap remains. Spotless resolves the ktlint runtime in a detached
configuration at execution time, and no `dependencies` task reports it. Those
checksums are recorded when `check` runs, and Gradle merges rather than replaces
entries on regeneration, so they survive. But if a Spotless upgrade changes the
ktlint version it depends on, the new artifacts are not recorded and CI fails on
verification. Recovery is to regenerate locally with the command in CLAUDE.md
and commit the result. This fails closed and is visible in CI rather than
silently accepting an unverified artifact.

Recording a checksum does not authenticate the artifact. The checksum is
whatever was published, so a version first adopted through an automated update
is trusted on first use. Signature verification (`verify-signatures` with
`trusted-keys`) would authenticate the publisher instead, at the cost of
maintaining a keyring and a trust decision per signing key.

### IDE-only artifacts

IntelliJ IDEA fetches sources and Javadoc archives while importing the project.
Under strict verification these downloads fail, because a normal build never
resolves them and so never records their checksums
([IDEA-258328](https://youtrack.jetbrains.com/issue/IDEA-258328)).

Those archives are not compile-time or runtime inputs, so `trusted-artifacts`
covers them by file-name pattern. Recording their checksums instead would mean
regenerating the metadata every time any dependency changes, because the IDE
fetches sources for the whole graph on demand rather than as part of a build.
POM files, Gradle Module Metadata and binary JARs stay under checksum
verification, including those resolved only to locate sources. Each rule is
scoped to a file-name pattern, and to component coordinates where that is
possible. If an artifact matching one of these rules ever becomes a build input,
the rule should be removed and its checksum recorded instead.

`--write-verification-metadata` rewrites this file from Gradle's own model and
drops XML comments, so the rationale lives here and only the `reason` attributes
stay in the file.

### Java toolchain provisioning

Applying the Foojay resolver means an environment without a matching JDK will
download one from the Foojay API and the vendor it points to. Gradle's
dependency verification does not cover toolchain downloads.

A download only happens where no Java 21 installation is present. CI installs
one with `actions/setup-java`, so nothing is downloaded there. Renovate's
environment does not provide one, which is what the resolver is there for. A
developer machine without Java 21 will download one as well; the build does not
require a locally installed JDK 21, so this is the expected path rather than an
edge case.

A JVM must already be present for any of this to work: Gradle 9 requires JVM 17
or later to run, and the resolver is a settings plugin, so it is evaluated far
too late to supply the JVM Gradle itself runs on. Declaring that JVM is tracked
as a separate change under `openspec/changes/pin-daemon-jvm/`.

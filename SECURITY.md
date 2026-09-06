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
verification, including those resolved only to locate sources, except for the
Groovy modules described below. Each rule is scoped to a file-name pattern, and
to component coordinates where that is possible. If an artifact matching one of
these rules ever becomes a build input, the rule should be removed and its
checksum recorded instead.

The Gradle Kotlin DSL tooling bundles Groovy, and IntelliJ IDEA resolves the
`org.apache.groovy` modules — Module Metadata, POMs and JARs — while building
the build-script model for editor support. No build resolves them: there are no
Groovy build scripts, and nothing in the `.gradle.kts` files or the version
catalog references Groovy. The set is the full Groovy distribution and its
version tracks whichever Groovy the current Gradle bundles, so each Gradle
upgrade would otherwise add a round of missing-checksum failures found only by
importing in the IDE. `trusted-artifacts` trusts the whole `org.apache.groovy`
group instead. If any build ever takes a Groovy artifact as a compile or runtime
input, remove the rule and record checksums.

`--write-verification-metadata` rewrites this file from Gradle's own model and
drops XML comments, so the rationale lives here and only the `reason` attributes
stay in the file.

### Java provisioning

Gradle uses two independently declared Java runtimes. The daemon JVM starts
Gradle itself; `gradle/gradle-daemon-jvm.properties` pins it to Java 21. The
project toolchain compiles the source and also requires Java 21. Daemon JVM
criteria match their declared version exactly. The CI workflows install the
same Java 21 version; update the criteria and each `actions/setup-java` Java version
together so CI does not need a separate daemon JVM. The declarations remain
separate because Gradle's runtime and the project's compiler toolchain have
different roles and can be changed independently when their requirements
diverge.

The generated criteria file contains Foojay redirect URLs. Linux `X86_64` is
the CI target; other generated platform entries require validation before they
are treated as supported. Run `./gradlew updateDaemonJvm --jvm-version=21`
whenever the required daemon Java version, vendor, native-image capability, or
supported platform set changes. Review the generated URLs as part of that
update.

Applying the Foojay resolver means an environment without the Java 21 project
toolchain downloads one from the Foojay API and the vendor it points to.
Gradle's dependency verification does not cover daemon or toolchain downloads.

A Java 21 download can occur where no matching installation is available and
toolchain auto-provisioning is enabled. Without auto-provisioning, Gradle fails
to start the daemon. Renovate and a developer machine without Java 21 must
therefore provide Java 21 or permit Gradle to provision it.

The Foojay resolver remains a settings plugin, so it is evaluated after the
Gradle daemon starts and cannot provide that daemon's JVM. The daemon criteria
file covers this earlier startup layer.

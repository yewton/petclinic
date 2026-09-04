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
| Pinned Gradle wrapper | `validateDistributionUrl=true` in `gradle/wrapper/gradle-wrapper.properties` |
| Gradle wrapper validation in CI | `gradle/actions/wrapper-validation` step |
| GitHub Actions pinned to commit SHA | Enforced by the repository's SHA pinning requirement; Renovate keeps the digests current |
| GitHub Actions restricted to an allow list | GitHub-owned actions, plus `gradle/*` and `mikepenz/action-junit-report@*` |
| Minimal CI token permissions | `permissions: {}` at workflow level; jobs grant only what they need |
| Automated dependency updates | Renovate bot with `config:best-practices` preset |
| Vulnerability alerting | Renovate OSV alerts, and GitHub Dependabot alerts fed by the `dependency-submission` job in CI |
| Secret scanning | GitHub secret scanning with push protection |
| Private vulnerability reporting | Enabled, so the advisory link above works for reporters outside the project |

### Why there is no dependency checksum verification

Gradle can verify every downloaded artifact against a checksum recorded in
`gradle/verification-metadata.xml`. This project does not do that. The reasoning
is recorded here so the decision can be revisited on its merits.

That mechanism detects an artifact being replaced *after* its checksum was
recorded. Dependencies here come from Maven Central and the Gradle Plugin
Portal, and neither allows a released version to be replaced, so the registries
already provide that guarantee.

What it does not cover is a maintainer's account being compromised and a
malicious *new* version being published. The checksum recorded when a version is
first adopted is the checksum of whatever was published, so an automated update
bot records the malicious artifact as trusted. That risk is addressed by
vulnerability alerting and by reviewing dependency updates.

Keeping the metadata current also required regenerating and committing it on
every dependency update. That broke automated updates and IntelliJ IDEA's
project import ([IDEA-258328](https://youtrack.jetbrains.com/issue/IDEA-258328)),
and automating the regeneration meant maintaining a custom validator that the
privileged CI job had to trust. The upkeep was not matched by the residual
protection.

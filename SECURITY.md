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
| Gradle distribution pinned to a version | `distributionUrl` in `gradle/wrapper/gradle-wrapper.properties` names an exact version; `validateDistributionUrl=true` checks that the URL is a Gradle distribution URL. Neither verifies the contents of the archive — see below |
| Gradle wrapper JAR verified in CI | `gradle/actions/wrapper-validation` checks the wrapper JAR against known-good checksums |
| GitHub Actions pinned to commit SHA | Enforced by the repository's SHA pinning requirement; Renovate keeps the digests current |
| GitHub Actions restricted to an allow list | GitHub-owned actions, plus `gradle/*` and `mikepenz/action-junit-report@*` |
| Minimal CI token permissions | `permissions: {}` at workflow level; jobs grant only what they need |
| Automated dependency updates | Renovate bot with `config:best-practices` preset |
| Vulnerability alerting | Renovate OSV alerts, and GitHub Dependabot alerts fed by the `dependency-graph` job in `ci.yml` together with the `submit` job in `dependency-graph-submit.yml` |
| Cooldown on automerged updates | `minimumReleaseAge` in `renovate.json` holds updates for three days after release |
| Secret scanning | GitHub secret scanning with push protection |
| Private vulnerability reporting | Enabled, so the advisory link above works for reporters outside the project |

### Why there is no dependency checksum verification

Gradle can verify every downloaded artifact against a checksum recorded in
`gradle/verification-metadata.xml`. This project does not do that. The reasoning
is recorded here so the decision can be revisited on its merits.

That mechanism detects an artifact being replaced *after* its checksum was
recorded. Dependencies here come from Maven Central and the Gradle Plugin
Portal, and both forbid replacing a released version, so the common case is
already covered by the registries' own policy.

Registry policy and cryptographic verification are not equivalent, so removing
the metadata does give up real coverage: a compromise of a registry, its CDN or
its storage, tampering at a TLS-terminating proxy, poisoning of a local or CI
Gradle cache, and any bug or policy violation in the registries' immutability
guarantees. What remains is the transport security of the download itself.

The largest thing given up is a gate rather than a detection. Strict
verification fails the build for *any* artifact with no recorded checksum, so a
new version, a new transitive artifact or a new plugin could not enter the build
until someone regenerated the metadata and committed it. It could not tell a
malicious version from a legitimate one — the recorded checksum is the checksum
of whatever was published — but it did stop anything new from being adopted
unattended. This project now automerges patch and minor updates and has no
required review, so new versions reach the default branch with no human in the
loop. That is a deliberate trade, and it is the difference most worth
remembering.

Vulnerability alerting does not fill that gap. It reports versions *already
known* to be vulnerable or malicious, so it helps after a problem has been
identified rather than before a version is merged. As a partial mitigation,
Renovate holds updates for three days after release (`minimumReleaseAge`), which
gives a malicious release some time to be reported or withdrawn before it is
merged here.

Keeping the metadata current required regenerating and committing it on every
dependency update. That broke automated updates and IntelliJ IDEA's project
import ([IDEA-258328](https://youtrack.jetbrains.com/issue/IDEA-258328)), and
automating the regeneration meant maintaining a custom validator that the
privileged CI job had to trust. For a project of this size the upkeep was not
matched by the residual protection; a project with a different threat model
should weigh this differently.

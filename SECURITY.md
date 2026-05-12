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
| Dependency checksum verification | `gradle/verification-metadata.xml` — SHA-256 for every resolved artifact |
| Pinned Gradle wrapper | `validateDistributionUrl=true` in `gradle/wrapper/gradle-wrapper.properties` |
| Gradle wrapper validation in CI | `gradle/actions/wrapper-validation` step |
| GitHub Actions pinned to commit SHA | All `uses:` entries include a commit SHA and version comment |
| Minimal CI token permissions | `permissions: {}` at workflow level; jobs grant only what they need |
| Automated dependency updates | Renovate bot with `config:best-practices` preset |

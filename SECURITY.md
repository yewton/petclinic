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
| Dependency checksum verification | `gradle/verification-metadata.xml` — SHA-256 for every artifact used as a build input (see below for the IDE-only exceptions) |
| Pinned Gradle wrapper | `validateDistributionUrl=true` in `gradle/wrapper/gradle-wrapper.properties` |
| Gradle wrapper validation in CI | `gradle/actions/wrapper-validation` step |
| GitHub Actions pinned to commit SHA | All `uses:` entries include a commit SHA and version comment |
| Minimal CI token permissions | `permissions: {}` at workflow level; jobs grant only what they need |
| Automated dependency updates | Renovate bot with `config:best-practices` preset |

### Gradle dependency verification metadata

`gradle/verification-metadata.xml` は、解決した依存成果物の SHA-256 checksum を記録する。CI は依存関係の更新時にこのファイルを再生成し、既存の座標に対応する checksum が変化していないことを確認する。同じ座標の成果物が差し替わった場合は、再公開や改竄の可能性があるためビルドを失敗させる。

IntelliJ IDEA は Gradle プロジェクトのインポート時にソースや Javadoc のアーカイブを遅延取得する。[IDEA-258328](https://youtrack.jetbrains.com/issue/IDEA-258328) の影響により、通常のビルドで生成した検証メタデータにこれらの checksum がない場合、IDE のインポートが strict dependency verification によって失敗する。

ソースと Javadoc のアーカイブはコンパイル時や実行時の入力に使用しないため、ファイル名パターンを限定して `trusted-artifacts` に登録する。Gradle ディストリビューションのソースは、ファイル名に加えて `gradle:gradle` のコンポーネント座標にも限定する。POM、Gradle Module Metadata、バイナリ JAR は、ソースの検索だけに使われるものも含めて引き続き checksum で検証する。

`trusted-artifacts` に一致する成果物をビルド入力として使うようになった場合は、該当する trust ルールを削除し、その成果物の checksum を記録する。trust ルールの追加、削除、変更は依存関係の検証方針に関わるため、明示的なレビューを必要とする。

詳細は Gradle の [dependency verification documentation](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:ignoring-javadocs-and-sources) を参照する。

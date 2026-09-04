## Why

このリポジトリは現在、Gradle の checksum ベースの依存関係検証を strict モードで利用しています。依存バージョンを更新するたびに `gradle/verification-metadata.xml` の checksum を更新する必要があり、依存関係更新の自動化と運用負荷の面で改善余地があります。

## What Changes

将来、依存関係検証を PGP 署名ベースへ移行する方法を調査・設計します。`verify-signatures` と `trusted-keys` を利用し、同じ鍵で署名された新バージョンについて検証メタデータの更新頻度を減らします。未署名の成果物には checksum 検証を併用します。

この change は移行方針の記録であり、今回の変更では署名検証を有効化しません。

## Impact

- armored 形式の `gradle/verification-keyring.keys` を生成し、管理する必要があります。
- group ごとに信頼する署名鍵を判断し、`trusted-keys` に登録する必要があります。
- `<key-servers enabled="false"/>` を設定し、検証時の鍵サーバー利用を無効化します。
- Gradle Plugin Portal のプラグインマーカーなど、署名されていない成果物には checksum 検証を残します。

## Reference

- [Gradle Dependency Verification](https://docs.gradle.org/current/userguide/dependency_verification.html)

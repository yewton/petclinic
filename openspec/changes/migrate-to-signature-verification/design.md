## Context

Gradle の checksum 検証は取得した成果物を特定の checksum と照合できますが、依存バージョンの更新ごとに検証メタデータの更新が必要です。PGP 署名検証では、信頼済みの同じ鍵で署名された新バージョンを、各バージョンの checksum を追加せずに検証できます。

## Proposed Design

1. 依存関係の署名鍵を調査し、採用する鍵だけを armored 形式で `gradle/verification-keyring.keys` に収録する。
2. Gradle の dependency verification 設定で `verify-signatures` と `trusted-keys` を有効にし、`<key-servers enabled="false"/>` を設定する。
3. group ごとに署名鍵の所有者・配布経路・ローテーション方針を確認し、信頼判断を記録する。
4. 署名のない成果物には checksum ルールを併用する。Gradle Plugin Portal のプラグインマーカーのように署名が付かない成果物も、checksum 検証の対象として残す。
5. CI で clean な依存関係検証を行い、署名済み・未署名の両方の代表的な成果物が期待どおり検証されることを確認してから checksum 依存を段階的に減らす。

## Non-Goals

- この change で署名鍵を選定・登録すること。
- checksum 検証を一括削除すること。
- 鍵サーバーから実行時に鍵を取得する構成を採用すること。

## Risks / Trade-offs

- 署名鍵の誤登録は、正規成果物の取得失敗または信頼境界の拡大につながる。group ごとの根拠を確認してから登録する。
- 署名されない成果物が残るため、署名検証だけでは全依存関係を保護できない。checksum 併用を前提にする。
- 鍵のローテーションや失効には運用手順が必要になる。

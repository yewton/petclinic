## 1. Keyring and Trust Decisions

- [ ] 1.1 依存関係ごとに署名の有無、署名者、配布元を調査する。
- [ ] 1.2 信頼する group と鍵を決定し、判断根拠とローテーション手順を記録する。
- [ ] 1.3 armored 形式の `gradle/verification-keyring.keys` を生成する。

## 2. Gradle Configuration

- [ ] 2.1 `verify-signatures`、`trusted-keys`、`<key-servers enabled="false"/>` を設定する。
- [ ] 2.2 未署名の成果物に対する checksum 検証を整理し、必要なルールを残す。

## 3. Verification and Rollout

- [ ] 3.1 署名済み成果物と未署名成果物の検証を CI で確認する。
- [ ] 3.2 鍵の追加・ローテーション・失効時の運用手順を文書化する。
- [ ] 3.3 checksum ベースの検証メタデータを段階的に縮小できるか評価する。

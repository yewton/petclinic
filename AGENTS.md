# AGENTS.md - PetClinic 開発ガイド

このリポジトリで作業するエージェントは、以下の手順と設定に従ってください。

## 必須コマンド
- **ビルド・検証**: `./gradlew check` (コミット前に必ずパスさせること)
- **フォーマット修正**: `./gradlew spotlessApply`
- **JOOQコード生成**: `./gradlew :petclinic-fullstack:app:jooqCodegen`
- **アプリ起動**: `./gradlew :petclinic-fullstack:app:bootRun`

## コーディング規約
- **スタイル**: `.editorconfig` の設定に基づき、Spotless (ktlint) で強制されます。
- **実装方針**: 既存のコード、プロジェクト構造、および `.editorconfig` を分析し、それらに合わせた実装を行ってください。
- **非同期処理**: Spring WebFlux および Kotlin Coroutines を活用してください。
- **テスト**: `WebTestClient` を使用し、正常系・異常系ともに AssertJ (`WithAssertions`) で検証してください。

## プロジェクト構造
- `petclinic-fullstack/app/`: 本体 (コード、テスト、DBマイグレーション)
- `build-logic/` & `lint-logic/`: ビルド・共通設定
- `references/`: 参照用リポジトリ群。外部の参考実装を submodule として配置しています。
  - `references/spring-petclinic`: 本プロジェクトのベースとなっている参照実装です。不足している機能の特定や、実装の参考にしてください。

## ワークフロー
1. 作業前にプロジェクト構造を把握する。不明点や不足機能の確認が必要な場合は、`references/spring-petclinic` を参照実装として確認する。
2. 実装後、`./gradlew spotlessApply` でコードを整える。
3. `./gradlew check` で全てのテストとチェックをパスすることを確認する。

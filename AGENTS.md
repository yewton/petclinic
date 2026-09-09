# AGENTS.md - PetClinic 開発ガイド

このリポジトリで作業するエージェントは、以下の手順と設定に従ってください。

## 必須コマンド
- **ビルド・検証**: `./gradlew check` (コミット前に必ずパスさせること)
- **フォーマット修正**: `./gradlew spotlessApply`
- **JOOQコード生成**: `./gradlew :core:lib:jooqCodegen`
- **アプリ起動**:
  - プレーンHTML版: `./gradlew :fullstack-html:app:bootRun`
  - HTMX版: `./gradlew :fullstack-htmx:app:bootRun`

## コーディング規約
- **スタイル**: `.editorconfig` の設定に基づき、Spotless (ktlint) で強制されます。
- **実装方針**: 既存のコード、プロジェクト構造、および `.editorconfig` を分析し、それらに合わせた実装を行ってください。
- **非同期処理**: Spring WebFlux および Kotlin Coroutines を活用してください。
- **テスト**: `WebTestClient` を使用し、正常系・異常系ともに AssertJ (`WithAssertions`) で検証してください。

## プロジェクト構造
- `core/`: 共有ライブラリの composite build。ドメインパッケージ、DBマイグレーション、JOOQ コード生成を含む
- `fullstack-html/`: Thymeleaf によるプレーンHTML版アプリケーションの composite build
- `fullstack-htmx/`: HTMX による部分レンダリング版アプリケーションの composite build
- `build-logic/` & `lint-logic/`: ビルド・共通設定
- `architecture/`: PetClinic を C4 モデル（softwareSystem → container → component）で表す LikeC4 project
- `references/`: 参照用リポジトリ群。外部の参考実装を submodule として配置しています。
  - `references/spring-petclinic`: 本プロジェクトのベースとなっている参照実装です。不足している機能の特定や、実装の参考にしてください。

## ワークフロー
1. 作業前にプロジェクト構造を把握する。不明点や不足機能の確認が必要な場合は、`references/spring-petclinic` を参照実装として確認する。
2. 実装後、`./gradlew spotlessApply` でコードを整える。
3. `./gradlew check` で全てのテストとチェックをパスすることを確認する。

`architecture/` は container の増減・依存方向の変更・外部 software system の変更・core の domain component の増減に合わせて更新する。Controller、Repository、エンティティ、テンプレートのみの変更では更新しない。詳細は `architecture-model` スキルを参照する。

## APM によるエージェント設定管理

Claude Code と Codex 向けの共有コンテキストは Microsoft APM で管理する。`.apm/` がスキル・コマンド・フックの正本であり、`.claude/`、`.codex/`、`.agents/skills/` は `apm install` の生成物である。

- エージェント設定を変更するときは `.apm/` を編集する。生成物を直接変更してはならない。
- 変更後は `apm install --force --target claude,codex,agent-skills` を実行して反映し、`apm audit` で整合性を確認する。
- `apm_modules/` はローカルキャッシュのためコミットしない。`apm.yml` と `apm.lock.yaml` はコミット対象である。

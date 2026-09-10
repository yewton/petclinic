# architecture-model Specification

## Purpose
PetClinic のリポジトリ構造を C4 モデルとして LikeC4 で記述し、エージェントと人間が参照できる単一の情報源として維持する。CI で構文を検証し、構造変更時にエージェントがモデルを追従更新する運用を定める。
## Requirements
### Requirement: アーキテクチャモデルの記述

リポジトリの構造は `architecture/` 配下の LikeC4 DSL で記述され、エージェントと人間が参照できる単一の情報源として維持される SHALL。

モデルは単一の LikeC4 project で C4 モデルの梯子 (person → software system → container → component) に従う SHALL。`petclinic` software system は、2 つのアプリケーション container (`fullstack-html` / `fullstack-htmx`)、data store container (PostgreSQL)、共有ライブラリ container (`core`) を含み、`core` 配下に各ドメインの component を持つ SHALL。両アプリケーションが `core` に依存し互いを参照しないこと、両者が機能同等の並行実装であること、外部の Grafana observability stack と OTLP パイプラインも含む SHALL。個々のクラス (Controller、Repository、エンティティ) は Code レベルとして含まない SHALL NOT。

ビュー階層は C4 の標準セット (System Context、Container、Component) を含む SHALL。

モデルは deployment model で Gradle composite build の構造を表す SHALL。`includeBuild` される各ビルド単位 (`core` / `fullstack-html` / `fullstack-htmx` / `platforms` / `lint-logic` / `build-logic` / `build-logic-settings`) と、ビルド単位間の `includeBuild` 依存を含み、専用の deployment view で可視化する SHALL。実行時の container である `core` / `fullstack-html` / `fullstack-htmx` は `instanceOf` で論理要素に対応づける SHALL。

`description` と関係ラベルは日本語で記述する SHALL。

#### Scenario: 並行実装の対応関係がモデルから読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** `fullstack-html` と `fullstack-htmx` が同じ機能を持つ並行実装であることが、関係またはメタデータとして表現されている

#### Scenario: 依存方向がモデルから読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** `fullstack-html` と `fullstack-htmx` がそれぞれ `core` に依存し、互いを参照しないことが表現されている

#### Scenario: 観測性のパイプラインが一箇所で読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** アプリケーションから OTLP で Alloy へ、Alloy から Tempo / Loki / Mimir へ、Grafana がそれらを参照する経路が一つのビューで表現されている

#### Scenario: C4 の標準ビューに必要な関係が存在する

- **WHEN** 開発者が `likec4 export json architecture` の結果を確認する
- **THEN** System Context、Container、各アプリケーションと `core` の Component view、observability / 並行実装の補助 view、および composite build 構造の deployment view に、孤立ノードなく期待する関係がある

#### Scenario: composite build の構造がモデルから読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** どのディレクトリが `includeBuild` される独立したビルド単位か、および `fullstack-html` / `fullstack-htmx` が `core` を dependency substitution で取り込むことが、`settings.gradle.kts` を読まずに deployment view から分かる

### Requirement: モデルの構文検証

`architecture/` 配下の LikeC4 記述は CI で構文とモデル内部の整合性が検証される SHALL。検証は既存の Gradle ビルドとは独立したジョブとして実行される SHALL。

#### Scenario: 不正な DSL が CI で検出される

- **WHEN** 構文またはモデル内部の整合性に問題のある `.c4` ファイルを含む変更が push される
- **THEN** CI の検証ジョブが失敗する

#### Scenario: モデルのみの変更で Gradle ビルドが走らない

- **WHEN** `.c4` ファイルのみを変更した pull request を作成する
- **THEN** LikeC4 の検証ジョブは実行され、Gradle のビルドジョブは実行されない

### Requirement: エージェントへのモデル更新指示

エージェントがモデルを更新すべき条件と更新が不要な条件は `.apm/` 配下のスキルとして定義され、`apm install` によって Claude Code、Codex、agent-skills の各ターゲットへ配布される SHALL。

#### Scenario: 構造の変更に伴いモデルが更新される

- **WHEN** エージェントが container の追加、container 間の依存方向の変更、外部 software system の追加・削除、`core` の domain component の増減、または composite build 構成の変更を伴う実装を行う
- **THEN** エージェントは対応する `.c4` ファイルを更新する

#### Scenario: 構造を変えない変更ではモデルが更新されない

- **WHEN** エージェントが Controller、Repository、エンティティ、またはテンプレートの追加のみを行う
- **THEN** エージェントは `.c4` ファイルを変更しない


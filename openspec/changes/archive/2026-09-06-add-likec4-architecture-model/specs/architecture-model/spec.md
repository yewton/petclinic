## ADDED Requirements

### Requirement: アーキテクチャモデルの記述

リポジトリの構造は `architecture/` 配下の LikeC4 DSL で記述され、エージェントと人間が参照できる単一の情報源として維持される SHALL。

モデルは composite build (`core` / `fullstack-html` / `fullstack-htmx`)、それらの依存方向、ドメインパッケージ、外部システム (PostgreSQL、Grafana スタック)、および観測性のパイプラインを含む SHALL。3 つの project model を import する `aggregate` project が system-level aggregate relation と全体 view の単一 owner である SHALL。Controller や Repository といったクラス単位の要素は含まない SHALL NOT。

#### Scenario: 並行実装の対応関係がモデルから読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** `fullstack-html` と `fullstack-htmx` が同じ機能を持つ並行実装であることが、関係またはメタデータとして表現されている

#### Scenario: 依存方向がモデルから読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** `fullstack-html` と `fullstack-htmx` がそれぞれ `core` に依存し、互いを参照しないことが表現されている

#### Scenario: 観測性のパイプラインが一箇所で読み取れる

- **WHEN** エージェントまたは開発者がモデルを参照する
- **THEN** アプリケーションから OTLP で Alloy へ、Alloy から Tempo / Loki / Mimir へ、Grafana がそれらを参照する経路が一つのビューで表現されている

#### Scenario: aggregate view に必要な関係が存在する

- **WHEN** 開発者が `likec4 export json architecture` の結果を確認する
- **THEN** aggregate の system-context、composite-build-dependencies、observability-pipeline、correspondence view に期待する関係があり、HTMX の correspondence view にも app-to-app 関係がある

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

- **WHEN** エージェントが composite build の追加、依存方向の変更、外部システムの追加・削除、またはドメインパッケージの増減を伴う実装を行う
- **THEN** エージェントは対応する `.c4` ファイルを更新する

#### Scenario: 構造を変えない変更ではモデルが更新されない

- **WHEN** エージェントが Controller、Repository、エンティティ、またはテンプレートの追加のみを行う
- **THEN** エージェントは `.c4` ファイルを変更しない

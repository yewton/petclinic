## Why

このリポジトリの構造には、ソースコードを読んでも分からない制約がいくつかあります。

- `fullstack-html` と `fullstack-htmx` は同じ機能を持つ並行実装であり、片方に機能を追加したらもう片方にも追加する。grep すれば「同名の Controller が 2 つある」ことは分かりますが、両者を等価に保つという規範はコードのどこにも書かれていません。
- composite build の依存は `core ← fullstack-html` と `core ← fullstack-htmx` の一方向で、`fullstack-html` と `fullstack-htmx` は互いを参照しません。`settings.gradle.kts` から現状は読み取れますが、「参照してはいけない」という制約としては表現されていません。
- 観測性のパイプライン (アプリ → OTLP → Alloy → Tempo / Loki / Mimir、および Grafana からの参照) は `docker-compose.yml`、`application.yml`、`OpenTelemetryAppenderInitializer.kt` に分散しており、全体像を得るには複数箇所を突き合わせる必要があります。

こうした構造をエージェントに渡す手段が現状はありません。散文で書いた場合に何が起きるかは、このリポジトリ自身が示しています。`AGENTS.md` は JOOQ codegen のコマンドを `:petclinic-fullstack:app:jooqCodegen` と記載し、プロジェクト構造を `petclinic-fullstack/app/` と説明していますが、実際のモジュール構成は `core` / `fullstack-html` / `fullstack-htmx` の 3 つの composite build であり、`petclinic-fullstack/` は git 管理外の残骸ディレクトリです。エージェントは今この記述から誤ったコマンドを読み取ります。

LikeC4 はアーキテクチャを DSL で記述し、そこから図の生成と検証を行うツールです。記述はテキストなので Git の差分に乗り、レビューと CI に載せられます。DSL は LLM が読み書きしやすい構文として設計されており、公式が DSL リファレンスの Agent Skill と、モデルを問い合わせる MCP サーバを提供しています。

導入のもう一つの目的は、より大規模なリポジトリに適用する前の試運転です。このリポジトリの規模に対して LikeC4 は過剰ですが、そのぶん構成を一通り試すコストが小さく、multi-projects 構成・エージェント連携・CI 検証といった要素を実物で確認できます。

## What Changes

`architecture/` ディレクトリに LikeC4 のモデルを追加し、エージェントがそれを参照・更新するワークフローを整えます。

- **モデル記述**: LikeC4 の multi-projects 構成を採り、`core` / `fullstack-html` / `fullstack-htmx` をそれぞれ独立した LikeC4 プロジェクトとして定義し、3 つを import する明示的な第 4 の `aggregate` プロジェクトを追加する。要素種別・タグ・スタイル、および外部システム (PostgreSQL、Grafana スタック) は `architecture/shared/` に置き、各プロジェクトから共有する。
- **粒度**: C4 の Container レベルまでを対象とし、Component はドメインパッケージ (`owner` / `pet` / `visit` / `vet` / `model`) 単位で止める。Controller や Repository といったクラス単位には降りない。
- **ビュー**: 全体の System Context、composite build の依存関係、`fullstack-html` と `fullstack-htmx` の対応関係、core のドメイン構成、観測性パイプラインを `aggregate` のビューとして定義する。aggregate が system-level aggregate edge の唯一の owner となり、個別アプリの views は app-level のローカル関係だけを表示する。HTMX の correspondence view は app-to-app edge を持つため zero-edge にならない。PostgreSQL の aggregate 直結は core 経由の system-level projection として文書化する。
- **エージェント連携**: `.apm/skills/` にモデルの更新トリガーを定めたスキルを追加し、`apm install` で Claude Code / Codex / agent-skills の各ターゲットに配布する。あわせて `@likec4/mcp` を MCP サーバとして登録する。
- **ツールチェーン**: ローカル実行用に `package.json` で `likec4` を devDependency として固定し、CI では digest-pinned `actions/setup-node`、`npm ci`、ローカルの `npx --no-install likec4 validate --json --no-layout architecture` を使う。
- **既存ドキュメントの修正**: `AGENTS.md` のモジュール構成と JOOQ codegen コマンドの記述を実態に合わせる。

## Capabilities

### New Capabilities
- `architecture-model`: リポジトリの構造を LikeC4 の DSL で記述し、エージェントと人間が参照できる単一の情報源として維持する。

### Modified Capabilities

## Impact

- 維持対象のファイルが増えます。モデルの更新が必要になるのは composite build の増減、依存方向の変更、外部システムの追加・削除、ドメインパッケージの増減であり、Controller や Repository の追加では更新不要です。この境界は `.apm/` のスキルに明記します。
- Node のツールチェーンがリポジトリに入ります。`package.json` と lockfile は Renovate の npm manager が扱い、Gradle の `gradle/verification-metadata.xml` とは独立しているため、既存の依存関係検証フローには影響しません。
- `likec4 validate` が検証するのは DSL 内部の整合性であり、モデルと実装の一致は保証しません。実装との乖離はレビューで検出することを前提とします。
- CI にジョブが 1 つ増えます。LikeC4 workflow は architecture の `.c4`、config、package/lock、workflow の変更を監視します。Gradle workflow 自体は常に起動して required check を pending にせず、push の全変更が `.c4` だけの場合だけ Gradle build step を skip します。
- About ページはモデル境界を変えない実装変更の proof-of-behavior として、HTML/HTMX の両方に Controller、template、navigation、messages、WebTestClient test を追加して検証します。
- Aggregate architecture views are validated with `likec4 export json architecture`; required view edge sets include PostgreSQL, core, observability, and both system/app correspondence relations.

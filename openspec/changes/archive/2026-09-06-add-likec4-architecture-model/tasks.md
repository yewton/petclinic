## 1. Set up the toolchain

- [x] 1.1 `package.json` を作成し、`likec4` を devDependency として追加して lockfile を生成する。
- [x] 1.2 `.gitignore` に `node_modules/` を追加する。
- [x] 1.3 `npx --no-install likec4 --version` が動作することを確認する。

## 2. Write the shared specification

- [x] 2.1 `architecture/shared/specification.c4` に要素種別を定義する。外部アクター、システム、アプリケーション、共有ライブラリ、ドメインコンポーネント、データストア、観測性コンポーネントを含める。
- [x] 2.2 タグとスタイルを定義する。`fullstack-html` と `fullstack-htmx` を視覚的に区別できるタグを含める。
- [x] 2.3 `architecture/shared/externals.c4` に外部システムを記述する。PostgreSQL と Grafana スタック (Alloy、Tempo、Loki、Mimir、Grafana) を含める。

## 3. Write the project models

- [x] 3.1 `architecture/core/` に `likec4.config.json` と `model.c4` を作成する。共有ライブラリと、ドメインパッケージ (`owner` / `pet` / `visit` / `vet` / `model`) をコンポーネントとして記述し、PostgreSQL への jOOQ over R2DBC の関係を書く。
- [x] 3.2 `architecture/fullstack-html/` に `likec4.config.json` と `model.c4` を作成する。Spring WebFlux と Thymeleaf によるアプリケーションと、外部アクターからの HTTP アクセス、OTLP による観測性への出力を記述する。
- [x] 3.3 `architecture/fullstack-htmx/` に同様の記述を作成する。HTMX による部分レンダリングを技術として明示する。
- [x] 3.4 `fullstack-html` と `fullstack-htmx` が同じ機能を持つ並行実装であるという制約を、関係またはメタデータとしてモデルに表現する。
- [x] 3.5 各 `likec4.config.json` の `include.paths` から `../shared` を参照させる。
- [x] 3.6 プロジェクトを跨ぐ関係 (アプリケーションから共有ライブラリへの依存) が表現できることを確認する。表現できない場合は design.md の記述に従って構成を見直し、判断を記録する。
- [x] 3.7 `architecture/aggregate/` に第 4 の LikeC4 project を作成し、3 つの project model を import する。aggregate model を system-level aggregate relation の唯一の owner とする。

## 4. Write the views

- [x] 4.1 aggregate の System Context view を作成する。外部アクター、PetClinic システム、PostgreSQL、Grafana スタックを含め、PostgreSQL direct relation を core 経由の system-level projection として文書化する。
- [x] 4.2 aggregate の composite build view を作成する。`core` への一方向の依存と、2 つのアプリケーションが互いを build/runtime 参照しないことが読み取れるようにする。
- [x] 4.3 aggregate の correspondence view と HTMX の app-to-app correspondence view を作成する。aggregate が system-level edge を所有し、HTMX view が zero-edge にならないことを export JSON で確認する。
- [x] 4.4 `core` のドメイン構成を示すビューを作成する。
- [x] 4.5 aggregate の観測性パイプライン view を作成する。アプリケーションから OTLP で Alloy へ、Alloy から Tempo / Loki / Mimir へ、Grafana がそれらを参照する経路を含める。
- [x] 4.6 `npx --no-install likec4 start architecture` で aggregate と各ローカルプロジェクトの全ビューを目視確認し、`validate --no-layout` はレイアウト差分を CI 判定から外す設計として記録する。
- [x] 4.7 `likec4 export json architecture` の aggregate/system-context、aggregate/composite-build-dependencies、aggregate/observability-pipeline、aggregate/correspondence、fullstack-htmx/correspondence の edge を確認する。
- [x] 4.8 aggregate/composite-build-dependencies から correspondence edge を除外し、HTML/HTMX の authored system-context views から PostgreSQL と未解決の app→PostgreSQL predicates を除外する。

## 5. Connect agents

- [x] 5.1 `.apm/skills/` にアーキテクチャモデルのスキルを作成する。モデルの更新が必要なトリガー (composite build の増減、依存方向の変更、外部システムの追加・削除、ドメインパッケージの増減) と、更新が不要なケース (Controller・Repository・エンティティ・テンプレートの追加) の両方を記述する。
- [x] 5.2 機能追加時に 2 つのアプリケーションの対応関係を確認する手順をスキルに含める。
- [x] 5.3 `apm.yml` の dependencies にスキルを追加する。
- [x] 5.4 `apm install --force --target claude,codex,agent-skills` を実行し、`apm audit` で整合性を確認する。
- [x] 5.5 `.mcp.json` に、repository root から相対パス `architecture` を読むローカル `@likec4/mcp` stdio サーバを登録し、Claude Code/Codex の client-specific workspace interpolation に依存しない構成にする。
- [x] 5.6 Codex の stdio 起動形態と同じ `npx --no-install @likec4/mcp architecture --stdio --no-watch` で 4 project（3 project と aggregate）のロードと MCP 起動ログを確認する。

## 6. Add CI validation

- [x] 6.1 digest-pinned `actions/setup-node`、`npm ci`、`npx --no-install likec4 validate --json --no-layout architecture` で検証するジョブを追加する。architecture の `.c4`、config、package、lockfile、workflow を paths に含める。
- [x] 6.2 Gradle workflow は常に起動して required check を pending にせず、commit object を `git cat-file` で確認できた変更一覧が `.c4` だけのときだけ build step を skip する。force-push や missing `github.event.before` は安全側で Gradle を実行する。
- [x] 6.3 意図的に不正な `.c4` を用意して CI が失敗することを確認し、確認後に元へ戻す。

## 7. Fix and document

- [x] 7.1 `AGENTS.md` のモジュール構成の記述を実態 (`core` / `fullstack-html` / `fullstack-htmx`) に合わせる。
- [x] 7.2 `AGENTS.md` の JOOQ codegen とアプリ起動のコマンドを実際のタスク名に修正する。
- [x] 7.3 `CLAUDE.md` に `architecture/` の位置づけと、モデルを更新すべき条件への参照を追記する。

## 8. Retrospective

- [x] 8.1 エージェントに 2 つのアプリケーションにまたがる機能追加を依頼し、対応関係を踏まえた実装になるか観察する。
- [x] 8.2 エージェントに実装の変更を依頼し、モデルを自発的に更新するか観察する。
- [x] 8.3 気づいたことを design.md の Retrospective に追記する。
- [x] 8.4 About ページの HTML/HTMX 両方の Controller、template、navigation、messages、WebTestClient test を proof-of-behavior として proposal/design に記録する。

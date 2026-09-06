---
name: architecture-model
description: PetClinic の composite build 構成、外部システム、ドメインパッケージを変更するときに LikeC4 アーキテクチャモデルを更新する手順。
---

# アーキテクチャモデルの更新

`architecture/` は、PetClinic の構造上の制約を LikeC4 DSL で表す情報源である。実装変更に以下のいずれかが含まれる場合は、対応する `.c4` ファイルを更新し、`npx --no-install likec4 validate --json --no-layout architecture` で検証する。

- composite build の追加・削除
- composite build 間の依存方向の変更
- 外部システムまたは観測性コンポーネントの追加・削除
- `core` のドメインパッケージの追加・削除

次の変更だけではモデルを更新しない。

- Controller の追加・変更
- Repository の追加・変更
- エンティティの追加・変更
- Thymeleaf テンプレートの追加・変更

## 並行実装を扱う手順

機能を追加または変更するときは、最初に `fullstack-html` と `fullstack-htmx` の両方に同じ利用者向け機能が必要か確認する。必要なら、両方のアプリケーションに対応する実装とテストを追加する。片方だけに適用する正当な理由がある場合は、その理由を変更の説明に記録する。

`fullstack-html` と `fullstack-htmx` は `core` に依存する並行実装であり、互いへの build または runtime dependency を追加しない。モデル上の対応関係は機能同等性を示すものであり、依存関係ではない。

## モデルの構成

- `architecture/shared/`: 要素種別、視覚スタイル、外部システム
- `architecture/core/`: 共有ライブラリとドメインパッケージ
- `architecture/fullstack-html/`: プレーン HTML アプリケーションとローカルビュー
- `architecture/fullstack-htmx/`: HTMX アプリケーションと対応関係ビュー
- `architecture/aggregate/`: 3 つのプロジェクトモデルを import する集約プロジェクトと全体ビュー

別プロジェクトの要素を参照するには LikeC4 の `import` を使う。`aggregate` は `core`、`fullstack-html`、`fullstack-htmx` の 3 プロジェクトを import し、system context、composite build、observability、aggregate correspondence の system-level 関係を単独で所有する。各アプリケーションの model は app-level のローカル関係を所有し、aggregate の system-level 投影を重複定義しない。

`fullstack-htmx/model.c4` は HTMX の app-level HTTP、core、OTLP 関係と、HTML app との app-level correspondence の定義場所である。`fullstack-html/model.c4` は HTMX の関係を再定義しない。HTMX のローカル views は app-level 関係だけを表示し、correspondence view も app-to-app edge を必ず含める。各アプリの authored system-context view は PostgreSQL を含めず、未解決の app→PostgreSQL predicate を置かない。PostgreSQL は実装上 core 経由だが、aggregate の system context ではアプリケーションから PostgreSQL への direct modeled relation を system-level projection として表示する。この意図を変えず、同じ aggregate edge を各プロジェクトの views にコピーしない。

## マルチプロジェクトの検証と目視確認

検証の起点は個別プロジェクトではなく `architecture/` ルートとする。ルートから実行すると、`aggregate` を含む 4 つの `likec4.config.json` と共有ファイル、プロジェクト間 import を同じワークスペースとして解決できる。

```sh
npx --no-install likec4 validate --json --no-layout architecture
```

`--no-layout` は CI の責務を DSL の構文・意味検証に限定するために付ける。レイアウトは Graphviz/wasm と実行環境に依存し、座標を成果物として管理していないため、レイアウト差分を CI の失敗条件にしない。見た目は `npx --no-install likec4 start architecture` で開発サーバーを起動し、`aggregate` の全体ビュー、`core` のドメインビュー、両アプリのローカルビューを確認する。特に aggregate の system-context、composite-build-dependencies、observability-pipeline、correspondence と HTMX の app-to-app correspondence が孤立せず、各 view に期待する edge があることをブラウザで確認する。

MCP は repository root を current working directory として起動する Claude Code/Codex の stdio 設定を対象にする。`.mcp.json` はクライアント固有の `${workspaceFolder}` 展開に依存せず、相対ワークスペース `architecture` とローカル npm パッケージを使う。手動の起動確認は次のコマンドで行う。

```sh
LIKEC4_WORKSPACE=architecture npx --no-install @likec4/mcp architecture --stdio --no-watch
```

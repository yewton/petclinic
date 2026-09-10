---
name: architecture-model
description: PetClinic の container 構成、外部システム、core のドメイン component、composite build 構成を変更するときに LikeC4 アーキテクチャモデルを更新する手順。
---

# アーキテクチャモデルの更新

`architecture/` は、PetClinic を C4 モデルで表す情報源である。単一の LikeC4 project で、要素は person → softwareSystem → container → component の梯子に従う。個々のクラス（Controller / Repository / エンティティ）は Code レベルとして対象外。

実装変更に以下のいずれかが含まれる場合は、対応する `.c4` ファイルを更新し、`npx --no-install likec4 validate --json --no-layout architecture` で検証する。

- container の追加・削除（アプリケーション、data store、共有ライブラリ）
- container 間の依存方向の変更
- 外部 software system または observability container の追加・削除
- `core` のドメイン component の追加・削除
- composite build（`includeBuild` されるビルド単位）の増減、または build 間の `includeBuild` 依存の変更 → `architecture/deployment.c4`

説明文（`description` と関係ラベル）は日本語で書く。

次の変更だけではモデルを更新しない。

- Controller の追加・変更
- Repository の追加・変更
- エンティティの追加・変更
- Thymeleaf テンプレートの追加・変更

## 並行実装を扱う手順

機能を追加または変更するときは、最初に `fullstack-html` と `fullstack-htmx` の両方に同じ利用者向け機能が必要か確認する。必要なら、両方のアプリケーションに対応する実装とテストを追加する。片方だけに適用する正当な理由がある場合は、その理由を変更の説明に記録する。

`htmlApp` と `htmxApp` は `core` に依存する並行実装であり、互いへの build または runtime dependency を追加しない。モデル上の `correspondence` 関係は機能同等性を示すものであり、依存関係ではない。

## モデルの構成

- `architecture/likec4.config.json`: project 定義
- `architecture/specification.c4`: 要素種別・関係種別・タグ・視覚スタイル
- `architecture/model.c4`: モデルツリー（`petOwner`、外部 `grafana`、`petclinic` software system とその container・component）
- `architecture/deployment.c4`: Gradle composite build の topology。各 `buildUnit` が `includeBuild` される独立したビルド単位。実行時 container の `core` / `htmlApp` / `htmxApp` は `instanceOf` で対応づける
- `architecture/views/context.c4`: System Context（C4 L1）
- `architecture/views/containers.c4`: Container（C4 L2）
- `architecture/views/components.c4`: Component（C4 L3。html / htmx / core）
- `architecture/views/supplementary.c4`: observability パイプライン、`grafana` 内部、並行実装の correspondence
- `architecture/views/build.c4`: `buildStructure` deployment view（composite build 構造）

`core` は C4 では共有ライブラリなので厳密には container ではないが、両アプリがコンパイル時に依存する独立したビルド単位なので `library` 種別（container の特化）として `petclinic` 内に置く。PostgreSQL は `db`（`database` 種別）で、`core -[persistence]-> db` を描く。両アプリから DB への直接エッジは張らない。

子要素を持つ要素には `view <name> of <element>` を 1 つ用意する。この view があると、他の view でその要素ノードにドリルダウン用のナビゲーションアイコンが出る。`petclinic` / `htmlApp` / `htmxApp` / `core` / `grafana` はいずれも専用 view を持つ。

## 検証と目視確認

```sh
npx --no-install likec4 validate --json --no-layout architecture
```

`--no-layout` は CI の責務を DSL の構文・意味検証に限定するために付ける。レイアウトは Graphviz/wasm と実行環境に依存し、座標を成果物として管理していないため、レイアウト差分を CI の失敗条件にしない。見た目は `npm run arch:start`（= `likec4 start architecture --listen 127.0.0.1 --port 5173`）で開発サーバーを起動し、`index`（System Context）、`containers`、`htmlComponents` / `htmxComponents` / `coreComponents`、`observability` / `grafanaStack` / `parallelImplementation`、`buildStructure`（composite build）の各 view に孤立ノードや欠けたエッジがないことをブラウザで確認する。

tailnet 経由で共有するときは、この開発サーバーに対して `tailscale serve --bg --http=5173 5173` を一度実行しておくと `http://<host>.<tailnet>.ts.net:5173/` で見られる（serve の設定は tailscaled 再起動をまたいで残る。開発サーバー自体は起動しておく必要がある）。

MCP は repository root を current working directory として起動する Claude Code / Codex の stdio 設定を対象にする。`.mcp.json` はクライアント固有の `${workspaceFolder}` 展開に依存せず、相対ワークスペース `architecture` とローカル npm パッケージを使う。手動の起動確認は次のコマンドで行う。

```sh
LIKEC4_WORKSPACE=architecture npx --no-install @likec4/mcp architecture --stdio --no-watch
```

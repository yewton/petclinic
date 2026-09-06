## Context

LikeC4 の記述は 3 つのブロックからなります。`specification` で使う要素種別・関係・タグ・色を定義し、`model` に実際の要素と階層と関係を書き、`views` で「モデルのどの部分をどう切り出すか」を述語で指定します。モデルが単一の情報源となり、モデルを変更すると全ビューが自動で更新されます。レイアウトは Graphviz の dot ベースで自動決定され、座標を手で置く操作はありません。

C4 model に着想を得ていますが、Context / Container / Component / Code の 4 階層を強制しません。要素の種類とネスト階層は `specification` に自分で定義します。この自由度は、C4 の抽象度を破った図も作れてしまうという裏返しを持ちます。

このリポジトリは Gradle と Kotlin で構成されており、`package.json` は存在しません。LikeC4 は npm パッケージであるため、何らかの形で Node のツールチェーンを持ち込む判断が必要になります。

## Decisions

### モデル化の粒度は composite build とドメインパッケージまで

要素の粒度は、対応する実装が変わったときにモデルの更新を要求する頻度を決めます。

| 粒度 | 変更頻度 | 採否 |
|---|---|---|
| composite build (`core` / `fullstack-html` / `fullstack-htmx`) | ほぼ変わらない | 採用 |
| ドメインパッケージ (`owner` / `pet` / `visit` / `vet` / `model`) | ほぼ変わらない | 採用 |
| Controller / Repository クラス | 機能追加のたびに変わる | 不採用 |
| Repository のメソッド | 頻繁に変わる | 不採用 |

ドメインパッケージは参照実装の Spring PetClinic に由来するドメイン区分であり、機能追加によって増減しません。クラス単位まで降りると `SpecialtyController` のような Controller を 1 つ追加するたびにモデルの更新が必要になり、更新漏れがそのままモデルと実装の乖離になります。パッケージで止めることで、Container レベルまでの網羅性と更新頻度の低さを両立させます。

### multi-projects 構成を採る

LikeC4 は `likec4.config.json` を置いたフォルダを 1 つのプロジェクトとして扱い、複数プロジェクトを併存させられます。プロジェクト間で仕様やスタイルを共有するには、config の `include.paths` に相対パスを列挙します。

このリポジトリの composite build 3 つに対して LikeC4 プロジェクトを 1 対 1 で対応させ、さらに 3 プロジェクトを横断する `aggregate` プロジェクトを置きます。個別プロジェクトはローカルな要素と app-level 関係を表し、aggregate が全体図の system-level 関係を一元的に所有します。

```
architecture/
  shared/
    specification.c4        要素種別・タグ・スタイル
    externals.c4            PostgreSQL、Grafana スタック
  core/
    likec4.config.json      name: "core", include: ["../shared"]
    model.c4
    views.c4
  fullstack-html/
    likec4.config.json      name: "fullstack-html", include: ["../shared"]
    model.c4
    views.c4
  fullstack-htmx/
    likec4.config.json      name: "fullstack-htmx", include: ["../shared"]
    model.c4
    views.c4
  aggregate/
    likec4.config.json      name: "aggregate", import: core/html/htmx
    model.c4                 system-level aggregate relations
    views.c4                  system context / dependencies / observability / correspondence
```

プロジェクトを跨ぐ関係は個別プロジェクトの app-level model と `aggregate` の system-level projection に分けて表現します。aggregate には `core`、`fullstack-html`、`fullstack-htmx` のモデルを `import` し、aggregate view で使う関係を aggregate model にだけ定義します。

#### プロジェクト間関係の所有

LikeC4 のプロジェクト import は要素参照を可能にする一方、関係を別プロジェクトの view へ自動的に持ち込みません。`fullstack-html/model.c4` と `fullstack-htmx/model.c4` は各 app の HTTP、core、OTLP 関係を所有し、HTMX model は HTML app との app-level correspondence も所有します。`aggregate/model.c4` は system-level の composite dependency、system context、observability、aggregate correspondence を唯一の所有者として定義します。PostgreSQL は実装では core が jOOQ/R2DBC で接続するため、core model に core→PostgreSQL がある一方、aggregate の system context ではアプリケーション→PostgreSQL の direct relation を「core 経由の system-level projection」として意図的にモデル化します。これにより各 aggregate view が非孤立の edge を持ち、同じ aggregate edge を HTML/HTMX の views にコピーしません。

#### 検証とレイアウトの判断

CI の検証単位は個別プロジェクトではなく `architecture/` ルートです。ルート起点なら 4 つの config、共有ファイル、プロジェクト間 import を一つの workspace として解決できます。

```sh
npx --no-install likec4 validate --json --no-layout architecture
```

`--no-layout` は意図的な選択です。Graphviz/wasm によるレイアウトは実行環境・依存バージョンの影響を受け、レイアウト座標を Git 管理していないため、CI では DSL の構文と意味整合性だけを安定して検証します。視覚的な確認は `npx --no-install likec4 start architecture` を起動してブラウザで行い、`aggregate` の全ビュー、`core` のドメインビュー、HTML/HTMX のローカルビューを確認します。各アプリの authored system-context では PostgreSQL を表示せず、aggregate の system-context だけで PostgreSQL の direct system-level projection を確認します。特に aggregate の system-context / composite-build-dependencies / observability-pipeline / correspondence と HTMX の app-to-app correspondence に edge があり、PostgreSQL が意図した direct system-level relation で接続され、zero-edge view や孤立したシステムがないことを確認します。

### ツールチェーンは package.json と GitHub Action の組み合わせ

| 方式 | バージョン固定 | Renovate | 採否 |
|---|---|---|---|
| `package.json` + lockfile | lockfile で固定 | npm manager が扱う | ローカル実行に採用 |
| `actions/setup-node` + `npm ci` + local `npx likec4 validate` | setup-node を digest 固定、npm lockfile で固定 | GitHub Actions/npm manager が扱う | CI に採用 |
| `npx likec4` のみ | 固定されない | 扱われない | 不採用 |
| Docker イメージ | digest で固定 | docker manager が扱う | 不採用 |

CI の `npx` は `npm ci` 直後に実行するため、package-lock に固定されたローカルバイナリだけを使い、ネットワークから未固定のパッケージを取得しません。このリポジトリの Renovate 設定は `minimumReleaseAge: 3 days` で更新を待機させており、固定されない依存はその方針と噛み合いません。Docker イメージは既存の `docker-compose.yml` と親和しますが、ローカルでのホットリロード付きプレビュー (`likec4 start`) を使う際の起動が重くなります。

`package.json` と lockfile は Gradle の `gradle/verification-metadata.xml` とは独立しているため、CLAUDE.md に記載されている依存関係検証メタデータの再生成手順には影響しません。

### ドリフト検出は `likec4 validate` のみ

`likec4 validate` が検証するのは DSL の構文とモデル内部の整合性であり、記述されたモデルと実装コードの一致ではありません。「`OwnerController` を削除したのにモデルに残っている」といった乖離は検出されません。

モデルと実装の一致を機械的に検証する手段としては、Vitest と LikeC4 API を使って独自の規約をテストとして書く方法 (公式ガイドの方式) と、コミュニティ製の drift detection ツール Erode があります。前者は Kotlin のリポジトリに Vitest とテストコードを持ち込む重さがあり、後者は何をどう検出するかが未調査です。

この change では両方を見送り、実装との乖離はレビューで検出することにします。モデル化の粒度を composite build とドメインパッケージに限定しているため、乖離が生じる頻度自体が低いという前提に立ちます。この前提が実際に守られないと分かった時点で、あらためて機械検証を検討します。

### MCP サーバを登録する

LikeC4 は `@likec4/mcp` パッケージまたは `likec4 mcp` コマンドで MCP サーバを起動し、要素検索・関係の探索・グラフ走査・ビュー描画などのツールを公開します。

このリポジトリのモデル規模であれば、エージェントは `.c4` ファイルを直接読むだけで全体を把握できるため、MCP がなくても目的は達成できます。それでも登録するのは、より大規模なリポジトリへ適用する際に MCP が前提となるためであり、この change の目的の一つがその試運転にあるためです。`.mcp.json` は Claude Code と Codex が repository root を current working directory として起動することだけを前提にし、`${workspaceFolder}` のクライアント固有展開を使わず、相対パス `architecture` と `npx --no-install @likec4/mcp architecture --stdio --no-watch` を指定します。検証は Codex の stdio 起動形態と同じ Node プロセスで行い、aggregate を含む 4 project のロードと `Starting MCP stdio server` ログを確認しました。

### エージェント向けの指示は `.apm/` に置く

このリポジトリではエージェント設定を Microsoft APM で管理しており、`.apm/` が正本で `.claude/` `.codex/` `.agents/skills/` は `apm install` の生成物です。モデルの更新トリガーを定めたスキルもこの規約に従い `.apm/skills/` に置き、`apm install --force --target claude,codex,agent-skills` で配布します。既存の `spring-boot-upgrade` スキルと同じ形です。

スキルには更新が必要なトリガーと、更新が不要なケースの両方を書きます。後者を明示しないと、Controller を 1 つ追加しただけでモデルを触ろうとする挙動を招きます。

## Non-Goals

- モデルと実装コードの一致を機械的に保証すること。上記のとおりレビューに委ねます。
- クラスレベル (C4 の Code レベル) までモデル化すること。
- 生成した図を GitHub Pages などで公開すること。`likec4 build` による静的サイト生成は将来の選択肢として残しますが、この change には含めません。
- PR ごとの図のプレビューを設けること。

## Risks / Trade-offs

- LikeC4 の multi-projects は要素 import と aggregate 側の system-level relation 定義を組み合わせる必要があります。aggregate と個別 project の relation ownership を混ぜると duplicate edge や孤立 view が生じるため、DSL 変更時は export JSON の view edges も確認します。
- Node のツールチェーンが Gradle 中心のリポジトリに加わり、依存の更新経路が 2 系統になります。Renovate が両方を扱うため運用上の追加作業は生じない見込みですが、リポジトリの構成としては複雑さが増します。
- モデルが実装と乖離した場合、エージェントは誤った構造を前提に実装やレビューを行います。散文のドキュメントが古くなる場合と比べて害が大きくなる可能性があり、粒度の判断がその防波堤になっています。
- `architecture/` はこのリポジトリの主目的である PetClinic の実装とは独立した成果物です。維持されなくなった場合、削除の判断を明示的に下す必要があります。
- Gradle workflow は workflow 自体を skip しません。push の変更一覧を job 内で判定し、`.c4` だけの push のときだけ Gradle build step を skip します。これにより architecture-only push でも required check が pending のまま残りません。

## Retrospective

About ページの追加をエージェントに依頼したところ、`architecture/` の並行実装の関係を参照し、`fullstack-html` と `fullstack-htmx` の両方に同じ Controller、テンプレート、ナビゲーション、メッセージ、WebTestClient テストを追加した。HTML/HTMX の各 About controller test が HTML レスポンスの見出しと説明文を検証するため、これはモデル境界を変えない機能追加の proof-of-behavior になっている。片方だけの実装にはならなかった。

この変更は Controller、テンプレート、メッセージ、テストだけで、composite build、依存方向、外部システム、ドメインパッケージを変えない。エージェントはモデルを変更せず、スキルで定義した更新境界に沿った。モデルを更新しない判断も、モデルを自発的に更新する判断と同じく重要である。

MCP サーバは 4 つの LikeC4 プロジェクト（3 つの実装 project と `aggregate`）を読み込み、`list-projects` 問い合わせに応答した。小規模なモデルでは `.c4` ファイルを直接読むだけでも把握できるが、プロジェクト横断の問い合わせを機械的に行えることを確認できた。

multi-projects の `import` はトップレベル要素の参照に利用できた。個別 project は app-level relation を保持し、`aggregate/model.c4` が system-level aggregate relation の唯一の source of truth になる構成にした。PostgreSQL は core の実際の接続を表す core→PostgreSQL と、全体図で意図した direct system-level projection の両方を使い分ける。HTMX の correspondence view は app-to-app edge を含み、aggregate の correspondence view は system-to-system edge を含むため、どちらも zero-edge にならない。

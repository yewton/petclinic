# petclinic

Spring Boot + Kotlin + R2DBC による Pet Clinic アプリケーション。`fullstack-html`（プレーン HTML）と `fullstack-htmx`（HTMX）の 2 つのアプリを含む。

## PR プレビュー環境

[Render](https://render.com) の Blueprint を使い、PR ごとに両アプリを自動デプロイする。

### セットアップ手順

1. Render アカウントを作成（無料、クレカ不要）
2. **Blueprints** → **New Blueprint Instance** を選択
3. このリポジトリ（`yewton/petclinic`）を接続
4. `render.yaml` が自動検出される → **Apply** をクリック
5. 初回に **primary サービス**（petclinic-html / petclinic-htmx）と **primary DB**（petclinic-db）が作成される
   - `autoDeploy: false` のため main ブランチへのプッシュでは自動デプロイされない
   - primary サービスは放置で構わない

以降は PR を open するたびに Render が自動でプレビュー環境を作成し、PR にコメントで URL を投稿する。

### プレビュー環境の構成

PR ごとに以下が作成される:

| リソース | 内容 |
|----------|------|
| `petclinic-html` (preview) | プレーン HTML 版 Web Service |
| `petclinic-htmx` (preview) | HTMX 版 Web Service |
| `petclinic-db` (preview) | PostgreSQL（スキーマ・シードデータ自動投入） |

PR をクローズすると、これらのリソースは自動削除される。7 日間非活動でも自動削除される（`expireAfterDays: 7`）。

### 注意事項

#### コールドスタート

Render Free の Web Service は **15 分間アクセスがないとスピンダウン**する。次のアクセス時に再起動が走り、**約 1 〜 2 分かかる**。最初のリクエストがタイムアウトに見えても、しばらく待ってからリロードすること。

#### Docker ビルド時間

プレビュー環境の初回ビルドは Gradle 依存関係のダウンロードを含むため、**5〜10 分程度かかる**場合がある。

#### Free Postgres の同時数制限

Render Free Postgres は **ワークスペースあたり 1 つまで**という制限がある可能性がある。複数 PR を同時に open すると 2 つ目以降の DB 作成が失敗するケースが報告されている。

この問題が発生した場合の対応案:

- **案 A**: 共有 DB に PR 番号でスキーマを分ける
- **案 B**: [Neon](https://neon.tech) に切り替える（無料枠あり、DB ブランチ機能で PR ごとに隔離 DB を作れる）

同時に複数 PR を open してみて問題が起きた場合は、どちらの案を採用するか判断する。

#### 本番デプロイについて

`render.yaml` で定義された primary サービスは `autoDeploy: false` のため、main ブランチをプッシュしても自動デプロイされない。本番デプロイが必要になった場合は render.yaml を修正すること。

## ローカル開発

```bash
# プレーンHTML版を起動（Docker Compose で PostgreSQL が自動起動する）
./gradlew :fullstack-html:app:bootRun

# HTMX版を起動
./gradlew :fullstack-htmx:app:bootRun
```

## ビルドとテスト

```bash
# フォーマット適用
./gradlew spotlessApply

# ビルド・テスト（CI と同じコマンド）
./gradlew check --parallel --warning-mode all --build-cache --configuration-cache

# ローカルで Docker イメージを作成（Cloud Native Buildpacks 使用）
./gradlew :fullstack-html:app:bootBuildImage
./gradlew :fullstack-htmx:app:bootBuildImage
```

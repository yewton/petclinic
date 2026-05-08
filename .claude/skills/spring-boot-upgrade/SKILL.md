---
name: spring-boot-upgrade
description: Spring Boot のバージョンを上げるときの手順。`platforms/gradle/libs.versions.toml` の `spring-boot-plugin` を変更する／Renovate からの spring-boot-gradle-plugin 更新 PR を取り込む／Spring Boot に追従して Kotlin・jOOQ・dependency-management-plugin を揃える、といった依頼で発火させる。
---

# Spring Boot アップグレード手順

本リポジトリは Spring Boot の dependency management を使うが、以下の依存は **その管理外** にあるため Spring Boot のバージョンを上げる際に手動で揃える必要がある。

- `org.jetbrains.kotlin:*`（`platforms/gradle/libs.versions.toml` の `kotlin`）
- `org.jooq:*`（同 `jooq`）
- `io.spring.gradle:dependency-management-plugin`（同 `[libraries]` セクション）
- `org.springframework.boot:spring-boot-gradle-plugin` 自身（同 `[libraries]` セクション、`spring-boot-plugin`）

これらは `renovate.json` の `packageRules` で Renovate 対象外にしてある（誤って単独昇格されないため）。Renovate が出すのは `spring-boot-gradle-plugin` の更新 PR のみで、それを起点に本 Skill の手順で他を合わせる。

なお `OpenTelemetry Instrumentation`（`opentelemetry = "..."`）は Spring Boot 管理外かつ独立した BOM のため、本手順の対象外。`libs/**` も Renovate と同様に対象外。

## 手順

### 1. 対象 Spring Boot バージョンの BOM から正解値を取り出す

`spring-boot-dependencies-<ver>.pom` を Maven Central から取得し、4 つのプロパティを抽出する:

```bash
VER=3.5.14  # 上げたい先のバージョン
curl -sS "https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/${VER}/spring-boot-dependencies-${VER}.pom" \
  | grep -E "kotlin\.version|jooq\.version|dependency-management-plugin\.version"
```

得られた `kotlin.version` / `jooq.version` / `dependency-management-plugin.version` がそれぞれ目標値。

### 2. `platforms/gradle/libs.versions.toml` を更新

- `[versions]` の `kotlin` を BOM の `kotlin.version` に
- `[versions]` の `jooq` を BOM の `jooq.version` に
- `[libraries]` の `spring-boot-plugin` の座標末尾バージョンを `<VER>` に
- `[libraries]` の `dependency-management-plugin` の座標末尾バージョンを BOM の `dependency-management-plugin.version` に

ファイル冒頭のコメント（`Spring Boot の依存バージョンに合わせる`）はそのまま維持する。

### 3. リリースノートのレビュー

`https://github.com/spring-projects/spring-boot/compare/v<from>...v<VER>` を見て、特に以下を点検する:

- **Noteworthy / Breaking changes** セクション
- 本プロジェクトが使う機能に関連する修正:
  - WebFlux / Netty
  - R2DBC（PostgreSQL）
  - jOOQ
  - Testcontainers（`spring-boot-testcontainers`、`org.testcontainers:postgresql` / `:r2dbc` / `:junit-jupiter`）
  - `spring-boot-docker-compose`
  - Micrometer / Micrometer Tracing（OTel ブリッジ）
  - Thymeleaf
  - Spring Security（依存に入っていないが、追加検討時のため）

非互換変更が見つかった場合は、対応方針を計画化し、必要な追従コードを併せて変更する。

### 4. 検証

```bash
./gradlew spotlessApply
./gradlew :petclinic-fullstack:app:jooqCodegen   # src/main/jooq に差分が出ないこと
git diff --stat petclinic-fullstack/app/src/main/jooq
./gradlew check --parallel --warning-mode all --build-cache --configuration-cache
./gradlew :petclinic-fullstack:app:bootRun       # local プロファイルで起動・OTel 連携確認
```

`jooqCodegen` の差分が出た場合は、jOOQ ジェネレータの出力フォーマット変更の可能性が高い。差分を精査し、生成物をコミットに含める。

### 5. コミットの分割（推奨）

レビュアビリティのため、単一 PR でも以下のように粒度を分ける:

1. `chore(deps): align kotlin/jooq/dependency-management-plugin with Spring Boot <VER>`
2. `chore(deps): update spring-boot-gradle-plugin to <VER>`
3. （リリースノート対応で必要になったコード変更があれば）追従コミット
4. `chore(jooq): regenerate sources` （`jooqCodegen` で差分が出た場合のみ）

## 注意点・落とし穴

- **`spring-boot-gradle-plugin` と `dependency-management-plugin` は別々に宣言されている**（`build-logic/dependency-management/build.gradle.kts`、`build-logic/spring-boot/build.gradle.kts`）。`platforms/plugins-platform/build.gradle.kts` の `constraints` を経由してカタログのバージョンに固定されるので、変更点はあくまで `libs.versions.toml` のみ。build-logic 側を直接編集する必要はない。
- **Kotlin プラグインバージョンを上げる場合**は、Spring Boot のマネージド版に合わせる。それより新しい版に上げたいという別要件があれば、本 Skill のスコープを超えるため別タスクで扱う。
- **jOOQ を Spring Boot のマネージド版より新しくしてはいけない**（過去にコミット `7261eb5` で誤って 3.20.11 まで上げてしまった事故あり）。`renovate.json` の `packageRules` で防いでいる。
- マイナー以上の昇格（例: 3.5 → 3.6, 3 → 4）では、Spring Framework / Reactor / Kotlin などの **メジャー or マイナー昇格** を伴うことがある。その場合はリリースノートの "Upgrade Notes" / migration guide を必ず読み、JDK 要件・廃止 API・設定プロパティ名変更を点検する。

## Context

Gradle の JVM は 2 つの層に分かれています。

1. **daemon を動かす JVM。** Gradle 9 では 17 以上 26 以下が必要です。ビルドを起動する時点で環境に存在しているか、Daemon JVM criteria によって client が取得する必要があります。
2. **プロジェクトをコンパイルする toolchain。** このリポジトリでは Java 21 を要求します。`build-logic/commons` が生成する規約プラグインが `JavaLanguageVersion.of(21)` を宣言しているためです。こちらは foojay resolver によって自動取得されます。

`build-logic` 自身は toolchain を宣言せず、`kotlin-dsl` プラグインに従って daemon の JVM でコンパイルされます。そのため daemon が JVM 17 であっても build-logic のコンパイルは成立します。実測でも、ビルド成果物を持たないクリーンな作業ツリーで JDK 17 を起動 JVM として完走しています。

## Proposed Design

1. daemon に要求する JVM バージョンを決める。プロジェクト toolchain と揃えて 21 にするか、Gradle の最低要件である 17 に留めるかを判断する。揃えると daemon 用の取得がそのまま toolchain にも使える可能性があり、分けると daemon 用 JVM の更新頻度を下げられる。
2. `./gradlew updateDaemonJvm --jvm-version=<N>` で `gradle/gradle-daemon-jvm.properties` を生成する。
3. 生成された `toolchainUrl.<OS>.<ARCH>` が、想定するプラットフォームを網羅しているか確認する。少なくとも CI が使う Linux x86_64 と、開発環境で使うプラットフォームを含める。
4. JVM 17 未満の環境を模して、daemon 用 JVM が実際に取得されることを確認する。ローカルでは起動 JVM を切り替え、ビルド成果物を持たない作業ツリーで検証する。
5. IntelliJ IDEA のインポートがこのファイルの影響を受けないことを確認する。IDE は独自の JVM 設定を持つため、意図しない競合が起きないか見る。
6. Renovate の実行時間が許容範囲に収まることを確認する。daemon 用とプロジェクト toolchain 用で 2 回の取得が発生する場合の所要時間を測る。

## Non-Goals

- foojay resolver を置き換えること。両者は担当する層が異なり、併存します。
- 取得する JDK に checksum 検証を導入すること。Gradle の dependency verification は toolchain の取得を対象としていません。

## Risks / Trade-offs

- `toolchainUrl` はプラットフォームごとの URL を焼き込むため、記録が古くなると取得に失敗します。失敗は明示的なエラーになるので無言の破損ではありませんが、更新の手間は残ります。
- Renovate 環境で JDK の取得が 2 回発生すると、実行時間が延びてタイムアウトに近づきます。導入前に所要時間を測る必要があります。
- 導入しない場合に残るリスクは、Renovate の実行環境や開発者の環境で既定の Java が 17 未満になった場合にビルドが起動しないことです。この場合のエラーは `Gradle requires JVM 17 or later to run.` と明示的で、原因の特定は容易です。

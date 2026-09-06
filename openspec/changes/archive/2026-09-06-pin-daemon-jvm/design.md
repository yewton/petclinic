## Context

Gradle の JVM は 2 つの層に分かれています。

1. **daemon を動かす JVM。** Gradle 9 は Java 17 以上を必要とします。この change では criteria に Java 21 を記録します。criteria は最低バージョンではなく正確なバージョンを照合するため、Java 21 がない環境では daemon 用 JVM の取得または起動失敗が起こりえます。
2. **プロジェクトをコンパイルする toolchain。** このリポジトリでは Java 21 を要求します。`build-logic/commons` が生成する規約プラグインが `JavaLanguageVersion.of(21)` を宣言しているためです。こちらは foojay resolver によって自動取得されます。

`build-logic` 自身は toolchain を宣言せず、`kotlin-dsl` プラグインに従って daemon の JVM でコンパイルされます。criteria 導入後も、JVM 17 未満の起動環境から Java 21 daemon を取得できることをクリーンな作業ツリーで検証します。

## Proposed Design

1. daemon には Java 21 を要求し、プロジェクト toolchain と揃える。Daemon JVM criteria は最低バージョンではなく正確なバージョンを照合するため、CI が `actions/setup-java` で用意する Java 21 を再利用できる見込みである。この挙動は CI で検証する。daemon の実行要件と project toolchain のコンパイル要件は別の宣言として維持し、将来要件が分岐したときに個別に変更できる。
2. `./gradlew updateDaemonJvm --jvm-version=21` で `gradle/gradle-daemon-jvm.properties` を生成する。
3. daemon criteria の version を変更するときは、CI workflow の各 `actions/setup-java` の Java version も同時に変更する。両者が乖離すると CI の daemon JVM download または起動失敗を招く。
4. 生成された `toolchainUrl.<OS>.<ARCH>` が、想定するプラットフォームを網羅しているか確認する。少なくとも CI が使う Linux x86_64 と、開発環境で使うプラットフォームを含める。
5. JVM 17 未満の環境を模して、daemon 用 JVM が実際に取得されることを確認する。ローカルでは起動 JVM を切り替え、ビルド成果物を持たない作業ツリーで検証する。
6. IntelliJ IDEA のインポートがこのファイルの影響を受けないことを確認する。IDE は独自の JVM 設定を持つため、意図しない競合が起きないか見る。
7. Renovate の実行時間が許容範囲に収まることを確認する。Java 21 が存在しない場合の daemon と project toolchain の取得経路を含めて所要時間を測る。

## Non-Goals

- foojay resolver を置き換えること。両者は担当する層が異なり、併存します。
- 取得する JDK に checksum 検証を導入すること。Gradle の dependency verification は toolchain の取得を対象としていません。

## Risks / Trade-offs

- `toolchainUrl` はプラットフォームごとの URL を焼き込むため、記録が古くなると取得に失敗します。失敗は明示的なエラーになるので無言の破損ではありませんが、更新の手間は残ります。
- Java 21 がない環境では daemon と project toolchain の取得が発生し、Renovate の実行時間が延びてタイムアウトに近づく可能性があります。auto-provisioning が無効な環境では明示的な起動失敗になります。
- `updateDaemonJvm` は FreeBSD/Unix の criteria も出力しますが、Linux URL と同一の値になります。CI target 以外は動作確認まで support を主張しません。

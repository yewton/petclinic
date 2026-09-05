## Why

Gradle 9 を実行するには JVM 17 以上が必要で、その JVM はビルドを起動する時点で環境に存在していなければなりません。`org.gradle.toolchains.foojay-resolver-convention` はこれを肩代わりできません。settings プラグインとして評価されるのはビルドの設定が始まった後であり、daemon が起動できない時点ではまだ動いていないためです。

実測でこの境界を確認しています。ビルド成果物を持たないクリーンな作業ツリーで、ローカル JDK の自動検出を無効化して次を実行しました。

| 起動時の JVM | 結果 |
|---|---|
| 無し | `ERROR: JAVA_HOME is not set and no 'java' command could be found` |
| Java 11 | `Gradle requires JVM 17 or later to run.` で失敗。JDK のダウンロードは発生しない |
| Java 17 | 成功。プロジェクトの toolchain である JDK 21 を foojay 経由で取得して完走 |

Gradle の [Daemon JVM criteria](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:configuring_daemon_jvm) はこの領域を扱います。`gradle/gradle-daemon-jvm.properties` に必要な JVM バージョンとプラットフォームごとのダウンロード URL を記録しておくと、Gradle の client が daemon を起動する前にその JVM を取得します。settings の評価より前に解決されるため、foojay では届かない範囲を埋められます。

現時点で実害はありません。CI は `actions/setup-java` で JDK 21 を用意しており、Renovate の実行環境も JVM 17 以上を満たしています。過去に Renovate が失敗した際も daemon は正常に起動しており、失敗したのはプロジェクト toolchain の解決でした。

## What Changes

`gradle/gradle-daemon-jvm.properties` を導入し、daemon を起動する JVM をリポジトリ側で宣言します。

- `./gradlew updateDaemonJvm --jvm-version=<N>` でファイルを生成する。生成されるのは `toolchainVersion` と、プラットフォームごとの `toolchainUrl.<OS>.<ARCH>` です。
- URL の解決には設定済みの toolchain download repository が必要です。foojay resolver は既に適用済みのため、この前提は満たしています。
- 生成した内容が CI、ローカル、IntelliJ IDEA、Renovate のいずれでも機能することを確認します。

## Capabilities

### New Capabilities
- `build-environment`: ビルドを実行する JVM をリポジトリ側で宣言し、環境ごとの差異に依存しないようにします。

### Modified Capabilities

## Impact

- 維持対象のファイルが 1 つ増えます。`toolchainUrl.<OS>.<ARCH>` はプラットフォームごとに記録されるため、対応プラットフォームを増減させるときや JVM バージョンを変えるときに `updateDaemonJvm` の再実行が必要になります。Renovate がこのファイルを管理する保証はありません。
- JVM 17 以上を持たない環境では、daemon 用の JDK がダウンロードされます。Renovate の実行環境では daemon 用とプロジェクト toolchain 用で最大 2 回の取得が発生しうるため、実行時間が延びてタイムアウトに近づく可能性があります。
- 取得される JDK は Gradle の dependency verification の対象外です。`toolchainUrl` に記録される URL が実質的な固定先になります。

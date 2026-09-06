## 1. Decide the daemon JVM version

- [x] 1.1 daemon に要求する JVM バージョンを決め、プロジェクト toolchain (Java 21) と揃えるかどうかの判断根拠を記録する。
- [x] 1.2 daemon criteria の Java version と CI の各 `actions/setup-java` の Java version を同時に更新する必要があることを記録する。

## 2. Generate the criteria file

- [x] 2.1 `./gradlew updateDaemonJvm --jvm-version=21` で `gradle/gradle-daemon-jvm.properties` を生成する。
- [x] 2.2 生成された `toolchainUrl.<OS>.<ARCH>` が、CI の Linux `X86_64` と開発環境で使うプラットフォームを網羅しているか確認する。

## 3. Verify across environments

- [x] 3.1 JVM 17 未満を起動 JVM として、ビルド成果物を持たない作業ツリーで daemon 用 JVM が取得されることを確認する。
- [x] 3.2 CI が従来どおり通り、`actions/setup-java` が用意する Java 21 を daemon が再利用して追加の JDK download が発生しないことを確認する。
- [x] 3.3 IntelliJ IDEA のインポートが影響を受けないことを確認する。
  - `idea.sh -Didea.config.path=<tmp-config> -Didea.system.path=<tmp-system> -Didea.log.path=<tmp-log> inspect <project> <inspection-profile> <output> -v2` を、ビルド成果物を持たない一時 worktree に対して実行した。隔離した IDEA 設定で project import と inspection report の生成に成功し、IDE ログに Gradle import failure、`ERROR`、`FATAL` はなかった。
- [x] 3.4 Renovate の実行時間を測り、Gradle 実行の有無と、Gradle が必要な場合の daemon JVM / project toolchain の取得経路を確認する。
  - Renovate 44.65.5 を `--platform=local --dry-run=lookup` で read-only の一時 worktree に実行した。25.402 秒で完了し、Gradle manager は 36 ファイル・18 dependencies を抽出した。Renovate process は Gradle を起動しないため JDK provisioning は発生せず、更新 PR の CI で `actions/setup-java` の Java 21 を daemon が再利用することは task 3.2 で確認済みである。

## 4. Document

- [x] 4.1 daemon JVM と プロジェクト toolchain の役割の違いを、どちらがどの層を担うかが分かる形で記録する。
- [x] 4.2 `toolchainUrl` の更新が必要になる条件と、CI target 以外の platform URL が未検証であることを記録する。

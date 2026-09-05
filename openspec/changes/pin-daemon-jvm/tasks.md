## 1. Decide the daemon JVM version

- [ ] 1.1 daemon に要求する JVM バージョンを決め、プロジェクト toolchain (Java 21) と揃えるかどうかの判断根拠を記録する。

## 2. Generate the criteria file

- [ ] 2.1 `./gradlew updateDaemonJvm --jvm-version=<N>` で `gradle/gradle-daemon-jvm.properties` を生成する。
- [ ] 2.2 生成された `toolchainUrl.<OS>.<ARCH>` が、CI と開発環境で使うプラットフォームを網羅しているか確認する。

## 3. Verify across environments

- [ ] 3.1 JVM 17 未満を起動 JVM として、ビルド成果物を持たない作業ツリーで daemon 用 JVM が取得されることを確認する。
- [ ] 3.2 CI が従来どおり通ることを確認する。
- [ ] 3.3 IntelliJ IDEA のインポートが影響を受けないことを確認する。
- [ ] 3.4 Renovate の実行時間を測り、JDK の二重取得がタイムアウトに至らないことを確認する。

## 4. Document

- [ ] 4.1 daemon JVM と プロジェクト toolchain の役割の違いを、どちらがどの層を担うかが分かる形で記録する。
- [ ] 4.2 `toolchainUrl` の更新が必要になる条件を記録する。

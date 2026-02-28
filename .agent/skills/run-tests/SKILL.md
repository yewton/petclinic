---
name: run-tests
description: Gradle テストを実行し、コンパイル時や実行時の警告・エラーを抽出して確認する
---

# `run-tests` SKILL

この SKILL は、Gradle テストを実行し、標準出力および標準エラー出力を一時ファイルに保存したのち、専用の解析スクリプトを用いて既知の問題（Kotlin / Java の Warning や Error）を抽出・報告するためのものです。
特定のテストクラスやメソッドに絞ってテストを実行しつつ、見過ごしがちなコンパイル時の警告にも確実に対処することを目的としています。

## 使い方

1. **テストの実行とログのキャプチャ**
   `capture-output` SKILL を使用してテストを実行し、出力を一時ログファイルに保存します。

   ```bash
   # 例: 特定のテストを実行する場合
   python3 .agent/skills/capture-output/scripts/capture.py stdout.tmp.log stderr.tmp.log ./gradlew :petclinic-fullstack:app:test \
       -Duser.language=en -Duser.country=US \
       --parallel --warning-mode all --build-cache --configuration-cache --info \
       --tests "*should process new owner form"
   ```

   > [!NOTE]
   > 完全にコンパイルをやり直したい場合は、必要に応じて `--rerun-tasks` を付与してください。
   > Windows の場合は `./gradlew` の代わりに `gradlew.bat` を指定してください。

2. **依存関係や問題の解析**
   直前の手順で生成したログファイルを対象に、パース用スクリプトを実行して結果を確認します。

   ```bash
   python3 .agent/skills/run-tests/scripts/parse_gradle_logs.py stdout.tmp.log stderr.tmp.log
   ```

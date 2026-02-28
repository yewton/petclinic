---
description: 任意のコマンドを実行し、標準出力・標準エラー出力を指定したファイルに保存しつつコンソールにも出力する
---

# `capture-output` SKILL

この SKILL は、コマンドの標準出力と標準エラー出力を別々のファイルに保存（キャプチャ）しつつ、コンソールにも通常通り出力させるためのラッパースクリプトを提供します。
Windows などの様々な環境でも動作する `tee` の代替として利用できます。

## 使い方

Python スクリプト `scripts/capture.py` の第一引数に標準出力の保存先、第二引数に標準エラー出力の保存先を指定し、第三引数以降に実行したいコマンドを指定します。

```bash
# 例: './gradlew build' の結果を保存する場合
python3 .agent/skills/capture-output/scripts/capture.py stdout.log stderr.log ./gradlew build
```

コマンドの終了コード（Exit Code）も、スクリプトから呼び出し元へそのまま透過的に伝播されます。

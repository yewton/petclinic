import sys
import re

# Kotlin/Gradle スタイル: "w: ...", "e: ...", "warning: ...", "error: ...", "Note: ..."
SIMPLE_PATTERN = re.compile(r'^(w:|e:|warning:|error:|Note:)\s(.*)', re.IGNORECASE)

# Java コンパイラスタイル: "/path/to/File.java:33: warning: [tag] message"
JAVAC_PATTERN = re.compile(r'^.+\.java:\d+:\s*(warning|error):\s*(.*)', re.IGNORECASE)

# コンテキスト行（コードスニペット・キャレット）: 先頭が空白始まりの行（コードスニペット）または先頭がスペース+^の行（キャレット）
CONTEXT_PATTERN = re.compile(r'^\s+\S|^\s*\^')

def parse_logs(stdout_path, stderr_path):
    warnings = []
    errors = []

    def classify(line):
        """行を分類して (kind, line) を返す。kind は 'warning'|'error'|None。"""
        m = SIMPLE_PATTERN.match(line)
        if m:
            prefix = m.group(1).lower()
            if prefix in ('w:', 'warning:', 'note:'):
                return 'warning'
            else:
                return 'error'
        if JAVAC_PATTERN.match(line):
            if 'warning' in JAVAC_PATTERN.match(line).group(1).lower():
                return 'warning'
            else:
                return 'error'
        return None

    def process_file(filepath):
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                lines = [l.rstrip('\n') for l in f]

            i = 0
            while i < len(lines):
                line = lines[i]
                stripped = line.strip()
                kind = classify(stripped)
                if kind is not None:
                    # 主問題行を記録
                    entry = [stripped]
                    # 続くコンテキスト行（コードスニペット・キャレット）も取り込む
                    j = i + 1
                    while j < len(lines):
                        next_line = lines[j]  # strip前の行で判定
                        next_stripped = next_line.strip()
                        # 次がコンテキスト行（先頭が空白始まり + コードスニペット，またはキャレット行）なら取り込む
                        # ただし strip 後の行が独立した問題行（SIMPLE/JAVAC）にマッチする場合は除外
                        is_next_problem = (
                            SIMPLE_PATTERN.match(next_stripped)
                            or JAVAC_PATTERN.match(next_stripped)
                        )
                        if not is_next_problem and next_stripped and CONTEXT_PATTERN.match(next_line):
                            entry.append('    ' + next_stripped)
                            j += 1
                        else:
                            break
                    block = '\n'.join(entry)
                    if kind == 'warning':
                        warnings.append(block)
                    else:
                        errors.append(block)
                    i = j
                else:
                    i += 1
        except Exception as e:
            print(f"Error reading {filepath}: {e}", file=sys.stderr)

    process_file(stdout_path)
    process_file(stderr_path)

    print("=== 解析結果 ===")
    if not errors and not warnings:
        print("特に警告やエラーは見つかりませんでした。")
        return

    if errors:
        print("\n[ERRORS]")
        for err in errors:
            print(f"  {err}")

    if warnings:
        print("\n[WARNINGS]")
        for warn in warnings:
            print(f"  {warn}")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python parse_gradle_logs.py <path_to_stdout.log> <path_to_stderr.log>")
        sys.exit(1)

    parse_logs(sys.argv[1], sys.argv[2])

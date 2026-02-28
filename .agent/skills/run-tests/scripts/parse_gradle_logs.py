import sys
import re

def parse_logs(stdout_path, stderr_path):
    warnings = []
    errors = []
    
    # 抽出したい正規表現パターン
    problem_pattern = re.compile(r'^(w:|e:|warning:|error:|Note:)\s(.*)', re.IGNORECASE)
    
    def process_file(filepath):
        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if problem_pattern.match(line):
                        if line.lower().startswith("w:") or line.lower().startswith("warning:") or line.lower().startswith("note:"):
                            warnings.append(line)
                        else:
                            errors.append(line)
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

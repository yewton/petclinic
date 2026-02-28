#!/usr/bin/env python3
import sys
import subprocess

def run_and_capture(stdout_path, stderr_path, command):
    print(f"Executing: {' '.join(command)}")
    print(f"Capturing stdout to: {stdout_path}")
    print(f"Capturing stderr to: {stderr_path}")
    print("-" * 40)
    
    try:
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            bufsize=1,
            universal_newlines=True
        )

        stdout_text, stderr_text = process.communicate()
        
        # 画面に元の出力をそのまま流す (teeの代わり)
        if stdout_text:
            sys.stdout.write(stdout_text)
        if stderr_text:
            sys.stderr.write(stderr_text)
            
        print("-" * 40)
        
        # ファイルにも書き込む
        with open(stdout_path, 'w', encoding='utf-8') as f:
            f.write(stdout_text or "")
        with open(stderr_path, 'w', encoding='utf-8') as f:
            f.write(stderr_text or "")
            
    except Exception as e:
        print(f"Error executing command: {e}", file=sys.stderr)
        return 1
        
    print(f"Command execution finished with exit code {process.returncode}")
    return process.returncode

if __name__ == "__main__":
    if len(sys.argv) < 4:
        print("Usage: python capture.py <stdout_file> <stderr_file> <command_and_args...>")
        sys.exit(1)
        
    stdout_file = sys.argv[1]
    stderr_file = sys.argv[2]
    command_to_run = sys.argv[3:]
    
    exit_code = run_and_capture(stdout_file, stderr_file, command_to_run)
    sys.exit(exit_code)

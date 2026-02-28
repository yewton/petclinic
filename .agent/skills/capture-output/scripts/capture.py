#!/usr/bin/env python3
import sys
import subprocess

import threading

def stream_reader(pipe, write_func, log_accumulator):
    """スレッドでストリームを読み込み、画面出力と蓄積を同時に行う"""
    for line in iter(pipe.readline, ''):
        write_func(line)
        sys.stdout.flush() if write_func == sys.stdout.write else sys.stderr.flush()
        log_accumulator.append(line)
    pipe.close()

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

        stdout_lines = []
        stderr_lines = []

        # stdout と stderr を並行して読み取るためのスレッドを開始
        t1 = threading.Thread(target=stream_reader, args=(process.stdout, sys.stdout.write, stdout_lines))
        t2 = threading.Thread(target=stream_reader, args=(process.stderr, sys.stderr.write, stderr_lines))

        t1.start()
        t2.start()

        # 両方のスレッドが終了するのを待つ
        t1.join()
        t2.join()
        
        process.wait()

        print("-" * 40)
        
        # ファイルに書き込む
        with open(stdout_path, 'w', encoding='utf-8') as f:
            f.writelines(stdout_lines)
        with open(stderr_path, 'w', encoding='utf-8') as f:
            f.writelines(stderr_lines)
            
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

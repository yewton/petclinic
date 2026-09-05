#!/bin/bash
set -euo pipefail

# Only run in remote environments (Claude Code on the web)
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# Start Docker daemon if not already running
if ! docker info > /dev/null 2>&1; then
  echo "Starting Docker daemon..."
  nohup dockerd --host=unix:///var/run/docker.sock > /tmp/dockerd.log 2>&1 &
  DOCKERD_PID=$!

  # Wait up to 30 seconds for Docker to become ready
  for i in $(seq 1 30); do
    if docker info > /dev/null 2>&1; then
      echo "Docker daemon is ready (${i}s)"
      break
    fi
    if [ "$i" -eq 30 ]; then
      echo "ERROR: Docker daemon failed to start within 30 seconds" >&2
      cat /tmp/dockerd.log >&2
      exit 1
    fi
    sleep 1
  done
else
  echo "Docker daemon is already running"
fi

# Pull the PostgreSQL image used by Testcontainers to warm up the cache
docker pull postgres:16.3 > /dev/null 2>&1 &

echo "Docker setup complete"

#!/usr/bin/env bash
# Starts a single-node Zipkin server for distributed tracing in Phase 10.
# Idempotent: reuses the existing container if it's already there.
set -euo pipefail

NAME=zipkin

if docker ps --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "Zipkin already running."
elif docker ps -a --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "Starting existing Zipkin container..."
  docker start "$NAME"
else
  echo "Creating Zipkin container..."
  docker run -d --name "$NAME" -p 9411:9411 openzipkin/zipkin:latest
fi

echo "Waiting for Zipkin to accept connections on :9411..."
for _ in $(seq 1 30); do
  if curl -sf http://localhost:9411/health >/dev/null 2>&1; then
    echo "Zipkin is ready: http://localhost:9411"
    exit 0
  fi
  sleep 1
done
echo "Zipkin did not become ready in time." >&2
exit 1

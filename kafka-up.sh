#!/usr/bin/env bash
# Starts a single-node Kafka broker (KRaft mode, no Zookeeper) for the saga in Phase 6.
# Idempotent: reuses the existing container if it's already there.
set -euo pipefail

NAME=kafka

if docker ps --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "Kafka already running."
elif docker ps -a --format '{{.Names}}' | grep -qx "$NAME"; then
  echo "Starting existing Kafka container..."
  docker start "$NAME"
else
  echo "Creating Kafka container..."
  docker run -d --name "$NAME" -p 9092:9092 \
    -e KAFKA_NODE_ID=1 \
    -e KAFKA_PROCESS_ROLES=broker,controller \
    -e KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
    -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 \
    apache/kafka:latest
fi

echo "Waiting for the broker to accept connections on :9092..."
for _ in $(seq 1 30); do
  if docker exec "$NAME" /opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 >/dev/null 2>&1; then
    echo "Kafka is ready. Topics:"
    docker exec "$NAME" /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
    exit 0
  fi
  sleep 1
done
echo "Kafka did not become ready in time." >&2
exit 1

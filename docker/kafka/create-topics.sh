#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="kafka:29092"

topics=(
  "vital-readings"
  "vital-batches"
  "vital-thresholds"
  "patient-vitals"
  "patient-predictions"
  "patient-alerts"
  "patient-notifications"
  "vital-events"
  "vital-alerts"
  "error-events"
  "escalation-events"
  "simulation-status"
  "notification-requests"
)

echo "Waiting for Kafka topic bootstrap at ${BOOTSTRAP_SERVER}"

for topic in "${topics[@]}"; do
  echo "Creating topic: ${topic}"
  kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" --create --if-not-exists --topic "${topic}" --partitions 3 --replication-factor 1
done

echo "Kafka topic bootstrap complete."

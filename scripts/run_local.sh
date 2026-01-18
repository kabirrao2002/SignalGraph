#!/usr/bin/env bash
set -euo pipefail

# Lightweight runner for local development.
# - prefers Maven if available (produces shaded jar)
# - falls back to javac if Maven not installed

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

echo "[run_local] project root: $ROOT_DIR"

if command -v mvn >/dev/null 2>&1; then
  echo "[run_local] Found mvn -> building with Maven"
  mvn -f orchestrator-java clean package

  # Shade plugin produces a shaded jar with default naming: <artifactId>-<version>-shaded.jar
  SHADED_JAR="orchestrator-java/target/orchestrator-java-1.0-SNAPSHOT-shaded.jar"
  PLAIN_JAR="orchestrator-java/target/orchestrator-java-1.0-SNAPSHOT.jar"

  if [ -f "$SHADED_JAR" ]; then
    echo "[run_local] Running shaded jar: $SHADED_JAR"
    java -jar "$SHADED_JAR" ingest data/sample
  elif [ -f "$PLAIN_JAR" ]; then
    echo "[run_local] Running plain jar: $PLAIN_JAR"
    java -cp "$PLAIN_JAR" com.signalgraph.orchestrator.Main ingest data/sample
  else
    echo "[run_local] No jar found after build"
    exit 1
  fi
else
  echo "[run_local] mvn not found -> using javac fallback"
  mkdir -p out
  javac -d out orchestrator-java/src/main/java/com/signalgraph/orchestrator/Main.java
  java -cp out com.signalgraph.orchestrator.Main ingest data/sample
fi

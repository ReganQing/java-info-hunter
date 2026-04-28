#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "[start-all] Running preflight config validation..."
if command -v pwsh >/dev/null 2>&1; then
  pwsh -ExecutionPolicy Bypass -File "$SCRIPT_DIR/validate-runtime-config.ps1" -EnvFile "$ROOT_DIR/.env"
elif command -v powershell >/dev/null 2>&1; then
  powershell -ExecutionPolicy Bypass -File "$SCRIPT_DIR/validate-runtime-config.ps1" -EnvFile "$ROOT_DIR/.env"
else
  echo "[start-all] PowerShell runtime not found (pwsh/powershell)."
  exit 1
fi

echo "[start-all] Starting API service..."
(cd "$ROOT_DIR" && nohup mvn spring-boot:run -pl javainfohunter-api -Dspring-boot.run.profiles=develop > "$SCRIPT_DIR/api.log" 2>&1 &)

echo "[start-all] Starting Crawler service..."
(cd "$ROOT_DIR" && nohup mvn spring-boot:run -pl javainfohunter-crawler -Dspring-boot.run.profiles=develop > "$SCRIPT_DIR/crawler.log" 2>&1 &)

echo "[start-all] Starting Processor service..."
(cd "$ROOT_DIR" && nohup mvn spring-boot:run -pl javainfohunter-processor -Dspring-boot.run.profiles=develop > "$SCRIPT_DIR/processor.log" 2>&1 &)

echo "[start-all] Startup commands issued."

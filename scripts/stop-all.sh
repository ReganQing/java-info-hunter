#!/usr/bin/env bash
set -euo pipefail

patterns=(
  "spring-boot:run -pl javainfohunter-api"
  "spring-boot:run -pl javainfohunter-crawler"
  "spring-boot:run -pl javainfohunter-processor"
)

echo "[stop-all] Stopping JavaInfoHunter module processes..."
for pattern in "${patterns[@]}"; do
  pids="$(pgrep -f "$pattern" || true)"
  if [[ -z "$pids" ]]; then
    continue
  fi
  while IFS= read -r pid; do
    [[ -z "$pid" ]] && continue
    echo "[stop-all] Stopping PID $pid ($pattern)"
    kill -9 "$pid" || true
  done <<< "$pids"
done

echo "[stop-all] Done."

#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
TOKEN="${TOKEN:-mp_demo_token_2026}"

echo "==> 使用 BASE_URL=$BASE_URL"
echo

request() {
  local method="$1"
  local path="$2"
  local data="${3:-}"

  echo "--> $method $path"
  if [ -n "$data" ]; then
    curl -s \
      -X "$method" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "$data" \
      "$BASE_URL$path"
  else
    curl -s \
      -X "$method" \
      -H "Authorization: Bearer $TOKEN" \
      "$BASE_URL$path"
  fi
  echo
  echo
}

request GET "/api/user/current"
request GET "/api/mini/equipment/resolve?code=EQ-000001"
request GET "/api/device/my"
request GET "/api/training/progress/today"
request POST "/api/training/session" '{"equipmentCode":"EQ-000001","deviceCode":"HB-3412","exerciseType":"chest_press","completedSets":2,"totalReps":24,"durationMin":8,"totalVolumeKg":0,"sets":[{"setNo":1,"reps":12,"durationSec":40,"startedAt":1777647000000,"endedAt":1777647040000},{"setNo":2,"reps":12,"durationSec":38,"startedAt":1777647070000,"endedAt":1777647108000}]}'

echo "自检请求已完成。"

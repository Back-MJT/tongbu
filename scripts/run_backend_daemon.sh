#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
JAVA_BIN="${JAVA_BIN:-/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java}"

cd "$ROOT_DIR/RuoYi-Backend"

exec env \
  DB_HOST="${DB_HOST:-127.0.0.1}" \
  DB_PORT="${DB_PORT:-3306}" \
  DB_NAME="${DB_NAME:-xindong}" \
  DB_USER="${DB_USER:-xindong}" \
  DB_PASSWORD="${DB_PASSWORD:-xindong123}" \
  REDIS_HOST="${REDIS_HOST:-127.0.0.1}" \
  REDIS_PORT="${REDIS_PORT:-6379}" \
  SERVER_PORT="${SERVER_PORT:-18080}" \
  WECHAT_APPID="${WECHAT_APPID:-wx71e4f887a62bccaa}" \
  WECHAT_SECRET="${WECHAT_SECRET:-}" \
  "$JAVA_BIN" -jar ruoyi-admin/target/ruoyi-admin.jar

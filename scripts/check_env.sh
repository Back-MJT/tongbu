#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# shellcheck disable=SC1091
source "$ROOT_DIR/scripts/activate_dev_env.sh" >/dev/null

echo "python=$("$ROOT_DIR/.venv/bin/python" --version 2>&1)"
echo "node=$(node -v)"
echo "npm=$(npm -v)"
echo "java=$(java -version 2>&1 | head -1)"
echo "maven=$(mvn -version | head -1)"

test -f "$ROOT_DIR/RuoYi-Backend/pom.xml"
test -f "$ROOT_DIR/RuoYi-Vue3-master/package.json"
test -f "$ROOT_DIR/mini-program/package.json"

if [ ! -d "$ROOT_DIR/device-integration" ]; then
  echo "warn=missing device-integration; full docker-compose build will fail until this source dir is restored"
fi

if [ ! -d "$ROOT_DIR/demo" ]; then
  echo "warn=missing demo; ble-simulator service in docker-compose cannot mount it"
fi

echo "env=ok"

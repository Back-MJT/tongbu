#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/RuoYi-Vue3-master"

exec npm run dev -- --host 127.0.0.1 --port 3000

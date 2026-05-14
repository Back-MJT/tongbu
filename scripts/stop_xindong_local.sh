#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> 停止 RuoYi 后端"
pids="$(pgrep -f "ruoyi-admin/target/ruoyi-admin.jar" || true)"
if [ -n "$pids" ]; then
  echo "$pids" | xargs kill
  echo "已停止后端进程: $pids"
else
  echo "未发现后端进程。"
fi

echo
echo "==> 停止后台管理前端"
admin_pids="$(lsof -tiTCP:3000 -sTCP:LISTEN || true)"
if [ -n "$admin_pids" ]; then
  echo "$admin_pids" | xargs kill
  echo "已停止后台前端进程: $admin_pids"
else
  echo "未发现后台前端进程。"
fi

echo
echo "==> 停止 Docker MySQL / Redis"
cd "$ROOT_DIR"
docker compose -f docker-compose.local.yml down

echo
echo "已停止本地数据服务。"
echo "说明: cloudflared 未自动停止，如需停止请在 Cloudflare/Docker 外自行处理，或运行: pkill -f 'cloudflared tunnel run'"

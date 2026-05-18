#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/RuoYi-Backend"
LOG_DIR="$ROOT_DIR/logs"
BACKEND_LOG="$LOG_DIR/local-backend.log"
ADMIN_LOG="$LOG_DIR/local-admin.log"

JAVA_BIN="${JAVA_BIN:-/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java}"
BACKEND_PORT="${BACKEND_PORT:-18080}"
API_DOMAIN="${API_DOMAIN:-https://api.black-mjt.cn}"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-xindong}"
DB_USER="${DB_USER:-xindong}"
DB_PASSWORD="${DB_PASSWORD:-xindong123}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
WECHAT_APPID="${WECHAT_APPID:-wx71e4f887a62bccaa}"
WECHAT_SECRET="${WECHAT_SECRET:-}"
WITH_ADMIN=0

info() {
  printf "\n==> %s\n" "$1"
}

warn() {
  printf "\n[WARN] %s\n" "$1"
}

fail() {
  printf "\n[ERROR] %s\n" "$1" >&2
  exit 1
}

for arg in "$@"; do
  case "$arg" in
    --with-admin)
      WITH_ADMIN=1
      ;;
    -h|--help)
      cat <<EOF
Usage:
  WECHAT_SECRET=xxx ./scripts/start_xindong_local.sh [--with-admin]

Options:
  --with-admin   同时启动后台管理前端 http://127.0.0.1:3000
EOF
      exit 0
      ;;
    *)
      fail "未知参数: $arg"
      ;;
  esac
done

mkdir -p "$LOG_DIR"

wait_for_docker() {
  for _ in $(seq 1 60); do
    if docker info >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

wait_for_http() {
  local url="$1"
  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  return 1
}

info "检查 Docker Desktop"
if ! docker info >/dev/null 2>&1; then
  if command -v open >/dev/null 2>&1; then
    open -a Docker || true
  fi
  wait_for_docker || fail "Docker Desktop 未启动。请手动打开 Docker Desktop 后重试。"
fi

info "启动 MySQL / Redis"
cd "$ROOT_DIR"
docker compose -f docker-compose.local.yml up -d
docker compose -f docker-compose.local.yml ps

info "检查 cloudflared"
if ! pgrep -f "cloudflared tunnel run" >/dev/null 2>&1; then
  warn "未检测到 cloudflared tunnel 进程。请在 Cloudflare Zero Trust 页面复制并运行 cloudflared tunnel run --token ...，否则 https://api.black-mjt.cn 会 502。"
else
  echo "cloudflared 正在运行。"
fi

info "检查 Java"
if [ ! -x "$JAVA_BIN" ]; then
  fail "找不到 Java: $JAVA_BIN。可以通过 JAVA_BIN=/path/to/java 覆盖。"
fi

info "停止旧后端进程"
old_pids="$(pgrep -f "ruoyi-admin/target/ruoyi-admin.jar" || true)"
if [ -n "$old_pids" ]; then
  echo "$old_pids" | xargs kill
  sleep 2
fi

if [ -z "$WECHAT_SECRET" ]; then
  warn "未设置 WECHAT_SECRET。真实微信登录不可用，本地开发会使用 dev openid。"
elif [[ "$WECHAT_SECRET" == *"当前小程序AppSecret"* || "$WECHAT_SECRET" == *"你的真实小程序AppSecret"* ]]; then
  fail "WECHAT_SECRET 仍是占位文本，请替换为微信公众平台的小程序 AppSecret。"
fi

info "启动 RuoYi 后端"
cd "$BACKEND_DIR"
(
  exec </dev/null
  nohup env \
    DB_HOST="$DB_HOST" \
    DB_PORT="$DB_PORT" \
    DB_NAME="$DB_NAME" \
    DB_USER="$DB_USER" \
    DB_PASSWORD="$DB_PASSWORD" \
    REDIS_HOST="$REDIS_HOST" \
    REDIS_PORT="$REDIS_PORT" \
    SERVER_PORT="$BACKEND_PORT" \
    WECHAT_APPID="$WECHAT_APPID" \
    WECHAT_SECRET="$WECHAT_SECRET" \
    "$JAVA_BIN" -jar ruoyi-admin/target/ruoyi-admin.jar \
    > "$BACKEND_LOG" 2>&1
) &

echo "后端日志: $BACKEND_LOG"

info "等待后端接口"
LOCAL_CHECK="http://127.0.0.1:$BACKEND_PORT/api/mini/equipment/resolve?code=EQ-000001"
wait_for_http "$LOCAL_CHECK" || {
  tail -n 120 "$BACKEND_LOG" || true
  fail "后端未能启动或接口不可用。"
}

info "本地接口自检"
curl -fsS "$LOCAL_CHECK"
printf "\n"

info "公网接口自检"
PUBLIC_CHECK="$API_DOMAIN/api/mini/equipment/resolve?code=EQ-000001"
if curl -fsS "$PUBLIC_CHECK"; then
  printf "\n"
  info "启动完成"
  cat <<EOF

本地后端: http://127.0.0.1:$BACKEND_PORT
公网 API:  $API_DOMAIN
演示器械: EQ-000001
演示设备: HB-3412 / gy_ble25t1

下一步:
1. 打开微信开发者工具
2. 导入 mini-program
3. 编译 / 上传体验版
4. 手机扫码测试微信登录、扫码器械、连接蓝牙、提交训练

EOF
else
  printf "\n"
  warn "本地接口正常，但公网接口不可用。请检查 cloudflared 是否运行，以及 Cloudflare Tunnel 是否转发到 http://localhost:$BACKEND_PORT。"
fi

if [ "$WITH_ADMIN" = "1" ]; then
  info "启动后台管理前端"
  if ! command -v npm >/dev/null 2>&1; then
    warn "未找到 npm，跳过后台管理前端启动。"
  else
    admin_pids="$(lsof -tiTCP:3000 -sTCP:LISTEN || true)"
    if [ -n "$admin_pids" ]; then
      echo "$admin_pids" | xargs kill || true
      sleep 1
    fi
    cd "$ROOT_DIR/RuoYi-Vue3-master"
    (
      exec </dev/null
      nohup npm run dev -- --host 127.0.0.1 --port 3000 > "$ADMIN_LOG" 2>&1
    ) &
    echo "后台管理前端日志: $ADMIN_LOG"
    echo "后台管理前端地址: http://127.0.0.1:3000"
  fi
fi

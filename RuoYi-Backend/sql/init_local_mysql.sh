#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SQL_DIR="$ROOT_DIR/sql"

MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DB="${MYSQL_DB:-xindong}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"

BASE_SQL="$SQL_DIR/ry_20260321.sql"
IOT_SQL="$SQL_DIR/iot/iot_equipment_mysql.sql"
IOT_MENU_SQL="$SQL_DIR/iot/iot_admin_menu_mysql.sql"

if ! command -v mysql >/dev/null 2>&1; then
  echo "未找到 mysql 客户端，请先安装 MySQL 命令行工具。"
  exit 1
fi

echo "==> 当前初始化目标"
echo "    host: $MYSQL_HOST"
echo "    port: $MYSQL_PORT"
echo "    db  : $MYSQL_DB"
echo "    user: $MYSQL_USER"
echo
echo "==> 将按顺序执行:"
echo "    1. $BASE_SQL"
echo "    2. $IOT_SQL"
echo "    3. $IOT_MENU_SQL"
echo

MYSQL_CMD=(mysql --default-character-set=utf8mb4 -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER")
if [ -n "$MYSQL_PASSWORD" ]; then
  MYSQL_CMD+=(-p"$MYSQL_PASSWORD")
fi

echo "==> 创建数据库 $MYSQL_DB"
"${MYSQL_CMD[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DB\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

echo "==> 导入若依基础表"
"${MYSQL_CMD[@]}" "$MYSQL_DB" < "$BASE_SQL"

echo "==> 导入 IoT / 小程序扩展表"
"${MYSQL_CMD[@]}" "$MYSQL_DB" < "$IOT_SQL"

echo "==> 导入 IoT 管理端菜单"
"${MYSQL_CMD[@]}" "$MYSQL_DB" < "$IOT_MENU_SQL"

echo "==> 基础检查"
"${MYSQL_CMD[@]}" -N -e "
SELECT 'sys_user', COUNT(*) FROM \`$MYSQL_DB\`.sys_user
UNION ALL
SELECT 'iot_device', COUNT(*) FROM \`$MYSQL_DB\`.iot_device
UNION ALL
SELECT 'xd_equipment', COUNT(*) FROM \`$MYSQL_DB\`.xd_equipment
UNION ALL
SELECT 'iot_device_binding', COUNT(*) FROM \`$MYSQL_DB\`.iot_device_binding
UNION ALL
SELECT 'interv_session', COUNT(*) FROM \`$MYSQL_DB\`.interv_session
UNION ALL
SELECT 'iot_admin_menus', COUNT(*) FROM \`$MYSQL_DB\`.sys_menu WHERE menu_id IN (2000, 2001, 2002);
"

echo
echo "初始化完成。"
echo "如果现场 IMU 设备编码不是 HB-3412，请继续修改:"
echo "  $IOT_SQL"
echo "中的演示设备编码与绑定数据。"

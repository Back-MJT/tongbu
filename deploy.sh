#!/bin/bash

# ============================================
# 昕动智能平台 - 一键部署脚本
# ============================================
# 使用说明：
# chmod +x deploy.sh
# ./deploy.sh
# ============================================

set -e  # 遇到错误立即退出

echo "=========================================="
echo "  昕动智能平台 - 一键部署脚本"
echo "=========================================="
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ 错误: Docker 未安装"
    echo "请先安装 Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker compose &> /dev/null; then
    echo "❌ 错误: Docker Compose 未安装"
    echo "请先安装 Docker Compose"
    exit 1
fi

echo "✅ Docker 和 Docker Compose 已安装"
echo ""

# 检查 .env 文件是否存在
if [ ! -f .env ]; then
    echo "⚠️  警告: .env 文件不存在"
    echo "正在从 .env.example 创建 .env 文件..."
    cp .env.example .env
    echo "✅ .env 文件已创建"
    echo ""
    echo "⚠️  重要提示："
    echo "   请编辑 .env 文件，修改以下配置："
    echo "   - DB_PASSWORD (数据库密码)"
    echo "   - REDIS_PASSWORD (Redis 密码)"
    echo "   - JWT_SECRET (JWT 密钥)"
    echo "   - WECHAT_APPID (微信小程序 AppID)"
    echo "   - WECHAT_SECRET (微信小程序密钥)"
    echo ""
    read -p "是否现在编辑 .env 文件？(y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        ${EDITOR:-nano} .env
    else
        echo "⚠️  请稍后手动编辑 .env 文件"
    fi
    echo ""
fi

# 询问部署模式
echo "请选择部署模式："
echo "1) 开发环境（仅启动 MySQL + Redis）"
echo "2) 生产环境（启动所有服务）"
read -p "请输入选项 (1 或 2): " deploy_mode
echo ""

if [ "$deploy_mode" = "1" ]; then
    echo "📦 启动开发环境..."
    docker compose -f docker-compose.local.yml up -d

    echo ""
    echo "⏳ 等待 MySQL 启动完成..."
    sleep 30

    echo "📊 执行数据库优化脚本..."
    docker exec -i xindong-local-mysql mysql -uxindong -pxindong123 xindong < RuoYi-Backend/sql/upgrade_equipment_device_binding.sql 2>/dev/null || echo "⚠️  数据库脚本执行失败（可能已执行过）"

    echo ""
    echo "✅ 开发环境启动完成！"
    echo ""
    echo "服务信息："
    echo "  - MySQL: localhost:3306"
    echo "  - Redis: localhost:6379"
    echo ""
    echo "下一步："
    echo "  1. 在 RuoYi-Backend 目录运行后端: mvn spring-boot:run"
    echo "  2. 在 RuoYi-Vue3-master 目录运行前端: npm run dev"

elif [ "$deploy_mode" = "2" ]; then
    echo "📦 启动生产环境..."

    # 构建镜像
    echo "🔨 构建 Docker 镜像..."
    docker compose -f docker-compose.production.yml build

    # 启动服务
    echo "🚀 启动所有服务..."
    docker compose -f docker-compose.production.yml up -d

    echo ""
    echo "⏳ 等待服务启动完成..."
    sleep 60

    # 检查服务状态
    echo ""
    echo "📊 服务状态："
    docker compose -f docker-compose.production.yml ps

    echo ""
    echo "✅ 生产环境启动完成！"
    echo ""
    echo "访问地址："
    echo "  - 后台管理: http://localhost:3080"
    echo "  - 后端 API: http://localhost:8080"
    echo "  - 默认账号: admin / admin123"
    echo ""
    echo "查看日志："
    echo "  docker compose -f docker-compose.production.yml logs -f"

else
    echo "❌ 无效的选项"
    exit 1
fi

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="

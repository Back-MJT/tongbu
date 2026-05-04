#!/bin/bash

# ============================================
# 昕动智能平台 - Windows 部署脚本
# ============================================
# 使用说明：
# 在 PowerShell 中运行：
# .\deploy-windows.ps1
# ============================================

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  昕动智能平台 - Windows 部署脚本" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker 是否安装
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "❌ 错误: Docker 未安装" -ForegroundColor Red
    Write-Host "请先安装 Docker Desktop: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Docker 已安装" -ForegroundColor Green
Write-Host ""

# 检查 .env 文件是否存在
if (-not (Test-Path .env)) {
    Write-Host "⚠️  警告: .env 文件不存在" -ForegroundColor Yellow
    Write-Host "正在从 .env.example 创建 .env 文件..." -ForegroundColor Yellow
    Copy-Item .env.example .env
    Write-Host "✅ .env 文件已创建" -ForegroundColor Green
    Write-Host ""
    Write-Host "⚠️  重要提示：" -ForegroundColor Yellow
    Write-Host "   请编辑 .env 文件，修改以下配置：" -ForegroundColor Yellow
    Write-Host "   - DB_PASSWORD (数据库密码)" -ForegroundColor Yellow
    Write-Host "   - REDIS_PASSWORD (Redis 密码)" -ForegroundColor Yellow
    Write-Host "   - JWT_SECRET (JWT 密钥)" -ForegroundColor Yellow
    Write-Host "   - WECHAT_APPID (微信小程序 AppID)" -ForegroundColor Yellow
    Write-Host "   - WECHAT_SECRET (微信小程序密钥)" -ForegroundColor Yellow
    Write-Host ""
    $response = Read-Host "是否现在编辑 .env 文件？(y/n)"
    if ($response -eq "y") {
        notepad .env
    } else {
        Write-Host "⚠️  请稍后手动编辑 .env 文件" -ForegroundColor Yellow
    }
    Write-Host ""
}

# 询问部署模式
Write-Host "请选择部署模式：" -ForegroundColor Cyan
Write-Host "1) 开发环境（仅启动 MySQL + Redis）"
Write-Host "2) 生产环境（启动所有服务）"
$deployMode = Read-Host "请输入选项 (1 或 2)"
Write-Host ""

if ($deployMode -eq "1") {
    Write-Host "📦 启动开发环境..." -ForegroundColor Cyan
    docker compose -f docker-compose.local.yml up -d

    Write-Host ""
    Write-Host "⏳ 等待 MySQL 启动完成..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30

    Write-Host "📊 执行数据库优化脚本..." -ForegroundColor Cyan
    Get-Content RuoYi-Backend\sql\upgrade_equipment_device_binding.sql | docker exec -i xindong-local-mysql mysql -uxindong -pxindong123 xindong

    Write-Host ""
    Write-Host "✅ 开发环境启动完成！" -ForegroundColor Green
    Write-Host ""
    Write-Host "服务信息：" -ForegroundColor Cyan
    Write-Host "  - MySQL: localhost:3306"
    Write-Host "  - Redis: localhost:6379"
    Write-Host ""
    Write-Host "下一步：" -ForegroundColor Yellow
    Write-Host "  1. 在 RuoYi-Backend 目录运行后端"
    Write-Host "  2. 在 RuoYi-Vue3-master 目录运行前端: npm run dev"

} elseif ($deployMode -eq "2") {
    Write-Host "📦 启动生产环境..." -ForegroundColor Cyan

    # 构建镜像
    Write-Host "🔨 构建 Docker 镜像..." -ForegroundColor Cyan
    docker compose -f docker-compose.production.yml build

    # 启动服务
    Write-Host "🚀 启动所有服务..." -ForegroundColor Cyan
    docker compose -f docker-compose.production.yml up -d

    Write-Host ""
    Write-Host "⏳ 等待服务启动完成..." -ForegroundColor Yellow
    Start-Sleep -Seconds 60

    # 检查服务状态
    Write-Host ""
    Write-Host "📊 服务状态：" -ForegroundColor Cyan
    docker compose -f docker-compose.production.yml ps

    Write-Host ""
    Write-Host "✅ 生产环境启动完成！" -ForegroundColor Green
    Write-Host ""
    Write-Host "访问地址：" -ForegroundColor Cyan
    Write-Host "  - 后台管理: http://localhost:3080"
    Write-Host "  - 后端 API: http://localhost:8080"
    Write-Host "  - 默认账号: admin / admin123"
    Write-Host ""
    Write-Host "查看日志：" -ForegroundColor Yellow
    Write-Host "  docker compose -f docker-compose.production.yml logs -f"

} else {
    Write-Host "❌ 无效的选项" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  部署完成！" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

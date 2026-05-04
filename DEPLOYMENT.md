# 昕动智能平台 - 生产部署指南

## 📋 目录
- [系统要求](#系统要求)
- [快速部署](#快速部署)
- [详细步骤](#详细步骤)
- [环境变量配置](#环境变量配置)
- [服务管理](#服务管理)
- [故障排查](#故障排查)

---

## 🖥️ 系统要求

### 服务器配置（推荐）
- **CPU**: 4 核心或以上
- **内存**: 8GB 或以上
- **硬盘**: 50GB 或以上
- **操作系统**: 
  - Linux (Ubuntu 20.04+, CentOS 7+)
  - Windows Server 2019+

### 软件依赖
- Docker 20.10+
- Docker Compose 2.0+

---

## 🚀 快速部署

### 1. 安装 Docker

**Linux (Ubuntu/Debian):**
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo systemctl start docker
sudo systemctl enable docker
```

**Windows Server:**
- 下载并安装 Docker Desktop for Windows
- 或使用 Windows Server 容器

### 2. 克隆项目
```bash
git clone <your-repo-url>
cd Xindong_Platform-main
```

### 3. 配置环境变量
```bash
# 复制环境变量模板
cp .env.example .env

# 编辑环境变量（重要！）
nano .env  # 或使用其他编辑器
```

### 4. 初始化数据库
```bash
# 启动数据库服务
docker compose -f docker-compose.local.yml up -d

# 等待 MySQL 启动完成（约 30 秒）
sleep 30

# 执行数据库优化脚本
docker exec -i xindong-local-mysql mysql -uxindong -pxindong123 xindong < RuoYi-Backend/sql/upgrade_equipment_device_binding.sql
```

### 5. 启动所有服务
```bash
# 启动后端服务
docker compose -f docker-compose.production.yml up -d

# 查看服务状态
docker compose -f docker-compose.production.yml ps
```

### 6. 访问系统
- **后台管理系统**: http://your-server-ip:3080
- **后端 API**: http://your-server-ip:8080
- **默认账号**: admin / admin123

---

## 📝 详细步骤

### 步骤 1: 准备服务器

#### Linux 服务器
```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装必要工具
sudo apt install -y curl git vim

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 添加当前用户到 docker 组（可选）
sudo usermod -aG docker $USER
```

#### Windows Server
1. 启用 Hyper-V 和容器功能
2. 下载并安装 Docker Desktop
3. 重启服务器

### 步骤 2: 配置环境变量

创建 `.env` 文件：
```bash
# 数据库配置
DB_HOST=mysql
DB_PORT=3306
DB_NAME=xindong
DB_USER=xindong
DB_PASSWORD=your-secure-password-here  # 修改为强密码

# Redis 配置
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password-here  # 修改为强密码

# JWT 配置
JWT_SECRET=your-jwt-secret-key-at-least-32-characters-long  # 修改为随机密钥

# 微信小程序配置
WECHAT_APPID=your-wechat-appid
WECHAT_SECRET=your-wechat-secret

# CORS 配置（生产环境使用具体域名）
CORS_ALLOWED_ORIGINS=https://your-domain.com

# 服务器端口
BACKEND_PORT=8080
FRONTEND_PORT=3080
```

### 步骤 3: 构建 Docker 镜像

```bash
# 构建后端镜像
cd RuoYi-Backend
docker build -t xindong-backend:latest .

# 构建前端镜像
cd ../RuoYi-Vue3-master
docker build -t xindong-frontend:latest .
```

### 步骤 4: 启动服务

```bash
# 返回项目根目录
cd ..

# 启动所有服务
docker compose -f docker-compose.production.yml up -d

# 查看日志
docker compose -f docker-compose.production.yml logs -f
```

---

## 🔧 环境变量配置

### 必须修改的配置

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `DB_PASSWORD` | 数据库密码 | `MySecurePass123!` |
| `REDIS_PASSWORD` | Redis 密码 | `RedisPass456!` |
| `JWT_SECRET` | JWT 密钥 | `random-32-char-secret-key` |
| `WECHAT_APPID` | 微信小程序 AppID | `wx1234567890abcdef` |
| `WECHAT_SECRET` | 微信小程序密钥 | `abcdef1234567890` |
| `CORS_ALLOWED_ORIGINS` | 允许的域名 | `https://yourdomain.com` |

### 可选配置

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `BACKEND_PORT` | 后端端口 | `8080` |
| `FRONTEND_PORT` | 前端端口 | `3080` |
| `LOGGING_LEVEL` | 日志级别 | `INFO` |

---

## 🛠️ 服务管理

### 启动服务
```bash
docker compose -f docker-compose.production.yml up -d
```

### 停止服务
```bash
docker compose -f docker-compose.production.yml down
```

### 重启服务
```bash
docker compose -f docker-compose.production.yml restart
```

### 查看日志
```bash
# 查看所有服务日志
docker compose -f docker-compose.production.yml logs -f

# 查看特定服务日志
docker compose -f docker-compose.production.yml logs -f backend
docker compose -f docker-compose.production.yml logs -f frontend
```

### 查看服务状态
```bash
docker compose -f docker-compose.production.yml ps
```

### 进入容器
```bash
# 进入后端容器
docker exec -it xindong-backend bash

# 进入数据库容器
docker exec -it xindong-mysql bash
```

---

## 🔍 故障排查

### 问题 1: 后端无法连接数据库

**症状**: 后端日志显示 "Connection refused" 或 "Unknown database"

**解决方案**:
```bash
# 检查 MySQL 是否运行
docker ps | grep mysql

# 检查数据库是否创建
docker exec xindong-mysql mysql -uroot -p<password> -e "SHOW DATABASES;"

# 重新初始化数据库
docker exec -i xindong-mysql mysql -uxindong -p<password> xindong < RuoYi-Backend/sql/ry_20260321.sql
```

### 问题 2: 前端无法访问后端 API

**症状**: 前端页面显示 "网络错误" 或 "API 请求失败"

**解决方案**:
```bash
# 检查后端是否运行
curl http://localhost:8080/actuator/health

# 检查 CORS 配置
# 确保 .env 中的 CORS_ALLOWED_ORIGINS 包含前端域名
```

### 问题 3: 小程序无法连接后端

**症状**: 小程序显示 "请求失败" 或 "网络异常"

**解决方案**:
1. 确保服务器防火墙开放了 8080 端口
2. 确保小程序配置了正确的 API 地址
3. 在微信公众平台配置服务器域名白名单

### 问题 4: Docker 容器启动失败

**症状**: `docker compose up` 报错

**解决方案**:
```bash
# 查看详细错误信息
docker compose -f docker-compose.production.yml logs

# 清理并重新启动
docker compose -f docker-compose.production.yml down -v
docker compose -f docker-compose.production.yml up -d
```

---

## 🔒 安全建议

### 1. 修改默认密码
- 数据库密码
- Redis 密码
- 管理员账号密码

### 2. 配置防火墙
```bash
# Ubuntu/Debian
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw allow 8080/tcp  # 后端 API
sudo ufw enable
```

### 3. 使用 HTTPS
- 申请 SSL 证书（Let's Encrypt 免费）
- 配置 Nginx 反向代理
- 强制 HTTPS 访问

### 4. 定期备份
```bash
# 备份数据库
docker exec xindong-mysql mysqldump -uxindong -p<password> xindong > backup_$(date +%Y%m%d).sql

# 备份 Redis
docker exec xindong-redis redis-cli SAVE
```

---

## 📊 监控和维护

### 查看资源使用情况
```bash
# 查看容器资源使用
docker stats

# 查看磁盘使用
df -h

# 查看内存使用
free -h
```

### 日志管理
```bash
# 清理旧日志
docker system prune -a

# 限制日志大小（在 docker-compose.yml 中配置）
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

## 🆘 获取帮助

如果遇到问题，请：
1. 查看日志：`docker compose logs -f`
2. 检查服务状态：`docker compose ps`
3. 查看本文档的故障排查部分
4. 联系技术支持

---

## 📚 相关文档

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [RuoYi 框架文档](http://doc.ruoyi.vip/)
- [Vue 3 文档](https://vuejs.org/)

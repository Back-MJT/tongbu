# 昕动智能平台 - 快速开始指南

## 🚀 5 分钟快速部署

### 前提条件
- ✅ 已安装 Docker Desktop
- ✅ 已下载项目文件

### 部署步骤

#### 1. 配置环境变量（2 分钟）

**复制配置文件：**
```bash
# Linux/Mac
cp .env.example .env

# Windows
copy .env.example .env
```

**编辑 .env 文件，修改以下配置：**
```bash
# 必须修改的配置
DB_PASSWORD=your-secure-password-here          # 数据库密码
REDIS_PASSWORD=your-redis-password-here        # Redis 密码
JWT_SECRET=your-jwt-secret-at-least-32-chars   # JWT 密钥（至少 32 字符）
WECHAT_APPID=your-wechat-appid                 # 微信小程序 AppID
WECHAT_SECRET=your-wechat-secret               # 微信小程序密钥

# 生产环境必须修改
CORS_ALLOWED_ORIGINS=https://your-domain.com   # 允许的域名
```

#### 2. 一键部署（3 分钟）

**Linux/Mac:**
```bash
chmod +x deploy.sh
./deploy.sh
# 选择 "2) 生产环境"
```

**Windows (PowerShell):**
```powershell
.\deploy-windows.ps1
# 选择 "2) 生产环境"
```

**或者手动部署：**
```bash
# 构建并启动所有服务
docker compose -f docker-compose.production.yml up -d

# 查看服务状态
docker compose -f docker-compose.production.yml ps

# 查看日志
docker compose -f docker-compose.production.yml logs -f
```

#### 3. 访问系统

- **后台管理系统**: http://localhost:3080
- **后端 API**: http://localhost:8080
- **默认账号**: admin / admin123

---

## 📊 服务说明

### 启动的服务

| 服务 | 端口 | 说明 |
|------|------|------|
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| 后端 API | 8080 | Spring Boot 服务 |
| 前端管理 | 3080 | Vue 3 管理界面 |

### 数据持久化

所有数据都会持久化保存在 Docker volumes 中：
- `mysql_data` - 数据库数据
- `redis_data` - Redis 数据
- `backend_logs` - 后端日志

---

## 🛠️ 常用命令

### 服务管理

```bash
# 启动所有服务
docker compose -f docker-compose.production.yml up -d

# 停止所有服务
docker compose -f docker-compose.production.yml down

# 重启所有服务
docker compose -f docker-compose.production.yml restart

# 重启单个服务
docker compose -f docker-compose.production.yml restart backend
```

### 查看日志

```bash
# 查看所有服务日志
docker compose -f docker-compose.production.yml logs -f

# 查看特定服务日志
docker compose -f docker-compose.production.yml logs -f backend
docker compose -f docker-compose.production.yml logs -f frontend
docker compose -f docker-compose.production.yml logs -f mysql
```

### 查看服务状态

```bash
# 查看所有服务状态
docker compose -f docker-compose.production.yml ps

# 查看资源使用情况
docker stats
```

### 进入容器

```bash
# 进入后端容器
docker exec -it xindong-backend bash

# 进入数据库容器
docker exec -it xindong-mysql bash

# 连接数据库
docker exec -it xindong-mysql mysql -uxindong -p
```

---

## 🔧 故障排查

### 问题 1: 端口被占用

**错误信息**: `bind: address already in use`

**解决方案**:
```bash
# 查看占用端口的进程
# Linux/Mac
lsof -i :8080
lsof -i :3080

# Windows
netstat -ano | findstr :8080
netstat -ano | findstr :3080

# 修改 .env 文件中的端口配置
BACKEND_PORT=8081
FRONTEND_PORT=3081
```

### 问题 2: 服务启动失败

**解决方案**:
```bash
# 查看详细日志
docker compose -f docker-compose.production.yml logs backend

# 重新构建并启动
docker compose -f docker-compose.production.yml down
docker compose -f docker-compose.production.yml build --no-cache
docker compose -f docker-compose.production.yml up -d
```

### 问题 3: 数据库连接失败

**解决方案**:
```bash
# 检查 MySQL 是否运行
docker ps | grep mysql

# 检查数据库日志
docker logs xindong-mysql

# 重启 MySQL
docker compose -f docker-compose.production.yml restart mysql
```

### 问题 4: 前端无法访问后端

**解决方案**:
1. 检查后端是否运行：`curl http://localhost:8080/actuator/health`
2. 检查 CORS 配置：确保 `.env` 中的 `CORS_ALLOWED_ORIGINS` 正确
3. 查看后端日志：`docker logs xindong-backend`

---

## 🔒 安全建议

### 生产环境必做

1. **修改所有默认密码**
   - 数据库密码
   - Redis 密码
   - 管理员密码（登录后修改）

2. **配置强密钥**
   - JWT_SECRET 至少 32 字符
   - 使用随机字符串生成器

3. **配置 CORS**
   - 不要使用 `*`
   - 使用具体的域名

4. **配置防火墙**
   ```bash
   # 只开放必要的端口
   # 80 (HTTP), 443 (HTTPS), 22 (SSH)
   ```

5. **使用 HTTPS**
   - 申请 SSL 证书
   - 配置 Nginx 反向代理

---

## 📦 数据备份

### 备份数据库

```bash
# 备份数据库
docker exec xindong-mysql mysqldump -uxindong -p<password> xindong > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i xindong-mysql mysql -uxindong -p<password> xindong < backup_20260503.sql
```

### 备份 Redis

```bash
# 备份 Redis
docker exec xindong-redis redis-cli SAVE
docker cp xindong-redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb

# 恢复 Redis
docker cp redis_backup_20260503.rdb xindong-redis:/data/dump.rdb
docker compose -f docker-compose.production.yml restart redis
```

---

## 🆙 更新部署

### 更新代码

```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建镜像
docker compose -f docker-compose.production.yml build

# 3. 重启服务
docker compose -f docker-compose.production.yml up -d
```

### 更新配置

```bash
# 1. 修改 .env 文件
nano .env

# 2. 重启服务
docker compose -f docker-compose.production.yml restart
```

---

## 📞 获取帮助

如果遇到问题：
1. 查看日志：`docker compose logs -f`
2. 查看服务状态：`docker compose ps`
3. 查看本文档的故障排查部分
4. 查看完整文档：`DEPLOYMENT.md`

---

## 🎯 下一步

部署完成后，你可以：

1. **登录后台管理系统**
   - 访问 http://localhost:3080
   - 使用 admin / admin123 登录
   - 立即修改管理员密码

2. **配置器械和传感器**
   - 进入"器械管理"页面
   - 添加器械信息
   - 添加蓝牙传感器
   - 建立绑定关系

3. **测试小程序连接**
   - 配置小程序 API 地址
   - 扫描器械二维码
   - 测试蓝牙连接
   - 上传训练数据

4. **配置域名和 HTTPS**（生产环境）
   - 申请域名
   - 配置 DNS 解析
   - 申请 SSL 证书
   - 配置 Nginx 反向代理

---

## ✅ 检查清单

部署前检查：
- [ ] Docker 已安装
- [ ] .env 文件已配置
- [ ] 所有密码已修改
- [ ] 端口未被占用

部署后检查：
- [ ] 所有服务运行正常
- [ ] 可以访问后台管理系统
- [ ] 可以访问后端 API
- [ ] 数据库连接正常
- [ ] Redis 连接正常

安全检查：
- [ ] 默认密码已修改
- [ ] JWT 密钥已配置
- [ ] CORS 配置正确
- [ ] 防火墙已配置
- [ ] HTTPS 已启用（生产环境）

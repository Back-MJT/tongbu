# HealthHub MVP 部署指南

## 架构概览

```
                    +------------------+
                    |   用户浏览器      |
                    |  http://HOST:80  |
                    +--------+---------+
                             |
                    +--------v---------+
                    |  ruoyi-frontend  |  Nginx (Vue3 静态资源 + 反向代理)
                    |   :80            |
                    +--+------+-------++
                       |      |       |
            /prod-api  |      | /device-api   | /engine-api
                       |      |       |
            +----------v+  +--v-------+---+  +v------------------+
            | ruoyi-    |  | device-     |  | intervention-     |
            | backend   |  | gateway     |  | engine            |
            | :8080     |  | :4000       |  | :4001             |
            +-----+-----+  +--+------+--+  +--+----------------+
                  |             |      |        |
         +--------v-------------v------v--------v--+
         |            共享基础设施                  |
         |  PostgreSQL:5432  Redis:6379            |
         |  Kafka:9092      EMQX MQTT:1883        |
         +-----------------------------------------+
                             |
                    +--------v---------+
                    | ble-simulator    |  BLE 数据模拟器
                    | :8765 (WebSocket)|
                    +------------------+
```

## 系统要求

| 项目 | 最低配置 | 推荐配置 |
|------|---------|---------|
| OS | Ubuntu 22.04 / CentOS 8 | Ubuntu 22.04 LTS |
| CPU | 4核 | 8核 |
| 内存 | 8 GB | 16 GB |
| 磁盘 | 40 GB | 100 GB SSD |
| Docker | 24.0+ | 25.0+ |
| Docker Compose | v2.20+ | v2.24+ |

## 快速开始

### 1. 准备环境

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# 安装 Docker Compose (如果未包含)
sudo apt install docker-compose-plugin

# 验证
docker --version
docker compose version
```

### 2. 获取代码

```bash
git clone <repository-url> /opt/healthhub
cd /opt/healthhub
```

### 3. 配置环境变量

```bash
cp .env.example .env
nano .env
```

`.env` 文件内容:

```env
# 数据库密码
DB_PASSWORD=xindong2024

# Redis 密码 (可选)
REDIS_PASSWORD=

# MQTT 认证
MQTT_USERNAME=
MQTT_PASSWORD=public

# API Key
API_KEY=xindong-device-gateway-key

# Anthropic API Key (用于干预引擎 Claude 调用)
ANTHROPIC_API_KEY=sk-ant-xxxxx
```

### 4. 构建并启动

```bash
# 构建所有镜像 (首次约 10-15 分钟)
docker compose build

# 启动所有服务
docker compose up -d

# 查看启动状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 5. 验证部署

```bash
# 检查所有服务健康状态
docker compose ps --format "table {{.Name}}\t{{.Status}}"

# 验证后端 API
curl http://localhost:8080/

# 验证前端
curl -I http://localhost:80/

# 验证设备网关
curl http://localhost:4000/health

# 验证干预引擎
curl http://localhost:4001/health

# 验证 MQTT
docker exec xindong-mqtt emqx ping
```

## 服务端口清单

| 服务 | 端口 | 说明 |
|------|------|------|
| ruoyi-frontend | 80 | Vue3 前端 (Nginx) |
| ruoyi-backend | 8080 | Spring Boot 后端 API |
| device-gateway | 4000 | 设备接入网关 |
| intervention-engine | 4001 | 干预引擎 |
| PostgreSQL | 5432 | TimescaleDB 数据库 |
| Redis | 6379 | 缓存 |
| Kafka | 9092 | 消息队列 |
| EMQX MQTT | 1883 | MQTT Broker |
| EMQX WebSocket | 8083 | MQTT WebSocket |
| EMQX Dashboard | 18083 | MQTT 管理面板 |
| BLE Simulator | 8765 | BLE 数据模拟 WebSocket |

## 数据库初始化

首次启动时，PostgreSQL 会自动执行 `RuoYi-Backend/sql/init/` 目录下的 SQL 脚本:

1. `ry_20xxxx.sql` - RuoYi 基础表结构
2. IoT 设备管理表 (`iot_device`, `iot_manufacturer`, `iot_device_group`)
3. IMU 数据表 (`iot_imu_data`)
4. 干预引擎相关表

管理员默认账号: `admin / admin123`

## MQTT 主题约定

| 主题 | 方向 | 说明 |
|------|------|------|
| `device/{deviceCode}/data` | 设备 → 平台 | IMU 传感器数据上报 |
| `device/{deviceCode}/status` | 设备 → 平台 | 设备状态更新 |
| `device/{deviceCode}/command` | 平台 → 设备 | 下发指令 |

数据格式 (JSON):
```json
{
  "deviceCode": "IMU-001",
  "timestamp": "2026-04-16T00:00:00Z",
  "accelX": 0.12,
  "accelY": -9.81,
  "accelZ": 0.03,
  "gyroX": 1.2,
  "gyroY": -0.5,
  "gyroZ": 0.8,
  "pitch": 15.3,
  "roll": -2.1,
  "yaw": 178.5
}
```

## 生产环境调优

### JVM 参数 (ruoyi-backend)
```yaml
environment:
  - JAVA_OPTS=-Xms1g -Xmx4g -XX:+UseG1GC -XX:+UseContainerSupport
```

### PostgreSQL
```yaml
command: >
  postgres
  -c shared_buffers=256MB
  -c effective_cache_size=1GB
  -c max_connections=200
```

### Kafka
生产环境建议 3 节点集群，单节点仅用于开发/演示。

### Nginx (前端)
已内置 gzip 压缩。生产环境建议增加 SSL:
```nginx
server {
    listen 443 ssl http2;
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
}
```

## 备份与恢复

### 数据库备份
```bash
docker exec xindong-postgres pg_dump -U xindong xindong > backup_$(date +%Y%m%d).sql
```

### 数据库恢复
```bash
cat backup_20260416.sql | docker exec -i xindong-postgres psql -U xindong xindong
```

### 全量备份 (volumes)
```bash
docker run --rm -v xindong_postgres_data:/data -v $(pwd):/backup alpine tar czf /backup/postgres_backup.tar.gz /data
```

## 故障排查

### 后端启动失败
```bash
# 查看日志
docker compose logs ruoyi-backend

# 常见问题:
# 1. 数据库连接失败 -> 检查 postgres 是否健康
# 2. 端口占用 -> lsof -i :8080
# 3. 内存不足 -> free -h, 调整 JAVA_OPTS
```

### 前端白屏
```bash
# 检查 Nginx 日志
docker compose logs ruoyi-frontend

# 检查 API 代理
curl http://localhost/prod-api/captchaImage
```

### MQTT 连接问题
```bash
# 检查 EMQX 状态
docker exec xindong-mqtt emqx ctl status

# 访问管理面板
# http://HOST:18083  (admin / public)
```

### 设备网关无数据
```bash
# 检查网关健康
curl http://localhost:4000/health

# 检查 Kafka 连接
docker compose logs device-gateway | grep -i kafka

# 手动测试 MQTT 发布
docker exec xindong-mqtt emqx_ctl pub 'device/TEST-001/data' '{"accelX":0.1,"accelY":-9.8,"accelZ":0.02,"gyroX":0,"gyroY":0,"gyroZ":0,"pitch":0,"roll":0,"yaw":0}'
```

## 更新部署

```bash
# 拉取最新代码
git pull origin main

# 重建并重启 (零停机)
docker compose up -d --build

# 仅重建某个服务
docker compose up -d --build ruoyi-backend
```

## 监控建议

生产环境建议添加:
- **Prometheus + Grafana**: 指标监控
- **Loki**: 日志聚合
- **AlertManager**: 告警通知
- **EMQX 内置监控**: http://HOST:18083

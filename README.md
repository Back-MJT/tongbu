# 昕动智能 (Xindong Corp) — 健康装备数字底座

> 当前重构主线: 小程序扫码器械 + GY-BLE25T 蓝牙连接 + 若依后端 + 本地 MySQL + 本地 Redis

## 当前推荐启动方式

当前项目默认运行主线已经切换为:

- 小程序负责扫码器械二维码、连接 `GY-BLE25T`、本地计数、上传训练结果
- `RuoYi-Backend` 作为统一后端入口
- 数据库使用本地 `MySQL`
- 缓存使用本地 `Redis`
- `ruoyi-iot` 集成在若依后端中

推荐初始化顺序:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/init_local_mysql.sh
```

初始化说明见:

- [RuoYi-Backend/sql/mysql-init-order.md](./RuoYi-Backend/sql/mysql-init-order.md)
- [RuoYi-Backend/docs/start-local-backend.md](./RuoYi-Backend/docs/start-local-backend.md)

## 历史 Docker Compose 说明

下面的 `docker-compose.yml` 仍然保留，用于早期网关 / Kafka / EMQX / Python 干预引擎联调参考。
它**不是**当前默认主链路。

## 一键启动（Legacy Docker Compose）

```bash
# 启动全部服务（前台）
docker-compose up

# 启动全部服务（后台）
docker-compose up -d

# 查看所有服务日志
docker-compose logs -f

# 停止所有服务
docker-compose down
```

**一键启动后访问：**
- 设备网关 API: http://localhost:4000
- 干预引擎 API: http://localhost:4001
- 干预引擎 Swagger 文档: http://localhost:4001/docs
- EMQX Dashboard: http://localhost:18083 (admin / public)

---

## 服务架构

```
┌─────────────────────────────────────────────────────────────┐
│                      昕动智能数字底座                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  设备 (BLE 心率带 / WiFi 血压计 / 体重秤)                    │
│       │                                                     │
│       ▼                                                     │
│  ┌─────────────┐    HTTP POST    ┌──────────────────┐     │
│  │ 设备接入网关  │ ──────────────►│   干预引擎        │     │
│  │ :4000 (TS)  │                 │   :4001 (Python) │     │
│  └──────┬──────┘                 └────────┬─────────┘     │
│         │ Kafka topic                     │                │
│         ▼                                 ▼                │
│  ┌─────────────┐    ┌──────┐    ┌──────────────────┐     │
│  │    Kafka    │    │ Redis│    │  PostgreSQL      │     │
│  │   :9092     │    │:6379 │    │  TimescaleDB :5432│     │
│  └─────────────┘    └──────┘    └──────────────────────┘  │
│                                                              │
│  ┌─────────────┐                                            │
│  │  MQTT/EMQX  │  ← 设备协议接入 (BLE/WiFi/Zigbee)         │
│  │ :1883 :8083 │                                            │
│  └─────────────┘                                            │
└─────────────────────────────────────────────────────────────┘
```

---

## 服务说明

| 服务 | 端口 | 技术栈 | 说明 |
|------|------|--------|------|
| **device-gateway** | 4000 | Node.js 20 + TypeScript | 设备接入网关，多协议适配 |
| **intervention-engine** | 4001 | Python 3.11 + FastAPI | 个体化干预引擎 |
| **postgres** | 5432 | TimescaleDB | 时序数据库 |
| **redis** | 6379 | Redis 7 | 缓存 + 消息队列 |
| **kafka** | 9092 | Apache Kafka 3.7 | 消息流平台 |
| **mqtt** | 1883 / 8083 / 18083 | EMQX 5.3 | MQTT broker + Dashboard |

---

## 本地开发

### 设备接入网关 (TypeScript)

```bash
cd device-integration
npm install
npm run dev     # 开发模式（热重载）
npm run build   # 编译 TypeScript
npm test        # 运行测试
```

### 干预引擎 (Python)

```bash
cd intervention-engine
pip install -r requirements.txt
python -m uvicorn src.api.fastapi_server:app --host 127.0.0.1 --port 4001 --reload

# 测试
python -m pytest tests/ -q        # 单元测试 (138 tests)
python -m e2e.runner              # E2E 场景测试
```

---

## 环境变量

启动前设置必要的环境变量：

```bash
export DB_PASSWORD=xindong          # PostgreSQL 密码
export MQTT_PASSWORD=public         # EMQX Dashboard 密码
export API_KEY=xindong-dev-key      # 设备网关 API 密钥
```

或在 `docker-compose.yml` 同目录下创建 `.env` 文件：

```env
DB_PASSWORD=xindong
MQTT_PASSWORD=public
API_KEY=xindong-dev-key
```

---

## 端到端联调 (XIN-26)

完整链路验证已通过（2/2 场景）：

```bash
# 运动心率干预场景
# 睡眠干预场景
# 详见 intervention-engine/e2e/XIN-26-REPORT.md
```

---

## CI/CD

| 服务 | CI 状态 | GitHub Actions |
|------|---------|----------------|
| 设备接入网关 | ✅ | `.github/workflows/ci.yml` |
| 干预引擎 | ✅ | `.github/workflows/ci.yml` |

---

## 相关文档

- [干预引擎 README](./intervention-engine/README.md)
- [设备接入网关 README](./device-integration/README.md)
- [技术架构](./TECHNICAL_ARCHITECTURE.md)
- [产品路线图](./ROADMAP.md)
- [BP 文档](./bp-draft-v1.md)

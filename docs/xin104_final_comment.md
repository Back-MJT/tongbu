## XIN-104 W1技术架构评审 — 完成报告

**完成时间**: 2026-04-15
**状态**: ✅ 交付物完成，Paperclip状态更新需CEO协助解除run lock

---

### 核心交付物

`docs/MVP_technical_architecture_review.md` (v1.1) — MVP技术架构评审文档

---

### 1. 技术架构评审 ✅

- **小程序框架: Taro 4.x (React)** — BLE兼容无障碍，团队React技能复用，多端扩展战略价值
- **BLE数据路径: GY-BLE25T GATT** — FFE4 Notify, 22字节/帧，国产9轴IMU+BLE5.0一体模组自带固件
- **后端架构: RuoYi单体** — ruoyi-iot(MQTT) + ruoyi-intervention(Claude) + ruoyi-system(认证)
- **数据库: PostgreSQL + Redis** — 训练数据+处方+设备绑定

---

### 2. API接口契约定义 ✅

**C端小程序API (`/api/c/*`)**:
- `POST /api/c/auth/wechat_login` — 微信code换JWT
- `POST /api/c/auth/register` — 用户建档
- `GET /api/c/dashboard` — 今日任务+本周趋势+AI建议
- `GET/POST /api/c/devices` — 设备列表/绑定
- `POST /api/c/training/upload` — 训练数据批量上传
- `POST /api/c/training/plan/generate` — Claude生成训练方案

---

### 3. 小程序技术选型确认 ✅

| 维度 | Taro 4.x | Uni-app | 原生 |
|------|----------|---------|------|
| BLE能力 | ✅ 完全兼容 | ⚠️ 需条件编译 | ✅ |
| 团队技能 | ✅ React积累 | ❌ 无Vue经验 | ⚠️ 无跨端价值 |
| 多端扩展 | ✅ 微信/抖音/京东 | ✅ | ❌ |
| 结论 | **胜出** | 备选 | 快速原型 |

**脚手架**: `intervention-engine/client/mini-program/` 已有Taro空项目

---

### 4. RuoYi认证扩展方案 ✅

微信授权 → `wx.login()` code → RuoYi后端换openid → 颁发JWT

```
JWT payload: { sub: openid, tenantId: manufacturerId, role: "end_user" }
token有效期: 7天，自动续期
```

---

### 5. 数据库表设计 ✅

```sql
-- user_training_record (训练记录)
CREATE TABLE user_training_record (
  user_id VARCHAR(64), device_id BIGINT, record_date DATE,
  completed_sets INT, target_sets INT, duration_sec INT, adherence DECIMAL(5,2),
  UNIQUE KEY uk_user_device_date (user_id, device_id, record_date)
);

-- training_prescription (训练处方)
CREATE TABLE training_prescription (
  prescription_id BIGINT, user_id VARCHAR(64), device_id BIGINT,
  prescription_json JSON, ai_message TEXT, stage VARCHAR(20),
  generated_at DATETIME, expires_at DATETIME
);
```

---

### 6. W1验收Checklist (10项)

| # | 验收项 | 成功标准 |
|---|--------|---------|
| 1 | Taro脚手架可运行 | `npm run dev:weapp` 无编译错误 |
| 2 | 微信登录API | `/api/c/auth/wechat_login` 返回有效JWT |
| 3 | MQTT IMU数据 | ruoyi-iot MqttListener收到并入库 |
| 4 | 设备绑定API | `/api/c/devices/bind` 绑定成功 |
| 5 | 训练数据上传 | `/api/c/training/upload` 数据入库 |
| 6 | Claude方案API | `/api/c/training/plan/generate` 返回有效处方 |
| 7 | Dashboard API | `/api/c/dashboard` 返回完整数据 |
| 8 | DB Migration | 两张新表创建成功 |
| 9 | BLE模拟器适配 | GY-BLE25T FFE4格式，小程序可解析 |
| 10 | Docker部署 | `docker-compose up` 全部服务启动 |

---

### 7. GY-BLE25T模组同步（XIN-111）

- ~~nRF52840 Eddystone广播~~ → **废弃**
- **GY-BLE25T GATT连接 + FFE4 Notify** → **确认**
- 22字节/帧：11个int16字段(ACC_X/Y/Z, GYR_X/Y/Z, MAG_X/Y/Y, PITCH/ROLL/YAW) + batteryLevel + temperature
- 固件自带，无需自研嵌入式代码

---

### 8. 下游依赖

```
XIN-104（本文档）→ 确认后启动:
- XIN-106: 小程序脚手架完善 (FE)
- XIN-107: ClaudeEngineService健身提示词适配 (AE)
- XIN-103: BLE模拟器适配GY-BLE25T格式 (FE)
- XIN-108: PostgreSQL Migration (FE)
```

---

### ⚠️ Blocker: Paperclip Run Lock

**问题**: XIN-104存在run ownership conflict (409)，状态已为`in_progress`但`checkoutRunId: null`，无法通过API更新状态。

**原因**: 可能是上次心跳run遗留的execution lock未释放。

**影响**: 状态无法从`in_progress`更新为`done`。

**请求**: CEO协助确认或等待锁自动过期后更新状态。

**说明**: 交付物已完整保存在 `docs/MVP_technical_architecture_review.md`，任务实质已完成。

# W1: MVP技术架构评审文档

> 版本: v1.0
> 日期: 2026-04-15
> 作者: Product Manager
> 任务: XIN-104
> 状态: 完成
> 关联文档: MVP_integration_plan.md, PRD_C_MINIPROGRAM_v2.0.md, XIN-82_IoT设备管理模块技术方案.md

---

## 1. 技术选型决策

### 1.1 小程序框架: **Taro 4.x (React)** ✅ 确认

| 维度 | 评估 | 结论 |
|------|------|------|
| BLE能力 | 微信小程序BLE API在Taro下完全可用，无兼容性损失 | ✅ 无障碍 |
| 团队技能 | b2b-frontend已用React+TypeScript，无需额外学习 | ✅ 匹配 |
| 多端扩展 | Taro支持微信/支付宝/抖音/京东/百度，Phase 3可用 | ✅ 战略价值 |
| 生态成熟度 | Taro 4.x + React 18稳定，NutUI组件库完善 | ✅ |
| 包体积 | 编译后符合微信8MB限制（核心代码+NutUI按需加载） | ✅ |

**决策**: 采用Taro 4.x，脚手架已存在于 `intervention-engine/client/mini-program/`，直接在此基础上开发。

**备选说明**: Uni-app虽支持更多平台，但团队无Vue技能积累，学习曲线不必要。微信小程序原生开发简单但无React复用价值。

### 1.2 BLE数据获取路径: **小程序直连BLE** ✅ 确认

MVP采用小程序通过BLE GATT连接GY-BLE25T模组，订阅FFE4 Notify接收IMU数据（22字节/帧）。数据流：

```
GY-BLE25T BLE GATT(FFE4 Notify) → 微信小程序BLE模块 → 小程序本地缓存 → HTTPS上传 → RuoYi后端
```

> **注意**: 之前架构基于nRF52840 Eddystone广播模式，已废弃。GY-BLE25T是国产9轴IMU+BLE 5.0一体模组，自带固件，使用GATT连接+Notify推送数据。
> 详见: `docs/GY-BLE25T_IMU模块验证报告.md`

**替代方案（暂不需要）**:
- 力康来展厅模式：设备MQTT直连EMQX，小程序走 HTTPS API 获取数据（不需要BLE）
- 未来：蓝牙网关（树莓派）作为中继，适合多设备场景

### 1.3 后端架构: **RuoYi单体 + ruoyi-iot + ruoyi-intervention** ✅ 确认

```
RuoYi-Backend (:8080)
  ├── ruoyi-admin       → 管理后台 API
  ├── ruoyi-system      → 用户/认证/RBAC
  ├── ruoyi-iot         → 设备管理 + MQTT监听 + IMU数据
  └── ruoyi-intervention → 训练引擎 + Claude API
```

**决策依据**:
- ruoyi-iot已完成MQTT Listener（`MqttListener.java`），Topic格式 `device/{deviceCode}/data|status|heartbeat`
- ruoyi-intervention已有 `ClaudeEngineService`，直接复用
- 小程序后端API走 `/api/c/*` 前缀（新增controller）

### 1.4 数据库: **PostgreSQL + Redis** ✅ 确认

- **PostgreSQL**: 现有RuoYi MySQL之外的独立库？待确认（建议复用现有MySQL）
- **Redis**: MQTT Listener已用，缓存IMU实时数据和设备状态
- **时序数据**: IMU原始数据写入PostgreSQL，训练数据聚合后供API使用

---

## 2. API接口契约

### 2.1 小程序C端API (`/api/c/*`)

所有接口均需JWT认证（微信登录后颁发）。

#### 认证模块

```
POST /api/c/auth/wechat_login
  Request:  { code: string }          // 微信wx.login()返回的code
  Response: { token: string, user: { id, nickname, avatar, tenantId } }
  Error:    401 未授权 / 500 服务错误

POST /api/c/auth/register
  Request:  { token: string, profile: { age?, gender?, height?, weight?, goal? } }
  Response: { profileId: string }
  约束:     首次登录必须注册，否则无法获取训练方案
```

#### 用户数据模块

```
GET /api/c/users/me
  Response: { id, nickname, avatar, tenantId, createdAt, profile: { age, gender, goal, stage } }

GET /api/c/dashboard
  Response: {
    todayTask: { prescriptionId, deviceId, deviceName, completedSets, targetSets, status },
    weeklyProgress: [{ date, adherence }],   // 0-100
    aiSuggestion: string,                    // Claude生成，一句话
    streak: number,                          // 连续训练天数
    healthScore: { power, endurance, flexibility, core, cardio }  // 五维评分 0-100
  }
```

#### 设备模块

```
GET /api/c/devices
  Response: { devices: [{ deviceId, deviceName, deviceType, bindTime, online, lastSeen }] }

POST /api/c/devices/bind
  Request:  { deviceCode: string }   // 扫码获取的设备编码（等同于MAC）
  Response: { success: boolean, device: Device }
  Error:    400 设备不存在 / 409 已被其他用户绑定

DELETE /api/c/devices/{deviceId}/unbind
  Response: { success: boolean }
  约束:     解绑后历史训练数据保留，新设备可重新绑定

GET /api/c/devices/{deviceId}/status
  Response: { online: boolean, lastSeen: datetime, batteryLevel?: number }
```

#### 训练数据模块

```
POST /api/c/training/upload
  Request: {
    deviceCode: string,
    records: [{
      timestamp: datetime,
      sets: number,           // 本地检测到的组数（增量）
      duration: number,       // 训练时长（秒）
      motionType: string      // "strength" | "cardio"
    }]
  }
  Response: { received: number }
  说明:     小程序本地缓存，训练结束后批量上传

GET /api/c/training/history?days=30
  Response: {
    records: [{
      date, deviceName, sets, duration,
      adherence: number       // 当日完成率
    }]
  }
```

#### 训练方案模块（调用Claude）

```
POST /api/c/training/plan/generate
  Request: {
    deviceId: string,
    forceRegenerate?: boolean   // 默认false，60分钟内缓存
  }
  Response: {
    prescriptionId: string,
    generatedAt: datetime,
    exercises: [{
      name: string,
      sets: number,
      reps: number,
      restSec: number
    }],
    aiMessage: string           // 一句话鼓励语
  }

GET /api/c/training/plan/current
  Response: { prescription: Prescription | null }

PUT /api/c/training/plan/adherence
  Request: { prescriptionId: string, completedSets: number }
  Response: { adherence: number }   // 更新后的依从率
```

### 2.2 内部微服务API (ruoyi-intervention → ruoyi-iot)

```
训练引擎调用设备数据（同一JVM内直接调用Service，不走HTTP）:

IImuDataService.getTrainingData(userId, deviceId, dateRange): TrainingData[]
  → 返回聚合后的训练记录（组数/时长/频率）

IIoTDeviceService.selectDeviceByCode(deviceCode): IoTDevice
  → 设备绑定校验
```

### 2.3 BLE模拟器 → MQTT

```
模拟器推送 Topic:  device/{deviceCode}/data
Payload: {
  "accelX": number, "accelY": number, "accelZ": number,
  "gyroX": number, "gyroY": number, "gyroZ": number,
  "sampleTime": "yyyy-MM-dd HH:mm:ss.SSS",
  "sequence": number,
  "batteryLevel": number,
  "motionType": "strength" | "idle",
  "stepCount": number
}
说明: 组数检测在模拟器固件层完成（Peak Detection算法），MQTT payload不含"组数"字段，
      组数由小程序BLE模块根据加速度阈值本地计算（或模拟器广播 rep_count 字段）
```

---

## 3. 关键技术决策

### 3.1 微信登录集成（RuoYi认证扩展）

**现状**: RuoYi使用Spring Security + JWT，登录需要username/password
**目标**: 小程序使用微信授权code换取JWT token

**实现方案**:
```
微信小程序                          RuoYi后端
    │                                  │
    ├── wx.login() ──────────────────→  │
    │   获得 code                       │
    │                                   ├── 用code换 openid（微信接口）
    │                                   ├── 查/创 C端用户记录
    │                                   └── 颁发自定义JWT（含 openid/tenantId）
    │ ←────────────────────────────── { token, user }
    │
    ├── 后续请求带 token ──────────────→ JWT验证（Shiro/JWT Filter）
```

**关键点**:
- 微信session_key不参与JWT颁发（已在服务端管理openid）
- JWT payload: `{ sub: openid, tenantId: manufacturerId, role: "end_user" }`
- token有效期7天，自动续期

### 3.2 数据库表设计（新增/确认）

沿用XIN-82设计的表，新增C端相关字段：

```sql
-- 新增: iot_device 绑定用户 (ruoyi-iot)
ALTER TABLE iot_device ADD COLUMN bind_user_id VARCHAR(64);  -- 绑定用户openid
ALTER TABLE iot_device ADD COLUMN bind_time DATETIME;         -- 绑定时间

-- 新增: 用户训练记录 (ruoyi-intervention)
CREATE TABLE user_training_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,           -- openid
  device_id BIGINT NOT NULL,              -- FK iot_device
  record_date DATE NOT NULL,
  completed_sets INT DEFAULT 0,
  target_sets INT DEFAULT 0,
  duration_sec INT DEFAULT 0,
  adherence DECIMAL(5,2) DEFAULT 0,        -- 0.00-100.00
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_device_date (user_id, device_id, record_date)
);

-- 新增: 训练处方 (ruoyi-intervention)
CREATE TABLE training_prescription (
  prescription_id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id VARCHAR(64) NOT NULL,
  device_id BIGINT NOT NULL,
  prescription_json JSON NOT NULL,         -- Claude返回的完整处方
  ai_message TEXT,                          -- 一句话鼓励
  stage VARCHAR(20) DEFAULT 'beginner',    -- beginner/growth/plateau/advanced
  generated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  expires_at DATETIME,                      -- 处方过期时间（7天）
  INDEX idx_user_id (user_id),
  INDEX idx_expires_at (expires_at)
);
```

### 3.3 Claude API调用流程（健身场景适配）

复用 `ruoyi-intervention ClaudeEngineService`，需新增健身场景的提示词模板：

```java
// 提示词模板（健身场景）
System: 你是一位专业健身教练，擅长抗阻训练处方。
User:
用户：{nickname}，{age}岁，{gender}
设备：{deviceType}
目标：{goal}（增肌/塑形/维持）
训练阶段：{stage}（初学/成长/停滞/进阶）
过去30天：训练{count}次，平均{minutes}分钟/次
最近一次：完成{completedSets}组/目标{targetSets}组，依从率{adherence}%
当前处方完成度：{currentAdherence}%

请用中文给出：
1. 今日训练建议（3条动作，含组数/次数/休息时长）
2. 一句话鼓励语（像私教朋友，30字以内）
```

**降级策略**: Claude API超时3秒自动降级为规则引擎（基于stage的默认处方）。

### 3.4 数据流架构（端到端）

```
┌──────────────────────────────────────────────────────────────────┐
│                    微信小程序 (Taro 4.x)                          │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────────┐  │
│  │ 首页看板 │  │ 每日任务 │  │ 进度追踪 │  │ BLE设备绑定      │  │
│  └────┬────┘  └────┬─────┘  └────┬─────┘  └────────┬────────┘  │
│       │            │              │                  │            │
│       └────────────┴──────────────┴──────────────────┘            │
│                            │ HTTPS /api/c/*                      │
└────────────────────────────┼─────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│              RuoYi-Backend (:8080)                                │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  /api/c/*  C端专用控制器                                      │ │
│  │  JWT验证 → Tenant隔离 → 业务逻辑                              │ │
│  └──────────────────────────────────────────────────────────────┘ │
│       │                    │                    │                │
│       ▼                    ▼                    ▼                │
│  ┌───────────┐      ┌───────────────┐    ┌─────────────────┐   │
│  │ruoyi-system│      │ ruoyi-iot     │    │ruoyi-intervention│   │
│  │用户/认证   │      │ MQTT Listener │    │ Claude训练引擎   │   │
│  │微信登录JWT │      │ 设备管理      │    │ 规则引擎        │   │
│  └───────────┘      └───────┬───────┘    └────────┬────────┘   │
│                             │                     │              │
│                       PostgreSQL               Claude API         │
│                       (训练记录)               (训练方案)         │
└──────────────────────────────────────────────────────────────────┘
                             ▲
                             │ MQTT / device/{code}/data
                      ┌──────┴──────────────┐
                      │  BLE模拟器 或        │
                      │  GY-BLE25T模组      │
                      │  (9轴IMU+BLE GATT) │
                      └─────────────────────┘
```

---

## 4. W1验收Checklist

| # | 验收项 | 成功标准 | 状态 |
|---|--------|---------|------|
| 1 | Taro脚手架可运行 | `npm run dev:weapp` 无编译错误，微信开发者工具可预览 | ⬜ |
| 2 | 小程序微信登录 | 调用 `/api/c/auth/wechat_login`，返回有效JWT | ⬜ |
| 3 | IMU数据MQTT上报 | 模拟器推送MQTT消息，ruoyi-iot `MqttListener` 收到并写入数据库 | ⬜ |
| 4 | 设备绑定API | 小程序扫码调用 `/api/c/devices/bind`，设备绑定成功 | ⬜ |
| 5 | 训练数据上传API | 小程序调用 `/api/c/training/upload`，数据入库 | ⬜ |
| 6 | Claude训练方案API | 调用 `/api/c/training/plan/generate`，返回有效训练处方 | ⬜ |
| 7 | Dashboard API | `/api/c/dashboard` 返回今日任务/本周趋势/AI建议 | ⬜ |
| 8 | 数据库Migration | `user_training_record` 和 `training_prescription` 表创建成功 | ⬜ |
| 9 | BLE模拟器适配 | 模拟器发送GY-BLE25T FFE4 Notify格式数据(22字节/帧)，小程序BLE模块可接收解析 | ⬜ |
| 10 | Docker部署验证 | `docker-compose up` 全部服务启动，RuoYi可访问 | ⬜ |

---

## 5. 关键风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 微信登录资质审核耗时 | 上线延迟 | MVP先用手机号登录测试，微信登录资质后补 |
| BLE在部分Android机型兼容性 | 设备绑定失败 | W2提前测试主流Android机型（华为/小米/OPPO） |
| Claude API延迟>3秒 | 体验差 | 3秒超时降级Mock，日志记录所有超时事件 |
| MQTT Listener启动依赖EMQX | 部署顺序依赖 | Docker Compose `depends_on` 确保EMQX先启动 |

---

## 6. 依赖关系

```
XIN-104 技术架构评审（本文档）
    ↓ 确认后输入
XIN-106 小程序脚手架完善（FE）
XIN-107 ClaudeEngineService健身提示词适配（AE）
XIN-103 BLE模拟器适配（FE）
XIN-108 PostgreSQL Migration（FE）
```

---

## 7. 文档更新记录

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-04-15 | PM | 初始版本：技术选型确认、API契约定义、W1验收checklist |
| v1.1 | 2026-04-15 | CEO | IMU模组修正：nRF52840(Eddystone广播) → GY-BLE25T(GATT连接+Notify)，同步更新数据流、架构图、验收项 |

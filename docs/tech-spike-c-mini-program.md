# C端微信小程序技术预研

> 版本: v0.2 (更新 scaffold 现状 + 补充健康师/设备页面)
> 日期: 2026-04-12
> 作者: Founding Engineer
> 状态: 预研完成，脚手架已创建，待 PM 确认 + 后端 API 实现

---

## 1. 背景与目标

Phase 3 战略方向已确定：**B2B2C**，通过机构网络触达终端消费者。C端首选载体为微信小程序。

本文档为 Phase 3 C端小程序 MVP 提供：
1. **技术选型建议**（微信小程序原生 vs 跨端框架）
2. **API设计**（C端需要哪些后端接口）
3. **数据流设计**（C端与现有B2B平台的数据对接点）
4. **开发脚手架**（项目骨架代码，已创建）

---

## 2. 技术选型对比

### 2.1 选项概览

| 维度 | 微信小程序原生 | Taro 4.x (React) | uni-app (Vue) |
|------|--------------|-----------------|---------------|
| 学习成本 | 低（原生语法） | 中（需熟悉React） | 低（Vue语法） |
| 多端支持 | 仅微信 | 微信/支付宝/抖音等 | 15+平台 |
| 生态成熟度 | 官方，最成熟 | 较成熟，社区活跃 | 成熟，插件丰富 |
| 团队现有技能 | TypeScript/React (b2b-frontend) | React（匹配） | Vue（非匹配） |
| 性能 | 最优 | 略低于原生 | 略低于原生 |
| 复杂交互支持 | 受限 | 好 | 好 |
| 包体积 | 小 | 较大 | 中等 |

### 2.2 推荐方案：Taro 4.x (React)

**推荐理由：**

1. **技能匹配**：b2b-frontend 已使用 React + TypeScript，团队无需学习新框架
2. **多端扩展**：Taro 支持微信/支付宝/抖音/京东/百度等，一次开发多端编译
3. **长期价值**：Phase 3 可能需要抖音/支付宝小程序，Taro 可覆盖
4. **社区成熟**：Taro 4.x + React 17/18 组合稳定，社区生态完善
5. **H5降级**：Taro 可编译为 H5，Phase 3 未来可不做 APP 只做小程序

**uni-app 的局限**：团队 Vue 技能不足，学习曲线不必要。

### 2.3 技术栈详情

```
C端小程序 (Taro 4.x)
├── 框架: Taro 4.x (React 18)
├── 语言: TypeScript 5.x
├── 状态管理: Zustand (轻量, 适合小程序)
├── 路由: Taro Router
├── HTTP: Taro Request (内置) + apiClient 封装
├── UI组件: NutUI (京东开源，支持 Taro)
├── 图表: echarts-for-wechat (运动/睡眠趋势图)
├── 构建: Webpack 5 (Taro 内置)
└── 样式: SCSS + CSS Modules

后端扩展 (FastAPI/Python)
├── 现有: intervention-engine/src/api/fastapi_server.py
├── 需新增: C端专用 API 路由 (/api/c/*)
└── 需新增: 小程序认证中间件 (JWT + 邀请码)
```

---

## 3. C端 API 设计

### 3.1 现有 API 盘点

| 现有端点 | 用途 | C端复用 |
|---------|------|--------|
| `POST /api/profiles` | 创建健康档案 | ✅ 直接复用 |
| `GET /api/profiles/:id` | 获取档案 | ✅ 直接复用 |
| `POST /api/prescriptions/exercise` | 运动处方生成 | ✅ 直接复用 |
| `POST /api/prescriptions/sleep` | 睡眠处方生成 | ✅ 直接复用 |
| `POST /api/effects` | 开始效果追踪 | ✅ 直接复用 |
| `POST /api/effects/:id/adherence` | 记录依从性 | ✅ 直接复用 |

### 3.2 C端新增 API

#### 认证相关（新增）

```
POST /api/c/auth/login_by_wechat
  Request: { code: string }          // 微信授权 code
  Response: { token: string, user: { id, nickname, avatar } }

POST /api/c/auth/register
  Request: { token: string, inviteCode: string, profile: { demographic } }
  Response: { profileId: string }

POST /api/c/auth/bind_invite_code
  Request: { inviteCode: string }
  Response: { success: boolean }
```

#### 用户数据（新增）

```
GET /api/c/users/me
  Response: { id, nickname, avatar, tenantId, createdAt }

GET /api/c/dashboard
  Response: {
    todayTasks: Prescription[],
    weeklyProgress: { date, adherence }[],
    healthScores: { category, score, trend }[],
    streak: number
  }

GET /api/c/health_scores
  Response: {
    overall: number,
    cardiovascular: number,
    sleep: number,
    nutrition: number,
    stress: number,
    history: { date, scores }[]
  }
```

#### 干预任务（新增/复用）

```
GET /api/c/prescriptions/today
  Response: { prescriptions: Prescription[] }

POST /api/c/prescriptions/:id/start
  Response: { effectId: string }

POST /api/c/prescriptions/:id/adherence
  Request: { completed: boolean, notes?: string }
  Response: { updatedEffect: Effect }

GET /api/c/prescriptions/:id/details
  Response: { prescription: Prescription, effect: Effect, progress: number }
```

#### 设备数据（复用/扩展）

```
GET /api/c/devices
  Response: { devices: Device[] }

POST /api/c/devices/bind
  Request: { deviceId: string }
  Response: { success: boolean }

GET /api/c/vital_signs/history?days=7
  Response: { history: { date, heartRate, sleepHours, steps }[] }
```

#### 健康师咨询（新增）

```
GET /api/c/coaches
  Response: { coaches: { id, name, title, avatar, online: boolean }[] }

POST /api/c/consultations/request
  Request: { coachId: string, question: string }
  Response: { consultationId: string }
```

### 3.3 API 前缀约定

| 前缀 | 访问控制 | 说明 |
|------|---------|------|
| `/api/profiles/*` | B2B 认证 (JWT) | B2B 平台现有端点 |
| `/api/b2b/*` | B2B 认证 (JWT) | B2B 平台管理端点 |
| `/api/c/*` | C端认证 (JWT) | C端小程序专用端点 |

---

## 4. 数据流设计

### 4.1 C端数据架构

```
┌─────────────────────────────────────────────────────────────┐
│  C端微信小程序 (Taro)                                        │
│  ┌─────────┐  ┌──────────┐  ┌─────────────┐  ┌──────────┐  │
│  │ 健康看板 │  │ 每日任务 │  │ 进度追踪    │  │ 健康师  │  │
│  └────┬────┘  └────┬─────┘  └──────┬──────┘  └────┬────┘  │
└───────┼────────────┼──────────────┼──────────────┼───────┘
        │            │              │              │
        ▼            ▼              ▼              ▼
┌───────────────────────────────────────────────────────────┐
│  FastAPI 网关 (C端专用路由: /api/c/*)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ JWT 验证 → 租户隔离 → 邀请码绑定 → 请求转发          │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐  ┌────────────┐  ┌─────────────┐
│ 干预引擎     │  │ B2B API    │  │ 设备数据    │
│ Python       │  │ (FastAPI)  │  │ (Kafka/    │
│ /api/profiles│  │ /api/b2b   │  │  PostgreSQL)│
│ /api/presc.. │  │            │  │             │
└──────────────┘  └────────────┘  └─────────────┘
```

### 4.2 多租户数据隔离

**C端用户 → 租户关联**：
- C端用户注册时必须提供 `inviteCode`
- `inviteCode` 映射到 `B2BOrganization.id`（机构）
- JWT token 中包含 `tenantId`，所有查询自动过滤

```
C端用户 JWT Payload:
{
  "sub": "user_id",
  "tenantId": "org_xxx",    // 关联机构
  "role": "end_user",
  "iat": ...,
  "exp": ...
}
```

### 4.3 与现有B2B平台的数据对接

| B2B 数据 | C端访问方式 | 隔离策略 |
|---------|-----------|---------|
| 健康档案 | 通过干预引擎 API | 用户粒度隔离，JWT 验证 |
| 处方数据 | 通过干预引擎 API | 用户粒度隔离 |
| 机构健康师 | 独立 `/api/c/coaches` | 仅展示，不暴露管理功能 |
| 机构用户列表 | C端不可访问 | B2B 专属 |
| 租户配置 | C端不可访问 | B2B 专属 |

---

## 5. 开发脚手架（已创建）

### 5.1 目录结构

```
intervention-engine/client/mini-program/
├── src/
│   ├── pages/
│   │   ├── index/                # 首页（健康看板）
│   │   │   ├── index.tsx
│   │   │   └── index.scss
│   │   ├── task/                # 每日任务详情
│   │   │   ├── task.tsx
│   │   │   └── task.scss
│   │   ├── progress/            # 进度追踪
│   │   │   ├── progress.tsx
│   │   │   └── progress.scss
│   │   ├── profile/            # 个人中心
│   │   │   ├── profile.tsx
│   │   │   └── profile.scss
│   │   └── auth/               # 登录/注册
│   │       ├── login.tsx
│   │       └── login.scss
│   ├── api/
│   │   ├── client.ts            # HTTP client 封装 (Taro Request + Token自动注入)
│   │   ├── auth.ts              # 认证 API (微信登录/手机号登录/注册)
│   │   ├── dashboard.ts         # 看板 API
│   │   ├── prescription.ts      # 处方 API
│   │   └── mock.ts              # API mock（开发用）
│   ├── store/
│   │   ├── index.ts             # Zustand store 入口
│   │   └── auth.ts              # 认证状态 (Zustand)
│   ├── models/
│   │   └── types.ts             # TypeScript 类型定义
│   ├── assets/                  # Tab bar 图标 (SVG)
│   ├── app.config.ts            # Taro 路由配置
│   ├── app.tsx                  # 应用入口
│   └── app.scss                 # 全局样式
├── package.json                 # Taro 4.x + React 18
├── tsconfig.json
└── project.config.json          # 微信小程序项目配置（需补充）
```

### 5.2 核心类型定义（已有）

```typescript
// src/models/types.ts

export type InterventionType = 'exercise' | 'sleep' | 'nutrition' | 'stress';
export type ScoreTrend = 'up' | 'down' | 'stable';
export type PrescriptionStatus = 'active' | 'completed' | 'paused';

export interface User {
  id: string;
  nickname: string;
  avatar?: string;
  tenantId: string;
  createdAt: string;
}

export interface Prescription {
  prescriptionId: string;
  userId: string;
  type: InterventionType;
  title: string;
  description: string;
  frequency: string;
  duration: string;
  startDate: string;
  endDate?: string;
  status: PrescriptionStatus;
}

export interface DailyTask {
  prescription: Prescription;
  effectId: string;
  progress: number; // 0-100
  completed: boolean;
  completedAt?: string;
}

export interface HealthScore {
  category: 'cardiovascular' | 'sleep' | 'nutrition' | 'stress' | 'overall';
  score: number; // 0-100
  trend: ScoreTrend;
}

export interface DailyAdherence {
  date: string; // YYYY-MM-DD
  adherence: number; // 0-100
}

export interface Dashboard {
  todayTasks: DailyTask[];
  weeklyProgress: DailyAdherence[];
  healthScores: HealthScore[];
  streak: number;
}

export interface Device {
  deviceId: string;
  type: 'smart_watch' | 'scale' | 'blood_pressure' | 'sleep_monitor';
  name: string;
  bound: boolean;
  lastSync?: string;
}

export interface Coach {
  id: string;
  name: string;
  title: string;
  avatar?: string;
  online: boolean;
}
```

### 5.3 API Client 封装（已有）

`client.ts` 实现了：
- 基于 Taro `request` 的 HTTP 封装
- Token 自动注入（从 `Taro.getStorageSync('token')`）
- 401 自动跳转登录页
- 统一错误处理

```typescript
// 使用方式
export const apiClient = {
  get: <T>(url: string, requiresAuth = true) => request<T>({ url, method: 'GET', requiresAuth }),
  post: <T>(url: string, data?: unknown, requiresAuth = true) => request<T>({ url, method: 'POST', data, requiresAuth }),
  put: <T>(url: string, data?: unknown, requiresAuth = true) => request<T>({ url, method: 'PUT', data, requiresAuth }),
  delete: <T>(url: string, requiresAuth = true) => request<T>({ url, method: 'DELETE', requiresAuth }),
};
```

### 5.4 Mock API（已有）

`mock.ts` 包含完整 mock 数据：
- `mockUser` - 模拟用户信息
- `mockPrescriptions` - 模拟运动/睡眠处方
- `mockDashboard` - 模拟完整看板数据（含依从性趋势、评分）

---

## 6. 补充脚手架：健康师咨询 + 设备绑定页面

以下页面脚手架尚未创建，建议在 Phase 3 MVP 开发时一并实现：

### 6.1 健康师列表页面 `pages/coach/list.tsx`

```typescript
// 需新增 API: GET /api/c/coaches
export interface CoachListResponse {
  coaches: Coach[];
}
```

页面功能：
- 展示机构健康师列表（头像、职称、在线状态）
- 点击发起咨询请求（需手机号验证）
- 页面路径: `/pages/coach/list`

### 6.2 设备绑定页面 `pages/device/bind.tsx`

```typescript
// 需新增 API: GET /api/c/devices, POST /api/c/devices/bind
export interface DeviceListResponse {
  devices: Device[];
}
```

页面功能：
- 展示已绑定设备列表
- 扫码绑定新设备（调用微信扫码 `wx.scanCode`）
- 设备数据同步状态展示
- 页面路径: `/pages/device/bind`

---

## 7. 实施建议

### 7.1 Phase 3 MVP 优先级（推荐）

| 优先级 | 功能 | 预计工作量 | 脚手架状态 |
|-------|------|----------|-----------|
| P0 | 微信登录 + 邀请码注册 | 1周 | ✅ 已有 login.tsx，需后端 API |
| P0 | 健康数据看板 | 1周 | ✅ 已有 index 页面，需后端 API |
| P0 | 每日干预任务 | 1周 | ✅ 已有 task 页面，需后端 API |
| P0 | 进度追踪（依从性 + 趋势图） | 1周 | ✅ 已有 progress 页面，需后端 API |
| P0 | 后端 `/api/c/*` 路由实现 | 1周 | ❌ 需开发 |
| P1 | 健康师咨询入口 | 0.5周 | ❌ 需新建页面 |
| P1 | 设备绑定 | 0.5周 | ❌ 需新建页面 |
| P2 | 成就徽章系统 | 0.5周 | ❌ 需新建页面 |
| P2 | 社交分享 | 0.5周 | ❌ 需新建页面 |

### 7.2 关键风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 微信登录资质审核 | 中 | 先用手机号登录，微信登录申请中 |
| 小程序包体积超限 | 低 | 按需加载，NutUI 按组件引入 |
| 后端 API 性能 | 中 | 先走 FastAPI，后续可加 Redis 缓存 |
| 多租户数据隔离 | 高 | JWT 中 tenantId，所有查询强制加 tenant 过滤 |

### 7.3 下一步行动

1. **PM 确认**：确认 Taro 方案，补充 C端 PRD（已有 `PRD_C_MINIPROGRAM_v1.0.md`）
2. **FE 启动**：基于脚手架创建项目，完成登录 + 注册流程
3. **BE 配合**：实现 `/api/c/*` 路由和微信登录中间件
4. **DevOps**：搭建微信小程序 CI/CD（GitHub Actions → 微信测试码）

---

## 8. 参考资料

- [Taro 官方文档](https://taro-docs.jd.com/)
- [NutUI 组件库](https://nutui.jd.com/)
- [微信小程序登录流程](https://developers.weixin.qq.com/miniprogram/dev/framework/open-ability/login.html)
- [echarts-for-wechat](https://github.com/ecomfe/echarts-for-wechat)
- 现有 `b2b-frontend/` (React + TypeScript) 可作为参考
- 现有 `intervention-engine/src/api/fastapi_server.py` 作为后端基础
- 现有脚手架 `intervention-engine/client/mini-program/`

# B2B 机构后台 MVP — 技术规格

> 版本: v0.1  
> 日期: 2026-04-11  
> 作者: Founding Engineer (昕动智能)  
> 状态: 草稿 — 待 CEO 审批后启动

---

## 1. 目标

在 `intervention-engine` FastAPI 服务 (port 4001) 上扩展 B2B 机构后台 API，为健康管理机构、康复医院、健身器材厂商提供多租户管理平台。

**MVP 范围（3周）**:
1. 多租户数据隔离层（基于 `tenant_id` 字段）
2. 机构/租户管理 API（管理员 CRUD）
3. 用户与角色权限体系（机构管理员、健康师、查看者）
4. Dashboard KPI API（活跃用户、设备在线率、运动人次、睡眠达标率）
5. 健康师工作台 API（查看/管理负责用户、干预方案追踪）

---

## 2. 系统架构

### 2.1 服务扩展策略

```
existing: intervention-engine (port 4001, FastAPI, in-memory)
new:      B2B APIs extend same FastAPI app, share PostgreSQL
```

**决策理由**：
- 不新增独立服务，减少部署复杂度
- intervention-engine 已有 FastAPI 基础设施
- 共享 PostgreSQL (TimescaleDB) 实现持久化
- Kafka/Redis 基础设施不变

### 2.2 数据流向

```
设备 → device-gateway → Kafka → intervention-engine (算法处理)
                                              ↓
机构用户 ← B2B API (port 4001) ← PostgreSQL ← intervention-engine (存储)
     ↓
健康师工作台 (查看/管理用户画像、干预方案)
```

### 2.3 多租户隔离模型

所有核心表增加 `tenant_id` 字段：

```
tenants            — 租户/机构表
users              — B2B 平台用户（含 role, tenant_id）
devices            — 设备表（已有，加 tenant_id）
health_profiles    — 健康画像（已有，加 tenant_id）
prescriptions      — 干预方案（已有，加 tenant_id）
```

**隔离策略**：中间件注入 `tenant_id`，所有查询默认按 `tenant_id` 过滤。

### 2.4 权限模型

| 角色 | 范围 | 权限 |
|------|------|------|
| 机构管理员 | 全租户 | 全部功能 + 用户管理 + 报表导出 |
| 健康师 | 负责的用户 | 查看用户数据 + 方案管理 |
| 查看者 | 授权范围 | 仅查看首页概览 |

**实现**：JWT Bearer Token，包含 `sub`（用户ID）、`tenant_id`、`role`。

---

## 3. API 规格

### 3.1 认证

```
POST /api/auth/login          — 账号密码登录，返回 JWT
POST /api/auth/refresh        — 刷新 Token
```

### 3.2 租户管理（仅超级管理员 / 系统初始化）

```
POST   /api/tenants                    — 创建租户
GET    /api/tenants                    — 租户列表
GET    /api/tenants/{tenant_id}        — 租户详情
PUT    /api/tenants/{tenant_id}        — 更新租户
DELETE /api/tenants/{tenant_id}        — 禁用租户
```

### 3.3 用户管理（机构管理员）

```
POST   /api/users                      — 创建 B2B 用户
GET    /api/users                      — 用户列表（当前租户）
GET    /api/users/{user_id}            — 用户详情
PUT    /api/users/{user_id}            — 更新用户
DELETE /api/users/{user_id}            — 删除用户
```

### 3.4 Dashboard KPI

```
GET /api/dashboard/kpi                 — 首页 4 个 KPI
  Response: {
    activeUsers: number,        // 今日活跃用户
    deviceOnlineRate: number,   // 设备在线率
    exerciseCount: number,      // 今日运动人次
    sleepComplianceRate: number // 睡眠达标率
  }

GET /api/dashboard/trends?range=7d|30d  — 趋势数据
  Response: {
    dates: string[],
    activeUsers: number[],
    exerciseCount: number[],
    sleepCompliance: number[]
  }
```

### 3.5 设备管理

```
GET /api/devices                       — 设备列表（当前租户）
GET /api/devices/{device_id}           — 设备详情
GET /api/devices/{device_id}/data      — 设备数据
```

### 3.6 健康师工作台

```
GET  /api/coach/users                  — 负责的用户列表
GET  /api/coach/users/{user_id}/profile   — 用户画像
GET  /api/coach/users/{user_id}/prescriptions — 干预方案列表
PUT  /api/coach/prescriptions/{prescription_id} — 更新方案
```

---

## 4. 数据模型

### 4.1 新增 Pydantic 模型

```python
# src/models/tenant.py

class Tenant(BaseModel):
    id: str
    name: str
    plan: Literal["trial", "basic", "professional"]
    status: Literal["active", "suspended", "pending"]
    created_at: datetime

class B2BUser(BaseModel):
    id: str
    tenant_id: str
    username: str
    role: Literal["org_admin", "health_coach", "viewer"]
    managed_user_ids: list[str]  # 健康师负责的用户
    created_at: datetime

class KPISnapshot(BaseModel):
    active_users: int
    device_online_rate: float
    exercise_count: int
    sleep_compliance_rate: float
    recorded_at: datetime
```

### 4.2 数据库 Schema（PostgreSQL）

```sql
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'trial',
    status VARCHAR(50) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE users ADD COLUMN tenant_id UUID REFERENCES tenants(id);
ALTER TABLE devices ADD COLUMN tenant_id UUID REFERENCES tenants(id);
-- health_profiles, prescriptions 等已有表加 tenant_id
```

---

## 5. 实现计划

### Week 1: 多租户基础设施

- [ ] 数据库迁移脚本（新增 tenant_id 列 + tenants 表）
- [ ] Tenant/B2BUser Pydantic 模型
- [ ] 租户 CRUD API
- [ ] 用户管理 API（含角色）
- [ ] JWT 认证中间件（tenant_id 注入）
- [ ] 单元测试

### Week 2: Dashboard + 设备

- [ ] Dashboard KPI API（聚合查询）
- [ ] 趋势数据 API
- [ ] 设备列表/详情 API（租户过滤）
- [ ] device-gateway → intervention-engine 数据流（加 tenant_id）
- [ ] 单元测试 + 集成测试

### Week 3: 健康师工作台

- [ ] 健康师用户列表 API
- [ ] 用户画像查看 API
- [ ] 方案查看/更新 API
- [ ] 干预追踪 API
- [ ] E2E 场景测试

---

## 6. 技术决策

| 决策点 | 选项 | 选择 | 理由 |
|--------|------|------|------|
| 认证 | JWT / Session | JWT | 无状态，易扩展，跨服务 |
| 租户隔离 | schema/DB vs tenant_id 列 | tenant_id 列 | 简单，PostgreSQL 行级安全可补充 |
| 密码存储 | bcrypt | bcrypt | 标准，安全 |
| API 版本 | /v1 前缀 | /api 前缀 | 保持现有风格一致 |

---

## 7. 依赖

- intervention-engine FastAPI 服务（已有）
- PostgreSQL/TimescaleDB（已有，docker-compose）
- Redis（已有，用于 JWT token blacklist）

---

## 8. 测试策略

- 单元测试：每模块 >90% 覆盖率
- 集成测试：API 端到端（FastAPI TestClient）
- 租户隔离验证：跨租户数据访问被拒绝

---

## 9. 下一步

1. CEO 审批此规格
2. 创建 XIN-40（如果批准）
3. 开始 Week 1 数据库迁移脚本

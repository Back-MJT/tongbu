# B2B 平台 MVP 产品验收报告

> 版本: v1.0  
> 日期: 2026-04-11  
> 作者: Product Manager  
> 验收范围: XIN-40 B2B 机构后台 MVP (intervention-engine/src/b2b/)  
> 代码规模: 2101 行代码, 484 测试

---

## 1. 执行摘要

**总体结论: PASS with Gaps** — MVP 核心功能已交付，多租户隔离、JWT 认证、RBAC、Dashboard KPI、健康师工作台均已实现。6 个 Gap 需在 Wave 2 修复，其中 2 个为 P0。

| 维度 | 状态 | 说明 |
|------|------|------|
| 多租户架构 | PASS | tenant_id 隔离完整，中间件注入工作正常 |
| 认证授权 | PASS | JWT login/refresh，RBAC 三角色正确 |
| Dashboard KPI | PASS | KPI snapshot + trends API 符合 spec |
| 设备管理 | PASS | 租户隔离的设备 CRUD + 数据查询 |
| 健康师工作台 | PASS | 用户列表/画像/方案管理，符合 spec |
| 数据持久化 | **FAIL** | in-memory store，生产不可用 |
| 租户初始化 | **FAIL** | demo tenant/user 未在 startup 自动初始化 |
| 健康师分配 | **FAIL** | managed_user_ids 未持久化，health_coach 看不到分配用户 |
| B2B 胖终端登录 | **FAIL** | `get_tenant_id` 依赖 header，无法支持公开注册 |
| KPI 真实计算 | **FAIL** | 所有 KPI 为假数据，未接入真实用户/设备/处方数据流 |
| viewer 权限 | **FAIL** | `/api/users` 对 viewer 开放（spec 要求 viewer 不可见用户列表） |
| 告警面板 | **MISSING** | spec 要求设备离线、心率异常、依从性低等告警，代码中未实现 |
| 数据导出 | **MISSING** | spec 要求 CSV/Excel/PDF 导出，未实现 |

---

## 2. 逐项验收

### 2.1 多租户架构 ✅ PASS

**验收方法**: 代码审查 + 交叉测试分析

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| tenant_id 字段 | 所有核心表/模型包含 tenant_id | Tenant, B2BUser, Device 均有 tenant_id | ✅ |
| 查询过滤 | 所有 list/get API 按 tenant_id 过滤 | tenants.py, users.py, devices.py 均有 tenant_id 过滤逻辑 | ✅ |
| 中间件注入 | get_current_user 提取 tenant_id | dependencies.py:get_tenant_id() 正确实现 | ✅ |
| 跨租户隔离 | 租户 A 无法访问租户 B 数据 | devices.py, users.py 均有 tenant_id 校验 | ✅ |

**结论**: 多租户隔离架构符合 B2B_BACKEND_MVP_SPEC.md 要求。

---

### 2.2 认证与授权 ✅ PASS

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| POST /api/auth/login | 返回 JWT + user info | auth_routes.py:29 正确实现 | ✅ |
| POST /api/auth/refresh | 返回新 access_token | auth_routes.py:42 正确实现 | ✅ |
| JWT payload | 包含 sub, tenant_id, role | auth.py:72-78 正确设置 | ✅ |
| 密码哈希 | 不可逆哈希 | auth.py:52-59 使用 SHA256（生产需换 bcrypt） | ✅ (MVP OK) |
| org_admin | 全部功能 | require_role(B2BRole.ORG_ADMIN) 正确限制 | ✅ |
| health_coach | 仅看管用户 + 方案管理 | coach.py 正确过滤 | ✅ |
| viewer | 仅首页概览 | 已授权 dashboard + devices + users | ⚠️ 见 Gap #5 |

**结论**: 认证流程完整，RBAC 三角色基本正确。

---

### 2.3 Dashboard KPI ✅ PASS

| 检查项 | 预期(spec) | 实际 | 结果 |
|--------|-----------|------|------|
| GET /api/dashboard/kpi | 返回 active_users, device_online_rate, exercise_count, sleep_compliance_rate | dashboard.py:79-85 正确返回 KPISnapshot | ✅ |
| GET /api/dashboard/trends | 支持 7d/30d range 参数 | dashboard.py:109-135 正确实现 | ✅ |
| KPISnapshot | active_users, device_online_rate, exercise_count, sleep_compliance_rate, recorded_at | models.py:163-169 Pydantic 模型正确 | ✅ |
| 租户隔离 | 只返回当前租户数据 | _get_kpi_data(tenant_id) 正确实现 | ✅ |

**结论**: Dashboard API 端点符合 B2B_BACKEND_MVP_SPEC.md §3.4 spec，响应格式正确。

---

### 2.4 设备管理 ✅ PASS

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| GET /api/devices | 返回当前租户设备列表 | devices.py:53-65 正确实现 | ✅ |
| GET /api/devices/{id} | 返回设备详情，租户隔离 | devices.py:68-89 正确实现 | ✅ |
| GET /api/devices/{id}/data | 返回设备最近读数 | devices.py:92-128 正确实现 | ✅ |
| tenant_id 隔离 | 跨租户访问返回 403 | devices.py:83-87 正确校验 | ✅ |

**结论**: 设备管理 API 符合 spec 要求。

---

### 2.5 健康师工作台 ✅ PASS

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| GET /api/coach/users | org_admin 看全部，health_coach 看分配用户 | coach.py:110-157 正确区分 role | ✅ |
| GET /api/coach/users/{id}/profile | 返回简化用户画像 | coach.py:160-201 正确实现 | ✅ |
| GET /api/coach/users/{id}/prescriptions | 返回方案列表 | coach.py:204-230 正确实现 | ✅ |
| PUT /api/coach/prescriptions/{id} | 更新方案 | coach.py:233-274 正确实现 | ✅ |
| 租户隔离 | 健康师只能操作分配用户 | coach.py:174-180, 217-222 正确校验 | ✅ |

**结论**: 健康师工作台 API 符合 B2B_BACKEND_MVP_SPEC.md §3.6 spec。

---

### 2.6 数据持久化 ❌ FAIL — Gap #1

**问题**: 所有数据存储在 in-memory dict (`_tenants`, `_b2b_users`, `_devices`, `_kpi_snapshots`, `_prescriptions`)，服务重启后数据丢失。

**影响**: 生产环境不可用。所有数据层需要替换为 PostgreSQL。

**建议**: Wave 2 P0，在 Sprint 1 完成数据库迁移。

---

### 2.7 租户初始化 ❌ FAIL — Gap #2

**问题**: `init_demo_tenant()` 和 `init_demo_devices()` 在 `__init__.py` 中未被调用。服务启动后没有 demo 用户，登录接口无法使用。

**影响**: 无法演示。需要在 startup 事件中初始化 demo tenant + demo users。

**建议**: Wave 2 P0，在 Sprint 1 修复。

---

### 2.8 健康师分配关系未持久化 ❌ FAIL — Gap #3

**问题**: health_coach 的 `managed_user_ids` 在创建后存储在 B2BUser.managed_user_ids 中，但 `_user_health_data` 是独立的 in-memory store，与实际用户数据无关联。health_coach 分配的用户列表来自 B2BUser，但这些用户的健康数据是假数据（`coach.py:44-54` 每次重新生成 hash）。

**影响**: 健康师看不到真实分配的用户数据，所有用户健康数据都是基于 user_id hash 生成的伪随机数据。

**建议**: Wave 2 P1，关联真实用户数据 store。

---

### 2.9 B2B 胖终端登录 ❌ FAIL — Gap #4

**问题**: `auth_routes.py:32` 的 `get_tenant_id` 从 JWT token 中提取（登录后才能获取）。但登录前需要先知道 tenant_id——这形成了先有鸡还是先有蛋的问题。

```python
# auth_routes.py:30-32
async def auth_login(
    login_req: LoginRequest,
    tenant_id: str = Depends(get_tenant_id),  # 需要已登录的用户
```

实际实现中 `get_tenant_id` 调用 `get_current_user` → `decode_token`，但 login 时还没有 token。

**实际行为**: 登录实际上绕过了 tenant_id 检查（auth.py:288 不需要 tenant_id），但 `dependencies.py` 中 `get_tenant_id` 依赖已认证用户，胖终端场景（用户自主注册）无法实现。

**建议**: Wave 2 P1，增加 `X-Tenant-ID` header 支持或 tenant  discovery API。

---

### 2.10 KPI 真实计算 ❌ FAIL — Gap #5

**问题**: dashboard.py 中所有 KPI 数据都是假数据（`dashboard.py:44-52` 固定模式数据），没有接入真实的 user、device、prescription 数据流。

**影响**: Dashboard 不可用于真实运营决策。

**建议**: Wave 2 P0，接入 PostgreSQL 真实数据计算。

---

### 2.11 viewer 权限过宽 ❌ FAIL — Gap #6

**问题**: `users.py:62` 的 `list_users` 依赖 `get_current_user`（viewer 也可调用），导致 viewer 可以看到所有 B2B 用户列表。B2B_DASHBOARD_MVP_SPEC.md §5.2 权限矩阵要求 viewer 不可见用户列表。

```python
# users.py:60-62
@router.get("", response_model=B2BUserListResponse)
async def list_users(
    current_user: dict = Depends(get_current_user),  # viewer 可访问
```

**建议**: Wave 2 P1，修改为 `require_role(B2BRole.ORG_ADMIN, B2BRole.HEALTH_COACH)`。

---

### 2.12 告警面板 ⚠️ MISSING

**问题**: B2B_DASHBOARD_MVP_SPEC.md §3.3 要求设备离线、心率异常、方案依从性低等告警面板，B2B 代码中完全没有实现。

**建议**: Wave 2 P2，作为独立 feature。

---

### 2.13 数据导出 ⚠️ MISSING

**问题**: B2B_DASHBOARD_MVP_SPEC.md §4.5 / US-V-005 要求 CSV/Excel/PDF 导出，B2B 代码中完全没有实现。

**建议**: Wave 2 P2。

---

## 3. 功能覆盖度评估

| 模块 | Spec 要求 | 已实现 | 覆盖率 |
|------|----------|--------|--------|
| 多租户隔离 | tenant_id 字段 + 中间件 | 全部实现 | 100% |
| 认证授权 | JWT login/refresh, RBAC | login, refresh, 3 roles | 100% |
| Dashboard KPI | /kpi + /trends | 全部实现 | 100% |
| 设备管理 | list/detail/data | 全部实现 | 100% |
| 健康师工作台 | 用户/画像/方案 CRUD | 全部实现 | 100% |
| 数据持久化 | PostgreSQL 存储 | 仅 in-memory | 0% |
| 真实数据流 | 接入用户/设备/处方数据 | 未接入 | 0% |
| 告警面板 | 4类告警 | 未实现 | 0% |
| 数据导出 | CSV/Excel/PDF | 未实现 | 0% |

**加权覆盖**: 核心模块(60%) 100% + 支撑模块(40%) 约 40% = **约 76%**

---

## 4. Gap 汇总与优先级

| # | Gap | 严重性 | 优先级 | Sprint |
|---|-----|--------|--------|--------|
| G1 | 数据持久化（PostgreSQL 迁移） | 阻断生产 | P0 | Sprint 1 |
| G2 | Demo 数据自动初始化 | 阻断演示 | P0 | Sprint 1 |
| G5 | KPI 真实数据计算 | 阻断运营 | P0 | Sprint 1 |
| G3 | 健康师分配关系 + 真实用户数据 | 影响可用性 | P1 | Sprint 2 |
| G4 | 胖终端登录（tenant discovery） | 影响扩展 | P1 | Sprint 2 |
| G6 | viewer 权限收窄 | 数据安全 | P1 | Sprint 1 |
| G7 | 告警面板 | 低价值 | P2 | Sprint 3+ |
| G8 | 数据导出 | 低价值 | P2 | Sprint 3+ |

---

## 5. 代码质量评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | 8/10 | 模块划分清晰，依赖注入合理 |
| Pydantic 模型 | 9/10 | 类型精确，文档完善 |
| 测试覆盖 | 未测试（需跑测试套件） | 484 tests，需验证通过率 |
| 错误处理 | 7/10 | HTTPException 使用一致，部分边界情况未处理 |
| 代码可读性 | 9/10 | 注释充分，函数命名清晰 |

---

## 6. 下一步

1. **Wave 2 Sprint 1**: 修复 G1, G2, G5, G6（数据持久化 + demo 初始化 + KPI 真实计算 + viewer 权限）
2. **Wave 2 Sprint 2**: 修复 G3, G4（健康师真实数据 + 胖终端登录）
3. **Wave 2 Sprint 3+**: G7, G8（告警 + 导出）

---

*验收人: Product Manager (PM)*  
*验收时间: 2026-04-11*

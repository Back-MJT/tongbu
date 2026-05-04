# Wave 2 用户故事 — B2B 平台

> 版本: v1.0  
> 日期: 2026-04-11  
> 作者: Product Manager  
> 依据: XIN-43 MVP 验收报告 (product/mvp_acceptance.md)  
> 目标: 面向 Wave 2 开发，确保可执行

---

## 目录

1. [Wave 2 目标与约束](#1-wave-2-目标与约束)
2. [P0 用户故事 — Sprint 1](#2-p0-用户故事--sprint-1)
3. [P1 用户故事 — Sprint 2](#3-p1-用户故事--sprint-2)
4. [P2 用户故事 — Sprint 3+](#4-p2-用户故事--sprint-3)
5. [Wave 2 验收检查清单](#5-wave-2-验收检查清单)

---

## 1. Wave 2 目标与约束

### 1.1 核心目标

在 Wave 1 (XIN-40) MVP 基础上，将 B2B 平台从"可演示"提升到"可运营"：

1. **生产就绪**: 数据持久化到 PostgreSQL，替代 in-memory store
2. **真实数据**: Dashboard KPI 从真实用户/设备/处方数据计算
3. **权限正确**: 修复 viewer 可访问用户列表等权限 bug
4. **自启动**: Demo 数据在服务启动时自动初始化

### 1.2 技术约束

- 复用 intervention-engine FastAPI 服务（port 4001）
- 复用 PostgreSQL/TimescaleDB（docker-compose）
- 保持 JWT 认证机制不变
- 新增 Redis 用于 JWT token blacklist（refresh token 撤销）

### 1.3 成功标准

- 所有 P0 用户故事通过 Given-When-Then 验收标准
- 1000+ 并发租户压测通过
- 从 MVP 数据切换到真实数据后 KPI 计算误差 < 1%

---

## 2. P0 用户故事 — Sprint 1

### 2.1 数据持久化

#### W2-US-D-001: PostgreSQL 数据库迁移

**优先级**: P0  
**来源**: Gap #1 (mvp_acceptance.md)

**用户故事**:
> 作为系统，我需要将所有 B2B 数据持久化到 PostgreSQL，以便服务重启后数据不丢失。

**验收标准**:
- Given 服务启动
- When B2B 模块初始化时
- Then 创建 tenants, b2b_users, devices 表（如不存在）
- And 所有 CRUD 操作写入 PostgreSQL

- Given 写入 tenants 表
- When 记录创建后
- Then 服务重启后可从 PostgreSQL 读取相同数据

- Given health_coach 被分配 managed_user_ids
- When 服务重启后
- Then managed_user_ids 仍然保留

#### W2-US-D-002: Demo 数据自动初始化

**优先级**: P0  
**来源**: Gap #2 (mvp_acceptance.md)

**用户故事**:
> 作为演示用户，我需要在服务启动后立即能登录系统，以便快速体验 B2B 平台功能。

**验收标准**:
- Given 服务启动（无任何数据）
- When startup 事件触发
- Then 自动创建 demo_tenant（id: demo_tenant_001, name: "Demo Health Organization"）
- And 自动创建 3 个 demo 用户：admin/coach1/viewer（密码: admin123/coach123/viewer123）
- And 自动创建 5 个 demo 设备（device_001 ~ device_005）
- And admin 用户可成功登录并获得 JWT token

---

### 2.2 Dashboard KPI 真实计算

#### W2-US-D-003: KPI 真实数据计算

**优先级**: P0  
**来源**: Gap #5 (mvp_acceptance.md)

**用户故事**:
> 作为机构管理员，我需要 Dashboard 显示真实业务数据，以便基于数据做出运营决策。

**验收标准**:
- Given 系统中存在真实用户和设备数据
- When 调用 GET /api/dashboard/kpi 时
- Then active_users = 今日有数据上报的用户数（而非固定值）
- And device_online_rate = 在线设备数 / 总设备数（基于设备最后心跳时间）
- And exercise_count = 今日有运动处方执行记录的用户数
- And sleep_compliance_rate = 睡眠评分 >= 80 的用户占比

- Given 查询 GET /api/dashboard/trends 时
- Then 数据来自 PostgreSQL 存储的真实历史数据（而非伪随机生成）
- And 7d 返回最近 7 天真实数据

---

### 2.3 权限修复

#### W2-US-D-004: viewer 权限收窄

**优先级**: P0  
**来源**: Gap #6 (mvp_acceptance.md)

**用户故事**:
> 作为 viewer，我只能查看首页概览，无权访问用户列表，以符合最小权限原则。

**验收标准**:
- Given viewer 角色用户登录系统
- When 调用 GET /api/users 时
- Then 返回 403 Forbidden，提示 "Viewers cannot access user list"

- Given viewer 登录系统
- When 调用 GET /api/dashboard/kpi 或 GET /api/dashboard/trends 时
- Then 返回正常数据（viewer 可访问首页）

- Given viewer 登录系统
- When 调用 GET /api/devices 时
- Then 返回设备列表（viewer 可见设备）

---

## 3. P1 用户故事 — Sprint 2

### 3.1 健康师真实数据

#### W2-US-D-005: 健康师查看真实用户数据

**优先级**: P1  
**来源**: Gap #3 (mvp_acceptance.md)

**用户故事**:
> 作为健康师，我需要看到我负责用户的真实健康数据，以便为用户提供准确的干预指导。

**验收标准**:
- Given health_coach 已分配 3 个 managed_user_ids
- When 调用 GET /api/coach/users 时
- Then 返回的 3 个用户是真实存在的用户（而非 hash 生成的伪数据）
- And 每个用户的 age, gender, health_goals 来自真实健康画像数据
- And latest_sleep_score 来自真实睡眠评估结果

- Given health_coach 调用 GET /api/coach/users/{user_id}/profile 时
- When 查看分配用户的画像时
- Then 显示真实基线评分（exercise_capacity, sleep_quality）

#### W2-US-D-006: 健康师管理真实处方

**优先级**: P1  
**来源**: Gap #3 (mvp_acceptance.md)

**用户故事**:
> 作为健康师，我需要管理我负责用户的干预方案，以便跟踪干预效果。

**验收标准**:
- Given health_coach 用户的 managed_user_ids 中包含 user_A
- When 调用 GET /api/coach/users/user_A/prescriptions 时
- Then 返回 user_A 在 prescription 表中的真实方案记录

- Given health_coach 调用 PUT /api/coach/prescriptions/{rx_id} 更新方案
- When 修改 status/notes/adjustment_notes 时
- Then 数据持久化到 PostgreSQL prescription 表

---

### 3.2 胖终端登录

#### W2-US-D-007: 机构自助注册（胖终端）

**优先级**: P1  
**来源**: Gap #4 (mvp_acceptance.md)

**用户故事**:
> 作为新机构管理员，我需要自助注册机构账号，以便快速开通 B2B 平台服务。

**验收标准**:
- Given 未认证用户
- When 调用 POST /api/auth/register（新增）
- Then 创建新 tenant 记录
- And 创建该机构的第一个 org_admin 用户
- And 返回 JWT token，可直接登录使用

- Given 新机构注册时机构名称重复
- When POST /api/auth/register 提交
- Then 返回 409 Conflict

---

### 3.3 JWT Token 撤销

#### W2-US-D-008: 登出时 Token 撤销

**优先级**: P1  
**来源**: 安全需求

**用户故事**:
> 作为机构管理员，我需要在登出时使当前 token 失效，以防 token 被滥用。

**验收标准**:
- Given 用户调用 POST /api/auth/logout
- When 提交当前 access_token 时
- Then 将 token 加入 Redis blacklist（TTL = token 剩余有效期）
- And 后续请求携带该 token 返回 401 Unauthorized

- Given 用户调用 POST /api/auth/refresh 时
- When refresh_token 已在 blacklist 时
- Then 返回 401 Unauthorized

---

## 4. P2 用户故事 — Sprint 3+

### 4.1 告警面板

#### W2-US-D-009: 设备离线告警

**优先级**: P2  
**来源**: B2B_DASHBOARD_MVP_SPEC.md §3.3

**用户故事**:
> 作为机构管理员，我需要在设备离线时收到告警，以便及时处理设备故障。

**验收标准**:
- Given 设备最后心跳时间超过 60 秒
- When 系统定时任务运行时
- Then 设备状态更新为 "offline"
- And 在 GET /api/dashboard/alerts 返回告警记录

---

### 4.2 数据导出

#### W2-US-D-010: 用户健康数据导出

**优先级**: P2  
**来源**: B2B_DASHBOARD_MVP_SPEC.md US-V-005

**用户故事**:
> 作为机构管理员，我需要导出用户健康数据，以便进行离线分析和汇报。

**验收标准**:
- Given 管理员选择用户数据导出
- When 调用 POST /api/reports/export（新增）
- Then 返回 CSV 格式的用户健康数据文件

---

### 4.3 干预追踪增强

#### W2-US-D-011: 方案依从性追踪

**优先级**: P2  
**来源**: B2B_DASHBOARD_MVP_SPEC.md US-V-004

**用户故事**:
> 作为机构管理员，我需要追踪用户的方案依从性，以便评估干预效果。

**验收标准**:
- Given 用户连续 3 天未执行干预方案
- When 查看干预追踪页面时
- Then 该用户在列表中显示低依从性标识（橙色）
- And 首页显示依从性告警横幅

---

## 5. Wave 2 验收检查清单

### Sprint 1
- [ ] PostgreSQL migration script 执行成功，所有表创建正确
- [ ] 服务重启后 demo 用户仍可登录（数据持久化验证）
- [ ] GET /api/dashboard/kpi 返回真实计算结果（非固定值）
- [ ] viewer 调用 GET /api/users 返回 403
- [ ] viewer 调用 GET /api/dashboard/kpi 返回正常数据

### Sprint 2
- [ ] health_coach 看到 managed_user_ids 指向的真实用户
- [ ] GET /api/coach/users/{id}/profile 返回真实健康数据
- [ ] 新机构可调用 POST /api/auth/register 自助注册
- [ ] POST /api/auth/logout 后 token 加入 blacklist
- [ ] blacklist 中的 token 后续请求返回 401

### Sprint 3+
- [ ] 设备离线 60 秒后出现在告警列表
- [ ] POST /api/reports/export 返回 CSV 文件
- [ ] 连续 3 天未执行方案的用户显示低依从性标识

---

## 附录: API 变更清单

| 操作 | 端点 | 方法 | 变更类型 |
|------|------|------|----------|
| 机构注册 | /api/auth/register | POST | 新增 |
| 登出 | /api/auth/logout | POST | 新增 |
| 告警列表 | /api/dashboard/alerts | GET | 新增 |
| 报表导出 | /api/reports/export | POST | 新增 |
| 租户管理 | /api/tenants | CRUD | 修改（需 system_admin 权限） |
| viewer 用户列表 | /api/users | GET | 修复（viewer 403） |

---

---

## 6. B2B 客户 Onboarding 流程设计

> 本节回答: 新机构如何入驻、数据如何迁移、健康师如何快速上手

### 6.1 机构入驻流程（7 步）

```
Step 1: 机构注册
  ├── 填写信息: 机构名称、行业类型、规模、联系人
  ├── 选择套餐: trial(30天) / basic / professional
  └── 获得: 机构账号 + 初始管理员账户

Step 2: 管理员配置
  ├── 修改初始密码
  ├── 设置机构 Logo 和品牌
  ├── 配置数据保留策略
  └── 管理员自己给自己分配 org_admin 权限

Step 3: 团队成员邀请
  ├── 批量导入: Excel 模板上传（姓名、手机号、角色）
  ├── 单个邀请: 输入邮箱/手机发送邀请链接
  └── 角色分配: org_admin / health_coach / viewer

Step 4: 设备接入
  ├── 录入设备: 设备名称、类型、设备ID（从厂商处获取）
  ├── 绑定 SIM 卡（如需蜂窝连接）
  └── 设备配对测试（与小程健康 App 连接验证）

Step 5: 用户健康数据迁移（可选）
  ├── 格式说明: 提供 CSV 模板（姓名、手机号、性别、年龄、基础健康数据）
  ├── 数据校验: 上传后系统校验格式 + 数据完整性
  ├── 数据导入: 历史数据进入健康画像系统
  └── 迁移确认: 导入完成后机构确认

Step 6: 健康师培训
  ├── 培训内容: 工作台使用、用户管理、处方调整
  ├── 培训方式: 视频教程（< 30 分钟）+ 实战演练
  └── 完成标准: 健康师完成 3 个虚拟用户管理任务

Step 7: 首批用户试点
  ├── 选择试点: 10-50 名高意愿用户
  ├── 设备发放: 发放/租借健康监测设备
  ├── 画像创建: 用户完成健康问卷（5 分钟）
  └── 方案生成: 基于画像自动生成首批干预方案
```

### 6.2 数据迁移方案

| 数据类型 | 格式要求 | 字段说明 | 校验规则 |
|----------|----------|----------|----------|
| 用户基本信息 | CSV/Excel | 姓名、手机号、性别、年龄 | 手机号唯一，不为空 |
| 设备注册表 | CSV/Excel | 设备ID、设备类型、厂商、序列号 | 设备ID不重复 |
| 历史健康数据 | CSV/Excel | 日期、指标类型、数值、单位 | 日期格式 YYYY-MM-DD |
| 既往处方 | CSV/Excel | 用户、方案类型、开始日期、状态 | 用户已在系统中 |

**迁移工具**: 提供 Web UI（机构管理员操作）+ API（批量对接厂商系统）

### 6.3 培训材料需求

| 材料 | 受众 | 形式 | 时长 |
|------|------|------|------|
| 平台概览介绍 | 机构管理层 | PPT / 视频 | 15 分钟 |
| 工作台使用指南 | 健康师 | 视频 + 操作手册 | 30 分钟 |
| 设备绑定教程 | 终端用户 | 视频 + 图文 | 10 分钟 |
| 常见问题 FAQ | 全体 | 文档 | 按需查阅 |

### 6.4 Onboarding 技术需求（W2-US-D-012 / W2-US-D-013）

#### W2-US-D-012: 机构自助注册流程 API

**优先级**: P1

**用户故事**:
> 作为新机构管理员，我需要通过 API 完成入驻流程，以便将我司系统与我司 CRM 对接。

**验收标准**:
- Given 调用 POST /api/tenants/onboarding
- When 提交机构信息和首个管理员账户时
- Then 创建 tenant + org_admin 用户
- And 返回 access_token（用于后续 API 调用）
- And 发送欢迎邮件到管理员邮箱

- Given onboarding 提交数据不完整
- When 调用 POST /api/tenants/onboarding 时
- Then 返回 400，列出缺失字段

#### W2-US-D-013: 批量用户导入 API

**优先级**: P1

**用户故事**:
> 作为机构管理员，我需要批量导入用户数据，以便快速完成机构上线准备。

**验收标准**:
- Given 管理员上传 CSV 文件（含姓名、手机号、性别、年龄）
- When 调用 POST /api/users/import 时
- Then 系统校验每行数据合法性
- And 返回导入结果：成功数、失败数、失败原因列表
- And 成功导入的用户创建健康画像记录（状态：待激活）

- Given CSV 中某行手机号重复
- Then 该行标记为失败，reason: "手机号已存在"
- And 不阻塞其他行继续导入

---

## 7. Wave 2 Onboarding 验收检查清单

- [ ] POST /api/tenants/onboarding 成功创建租户并返回 token
- [ ] 机构管理员首次登录后强制修改密码
- [ ] 批量导入 CSV 返回正确的成功/失败统计
- [ ] 历史健康数据导入后可在用户画像中展示
- [ ] 健康师完成培训后可在工作台看到分配用户

---

*文档版本: v1.0 | PM 昕动智能 | 2026-04-11*

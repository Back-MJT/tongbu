# XIN-144 Phase 2 实现计划 — 后端统一 + 多租户隔离

> 版本: v1.0
> 日期: 2026-04-27
> 作者: Founding Engineer (b6c9315c)
> 状态: 进行中
> 依赖: MVP_integration_plan_v2.md

---

## 1. 现状分析

### 1.1 干预引擎 API 端点 (intervention-engine :4001)

| 端点 | 方法 | 功能 | 归属 |
|------|------|------|------|
| `/health` | GET | 健康检查 | 基础设施 |
| `/api/integration/status` | GET | 集成状态 | 基础设施 |
| `/api/profiles` | POST | 创建健康档案 | ruoyi-intervention |
| `/api/profiles/{id}` | GET | 获取档案 | ruoyi-intervention |
| `/api/profiles/{id}` | PUT | 更新档案 | ruoyi-intervention |
| `/api/prescriptions/exercise` | POST | 运动处方生成 | ruoyi-intervention |
| `/api/prescriptions/sleep` | POST | 睡眠处方生成 | ruoyi-intervention |
| `/api/prescriptions/{id}` | GET | 获取处方详情 | ruoyi-intervention |
| `/api/miniprogram/prescription` | POST | 小程序处方生成 | ruoyi-intervention |
| `/api/miniprogram/device-tasks` | POST | 设备任务生成 | ruoyi-intervention |
| `/api/integration/data` | POST | 设备数据接入 | ruoyi-iot |
| `/b2b/auth/*` | * | B2B认证 | intervention-engine |
| `/b2b/tenants/*` | * | 租户管理 | intervention-engine |
| `/b2b/users/*` | * | 用户管理 | intervention-engine |
| `/b2b/dashboard/*` | * | 数据看板 | intervention-engine |
| `/b2b/devices/*` | * | 设备管理 | intervention-engine |
| `/b2b/coach/*` | * | 教练管理 | intervention-engine |
| `/b2b/reports/*` | * | 报表 | intervention-engine |
| `/b2b/compliance/*` | * | 依从性 | intervention-engine |
| `/b2b/manufacturer/*` | * | 厂家管理 | intervention-engine |

### 1.2 RuoYi-Backend 现有模块

| 模块 | 路径 | 功能 | 状态 |
|------|------|------|------|
| ruoyi-system | ruoyi-system/ | 用户/认证/RBAC/租户 | 完整运行 |
| ruoyi-intervention | ruoyi-intervention/ | 训练计划/AI引擎 | 有控制器，接口未知 |
| ruoyi-iot | ruoyi-iot/ | IMU数据/MQTT/设备 | 有控制器 |

### 1.3 关键发现

**问题**: intervention-engine 独立部署于 :4001，其 B2B 模块(RBAC/租户)与 RuoYi-Backend 的 ruoyi-system 重复/冲突。需要将 intervention-engine 的核心算法作为微服务或合并到 RuoYi-Backend。

**架构决策**: 
- intervention-engine 保留 Python 核心算法(FastAPI)，作为 RuoYi-Backend 的 **外调微服务**
- B2B 租户/用户/认证统一走 RuoYi-Backend (ruoyi-system)
- ruoyi-intervention 作为桥接层，调用 Python 算法服务

---

## 2. 实施步骤

### Step 1: 盘点现有接口 (今日完成)

- [ ] 读取 intervention-engine 所有端点定义 → 接口清单
- [ ] 读取 ruoyi-intervention Controller → 现有接口
- [ ] 读取 ruoyi-iot Controller → 现有接口
- [ ] 识别接口覆盖差距(GAP Analysis)
- [ ] 输出: `XIN-144_INTERFACE_GAP_ANALYSIS.md`

### Step 2: 租户隔离数据模型设计

- [ ] 分析 RuoYi 现有租户表(ry_tenant/ry_tenant_package)
- [ ] 设计干预数据租户隔离字段(tenant_id)
- [ ] 确认 ruoyi-intervention 各表的 tenant_id 覆盖
- [ ] 输出: `sql/xindong_intervention_tenant_migration.sql`

### Step 3: 干预引擎微服务化

- [ ] intervention-engine 以微服务形态独立运行(:4001)
- [ ] ruoyi-intervention 通过 HTTP 调用干预引擎
- [ ] 配置 RuoYi-Backend 的 HTTP Client 连接 :4001
- [ ] 验证训练计划API → Python算法 → 返回结果的链路

### Step 4: 多租户认证统一

- [ ] intervention-engine B2B auth 废弃，统一走 RuoYi JWT
- [ ] RuoYi-Backend 增加租户识别中间件
- [ ] 小程序端租户绑定流程
- [ ] 输出: `docs/XIN-144_MULTI_TENANT_AUTH.md`

### Step 5: 部署与验证

- [ ] 更新 docker-compose.yml 加入干预引擎服务
- [ ] 更新 deployment_guide.md
- [ ] 端到端验证: 小程序 → RuoYi-Backend → Python算法 → 返回
- [ ] 多租户数据隔离验证

---

## 3. 接口 GAP 分析 (初步)

### 已有对应关系

| intervention-engine | RuoYi-Backend | 对应状态 |
|---------------------|---------------|---------|
| POST /api/profiles | HealthProfileController | 需验证 |
| POST /api/prescriptions/exercise | TrainingApiController | 需验证 |
| GET /api/prescriptions/{id} | TrainingApiController | 需验证 |
| POST /api/integration/data | IMUDataController (推测) | 需验证 |

### 需要新增

- [ ] `/api/miniprogram/prescription` → ruoyi-intervention
- [ ] `/api/miniprogram/device-tasks` → ruoyi-intervention  
- [ ] B2B租户认证 → 统一到 ruoyi-system

### 废弃

- [ ] intervention-engine 的 B2B auth/tenants/users 等路由 → 全部迁移到 RuoYi-Backend

---

## 4. 技术债务

1. **冗余B2B模块**: intervention-engine 的 B2B 租户系统和 RuoYi-Backend 重复，需废弃
2. **MQTT/设备管理**: intervention-engine 的设备管理 vs ruoyi-iot，需统一
3. **测试覆盖**: ruoyi-intervention 的 Java 单元测试已有(见 target/surefire-reports)，需验证

---

## 5. 验收标准

- [ ] intervention-engine 只保留核心算法 API (:4001)
- [ ] 所有租户/用户/认证统一到 RuoYi-Backend
- [ ] ruoyi-intervention 正确桥接 Python 算法
- [ ] 多租户数据隔离通过验证
- [ ] 一套docker-compose可启动完整后端

---

*FE — b6c9315c — 2026-04-27*

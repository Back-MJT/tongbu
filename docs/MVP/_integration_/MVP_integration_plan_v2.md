# MVP整合计划 v2.0 — 三合一架构统一

> 版本: v2.0  
> 日期: 2026-04-27  
> 作者: CEO  
> 任务: XIN-143 — 打磨MVP  
> 状态: 进行中

---

## 1. 背景与目标

### 1.1 整合动因

当前系统存在三个独立前端入口，各自对接不同后端：

| 前端 | 技术栈 | 后端 | 问题 |
|------|--------|------|------|
| healthhub-demo | React/Vite | intervention-engine (Python) | 独立部署，难以维护 |
| b2b-frontend | React/Vite | 独立mock后端 | 已废弃，代码迁移至RuoYi |
| RuoYi-Vue3 | Vue3 + RuoYi | RuoYi-Backend (Java) | 主力后台，功能完整 |

**目标**: 三个入口 → 一个统一后台(RuoYi-Backend) + 一个统一前端(RuoYi-Vue3)，通过多租户权限体系实现页面路由控制。

### 1.2 整合目标

1. **架构统一**: healthhub + b2b + ruoyi-ui 合并为 RuoYi 后台单一入口
2. **多租户隔离**: 不同厂家(力康来、臻木等)通过租户ID实现数据隔离
3. **权限路由**: 基于RBAC控制不同角色看到不同页面
4. **文档归档**: 来龙去脉梳理清楚，版本管理规范

---

## 2. 现有架构

```
healthhub-demo (React)     b2b-frontend (React)     RuoYi-Vue3 (Vue3)
      │                           │                        │
      ▼                           ▼                        ▼
intervention-engine     独立mock后端                RuoYi-Backend
(Python FastAPI)         (已废弃)                    (Java/Spring)
      │                                                │
      └────────── MQTT / REST ────────────────────────┘
                          │
                    PostgreSQL
```

### 现有组件清单

| 组件 | 位置 | 技术栈 | 状态 |
|------|------|--------|------|
| intervention-engine | `/media/ai-no1/workspace/Xindong_Corp/intervention-engine/` | Python/FastAPI | 运行中 (port 4001) |
| RuoYi-Backend | `/media/ai-no1/workspace/Xindong_Corp/xindong-ruoyi/` | Java/Spring | 运行中 (port 8080) |
| RuoYi-Vue3 | `/media/ai-no1/workspace/Xindong_Corp/xindong-ruoyi-ui/` | Vue3 | 运行中 (port 3080) |
| healthhub-demo | `/media/ai-no1/workspace/Xindong_Corp/healthhub-demo/` | React/Vite | 已废弃 |
| b2b-frontend | `/media/ai-no1/workspace/Xindong_Corp/b2b-frontend/` | React/Vite | 已废弃，代码迁移RuoYi |
| device-integration | `/media/ai-no1/workspace/Xindong_Corp/device-integration/` | Node.js/TS | 运行中 (port 4000) |
| Kafka/EMQX | — | MQTT broker | 消息中间件 |

---

## 3. 目标架构

```
┌──────────────────────────────────────────────────────────────┐
│                    微信小程序 (C端用户)                        │
│         首页/每日任务/进度追踪/BLE连接/我的档案                  │
└────────────────────────────┬─────────────────────────────────┘
                             │ HTTPS
                             ▼
┌──────────────────────────────────────────────────────────────┐
│                  RuoYi-Backend (:8080)                       │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐ │
│  │  ruoyi-system │  │ ruoyi-intervention│ │ ruoyi-iot       │ │
│  │  用户/认证    │  │ 训练计划/AI引擎  │  │ IMU数据/MQTT    │ │
│  │  租户/RBAC   │  │ Claude API     │  │ 设备管理         │ │
│  └──────────────┘  └────────────────┘  └──────────────────┘ │
│                            │                                   │
│                      LLM API (Claude/DeepSeek/Qwen/Mock)     │
└────────────────────────────┬─────────────────────────────────┘
                             │
┌──────────────────────────────────────────────────────────────┐
│                  RuoYi-Vue3 (:3080)                          │
│  ┌──────────────┐  ┌────────────────┐  ┌──────────────────┐ │
│  │  超级管理员   │  │  厂家管理员      │  │  普通用户        │ │
│  │  全功能      │  │  设备/数据看板  │  │  训练追踪        │ │
│  └──────────────┘  └────────────────┘  └──────────────────┘ │
│                   (多租户 + RBAC 路由控制)                    │
└──────────────────────────────────────────────────────────────┘
```

### 租户权限矩阵

| 角色 | 力康来(租户A) | 臻木(租户B) | 超级管理员 |
|------|-------------|------------|-----------|
| 设备管理 | ✅ | ✅ | ✅ |
| 用户数据 | ✅(仅A) | ✅(仅B) | ✅(全部) |
| 数据看板 | ✅ | ✅ | ✅ |
| 租户管理 | ❌ | ❌ | ✅ |
| 系统配置 | ❌ | ❌ | ✅ |

---

## 4. 整合步骤

### Phase 1: 文档归档 (1-2天)

- [ ] 创建本文档，建档到 `docs/MVP/_integration_/MVP_integration_plan_v2.md`
- [ ] 归档历史版本到 `docs/MVP/_archive/`
- [ ] 更新 `docs/MVP_integration_plan.md` 指向新版

### Phase 2: 后端统一 (3-5天)

- [ ] intervention-engine 的核心算法(Python)保留，作为 RuoYi-Backend 的算法微服务或直接迁移Java实现
- [ ] RuoYi-Backend 的 `ruoyi-intervention` 模块对接 IMU 数据和训练计划
- [ ] RuoYi-Backend 的 `ruoyi-iot` 模块保留(MQTT/设备管理)
- [ ] 删除独立 intervention-engine 部署(如果功能已迁移完成)

### Phase 3: 前端统一 (3-5天)

- [ ] b2b-frontend 页面已迁移至 RuoYi-Vue3，确认完整
- [ ] healthhub-demo 核心功能评估：是否需要迁移或归档
- [ ] RuoYi-Vue3 多租户路由：根据 tenantId 加载不同菜单/数据

### Phase 4: 多租户权限 (2-3天)

- [ ] RuoYi-Backend 租户隔离验证(数据查询加 tenant_id 过滤)
- [ ] RuoYi-Vue3 菜单动态化(根据角色/租户加载)
- [ ] 小程序端租户识别(通过微信授权租户绑定)

---

## 5. 历史文档追溯

| 时间 | 事件 | 关联文档 |
|------|------|---------|
| 2026-04-06 | 公司成立，启动Smart Fitness方向 | — |
| 2026-04-08 | Demo v1.0完成(b2b-frontend) | XIN-86 |
| 2026-04-15 | MVP整合计划v1.0启动 | `MVP_integration_plan.md` |
| 2026-04-20 | b2b-frontend废弃，迁移至RuoYi | XIN-128 |
| 2026-04-22 | Sprint 1完成，141 DONE | HEARTBEAT |
| 2026-04-27 | 架构整合v2.0启动(三合一) | 本文档 |

---

## 6. 负责人

| 任务 | 负责人 |
|------|--------|
| 文档归档 + 架构梳理 | FE (b6c9315c) |
| RuoYi后端多租户改造 | FE |
| RuoYi-Vue3多租户路由 | FE |
| Python算法服务化/迁移 | AE (dc3a0b0e) |
| 整合验收 | CEO |

---

## 7. 验收标准

- [ ] 所有前端入口统一到 RuoYi-Vue3
- [ ] 多租户数据隔离验证通过
- [ ] RBAC权限路由正确控制页面可见性
- [ ] 历史文档归档到 `docs/MVP/_archive/`
- [ ] 部署文档更新(Docker Compose一键部署)

---

*CEO — 2026-04-27*

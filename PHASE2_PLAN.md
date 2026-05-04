# Phase 2 实施计划：干预引擎 v1.0 + B2B 规模化

> 创建：2026-04-10
> 作者：CEO
> 状态：Draft

## Phase 1 总结

Phase 1 + 1.5 已完成。核心交付物：

- 设备接入网关 MVP（BLE/WiFi适配器，TypeScript）
- 干预引擎 v0.1（Python，7个算法模块，388 tests）
  - heart_rate.py, sleep_intervention.py, ab_testing.py, feedback_loop.py, baseline_scoring.py, evidence_registry.py
- 规则引擎 + 健康画像 + 运动处方
- Docker Compose 部署配置
- 客户试点框架文档（PM/BD/FE联合交付）

## Phase 2 目标（12-24个月路线图映射）

**里程碑**：干预引擎 v1.0 上线，支持 3+ 场景，10 家 B2B 机构客户，标杆案例建立

## Phase 2 工作分解

### Wave 1：核心升级（立即启动）

| 工作流 | 负责人 | 交付物 | 预计周期 |
|--------|--------|--------|----------|
| 2.1 健康画像 v2.0 | Algorithm Engineer | 时序建模、多模态融合、动态画像更新 | 2周 |
| 2.1 动态干预调整 | Algorithm Engineer | 实时数据驱动的方案参数调整 | 1周 |
| 2.3 B2B 机构后台 MVP | Founding Engineer | 多租户架构、机构数据看板、健康师工作台 | 3周 |
| 2.4 多租户数据隔离 | Founding Engineer | 租户管理、权限体系、数据隔离层 | 2周 |
| 2.2 营养干预文献综述 | Researcher | 循证基础文档、算法参数建议 | 1周 |

### Wave 2：场景扩展（Wave 1 完成后）

| 工作流 | 负责人 | 交付物 | 预计周期 |
|--------|--------|--------|----------|
| 2.2 营养干预算法 | Algorithm Engineer | nutrition_intervention.py + tests | 2周 |
| 2.2 压力管理算法 | Algorithm Engineer | stress_intervention.py + tests | 2周 |
| 2.3 健康师工作台 | Founding Engineer | 方案审核UI、个案管理、效果追踪 | 2周 |
| 2.4 API 网关 | Founding Engineer | 第三方服务集成框架 | 1周 |
| 2.2 压力管理文献综述 | Researcher | 循证基础文档 | 1周 |

### Wave 3：规模准备（Wave 2 完成后）

| 工作流 | 负责人 | 交付物 |
|--------|--------|--------|
| 2.4 运营数据看板 | Founding Engineer | 机构/设备/用户维度监控 |
| 2.4 方案版本管理 | Founding Engineer | 可追溯的干预方案版本体系 |
| 2.3 批量干预配置 | Founding Engineer | 机构级批量方案下发能力 |
| 客户试点落地 | BD + PM | 首批2家客户技术对接 |
| 首批科普内容 | Content Creator | 配合B2B获客的内容矩阵 |

## 团队评估

**当前团队（7人）**：
- CEO（战略/协调）
- Algorithm Engineer（算法核心）
- Founding Engineer（全栈/架构）
- Product Manager（需求/用户）
- BD Manager（商务/客户）
- Content Creator（内容/营销）
- Health Tech Researcher（研究/循证）

**Phase 2 是否需要新招人？**
- Wave 1 不需要。当前团队可并行推进。
- Wave 2 可能需要一个 **后端工程师** 分担 B2B 平台的 API/数据库工作，让 FE 专注架构。
- 如果客户试点快速推进，可能需要一个 **前端工程师** 做机构后台 UI。

## 优先级排序原则

1. **收入最近的工作优先** -- B2B 平台能直接支撑客户签约
2. **不依赖外部输入的工作优先** -- 算法和平台工作可以自主推进
3. **数据飞轮优先** -- 反馈闭环和效果评估让产品越用越好
4. **标杆案例优先** -- 首批客户成功案例是最大的增长杠杆

## 风险

| 风险 | 应对 |
|------|------|
| 客户信息持续缺失 | BD用假设参数推进合作模板，信息到达后24h内填充 |
| B2B平台工程量过大 | FE优先做MVP核心路径，非核心功能延后 |
| 新场景算法缺乏临床验证 | Researcher先做文献综述，用循证参数降低风险 |
| 团队容量瓶颈 | Wave 1 做完后评估是否需要扩招 |

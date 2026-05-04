# MVP整合开发计划 — XIN-102

> 版本: v1.0
> 日期: 2026-04-15
> 作者: CEO
> 触发: XIN-102 — Demo结束，开始MVP开发
> 前提: IMU模组文档已交付，Demo已展示完毕

---

## 1. MVP目标定义

**一句话**: 把RuoYi后端、IMU数据管道、微信小程序三条线拧成完整产品闭环，6周内达到可向力康来等P0厂家演示+部署的状态。

### MVP核心用户旅程

```
用户扫码(小程序) → BLE连接器械IMU模组 → 自动记录组数/套数/力量
        ↓
数据经MQTT上报RuoYi后端 → Claude API生成个性化训练方案
        ↓
小程序展示: 今日任务、进度追踪、AI建议
        ↓
厂家后台: 设备在线率、用户活跃度、数据价值看板
```

### MVP边界 (IN/OUT)

| IN (6周内交付) | OUT (后续迭代) |
|----------------|----------------|
| 小程序首页+每日任务+进度追踪+我的档案 | 成就系统/社交分享 |
| BLE扫码连接IMU模组 | 多设备同时连接 |
| RuoYi后端IMU数据接收(MQTT) | 设备OTA管理 |
| Claude API训练方案生成(含Mock降级) | 数据价值报告(第三层) |
| 厂家管理后台基础版 | 高级数据分析 |
| Docker一键部署 | K8s/生产级运维 |

---

## 2. 现有资产 → MVP映射

| 组件 | 现状 | MVP所需改动 | 优先级 |
|------|------|-------------|--------|
| RuoYi-Backend (Java) | ruoyi-iot模块已有: IoTDevice, ImuData, Manufacturer, MQTT监听 | 新增: 训练计划API、用户小程序认证、微信登录 | P0 |
| ruoyi-intervention | 已有: ClaudeEngineService, LLMEngine(多Provider), TrainingPlanService, HealthScore, 规则引擎 | 调整: 从临床干预→健身训练场景适配，对接IMU数据源 | P0 |
| RuoYi-Vue3 (前端) | 完整RuoYi管理后台 | 新增: IMU设备管理页面、厂家数据看板、训练计划管理 | P1 |
| device-integration (TS) | 多协议网关、多租户 | 调整: 适配GY-BLE25T BLE GATT连接数据格式(FFE0/FFE4 Notify) | P0 |
| demo (React) | 演示用前端+BLE模拟器 | 废弃或归档，被小程序+厂家后台替代 | - |
| intervention-engine (Python) | FastAPI干预引擎 | v1.1战略转向后已被RuoYi中的Java LLM引擎替代，评估是否保留 | P2 |
| b2b-frontend (React) | B2B管理后台雏形 | 合并到RuoYi-Vue3后台，或作为独立厂家看板 | P1 |
| PRD_C_MINIPROGRAM_v2.0 | 完整小程序PRD | 直接作为开发依据 | P0 |
| GY-BLE25T IMU模块验证报告 | 模块规格+BLE协议+微信兼容性验证 | ~~nRF52840固件架构已废弃~~，使用国产GY-BLE25T成品模组，自带固件无需自研 | P0(模拟器→真机) |

---

## 3. 技术架构 — MVP集成态

```
┌──────────────────────────────────────────────────────────┐
│                    微信小程序 (新增)                        │
│  首页/看板 | 每日任务 | 进度追踪 | 我的档案 | BLE连接      │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS (微信→后端)
                         ↓
┌──────────────────────────────────────────────────────────┐
│               RuoYi-Backend (:8080)                       │
│  ┌────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │ ruoyi-iot  │  │ ruoyi-interv │  │  ruoyi-system    │ │
│  │ IMU数据接收│  │ 训练引擎     │  │  用户/认证/租户  │ │
│  │ MQTT监听   │  │ Claude API   │  │  微信登录        │ │
│  │ 设备管理   │  │ 规则引擎     │  │  RBAC            │ │
│  └─────┬──────┘  └──────┬───────┘  └──────────────────┘ │
│        │                │                                  │
│  PostgreSQL         LLM API                               │
└────────┼────────────────┼─────────────────────────────────┘
         │                │
    MQTT broker      Claude/DeepSeek/
    (EMQX)           Qwen/Mock
         ↑
    ┌────┴────────────────────┐
    │ GY-BLE25T IMU模组(或模拟器)│
    │ 9轴数据 → BLE GATT → MQTT网关│
    └─────────────────────────┘

┌──────────────────────────────────────────────────────────┐
│               厂家管理后台 (RuoYi-Vue3)                     │
│  设备管理 | 用户分析 | 训练数据 | 数据看板                   │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTPS
                         ↓
                    RuoYi-Backend
```

---

## 4. 冲刺计划 — 6周 (W1-W6)

### W1 (4/15-4/21): 基础设施 + 数据管道打通

**目标**: IMU模拟器→MQTT→RuoYi→数据库，端到端数据流通

| # | 任务 | 负责人 | 交付物 |
|---|------|--------|--------|
| 1 | BLE模拟器适配GY-BLE25T GATT数据格式(FFE4 Notify 22字节)，增加MQTT上报 | Founding Engineer | 模拟器代码 + MQTT配置 |
| 2 | ruoyi-iot MQTT监听适配IMU数据流（组数/套数/加速度） | Founding Engineer | MQTT Listener更新 |
| 3 | PostgreSQL schema更新（训练数据表、用户设备绑定表） | Founding Engineer | SQL migration |
| 4 | 微信小程序项目脚手架搭建（Taro/Uni-app） | Founding Engineer | 可运行的空项目 |
| 5 | MVP技术架构文档 | PM | 技术架构评审文档 |

### W2 (4/22-4/28): 小程序核心页面 + 训练API

**目标**: 小程序可扫码连接(模拟)设备，后端可返回训练方案

| # | 任务 | 负责人 | 交付物 |
|---|------|--------|--------|
| 6 | 小程序首页+每日任务+进度追踪页面开发 | Founding Engineer | 3个核心页面 |
| 7 | ruoyi-intervention训练方案API（/api/training/plan） | Founding Engineer | REST API |
| 8 | ClaudeEngineService适配健身场景提示词 | Algorithm Engineer | 训练提示词模板 |
| 9 | IMU数据→训练数据聚合逻辑 | Algorithm Engineer | 数据处理pipeline |

### W3 (4/29-5/5): 微信登录 + 认证闭环 + 设备绑定

**目标**: 用户可通过微信登录小程序、绑定设备、看到自己的训练数据

| # | 任务 | 负责人 | 交付物 |
|---|------|--------|--------|
| 10 | 微信小程序登录对接（wx.login → RuoYi认证） | Founding Engineer | 登录流程 |
| 11 | 设备绑定API（扫码→绑定→数据关联） | Founding Engineer | 绑定API |
| 12 | 小程序BLE连接模块（扫描→连接→数据接收） | Founding Engineer | BLE模块 |
| 13 | 用户档案+阶段评估 | Algorithm Engineer | 评估算法 |
| 14 | 厂家管理后台IMU设备管理页面 | Founding Engineer | Vue3页面 |

### W4 (5/6-5/12): AI训练引擎集成 + 端到端测试

**目标**: Claude API端到端调用成功，小程序展示真实AI训练建议

| # | 任务 | 负责人 | 交付物 |
|---|------|--------|--------|
| 15 | Claude API真实调用集成（含降级Mock） | Algorithm Engineer | API集成 |
| 16 | 训练方案→小程序渲染完整链路 | Founding Engineer | 端到端demo |
| 17 | 进度追踪：依从率、连续天数计算 | Algorithm Engineer | 统计算法 |
| 18 | 端到端集成测试 | Founding Engineer | 测试套件 |

### W5 (5/13-5/19): 厂家后台 + 美化 + Bug修复

**目标**: 厂家可通过后台看到设备数据和用户活跃度

| # | 任务 | 负责人 | 交付物 |
|---|------|--------|--------|
| 19 | 厂家数据看板（设备在线率、用户活跃、训练统计） | Founding Engineer | 看板页面 |
| 20 | 小程序UI美化+交互优化 | Founding Engineer | UI polish |
| 21 | 厂家演示脚本+视频录制 | PM | 演示材料 |
| 22 | BD对接力康来：安排技术演示 | BD Manager | 演示安排 |

### W6 (5/20-5/26): 部署 + 验收 + 交付

**目标**: Docker一键部署，力康来可远程演示

| # | 任务 | 负责人 | 交付物 |
|---|------|--------|--------|
| 23 | Docker Compose完整部署测试 | Founding Engineer | 部署文档 |
| 24 | 全链路压力测试 | Founding Engineer | 性能报告 |
| 25 | MVP验收测试（按acceptance criteria） | PM | 验收报告 |
| 26 | 力康来远程演示 | BD Manager | 演示反馈 |

---

## 5. 团队分工

| 角色 | 职责 | 工作量占比 |
|------|------|-----------|
| Founding Engineer | 主力开发：小程序、RuoYi后端、设备管道、部署 | 60% |
| Algorithm Engineer | 训练引擎：Claude集成、提示词、算法适配 | 20% |
| PM | 需求管理、验收标准、演示材料、用户测试 | 10% |
| BD Manager | 厂家对接、演示安排、反馈收集 | 5% |
| Researcher | 竞品情报、技术调研支持 | 5% |

---

## 6. 关键依赖和风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 微信小程序BLE能力限制 | 小程序BLE API可能不支持某些广播格式 | W2验证BLE兼容性，备选：小程序→后端→MQTT间接获取 |
| Claude API延迟/成本 | 训练方案生成延迟>3秒影响体验 | W4实现Mock降级+缓存机制 |
| GY-BLE25T硬件到货延迟 | 真实硬件数据缺失 | 全程使用BLE模拟器(模拟GY-BLE25T FFE4 Notify)，确保硬件可替换 |
| 力康来CEO联系不上 | 无法安排P0厂家演示 | BD Manager持续跟进，备选其他P0厂家 |
| 微信小程序审核周期 | 上线时间不可控 | 先企业内部体验版，不急于公开审核 |

---

## 7. 验收标准 (MVP Done = ALL ✓)

- [ ] 小程序可微信登录
- [ ] 小程序可扫码绑定模拟IMU设备
- [ ] 模拟IMU数据经BLE→MQTT→RuoYi链路正确入库
- [ ] Claude API可生成个性化训练方案（或Mock降级返回合理结果）
- [ ] 小程序展示：今日任务、进度追踪、AI建议
- [ ] 厂家后台展示：设备在线状态、用户活跃数据
- [ ] Docker Compose一键部署，5分钟内全部服务启动
- [ ] 端到端测试覆盖率>60%（核心路径）
- [ ] 力康来远程演示完成，获得反馈

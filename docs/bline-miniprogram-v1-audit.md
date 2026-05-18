# B Line 小程序成熟版 V1 现状审计

## 1. 项目概览

**项目类型**: 传统健身器械智能化改造系统
**核心技术**: 微信小程序 + 六轴传感器(GY-BLE25T) + 若依后台

### 代码位置
```
~/Desktop/try/Xindong_Platform-main/
├── mini-program/          # 微信小程序 (Taro + Vue3)
├── RuoYi-Backend/         # Java 后端 (Spring Boot)
│   ├── ruoyi-iot/        # IoT 模块 (设备/器械/训练会话)
│   ├── ruoyi-system/      # 系统模块
│   └── ruoyi-intervention/ # A Line 预留模块
├── RuoYi-Vue3-master/    # 若依 Vue3 前端
└── docs/                 # 项目文档
```

---

## 2. 小程序页面路径与核心流程

### 2.1 页面结构

| 页面路径 | 文件 | 功能 |
|---------|------|------|
| `/pages/home/index` | home/index.vue | 首页：场馆选择、今日概览、AE建议、快速开始 |
| `/pages/daily-task/index` | daily-task/index.vue | 今日训练任务列表、AI训练建议 |
| `/pages/progress/index` | progress/index.vue | 训练进度：依从率、连续天数、历史记录 |
| `/pages/profile/index` | profile/index.vue | 用户档案、设备管理、训练数据摘要 |
| `/pages/device-binding/index` | device-binding/index.vue | **核心页**：扫码、BLE连接、实时计数、训练记录 |
| `/pages/login/index` | login/index.vue | 登录页 |
| `/pages/training-plan/index` | training-plan/index.vue | 训练计划页 |
| `/pages/privacy/index` | privacy/index.vue | 隐私政策 |
| `/pages/user-agreement/index` | user-agreement/index.vue | 用户协议 |

### 2.2 TabBar 结构
- **首页** → `/pages/home/index`
- **训练** → `/pages/daily-task/index`
- **进度** → `/pages/progress/index`
- **我的** → `/pages/profile/index`

### 2.3 核心业务流程

```
首页 → 选择场馆 → 训练页(daily-task) → 点击任务 → 设备绑定页(device-binding)
                                                           ↓
                                                    扫码器械二维码
                                                           ↓
                                          调用 /api/mini/equipment/resolve
                                                           ↓
                                            occupyEquipment (占用器械)
                                                           ↓
                                         扫描并连接 BLE 传感器 (GY-BLE25T)
                                                           ↓
                                           IMU 数据实时计数 (counter.ts)
                                                           ↓
                                         结束训练 → submitTrainingSession
                                                           ↓
                                            releaseEquipment (释放器械)
```

### 2.4 关键前端文件

| 文件 | 行数 | 职责 |
|------|------|------|
| `mini-program/src/services/ble.ts` | 480 | BLE扫描、连接、IMU数据解析、GY25T协议 |
| `mini-program/src/services/counter.ts` | 218 | 本地动作计数（状态机：idle/moving_up/peak/moving_down/resting） |
| `mini-program/src/services/api.ts` | 1144 | 所有后端API调用、演示模式数据 |
| `mini-program/src/pages/device-binding/index.vue` | 1303 | 核心训练页：扫码、BLE连接、实时计数、结果保存 |
| `mini-program/src/app.config.ts` | 56 | 小程序全局配置（页面路径、TabBar、窗口样式） |

---

## 3. 蓝牙连接相关文件

### 3.1 核心蓝牙文件

**`mini-program/src/services/ble.ts`**
- `BleService` 类，单例 `bleService`
- `checkAdapter()`: 检查蓝牙适配器可用性
- `startScan()` / `stopScan()`: 扫描BLE设备
- `connectGyDevice(deviceId, options)`: 连接GY-BLE25T传感器
  - 自动查找 serviceUuid `0000FFE0` 和 notify characteristic `0000FFE4`
  - 每200ms轮询一次姿态命令（GY25T协议）
  - 支持 MTU 设置
- `disconnect()`: 断开连接（关闭notify、关闭BLE连接）
- `bindNotifyListener()`: 监听BLE特征值变化
- `parseImuData()`: 解析GY25T协议帧（pitch/roll/yaw/加速度/角速度）
- `extractImuPayload()`: 帧解析，含校验和验证

**`mini-program/src/pages/device-binding/index.vue`** (关键连接逻辑)
- `onSelectDevice(device, fromAuto)`: 触发 BLE 连接
- `startBleScan(autoConnect)`: 开始扫描，支持自动连接匹配设备
- `stopBleScan()`: 停止扫描
- `switchToEquipment(equipment, autoScanBle)`: 切换器械时断开旧连接
- `stopCurrentBleSession()`: 清理BLE会话
- `releaseCurrentUsage()`: 释放器械占用
- `getDeviceMatchScore(device)`: BLE设备名称匹配评分（决定是否自动连接）

### 3.2 已知蓝牙问题

**问题1: 用户离开未主动断开**
- `disconnect()` 在 `onUnmounted` 中调用，但如果用户直接关闭小程序或页面退出（不是卸载），可能导致连接未释放
- `onUnmounted` 时 `releaseCurrentUsage()` 顺序在 `bleService.disconnect()` 之前，存在时间窗口

**问题2: 扫码另一台器械时连接未及时释放**
- `switchToEquipment()` 中 `stopCurrentBleSession()` 调用 `bleService.disconnect()`
- 但 `bleService.disconnect()` 是异步的，内部 `wx.closeBLEConnection()` 没有等待完成就继续执行后续逻辑
- `stopCurrentBleSession()` 不是 async，但后续 `switchToEquipment()` 继续执行，可能造成race condition

**问题3: 多用户同时扫同一台器械**
- 占用机制依赖 `occupyEquipment()` API 和 30秒心跳 `heartbeatEquipment()`
- 若用户A正在训练，用户B扫码 → API返回"器械使用中" → B被拒绝，但A的连接状态没有感知
- 前端只通过 API 错误信息判断，没有独立的连接状态同步机制

**问题4: 自动连接逻辑**
- `autoConnect=true` 时，找到设备后自动调用 `onSelectDevice(device, true)`
- 但 `getDeviceMatchScore()` 评分规则依赖 `bluetoothName` 字段，该字段可能为空或不准确
- 评分低于70分时仍可手动连接，无强制限流

---

## 4. 训练计数相关文件

### 4.1 计数文件

**`mini-program/src/services/counter.ts`**
- `ImuCounterService` 类
- 状态机 phases: `idle` → `moving_up` → `peak` → `moving_down` → `resting` → `idle`
- 配置参数: `mainAxis`(默认pitch), `upThreshold`(20), `downThreshold`(5), `minIntervalMs`(600), `minRange`(15), `smoothingWindow`(5), `setRestMs`(25000)
- `pushSample(sample)`: 处理每个IMU样本，返回 `CountingState`
- `finalizeCurrentSet()`: 自动切组（25秒无动作视为组间休息）
- `finalizeSession()`: 结束整个训练会话
- `recordRep()`: 记录一次有效次数

**`mini-program/src/pages/device-binding/index.vue`** (计数相关逻辑)
- `counterState`: 当前计数状态实时显示
- `counter`: `ImuCounterService` 实例，页面级别共享
- `counterState.value = counter.pushSample(sample)`: 每收到IMU样本更新计数
- `setSummaries`: 已完成组列表（组号、次数、时长）
- `phaseLabel`: 当前相位中文标签显示

### 4.2 数据流向

```
bleService.onSample → counter.pushSample(sample)
                         ↓
                 更新 phase (状态机)
                         ↓
                 CountingState { reps, sets, currentSetReps, phase, ... }
                         ↓
                 device-binding/index.vue 实时展示
```

---

## 5. 若依后台模块梳理

### 5.1 后端模块 (RuoYi-Backend)

**ruoyi-iot 模块**
```
controller/
├── EquipmentController.java      # 器械管理
├── IoTDeviceController.java      # 传感器/IMU设备管理
├── DeviceGroupController.java    # 设备分组
├── TrainingSessionController.java # 训练会话
├── TrainingAnalyticsController.java # 训练数据分析
├── ImuDataController.java        # IMU原始数据
├── ConfigController.java         # 配置管理
├── DashboardController.java      # 仪表盘
└── ManufacturerController.java    # 厂商管理

service/
mapper/
domain/
mqtt/                             # MQTT协议处理
```

**ruoyi-intervention 模块** (A Line 预留)
- 预留 `createHealthProfile`, `getHealthProfile`, `getExercisePrescription` 等接口

### 5.2 前端模块 (RuoYi-Vue3-master)

```
src/api/iot/
├── device.js       # 设备API
├── equipment.js    # 器械API
├── deviceGroup.js  # 分组API
├── trainingSession.js  # 训练会话API
├── trainingPlan.js # 训练计划API
├── imuData.js      # IMU数据API
├── config.js       # 配置API
├── dashboard.js    # 仪表盘API
└── ... 其他

src/views/iot/
├── device/         # 设备管理页
├── equipment/      # 器械管理页
├── deviceGroup/    # 分组管理页
├── training/       # 训练相关页
└── ...
```

### 5.3 数据库相关表

| 表名 | 用途 |
|------|------|
| `iot_device` | IMU传感器设备 |
| `iot_equipment` | 健身器械（一对一绑定传感器） |
| `iot_device_group` | 场馆/分组 |
| `iot_training_session` | 训练会话记录 |
| `iot_training_set` | 训练组明细 |
| `iot_manufacturer` | 厂商 |

---

## 6. 现有问题汇总

### P0 - 蓝牙稳定性（核心阻塞）

1. **连接未及时释放**: 用户离开器械时连接未断开，导致其他人无法连接
2. **扫码切换器械**: 当前用户扫另一个器械二维码时，旧传感器连接可能未完全断开，新连接受影响
3. **多人并发占用**: 两个用户同时扫同一台器械，心跳机制不能及时感知前一用户离线

### P1 - 小程序体验

4. **界面demo感**: 
   - 进度页柱状图用纯色块，缺少数值标签
   - 实时计数面板用简单文字展示，缺少动效
   - 整体视觉层级不够清晰
5. **器械列表无分页**: `getMyDevices()` 无分页，大型健身房设备多时体验差
6. **错误提示不够友好**: 网络失败仅 Toast，没有重试引导

### P2 - 训练结果页 & A Line 预留

7. **无训练结果展示页**: 训练完成后直接显示 sessionId，没有专门的训练结果汇总页
8. **A Line API 未接入**: `ruoyi-intervention` 模块预留，但 `api.ts` 中 `submitTrainingSession` 等接口未携带运动处方结果
9. **结果页无后端适配层**: 训练结果需要适配不同 API 响应格式，目前硬编码

### P3 - 后台管理

10. **若依后台 UI/UX**: 页面风格偏传统，器械状态无实时刷新
11. **设备分组管理**: 二维码生成、批量绑定功能缺失或入口隐蔽
12. **使用记录展示**: 历史训练记录列表不支持筛选和导出

---

## 7. 后续 Issue 建议

### Issue 1: 蓝牙连接状态管理重构
**背景**: 当前 `bleService.disconnect()` 异步处理存在 race condition，用户切换器械或离开时连接未可靠释放，导致其他人无法连接  
**任务**: 
- 重构 `BleService.disconnect()` 为可靠断开（等待 `wx.closeBLEConnection()` 完成）
- 在页面 `onUnmounted` 中确保 disconnect 和 release 顺序正确
- 增加连接状态变更事件通知机制
- 添加 5 分钟无 IMU 数据自动断连保护
**影响范围**: `ble.ts`, `device-binding/index.vue`  
**验收标准**: 
- 用户A连接传感器 → 用户A离开页面/小程序 → 传感器立即释放 → 用户B可正常连接
- 用户扫新器械二维码 → 旧传感器可靠断开 → 新连接不受旧连接影响
**测试场景**:
1. A连接器械训练中，强退小程序 → B尝试连接同一器械 → 成功
2. A扫码器械1 → 连接传感器 → A扫器械2二维码 → 传感器切换无延迟

---

### Issue 2: 器械占用冲突处理
**背景**: 多用户同时扫同一台器械时，心跳间隔30秒导致冲突感知慢；前端对"占用中"情况的处理不够友好  
**任务**:
- 前端：检测到 `occupyEquipment` 返回占用中时，显示"器械正被使用，请稍后"并引导刷新
- 心跳失败后立即触发重连提示而非静默失败
- 可选：增加短轮询（5秒）替代30秒心跳，提升冲突检测速度
**影响范围**: `device-binding/index.vue`, `api.ts`  
**验收标准**: 多用户并发扫同一器械时，每个用户都能在10秒内得到明确的状态反馈  
**测试场景**: 用户A连接器械，用户B扫码同一器械 → B在10秒内看到"器械正被使用"提示

---

### Issue 3: 训练结果汇总页
**背景**: 当前训练完成后仅保存记录并显示 sessionId，用户没有直观的训练结果展示页面  
**任务**:
- 新增 `/pages/training-result/index` 训练结果页
- 展示：完成组数/次数/时长、每组详情、峰值数据、AI反馈（预留A Line接口）
- 支持"分享到微信聊天"或"保存到相册"（小程序码生成）
- 训练完成后自动跳转结果页
**影响范围**: 新增页面，`device-binding/index.vue`（结束后跳转逻辑）  
**验收标准**:
- 训练结束点击"保存"后自动展示结果页
- 结果页显示全部已记录组明细
- 结果页数据与实际训练记录一致

---

### Issue 4: A Line API 接入与结果页适配层
**背景**: `ruoyi-intervention` 模块已预留但未接入，训练结果无法对接 AI 反馈  
**任务**:
- 后端：实现 `/api/training/session` 提交后触发 A Line 分析任务
- 前端：在结果页预留 AI 反馈展示区域，调用 `getRenderedTrainingPlan` 或新接口
- 适配层：`api.ts` 中增加 `parseTrainingResult` 统一不同 API 响应格式
**影响范围**: `api.ts`, `training-result/index.vue`, 后端 `TrainingSessionController`  
**验收标准**:
- 训练提交后，结果页在5秒内展示AI分析结果（或"分析中"状态）
- 网络异常时展示降级UI，不阻塞结果查看

---

### Issue 5: 小程序 UI/UX 成熟化
**背景**: 当前界面整体偏 demo 风格，不能支撑正式健身房场景  
**任务**:
- 进度页：柱状图增加数值标签，鼠标悬停显示详情
- 实时计数页：增加呼吸动效/计数爆发动效，区分组间休息状态
- 整体：统一按钮点击反馈、统一错误状态设计、统一加载状态设计
- 首页：增加当前训练任务快捷入口
- 设备绑定页：已绑定设备列表支持左滑删除
**影响范围**: `home/index.vue`, `progress/index.vue`, `device-binding/index.vue`, `daily-task/index.vue`  
**验收标准**:
- 各页面符合昕动健康品牌设计规范
- 所有交互有明确的状态反馈（加载中/成功/失败/空状态）
- 真机测试无明显性能问题

---

### Issue 6: 若依后台器械管理优化
**背景**: 后台器械管理功能可用但入口隐蔽，页面风格传统  
**任务**:
- 器械列表页增加状态筛选（在线/离线/维护中）
- 器械详情页增加实时在线状态（WebSocket 或轮询）
- 二维码生成功能移到器械列表操作栏
- 设备分组管理页面优化，支持拖拽排序
**影响范围**: `RuoYi-Vue3-master/src/views/iot/equipment/`, `RuoYi-Backend/ruoyi-iot/`  
**验收标准**:
- 后台管理员能在3步内完成"扫码绑定新器械到现有分组"
- 器械在线状态变更在后台5秒内刷新

---

### Issue 7: 已绑定设备管理增强
**背景**: `getMyDevices()` 无分页，且设备解绑后前端状态更新不及时  
**任务**:
- `getMyDevices()` 增加分页参数
- 解绑成功后前端乐观更新设备列表
- 解绑失败时显示明确原因（"该设备正在使用中"）
- 设备详情页增加"最后在线时间"和"累计训练次数"
**影响范围**: `api.ts`, `profile/index.vue`  
**验收标准**:
- 100台设备的用户解绑操作，前端列表无闪烁
- 解绑正在使用中的设备时，显示"设备在线，是否确认强制解绑"

---

## 8. 不纳入本期改造范围

- `login/index.vue` 登录流程重构（当前微信授权登录已可用）
- `training-plan/index.vue`（未在核心流程中，当前任务来自 `daily-task`）
- 若依用户权限管理模块（RuoYi自带功能已满足）
- IMU原始数据存储方案（当前仅前端实时使用，未持久化）
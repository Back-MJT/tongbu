# IMU 器械小程序重构规划

## 1. 真实业务场景

用户在健身器械前打开微信小程序，扫描器械二维码。小程序根据二维码识别器械，并从若依后端获取这台器械绑定的 IMU 蓝牙信息。随后小程序连接安装在器械上的 GY-BLE25T 传感器，实时接收 IMU 数据，在本地完成动作次数、组数和训练时长统计。训练结束后，小程序把训练结果上传到若依后端，后台管理端用于展示器械使用情况、用户训练记录和设备状态。

核心原则：

- 小程序负责扫码、蓝牙连接、实时 IMU 数据解析和本地计数。
- 若依后端负责设备、器械、用户、训练记录、统计展示和权限管理。
- 后台 Vue 管理端负责展示使用情况，不参与实时蓝牙数据处理。
- MySQL 保存业务数据，Redis 保存短期状态和最近使用信息。

## 2. 目标链路

```text
小程序扫码器械二维码
  -> 调用若依后端解析器械
  -> 后端返回器械信息、绑定 IMU 信息、BLE UUID、计数配置
  -> 小程序扫描并连接 GY-BLE25T
  -> 订阅 BLE Notify 数据
  -> 小程序解析 ax/ay/az/gx/gy/gz/roll/pitch/yaw
  -> 小程序本地计数
  -> 训练结束上传训练摘要
  -> 若依后台展示器械使用情况
```

## 3. GY-BLE25T 协议约定

来自 `/Users/black/Desktop/GY_ble25t配套资料/arduino/arduino_BLE/BLE_client/gy_ble25t_client/gy_ble25t_client.ino`：

| 项 | 值 |
| --- | --- |
| BLE 设备名示例 | `gy_ble25t1` |
| Service UUID | `0000FFE0-0000-1000-8000-00805F9B34FB` |
| Notify Characteristic UUID | `0000FFE4-0000-1000-8000-00805F9B34FB` |
| CCCD UUID | `00002902-0000-1000-8000-00805F9B34FB` |
| 建议输出频率 | 10Hz |

BLE Notify 数据按 22 字节解析，高字节在前：

| 字节 | 字段 | 说明 |
| --- | --- | --- |
| 0-1 | ax | 加速度 X |
| 2-3 | ay | 加速度 Y |
| 4-5 | az | 加速度 Z |
| 6-7 | gx | 角速度 X |
| 8-9 | gy | 角速度 Y |
| 10-11 | gz | 角速度 Z |
| 12-13 | roll | 横滚角，`raw / 100` |
| 14-15 | pitch | 俯仰角，`raw / 100` |
| 16-17 | yaw | 航向角，`raw / 100` |
| 18-19 | temp | 温度，`raw / 100` |
| 20-21 | ho | 保留或高度字段，先原样保留 |

## 4. 数据模型规划

### 4.1 器械表 `xd_equipment`

用于描述用户实际扫码和使用的健身器械。

| 字段 | 说明 |
| --- | --- |
| equipment_id | 主键 |
| equipment_code | 器械编号，例如 `EQ-000001` |
| equipment_name | 器械名称 |
| equipment_type | 器械类型，例如 chest_press、rowing、leg_press |
| location | 安装位置 |
| qr_content | 二维码内容 |
| status | 启用、停用、维护 |
| remark | 备注 |

### 4.2 IMU 设备表 `iot_device`

沿用若依 `ruoyi-iot` 模块设备表，但字段要适配 GY-BLE25T。

关键字段：

- `device_code`
- `device_name`
- `bluetooth_name`
- `service_uuid`
- `notify_char_uuid`
- `battery_level`
- `status`
- `firmware_version`

### 4.3 器械-IMU 绑定表 `xd_equipment_device`

一台器械当前绑定一颗 IMU，后续支持更换。

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| equipment_id | 器械 ID |
| device_id | IMU 设备 ID |
| bind_time | 绑定时间 |
| unbind_time | 解绑时间 |
| status | active、inactive |

### 4.4 计数配置表 `xd_equipment_counting_config`

不同器械动作方向不同，需要后台可配置。

| 字段 | 说明 |
| --- | --- |
| config_id | 主键 |
| equipment_type | 器械类型 |
| main_axis | 主判断轴，roll/pitch/yaw/ax/ay/az/gx/gy/gz |
| up_threshold | 上行阈值 |
| down_threshold | 下行阈值 |
| min_interval_ms | 两次计数最短间隔 |
| min_range | 有效动作最小幅度 |
| smoothing_window | 平滑窗口 |

### 4.5 训练会话表 `xd_training_session`

小程序训练结束后上传训练摘要。

| 字段 | 说明 |
| --- | --- |
| session_id | 主键 |
| user_id | 若依用户或小程序用户 ID |
| equipment_id | 器械 ID |
| device_id | IMU 设备 ID |
| start_time | 开始时间 |
| end_time | 结束时间 |
| duration_sec | 训练时长 |
| total_reps | 总次数 |
| total_sets | 总组数 |
| status | in_progress、finished、aborted |
| source | mini_program |

### 4.6 训练组表 `xd_training_set`

| 字段 | 说明 |
| --- | --- |
| set_id | 主键 |
| session_id | 训练会话 ID |
| set_no | 第几组 |
| reps | 本组次数 |
| duration_sec | 本组时长 |
| quality_score | 动作质量分，第一版可为空 |

## 5. 后端 API 规划

### 5.1 小程序器械解析

```text
GET /api/mini/equipment/resolve?code=EQ-000001
```

返回：

```json
{
  "equipmentId": 1,
  "equipmentCode": "EQ-000001",
  "equipmentName": "坐姿推胸训练器",
  "equipmentType": "chest_press",
  "deviceCode": "IMU-000001",
  "bluetoothName": "gy_ble25t1",
  "serviceUuid": "0000FFE0-0000-1000-8000-00805F9B34FB",
  "notifyCharUuid": "0000FFE4-0000-1000-8000-00805F9B34FB",
  "countingConfig": {
    "mainAxis": "pitch",
    "upThreshold": 20,
    "downThreshold": 5,
    "minIntervalMs": 600,
    "minRange": 15,
    "smoothingWindow": 5
  }
}
```

### 5.2 训练会话

```text
POST /api/mini/training/session/start
POST /api/mini/training/session/update
POST /api/mini/training/session/finish
```

第一版可以只实现 `start` 和 `finish`，`update` 用于后台实时看板时再加。

`finish` 请求：

```json
{
  "sessionId": 10001,
  "equipmentCode": "EQ-000001",
  "deviceCode": "IMU-000001",
  "startTime": "2026-05-01 10:00:00",
  "endTime": "2026-05-01 10:08:30",
  "durationSec": 510,
  "totalReps": 48,
  "totalSets": 4,
  "sets": [
    { "setNo": 1, "reps": 12, "durationSec": 45 },
    { "setNo": 2, "reps": 12, "durationSec": 48 }
  ]
}
```

## 6. 小程序改造规划

现有小程序路径：

```text
/Users/black/Desktop/try/Xindong_Platform-main/mini-program
```

重点文件：

- `src/pages/device-binding/index.vue`
- `src/services/ble.ts`
- `src/services/api.ts`
- `src/config/env.ts`

改造方向：

1. 将“绑定设备”页面改为“扫码使用器械”或新增训练入口页面。
2. 扫码后调用 `/api/mini/equipment/resolve`。
3. `ble.ts` 增加 GY-BLE25T 专用连接流程：
   - 初始化蓝牙适配器。
   - 扫描设备。
   - 按 `bluetoothName` 或设备 ID 匹配。
   - 连接设备。
   - 获取 `FFE0` 服务。
   - 订阅 `FFE4` 特征值 Notify。
   - 解析 22 字节 IMU 数据。
4. 新增本地计数服务：
   - 输入 IMU 样本。
   - 根据后台配置选择主轴。
   - 平滑滤波。
   - 状态机计数。
   - 生成 set 和 session 结果。
5. 训练结束后调用若依后端上传摘要。

## 7. 第一版计数算法

先采用阈值状态机，适合 MVP 验证。

状态：

```text
idle -> moving_up -> peak -> moving_down -> counted -> idle
```

输入：

- `axisValue`：主轴值。
- `timestamp`：采样时间。

基本规则：

- `axisValue >= upThreshold` 进入上行动作。
- 达到峰值后回落。
- `axisValue <= downThreshold` 且距离上次计数超过 `minIntervalMs`，计 1 次。
- 本次动作最大值和最小值差小于 `minRange` 时不计数。

后续再升级：

- 根据不同器械类型使用不同轴。
- 引入角速度峰值过滤。
- 增加动作质量评分。
- 增加异常识别：半程、过快、停顿、抖动。

## 8. 若依后台展示规划

后台管理端用于展示和运营，不参与实时计数。

第一版页面：

- 器械管理
- IMU 设备管理
- 器械-IMU 绑定管理
- 训练记录
- 用户训练统计
- 器械使用排行

首页看板指标：

- 今日训练人数
- 今日训练次数
- 今日总次数
- 器械使用排行
- 设备在线 / 离线数量

## 9. 实施顺序

1. 统一若依后端 MySQL 和 Redis 配置。
2. 新增 MySQL 表：器械、器械-IMU 绑定、计数配置、训练会话、训练组。
3. 实现 `/api/mini/equipment/resolve`。
4. 改造小程序 `ble.ts`，支持 GY-BLE25T 扫描、连接、Notify 和数据解析。
5. 新增小程序本地计数服务。
6. 实现训练会话 `start` / `finish` API。
7. 小程序完成“扫码 -> 连接 IMU -> 计数 -> 上传结果”闭环。
8. 后台管理端补器械和训练记录展示。

## 10. 暂不做的事情

- 不在后端实时接收全部原始 IMU 高频数据。
- 不把 Kafka、独立 Node device-gateway、独立 Python 干预引擎作为第一版主链路。
- 不先做复杂 AI 动作识别。
- 不先做多传感器融合。

第一版目标是让真实器械上的 GY-BLE25T 能被小程序稳定连接，并完成可靠的动作次数统计和后台展示闭环。

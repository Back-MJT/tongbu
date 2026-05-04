# BLE Scanner 广播适配器技术方案

> **⚠️ 已废弃 (DEPRECATED) — 2026-04-15**
> 
> 本文档描述的 Eddystone-UID 广播监听方案基于 nRF52840 自研固件架构，
> 现已随 nRF52840 方案一起被 **GY-BLE25T 国产9轴IMU模组**替代。
> 
> GY-BLE25T 使用标准 GATT 连接模式（FFE0 Service, FFE4 Notify），
> 已有 `BLEAdapter` 完全兼容，无需新增 `BLEScannerAdapter`。
> 
> 替代文档: `docs/GY-BLE25T_IMU模块验证报告.md`
> 废弃原因: XIN-96 确认使用已采购国产模组，XIN-111 同步更新
> 
> 以下内容保留作参考，不再作为开发依据。

> 文档版本: v0.1  
> 日期: 2026-04-13  
> 作者: Founding Engineer  
> 状态: ~~草稿 — 待评审~~ → **已废弃**  
> 关联: ~~nRF52840固件架构文档 (XIN-73)~~ → 已废弃

---

## 1. 背景与目标

### 1.1 问题

nRF52840 固件（Board v1.1 第零层）使用 Eddystone-UID 非连接广播模式，device-integration 现有 `BLEAdapter` 只支持 GATT 连接模式，无法接收 BLE 广播数据。

### 1.2 目标

新增 `BLEScannerAdapter`（广播监听模式），解析 nRF52840 固件广播的 Eddystone-UID Service Data，将组数/套数/时长等数据转换为 `IMUPowerTrainingData`，纳入统一数据管道。

---

## 2. 架构设计

### 2.1 整体数据流

```
nRF52840 模组 (Eddystone-UID 广播, 1Hz)
    │
    ▼ BLE 广播 (Service UUID: 0xFEAA)
┌─────────────────────────────────────────────┐
│  BLEScannerAdapter                          │
│  1. 扫描 BLE 广播包                         │
│  2. 过滤 HealthHub 设备 (Eddystone-UID)     │
│  3. 解析 Service Data (26 bytes)            │
│  4. 转换为 IMUPowerTrainingData              │
│  5. 通过 getDataStream() 推送 DeviceData     │
└─────────────────────────────────────────────┘
    │
    ▼ DeviceData (source: "imu_addon")
┌─────────────────────────────────────────────┐
│  DataPipeline → Kafka → TimescaleDB         │
└─────────────────────────────────────────────┘
```

### 2.2 与现有 BLEAdapter 的关系

| 维度 | BLEAdapter (GATT模式) | BLEScannerAdapter (广播模式) |
|------|---------------------|----------------------------|
| BLE角色 | Central (连接者) | Observer (监听者) |
| 连接方式 | 连接 GATT Server | 仅扫描，不连接 |
| 数据获取 | 订阅 Notification | 解析广播包 Service Data |
| 适用设备 | 心率带、血压计等 | nRF52840 模组 |
| 协议 | BLE GATT | Eddystone-UID |
| 设备ID | Peripheral MAC | Eddystone namespace+instance |

两者可共存，同时支持不同类型设备。

---

## 3. nRF52840 广播数据格式

### 3.1 Eddystone-UID 广播包结构

```
Byte 0:     Protocol Version (0x01)
Byte 1:     Device Type Flags (0b00000001)  bit0=IMU, bit1=HasPower
Byte 2-3:   Device ID（16-bit，模组唯一ID）
Byte 4-5:   Firmware Version (BCD, e.g. 0x0100 = v1.0.0)
Byte 6:     Battery Level (%) — 0xFF=外部供电
Byte 7:     Current Rep Count (本组)
Byte 8:     Current Set Count (累计，本 session)
Byte 9-10:  Session Duration (秒)
Byte 11-12: Last Rep Magnitude (0.01g resolution)
Byte 13-14: Rest Time (秒，距上次 rep)
Byte 15:    RSSI @ 1m (signed, dBm)
Byte 16-27: Reserved (checksum, future use)
```

### 3.2 Service UUID

HealthHub 使用 Eddystone-UID，`Service UUID = 0xFEAA`（Google定义）。

### 3.3 解析规则

```typescript
function parseEddystoneUIDServiceData(data: Buffer): ParsedBroadcast {
  const version = data[0];           // 协议版本，必须为 0x01
  const flags = data[1];              // 设备类型标志
  const deviceId = data.readUInt16LE(2);   // 16-bit 设备ID
  const firmwareVersion = data.readUInt16LE(4); // BCD 格式
  const batteryLevel = data[6];      // 0xFF 表示外部供电
  const currentReps = data[7];        // 本组次数
  const currentSets = data[8];        // 累计组数
  const sessionDurationSec = data.readUInt16LE(9); // 秒
  const lastRepMagnitude = data.readUInt16LE(11) * 0.01; // g
  const restTimeSec = data.readUInt16LE(13); // 秒
  const rssiAt1m = data[15];          // dBm (signed)

  const isIMU = (flags & 0x01) !== 0;
  const hasPower = (flags & 0x02) !== 0;

  return {
    deviceId: `HB-${deviceId.toString(16).toUpperCase().padStart(4, '0')}`,
    version,
    firmwareVersion: `${(firmwareVersion >> 8)}.${(firmwareVersion >> 4) & 0x0F}.${firmwareVersion & 0x0F}`,
    battery: batteryLevel === 0xFF ? null : batteryLevel,
    currentReps,
    currentSets,
    sessionDurationSec,
    lastRepMagnitude,
    restTimeSec,
    rssiAt1m,
    isIMU,
    hasPower,
  };
}
```

---

## 4. BLEScannerAdapter 实现

### 4.1 接口设计

`BLEScannerAdapter` 实现 `IProtocolAdapter` 接口，但工作模式为广播监听：

```typescript
// BLEScannerAdapter 关键方法

// 扫描广播包 (Observable<ScannedDevice>)
// 每收到一个匹配的 BLE 广播，触发 next()

// connect() / disconnectDevice()
// 在广播监听场景下无意义（不建立连接）
// 实现为空操作，仅记录日志

// sendCommand()
// 在广播监听场景下无意义
// 实现为空操作或抛出异常

// getDataStream() 返回 Observable<DeviceData>
// 每次解析到有效的 Service Data，推送 DeviceData
```

### 4.2 设备类型映射

解析后的广播数据映射到 `IMUPowerTrainingData`：

```typescript
function toIMUPowerTrainingData(parsed: ParsedBroadcast): IMUPowerTrainingData {
  return {
    dataType: 'imu_power_training',
    imu: {
      // 注: 固件只广播汇总数据，不含 IMU 原始加速度
      // 原始数据通过后续 GATT 连接获取（如有）
      accelX: 0,
      accelY: 0,
      accelZ: 0,
      gyroX: 0,
      gyroY: 0,
      gyroZ: 0,
    },
    sensorPosition: 'device_fixed',
    calibrationStatus: 'calibrated',
    sensorId: parsed.deviceId,
    derived: {
      estimatedReps: parsed.currentReps,
      estimatedLoadKg: undefined, // 需用户输入或设备联动
      movementVelocity: undefined,
      peakAcceleration: parsed.lastRepMagnitude,
      exerciseType: undefined, // 需设备动作识别
      confidence: parsed.currentReps > 0 ? 0.7 : 0,
    },
  };
}
```

### 4.3 设备数据映射

```typescript
function toDeviceData(parsed: ParsedBroadcast, rssi: number): DeviceData {
  return createDeviceData({
    id: parsed.deviceId,
    deviceType: 'imu_power_training',
    protocol: 'ble',
    status: 'online',
    metadata: {
      manufacturer: 'HealthHub',
      model: `HB-IMU-${parsed.firmwareVersion}`,
      signalStrength: rssi,
      firmwareVersion: parsed.firmwareVersion,
      online: true,
      connectionType: 'BLE-Broadcast',
    },
    data: toIMUPowerTrainingData(parsed),
    battery: parsed.battery ?? undefined,
    signal: rssi,
    timestamp: new Date().toISOString(),
    receivedAt: new Date().toISOString(),
  });
}
```

### 4.4 去重与防抖策略

由于固件 1Hz 广播推送，同一设备每秒推送一次相同数据。管道需要去重：

```typescript
// 在 BLEScannerAdapter 内部去重
// 同一个 deviceId 的数据，如果与上一次推送完全相同（currentReps, currentSets），
// 则跳过推送，避免管道处理冗余数据

private lastDataCache = new Map<string, ParsedBroadcast>();

private shouldEmit(parsed: ParsedBroadcast): boolean {
  const key = parsed.deviceId;
  const last = this.lastDataCache.get(key);
  
  if (!last) return true;
  if (last.currentReps !== parsed.currentReps) return true;
  if (last.currentSets !== parsed.currentSets) return true;
  if (last.sessionDurationSec !== parsed.sessionDurationSec) return true;
  
  return false; // 数据未变，跳过
}
```

---

## 5. device.model.ts 扩展

### 5.1 新增设备类型

`DeviceTypeSchema` 需新增 `imu_power_training` (已存在于 DEVICE_SCHEMA_UNIFIED.md):

```typescript
export const DeviceTypeSchema = z.enum([
  // ... 现有类型 ...
  'imu_power_training',  // IMU加装力量训练设备 (新增)
]);
```

### 5.2 新增 IMUPowerTrainingData Schema

```typescript
export const IMUPowerTrainingDataSchema = z.object({
  dataType: z.literal('imu_power_training'),
  imu: z.object({
    accelX: z.number(),
    accelY: z.number(),
    accelZ: z.number(),
    gyroX: z.number(),
    gyroY: z.number(),
    gyroZ: z.number(),
  }),
  sensorPosition: z.enum(['wrist', 'waist', 'device_fixed']),
  calibrationStatus: z.enum(['calibrated', 'needs_calibration']),
  sensorId: z.string(),
  derived: z.object({
    estimatedReps: z.number().optional(),
    estimatedLoadKg: z.number().optional(),
    movementVelocity: z.number().optional(),
    peakAcceleration: z.number().optional(),
    exerciseType: z.string().optional(),
    confidence: z.number().optional(),
  }),
});
export type IMUPowerTrainingData = z.infer<typeof IMUPowerTrainingDataSchema>;
```

---

## 6. 实现计划

### 6.1 文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `device-integration/src/protocols/ble.scanner.ts` | 新增 | BLEScannerAdapter 实现 |
| `device-integration/src/protocols/adapter.interface.ts` | 修改 | ScannedDevice 扩展 Eddystone 字段 |
| `device-integration/src/models/device.model.ts` | 修改 | 新增 IMUPowerTrainingDataSchema |
| `device-integration/src/protocols/index.ts` | 修改 | 导出 BLEScannerAdapter |
| `device-integration/src/gateway/device.gateway.ts` | 修改 | 注册 BLEScannerAdapter |

### 6.2 开发工作量

| 任务 | 工作量 | 优先级 |
|------|--------|--------|
| 扩展 DeviceTypeSchema + IMUPowerTrainingDataSchema | 0.5d | P0 |
| 实现 BLEScannerAdapter 核心解析逻辑 | 1d | P0 |
| 集成到 device.gateway.ts | 0.5d | P0 |
| 单元测试 (mock BLE 广播数据) | 0.5d | P1 |
| **合计** | **2.5d** | |

### 6.3 前置依赖

- nRF52840 固件蓝牙广播格式（已文档化，固件文档第 6.2 节）
- device.model.ts 设备类型扩展（需新增 `imu_power_training`）
- 实际联调需等待 nRF52840 硬件到位

---

## 7. 联调计划

### 7.1 模拟数据测试（不依赖硬件）

使用 `noble` 或 `bleno` 模拟 Eddystone-UID 广播，进行协议解析测试：

```typescript
// 测试用例
const mockBroadcast = Buffer.from([
  0x01,       // Version
  0x01,       // Flags: IMU device
  0x12, 0x34, // Device ID: 0x3412
  0x01, 0x00, // FW: v1.0.0
  0xFF,       // External power
  10,         // Current reps: 10
  3,          // Current sets: 3
  0x10, 0x00, // Session: 16 seconds
  0x96, 0x00, // Magnitude: 150 * 0.01 = 1.5g
  0xE8, 0x03, // Rest: 1000 seconds
  -45,        // RSSI @1m: -45 dBm
  // ... reserved
]);

const parsed = parseEddystoneUIDServiceData(mockBroadcast);
assert(parsed.deviceId === 'HB-3412');
assert(parsed.currentReps === 10);
assert(parsed.currentSets === 3);
assert(parsed.lastRepMagnitude === 1.5);
```

### 7.2 真实硬件联调（等待硬件到位）

1. nRF52840 模组上电，开始广播
2. 运行 BLEScannerAdapter，扫描设备
3. 验证解析出的 currentReps/currentSets 与固件一致
4. 验证数据管道 Kafka 写入正确

---

## 8. 已知限制

1. **无原始 IMU 数据**: 固件广播只含汇总数据（组数/套数），不含 50Hz 原始加速度。原始数据需要后续 GATT 连接获取（或通过 WiFi 网关方案补充）。
2. **无法下发命令**: 广播模式是单向的，配置命令（如调整阈值）需要通过 BLE GATT 连接实现，这是 Phase 2 的工作。
3. **多设备区分**: 当多人在同一器械附近时，无法仅靠广播数据区分用户。依赖手机小程序做时间窗口推断。解决方案在 Phase 2 考虑。
4. **RSSI 精度**: BLE RSSI 受环境影响大，仅作为辅助参考。

---

## 9. 参考文档

- nRF52840固件技术架构 v1.0.md (docs/nRF52840_固件技术架构_v1.0.md) 第 6 节
- DEVICE_SCHEMA_UNIFIED.md 第 2.1 节 (IMUPowerTrainingData)
- device-integration/src/protocols/ble.adapter.ts (现有 BLEAdapter 参考)
- device-integration/src/protocols/adapter.interface.ts (IProtocolAdapter 接口定义)

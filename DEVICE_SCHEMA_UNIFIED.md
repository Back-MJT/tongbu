# 设备数据统一接入Schema
> 文档版本: v0.1
> 日期: 2026-04-10
> 作者: Product Manager
> 状态: 初稿 — 待工程评审

---

## 设计目标

**问题**: 不同品牌、类型的健康设备使用不同的数据格式和通信协议。昕动数字底座需要一种与设备无关的统一数据模型，对上屏蔽协议差异，对下兼容各类设备。

**解决方案**: 所有设备数据在设备接入网关层统一转换为 `DeviceData` 结构化格式，后续所有服务只依赖此统一 schema，不直接接触设备原始协议。

---

## 核心理念

1. **协议解耦**: 设备接入网关负责将 BLE/MQTT/HTTP/私有协议转换为统一格式
2. **设备无关**: schema 设计覆盖力量训练、心率监测、睡眠分析、体脂管理等全品类
3. **最小上界**: `metrics` 使用 `Record<string, number>` + 标准化字段清单，兼顾灵活性和规范性
4. **时序优先**: 统一毫秒时间戳，支持时序数据库高效写入

---

## 统一数据结构

### DeviceData (顶层封装)

```typescript
interface DeviceData {
  // 设备标识
  deviceId: string;        // 设备唯一ID (MAC地址或厂商分配ID)
  deviceType: DeviceType;   // 设备类型枚举
  deviceModel: string;      // 设备型号 (厂商型号字符串)

  // 数据来源
  source: DataSource;       // 数据来源: "device_direct" | "imu_addon" | "manual"
  userId?: string;          // 关联用户ID (若已绑定)

  // 时序信息
  timestamp: number;        // 采集时间戳 (毫秒, UTC)
  receivedAt: number;      // 服务端接收时间戳 (毫秒, UTC)

  // 数据内容
  sessionId: string;       // 训练session ID (用于关联一组训练)
  data: DeviceRawData;      // 设备原始数据 (每种设备类型不同)

  // 设备状态
  metadata: DeviceMetadata; // 信号强度、电池等
}

interface DeviceMetadata {
  signalStrength?: number;  // RSSI (dBm)
  battery?: number;         // 电量 0-100
  firmwareVersion?: string; // 固件版本
  online: boolean;          // 设备在线状态
  connectionType?: string;  // "BLE" | "WiFi" | "USB"
}
```

### 设备类型枚举

```typescript
type DeviceType =
  | "power_training"      // 数字力量训练设备 (客户A)
  | "imu_power_training" // IMU加装力量训练设备 (客户B)
  | "heart_rate_monitor" // 心率带
  | "sleep_mat"          // 睡眠监测垫
  | "scale"              // 体脂秤
  | "blood_pressure"     // 血压计
  | "cycling"            // 功率车
  | "treadmill"          // 跑步机
  | "unknown";           // 未知设备
```

### 数据来源枚举

```typescript
type DataSource =
  | "device_direct"  // 设备原生数据 (如客户A数字力量器械)
  | "imu_addon"      // IMU传感器加装方案 (如客户B)
  | "manual";        // 用户手动输入 (如体重秤)
```

---

## 力量训练特定数据模型

### PowerTrainingData (客户A — 数字力量器械直接接入)

```typescript
interface PowerTrainingData {
  dataType: "power_training";

  // 动作信息
  exerciseName: string;     // 动作名称: "bench_press" | "squat" | ...
  setNumber: number;        // 第几组

  // 力量指标
  reps: number;             // 重复次数
  loadKg: number;           // 负荷重量 (kg)
  powerWatts?: number;      // 即时功率 (W) — 数字力量器械核心指标
  velocityMs?: number;      // 动作速度 (m/s)
  peakPower?: number;       // 峰值功率 (W)

  // 时间
  durationSeconds: number;  // 本组时长 (秒)
  restSeconds?: number;     // 组间休息时长 (秒)

  // 生理指标
  heartRateBpm?: number;    // 心率 (bpm)
  caloriesBurned?: number;  // 本组消耗卡路里

  // 质量评分
  qualityScore?: number;    // 动作质量评分 0-100 (若有传感器)
}
```

### IMUPowerTrainingData (客户B — IMU加装方案)

```typescript
interface IMUPowerTrainingData {
  dataType: "imu_power_training";

  // IMU原始数据 (100Hz采样)
  imu: {
    accelX: number;  // 加速度 X轴 (g)
    accelY: number;  // 加速度 Y轴 (g)
    accelZ: number;  // 加速度 Z轴 (g)
    gyroX: number;   // 角速度 X轴 (°/s)
    gyroY: number;   // 角速度 Y轴 (°/s)
    gyroZ: number;   // 角速度 Z轴 (°/s)
  };

  // 传感器元数据
  sensorPosition: "wrist" | "waist" | "device_fixed"; // 安装位置
  calibrationStatus: "calibrated" | "needs_calibration";
  sensorId: string;  // IMU传感器唯一ID

  // 算法输出 (由边缘端或云端计算)
  derived: {
    estimatedReps?: number;       // 估算重复次数
    estimatedLoadKg?: number;     // 估算负荷 (需用户输入或设备联动)
    movementVelocity?: number;    // 动作速度 (m/s)
    peakAcceleration?: number;    // 峰值加速度 (g)
    exerciseType?: string;         // 算法估算的动作类型
    confidence?: number;           // 算法置信度 0-1
  };
}
```

---

## 健康指标统一映射

无论数据来源，最终都映射至标准 `HealthMetrics`:

```typescript
interface HealthMetrics {
  userId: string;
  timestamp: number;           // 毫秒 UTC
  sessionId: string;
  deviceId: string;
  deviceType: DeviceType;

  // 标准化健康指标 (可选字段，按设备能力填充)
  metrics: {
    // 运动通用
    exerciseType?: string;       // 动作类型
    exerciseDuration?: number;     // 运动时长 (秒)
    exerciseIntensity?: number;   // 强度 1-10
    caloriesBurned?: number;      // 卡路里

    // 力量训练专用
    exerciseReps?: number;        // 重复次数
    load?: number;                // 负荷 (kg)
    power?: number;               // 功率 (W)
    movementVelocity?: number;    // 速度 (m/s)

    // 生理指标
    heartRate?: number;           // 心率 (bpm)
    hrv?: number;                 // 心率变异性 (ms)
    bloodOxygen?: number;         // 血氧饱和度 (%)

    // 睡眠指标 (睡眠垫)
    sleepDuration?: number;       // 睡眠时长 (秒)
    deepSleepRatio?: number;      // 深睡比例 0-1
    remSleepRatio?: number;      // REM睡眠比例 0-1

    // 体征指标 (体脂秤)
    weight?: number;              // 体重 (kg)
    bodyFatRatio?: number;       // 体脂率 (%)
    bmi?: number;                // BMI

    // [可扩展] 其他指标
    [key: string]: number | string | undefined;
  };

  // 数据质量标记
  quality: {
    score: number;               // 数据质量评分 0-100
    issues?: string[];           // 已知的质量问题
    algorithmConfidence?: number; // 算法置信度 (IMU方案)
  };
}
```

---

## 数据转换管道

```
设备原始数据
    │
    ▼
┌─────────────────────────────────────────┐
│           设备接入网关 (Device Gateway)  │
│                                          │
│  1. 协议解析 (BLE Parser / MQTT Parser)  │
│  2. 字段映射 → DeviceData (统一格式)     │
│  3. 数据校验 (字段完整性、范围检查)       │
│  4. 时间戳对齐 (NTP同步)                  │
│  5. 写入 Kafka: device.raw               │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│           Flink 数据处理                  │
│                                          │
│  1. 消费 device.raw                      │
│  2. 数据清洗/异常检测                     │
│  3. 聚合 (session级汇总)                 │
│  4. 写入 TimescaleDB: health_metrics     │
└─────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────┐
│           干预引擎 (Intervention Engine)  │
│                                          │
│  1. 读取 HealthMetrics                    │
│  2. 更新用户画像                         │
│  3. 触发效果评估                         │
│  4. 生成/调整运动处方                    │
└─────────────────────────────────────────┘
```

---

## 字段映射规范

每个新设备接入需定义 `字段映射表`:

| 设备原始字段 | 数据类型 | 映射至 DeviceData 字段 | 转换函数 |
|-------------|---------|----------------------|---------|
| `set_id` (客户A) | string | `sessionId` | 直接复制 |
| `weight_kg` (客户A) | float | `data.loadKg` | 直接复制 |
| `accel_x` (IMU) | float | `data.imu.accelX` | 量程转换 (16g→-16~16) |
| ... | | | |

---

## 接入检查清单

新设备接入时，工程师需确认:

- [ ] 设备通信协议已解析 (BLE/WiFi/MQTT)
- [ ] 字段映射表已定义并测试
- [ ] DeviceData schema 校验通过 (Pydantic model)
- [ ] 时间戳同步正常 (设备时间 vs 服务端时间误差 < 1s)
- [ ] 数据延迟测试: P99 < 500ms
- [ ] Kafka topic `device.raw` 写入正常
- [ ] TimescaleDB 写入正常
- [ ] 数据质量日志正常

---

*文档状态: 初稿 — 待与工程团队(XIN-36)评审确认*

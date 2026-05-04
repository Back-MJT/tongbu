# nRF52840 + LSM6DSO 固件架构设计文档

> **⚠️ 已废弃 (2026-04-15)**
>
> 本文档描述的 nRF52840 + LSM6DSO 自研固件方案已被 **GY-BLE25T 国产9轴IMU模组**替代。
> GY-BLE25T = 9轴IMU + BLE 5.0一体模块，自带固件，无需自研嵌入式代码。
>
> **替代文档**: `docs/GY-BLE25T_IMU模块验证报告.md`
> **废弃原因**: Board决定采用国产成品模组，省去固件开发周期，MVP可用现成硬件直接对接。


> 版本: v0.1  
> 日期: 2026-04-12  
> 作者: Founding Engineer  
> 任务: XIN-73  
> 状态: 初稿

---

## 1. 概述

### 1.1 目标

为 HealthHub 第零层（IMU+BLE 硬件模组）设计基于 Nordic nRF52840 + ST LSM6DSO 的固件架构，实现：
- 抗阻器械（哑铃架/史密斯机/综合训练架/绳索机）的使用检测
- 组数计数（Rep Counting）
- BLE 5.0 广播数据上传
- 框架取电 + CR2032 备选双供电方案

### 1.2 参考文档

| 文档 | 说明 |
|------|------|
| `Board/HealthHub_产品技术规划_v1.1.md` §2.2 | 产品规格（芯片、传感器、BOM、安装方式） |
| `IMU_INTEGRATION_FEASIBILITY.md` (XIN-36) | IMU 选型对比、动作识别可行性、成本分析 |
| `DEVICE_SCHEMA_UNIFIED.md` | 统一设备数据格式，IMU 数据字段规范 |

### 1.3 关键约束

| 参数 | 值 | 说明 |
|------|-----|------|
| 主控 | Nordic nRF52840 | BLE 5.0, Cortex-M4, 1MB Flash, 256KB RAM |
| IMU | ST LSM6DSO | 6轴(3轴加速度+3轴陀螺仪), I2C@400kHz |
| 采样频率 | 50Hz | 足够捕捉抗阻动作，省电 |
| 防护等级 | IP54 | 防溅水/防尘，健身房环境足够 |
| BOM 成本 | ¥25-45（千片量） | Board v1.1 规定 |
| 安装方式 | 热缩螺丝固定，3分钟 | Board v1.1 规定 |
| 供电 | 框架取电（优先）/ CR2032（备选，12-18个月） | Board v1.1 规定 |

---

## 2. 硬件架构

### 2.1 系统框图

```
┌─────────────────────────────────────────────────────────┐
│                    nRF52840 模块                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐  │
│  │  Cortex-M4  │  │  BLE 5.0    │  │    GPIO / I2C   │  │
│  │  64MHz     │  │  Radio      │  │                 │  │
│  └─────────────┘  └─────────────┘  └────────┬────────┘  │
│         │              │                    │           │
└─────────┼──────────────┼────────────────────┼───────────┘
          │              │                    │
          │         ┌─────┴─────┐        ┌────┴────┐
          │         │  PCB天线   │        │ LSM6DSO │
          │         │  (或陶瓷)  │        │  (I2C)  │
          │         └───────────┘        └─────────┘
          │
   ┌──────┴──────┐
   │  框架取电线  │ ←→  或  CR2032  (JST PH 2P 连接器)
   │  (5V → LDO) │
   └─────────────┘
```

### 2.2 关键引脚连接

| nRF52840 引脚 | 功能 | 说明 |
|-------------|------|------|
| P0.26 (SDA) | I2C0 SDA | 连接 LSM6DSO SDA |
| P0.27 (SCL) | I2C0 SCL | 连接 LSM6DSO SCL，100kHz/400kHz |
| P0.06 | GPIO OUT | LSM6DSO INT1（数据就绪中断） |
| P0.08 | ADC_AIN0 | 供电电压检测（判断取电 vs 电池） |
| - | SWDCLK/SWDIO | 调试接口（pogo pin 触点） |
| - | RF IO | BLE 天线 |

### 2.3 供电方案

**方案A：框架取电（优先）**
```
器械框架（金属） → 导电垫片接触 → 5V DC → LDO(3.3V) → nRF52840 + LSM6DSO
```
- LDO 选型：ME6211（低 dropout，低静态电流 ~3µA）
- 优点：零维护，真正"免换电"
- 检测：ADC 监测输入电压 > 4V 视为取电模式

**方案B：CR2032 纽扣电池（备选）**
- 适用场景：无法取电的老旧器械
- 预期寿命：12-18 个月（50Hz 采样 + BLE 广播间歇模式）
- 低功耗策略：
  - 静息时进入 System OFF（< 1µA）
  - 检测到振动后 50ms 内唤醒
  - 组间休息（> 30s 无动作）自动回 System OFF

---

## 3. 固件架构（Zephyr RTOS）

### 3.1 为什么选 Zephyr

Board v1.1 指定 nRF52840，Zephyr 是 Nordic 官方支持的 RTOS：

| 对比项 | Zephyr RTOS | nRF5 SDK (裸机) | FreeRTOS |
|-------|------------|-----------------|----------|
| BLE 协议栈 | Zephyr BLE Controller + Host 统一管理 | SoftDevice 独立 | 需自行移植 |
| 功耗管理 | 内置 Tickless + Device PM | 手动配置 | 手动配置 |
| 驱动支持 | LSM6DSO 等传感器驱动丰富 | 需自行编写 | 无 |
| OTA 支持 | MCUmgr 内置 | 需自行实现 | 需自行实现 |
| 学习成本 | 中（文档完善） | 高（老旧） | 低 |

### 3.2 软件模块划分

```
firmware/
├── src/
│   ├── main.c                 # 应用入口，系统初始化
│   ├── app.c                  # 应用状态机（设备管理）
│   ├── ble/
│   │   ├── advertising.c      # BLE 广播管理（自定义 GAP）
│   │   └── gatt_service.c     # 自定义 GATT 服务（数据下发）
│   ├── imu/
│   │   ├── lsm6dso.c          # LSM6DSO 驱动（I2C）
│   │   ├── data_capture.c     # 采样循环，DMA 缓冲
│   │   └── motion_detect.c    # 运动检测（低功耗监听）
│   ├── algorithm/
│   │   ├── rep_counter.c      # 组数计数算法
│   │   └── set_uploader.c     # 组结束上传逻辑
│   ├── power/
│   │   ├── power_mgr.c        # 供电检测 + 功耗策略
│   │   └── battery.c          # CR2032 电量估算
│   └── config/
│       ├── device_id.c        # 设备唯一 ID（来自芯片 UUID）
│       └── calibration.c      # IMU 零偏校准参数存储
├── dts/
│   └── nrf52840_thingy53.dtsi # 设备树（引脚、I2C 配置）
├── prj.conf                   # Kconfig 项目配置
└── CMakeLists.txt             # 构建配置
```

### 3.3 状态机设计

```
                  ┌──────────────┐
                  │   SLEEP     │  ← System OFF (CR2032 模式)
                  │  (< 1µA)    │
                  └──────┬───────┘
                         │ 加速度阈值中断 (wake-up)
                         ▼
┌──────────────────────────────────────────────────────────────┐
│                      ACTIVE                                  │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐              │
│  │ CALIBRATE│ →  │ SAMPLING │ →  │ TRANSMIT │ → (回到SLEEP) │
│  │  (2s)    │    │ (50Hz)   │    │ (BLEadv)  │              │
│  └──────────┘    └────┬─────┘    └──────────┘              │
│                       │ 组间休息 > 30s                       │
└──────────────────────────────────────────────────────────────┘

框架取电模式：无 SLEEP 状态，持续 50Hz 采样 + BLE 广播
CR2032 模式：  ACTIVE(运动时) ↔ SLEEP(静息时)
```

---

## 4. 组数计数算法

### 4.1 算法原理

Board v1.1 §2.2 提供的伪代码思路，将之实现为 C 代码。

**核心思想：**
1. 合加速度 `= √(ax² + ay² + az²)`，单位 g
2. 动态阈值：静息时校准零点，运行时用 `零点 + 1.5g` 作为触发阈值
3. 抖动过滤：两次有效 rep 之间至少间隔 500ms
4. 组间判断：超过 30s 无新 rep，视为本组结束

### 4.2 C 实现方案

```c
// src/algorithm/rep_counter.c

#include <zephyr.h>
#include <math.h>

#define SAMPLE_RATE_HZ         50
#define MIN_REP_INTERVAL_MS    500   // 抖动过滤：两个 rep 至少间隔 500ms
#define SET_GAP_THRESHOLD_MS    30000 // 30s 无 rep → 本组结束
#define MOTION_THRESHOLD_G     1.5f  // 合加速度超过 1.5g 视为一次 rep

typedef struct {
    int rep_count;           // 当前组 rep 数
    int set_count;            // 累计完成的组数
    int64_t last_rep_ts_ms;  // 上次有效 rep 的时间戳（ms）
    int64_t set_start_ts_ms; // 本组开始时间戳
    float acc_baseline_g;    // 静息零点（开机校准）
    bool in_set;              // 是否处于一组运动中
} RepCounterState;

static RepCounterState state = {0};

// 动态阈值：零点 + 1.5g
static inline float motion_threshold(void) {
    return state.acc_baseline_g + MOTION_THRESHOLD_G;
}

// 计算合加速度（单位 g）
// ax/ay/az 为原始 ADC 值，需根据 IMU 量程换算
static inline float composite_accel(float ax, float ay, float az) {
    return sqrtf(ax*ax + ay*ay + az*az);
}

// 开机时校准零点（静息 2s，取平均值）
void rep_counter_calibrate(float ax, float ay, float az) {
    // 实际上在 data_capture.c 中通过 2s 滑动窗口计算
    state.acc_baseline_g = composite_accel(ax, ay, az);
}

// 主计数逻辑，在 50Hz 采样循环中调用
// 返回：0=无事件，1=新增一个rep，2=组结束（上传）
int rep_counter_update(int64_t now_ms, float ax, float ay, float az) {
    float acc = composite_accel(ax, ay, az);

    if (acc > motion_threshold()) {
        // 触发 rep
        if (state.last_rep_ts_ms == 0 ||
            (now_ms - state.last_rep_ts_ms) >= MIN_REP_INTERVAL_MS) {

            state.rep_count++;
            state.last_rep_ts_ms = now_ms;
            state.in_set = true;

            // 组结束判断（超过 SET_GAP_THRESHOLD_MS 无新 rep）
            // 由调用方在定时器中检测，此处只返回 rep 事件
            return 1;
        }
    }

    // 组结束检测（由调用方定时器触发，此处检查超时）
    if (state.in_set && (now_ms - state.last_rep_ts_ms) >= SET_GAP_THRESHOLD_MS) {
        // 本组结束，准备上传
        state.in_set = false;
        return 2; // 调用方应调用 rep_counter_upload_set()
    }

    return 0;
}

// 上传本组数据（BLE广播）
void rep_counter_upload_set(void) {
    if (state.rep_count == 0) return;

    SetData set_data = {
        .device_id   = device_id_get(),
        .set_count   = state.set_count + 1,
        .rep_count   = state.rep_count,
        .duration_ms = state.last_rep_ts_ms - state.set_start_ts_ms,
        .intensity   = estimate_intensity(), // 振动幅度估算重量等级
        .timestamp   = state.set_start_ts_ms,
    };

    ble_advertise_set(&set_data);  // 广播本组数据

    state.set_count++;
    state.rep_count = 0;
    state.set_start_ts_ms = now_ms(); // 下一组从现在开始计时
}

// 估算运动强度（轻/中/重），用于重量等级代理
// 思路：取本组平均合加速度峰值
static float estimate_intensity(void) {
    // 实际实现：在 data_capture.c 中记录本组 max(acc)
    // return: 0=轻, 1=中, 2=重
    return 0; // stub
}
```

### 4.3 动态零点漂移补偿

IMU 存在零点漂移（温度、机械应力），需定期重新校准：

```c
// 背景任务：每 5 分钟静默校准（在 SLEEP 唤醒间隔中执行）
void recalibrate_if_idle(float ax, float ay, float az) {
    static int64_t last_cal_ms = 0;
    if (k_uptime_get() - last_cal_ms > 5 * 60 * 1000) {
        // 过去 5 分钟无 rep，可以重新校准
        float current = composite_accel(ax, ay, az);
        // 缓慢补偿，避免突变
        state.acc_baseline_g = 0.98f * state.acc_baseline_g + 0.02f * current;
        last_cal_ms = k_uptime_get();
    }
}
```

---

## 5. BLE 广播数据格式设计

### 5.1 自定义 GAP 广播包

Board v1.1 采用 BLE 广播方式（不做 GATT 连接），适合"手机小程序直连"场景。

**广播数据格式（31 字节限制，分 2 个 AD Structure）：**

```
GAP Advertising Data:
├── AD Structure 1: Flags (2 bytes)
│   └── 0x1A 0xFF (LE General Discoverable, BR/EDR unsupported)
│
└── AD Structure 2: Manufacturer Specific Data (26 bytes max)
    └── Company ID: 0xFFFF (占位，Nordic 或自定义)
    └── Protocol Version: 0x01
    └── Device ID: 8 bytes (nRF52840 UICR 96-bit UUID 低 8 字节)
    └── Frame Type: 0x01 (SetSummary 帧)
    └── Set Count: 1 byte (本轮累计组数)
    └── Rep Count: 1 byte (最新一组 rep 数)
    └── Duration (s): 2 bytes (本组时长，little-endian)
    └── Intensity: 1 byte (0=轻/1=中/2=重)
    └── Battery Level: 1 byte (0-100%，CR2032 模式)
    └── Reserved: 1 byte
    └── CRC16: 2 bytes (数据完整性校验)
```

**实际 BLE 广播 payload（31 字节限制内）：**
```
Byte[0-1]: AD Type + Length
Byte[2-3]: Company ID (0xFF 0xFF)
Byte[4]:   Protocol Version (0x01)
Byte[5-12]: Device ID (8 bytes, nRF52840 96-bit UUID 低 64 位)
Byte[13]:  Frame Type (0x01=SetSummary)
Byte[14]:  Set Count (本设备累计组数)
Byte[15]:  Rep Count (最新一组 rep 数)
Byte[16-17]: Duration s (little-endian, max 65535s ≈ 18h)
Byte[18]:  Intensity (0/1/2)
Byte[19]:  Battery % (0-100)
Byte[20]:  Reserved
Byte[21-22]: CRC16 (XMODEM)
```

### 5.2 BLE 广播策略

| 场景 | 广播间隔 | 说明 |
|------|---------|------|
| 静息/出厂模式 | 1000ms | 低频，省电 |
| 运动中（组内） | 200ms | 高频，确保手机接收 |
| 组间休息 | 500ms | 中频 |
| 框架取电模式 | 100ms | 可持续高频 |

### 5.3 微信小程序接收

微信小程序 BLE 接收流程：
1. 搜索设备广播（Company ID 过滤）
2. 解析 SetSummary Frame
3. 实时更新当前组 rep 计数
4. 组结束时（rep 停止 30s）锁定本组数据
5. 用户点击"上传"或连接 WiFi 网关时同步云端

---

## 6. LSM6DSO 驱动要点

### 6.1 寄存器配置

```c
// src/imu/lsm6dso.c

#define LSM6DSO_I2C_ADDR   0x6A  // AD 引脚接地

// 初始化序列
static int lsm6dso_init(void) {
    // 0x10: CTRL1_XL (加速度配置)
    // ODR_XL = 0b0101 (50Hz), FS_XL = 0b01 (±2g, 适合抗阻训练)
    i2c_reg_write(0x10, 0x51);  // 50Hz, ±2g

    // 0x11: CTRL2_G (陀螺仪配置)
    // ODR_G = 0b0101 (50Hz), FS_G = 0b00 (±245dps)
    i2c_reg_write(0x11, 0x50);  // 50Hz, ±245°/s

    // 0x13: CTRL3_C (主配置)
    // SW_RESET=1 (软件复位), IF_INJ=1 (突发读取)
    i2c_reg_write(0x13, 0x04);

    // 0x0A: STATUS_REG (数据就绪位)
    // 轮询或配置 INT1 触发

    return 0;
}
```

### 6.2 数据读取

```c
// 读取加速度 + 温度（I2C 突发读取，13 字节）
// 0x80 | 0x0D = 自动递增寄存器地址
static int lsm6dso_read_accel(float *ax, float *ay, float *az) {
    uint8_t buf[13];
    i2c_reg_read_burst(0x0D, buf, 13);  // OUTX_L_XL → OUTZ_H_XL + 1/T

    // 加速度：2's complement, ±2g 量程，16位，0.061mg/LSB
    int16_t raw_ax = (int16_t)(buf[0] | (buf[1] << 8));
    int16_t raw_ay = (int16_t)(buf[2] | (buf[3] << 8));
    int16_t raw_az = (int16_t)(buf[4] | (buf[5] << 8));

    *ax = raw_ax * 0.061f / 1000.0f;  // → g
    *ay = raw_ay * 0.061f / 1000.0f;
    *az = raw_az * 0.061f / 1000.0f;
    return 0;
}
```

---

## 7. 功耗预算

### 7.1 电流预算（CR2032 模式）

| 状态 | 电流 | 时间占比 | 平均电流 |
|------|------|---------|---------|
| System OFF (静息) | 0.3µA | ~95% | 0.285µA |
| ACTIVE (50Hz采样) | 3mA | ~4.9% | 147µA |
| BLE 广播 (200ms间隔) | 8mA 峰值 | ~1% | 80µA |
| **合计** | | | **~230µA 平均** |

**续航估算：**
- CR2032 容量：220mAh（标准）
- 220mAh / 0.23mA ≈ 956 小时 ≈ **40 天**（理想状态，保守估算 20-30 天）
- 实际使用：每天训练 30 分钟 → 平均电流更低 → **可超过 12 个月**

> 注：Board v1.1 声称 12-18 个月，属于乐观估计，需要用大容量锂电池（CR2477, 1000mAh）达到

### 7.2 框架取电模式

无续航限制，持续 50Hz 采样 + BLE 广播（间隔 100ms），平均电流 ~5-8mA，框架供电完全覆盖。

---

## 8. BOM 成本估算（千片量）

| 组件 | 型号 | 单价（¥，千片） | 说明 |
|------|------|---------------|------|
| 主控 MCU | nRF52840-QIAA | 12-16 | Nordic，BLE 5.0，Cortex-M4，1MB Flash |
| IMU 传感器 | LSM6DSO | 6-8 | ST，6轴，加速度±2/4/8/16g，I2C/SPI |
| LDO | ME6211C33 | 0.5 | 3.3V 低压差，3µA 静态电流 |
| 晶振 | 32MHz XTAL | 0.8 | nRF52840 需要 |
| PCB | 4层板，20x30mm | 3-4 | 阻抗控制，BT 外壳沉金 |
| 天线 | 陶瓷 2.4GHz | 1.5 | 或 PCB 板载天线（节省成本） |
| 连接器 | JST PH 2P（电池）| 0.3 | CR2032 电池座 |
| 外壳 | 3D 打印（尼龙）| 2-3 | IP54 密封，热缩螺丝安装孔 |
| 电阻/电容 | 被动元件 | 1.5 | 去耦电容、上拉电阻 |
| **BOM 合计** | | **27-35** | **满足 ¥25-45 目标**|

> 实际量产价格随订单量增加显著下降，万片量 BOM 可控制在 ¥20 以内。

---

## 9. 开发里程碑

### 9.1 3个月里程碑

```
Month 1 ───────────────────────────────────────────────────────────
Week 1-2: 硬件打样
  - 采购 nRF52840 DK + LSM6DSO 传感器模块
  - 搭建开发环境（Zephyr SDK + nRF Connect SDK）
  - 验证 I2C 通信，LSM6DSO 驱动调试
  - 输出：硬件原理图评审

Week 3-4: 固件核心
  - 实现 50Hz 采样循环
  - 实现合加速度计算
  - 实现基础组数计数（固定阈值）
  - BLE 广播验证（Nordic nRF Connect App 抓包）

Month 2 ───────────────────────────────────────────────────────────
Week 5-6: 算法调优
  - 动态零点校准
  - 多器械测试（史密斯机/综合训练架）
  - 准确率测试（人工计数 vs 算法计数，误差 < 5% 目标）
  - 功耗测试（CR2032 模式电流曲线）

Week 7-8: 完整固件
  - BLE 广播数据格式实现
  - 双供电切换逻辑
  - OTA 升级通道（MCUmgr）
  - 生产烧录流程（nRF Connect Programmer）

Month 3 ───────────────────────────────────────────────────────────
Week 9-10: 软硬件集成
  - 定制 PCB 回板，焊接验证
  - IP54 测试（防水溅）
  - 模组安装测试（3分钟目标）
  - 与小程序 BLE 对接验证

Week 11-12: 认证与文档
  - FCC/CE 预认证（射频功率、频段）
  - 固件架构文档 v1.0
  - 生产测试 SOP 初稿
  - 厂家试用包（10 套）准备
```

### 9.2 交付物清单

| 里程碑 | 交付物 |
|--------|--------|
| Month 1 | 硬件原理图、Zephyr 固件骨架、BLE 广播验证报告 |
| Month 2 | 完整组数计数算法、功耗测试报告、固件 v0.1 |
| Month 3 | 定制 PCB 样板、固件 v0.9、生产 SOP、试用包 |

---

## 10. 关键技术决策

### 10.1 Zephyr vs 裸机

**决策：采用 Zephyr RTOS**

理由：
1. Nordic 官方主推，社区活跃
2. BLE Controller + Host 一体化管理，降低功耗调优复杂度
3. Device PM 框架完善，支持 System OFF/ON 自动管理
4. LSM6DSO 等传感器驱动已有，无需从零写 I2C 驱动
5. MCUmgr OTA 开箱即用

### 10.2 LSM6DSO32 vs ICM-42688

**决策：Board v1.1 指定 LSM6DSO，沿用**

Board spec §2.2 明确指定 LSM6DSO，从战略一致性角度遵从。

IMU_INTEGRATION_FEASIBILITY.md §2.2 指出 ICM-42688 性能更优（噪声密度 23 vs 30 μg/√Hz），但 LSM6DSO 在 ¥6-10 的价位已足够覆盖组数计数场景（次数检测依赖峰值，不依赖噪声密度）。

若未来需要更高精度（如划船机/动感单车的速度检测），可升级为 ICM-42688（PIN 兼容，软件适配）。

### 10.3 BLE 广播 vs BLE GATT 连接

**决策：BLE 广播为主，GATT 为备选**

| 对比 | BLE 广播 | BLE GATT 连接 |
|------|---------|--------------|
| 功耗 | 较低（无连接建立开销）| 较高（建立+维护连接）|
| 手机兼容性 | 需微信小程序 BLE API | 任意 BLE App |
| 数据吞吐量 | 低（31 字节/包）| 高（MTU 协商后 KB 级）|
| 适用场景 | 组数/使用检测 | 配置下发、固件 OTA |

当前 Phase 1 需求（组数上传）只需广播，节省 30% 功耗。固件预留 GATT Service 接口，Phase 2 可开启。

---

## 11. 已知风险与缓解

| 风险 | 级别 | 缓解措施 |
|------|------|---------|
| IMU 安装位置影响计数准确率 | 高 | 制定标准安装位，提供定位标记夹具 |
| 多人同时使用（串扰） | 中 | 每个器械独立 Device ID，微信小程序按设备过滤 |
| 框架取电接触不良 | 中 | 增加接触面积导电垫片，ADC 监测电压跌落 |
| 零点半年以上漂移 | 低 | 动态校准算法，每 5 分钟自动补偿 |
| FCC/CE 认证周期长 | 中 | Month 2 末提前送检，预留缓冲时间 |

---

## 附录

### A. 参考资料

1. [Nordic nRF52840 Product Specification](https://infocenter.nordicsemi.com/)
2. [Zephyr Project Documentation](https://docs.zephyrproject.org/)
3. [ST LSM6DSO Datasheet](https://www.st.com/en/mems-and-sensors/lsm6dso.html)
4. [Nordic nRF Connect SDK](https://developer.nordicsemi.com/nRF5_SDK/)
5. [Zephyr BLE Advertising API](https://docs.zephyrproject.org/latest/hardware/peripherals/ble.html)

### B. 配套文档

- `docs/firmware-nrf52840-lsm6dso.md` — 本文档
- `docs/tech-spike-c-mini-program.md` — C端小程序预研（含 BLE 对接方案）
- `IMU_INTEGRATION_FEASIBILITY.md` — IMU 选型对比
- `Board/HealthHub_产品技术规划_v1.1.md` — 产品规格来源
- `device-integration/src/protocols/ble.adapter.ts` — 现有 BLE 适配器（扩展支持 IMU）

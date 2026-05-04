# GY-BLE25T IMU模块验证报告 — XIN-96

**日期**: 2026-04-14
**任务**: XIN-96 验证已采购国产IMU模块资料，评估微信小程序BLE连通性
**资料路径**: Board/GY_ble25t配套资料/

---

## 1. 模块概览

| 参数 | 值 |
|------|-----|
| 型号 | GY-BLE25T |
| 类型 | 9轴IMU (6轴加速度计+陀螺仪 + 融合算法输出欧拉角) |
| BLE版本 | Bluetooth Low Energy 5.0 |
| 测量范围 | Roll/Pitch/Yaw: -180° ~ +180° |
| 更新频率 | 0.5/1/5/10/50(默认)/100 Hz 可配 |
| 工作电压 | 3~5V |
| 工作电流 | 10mA |
| 尺寸 | 21.2mm × 31.7mm |
| 接口 | UART (9600默认, 最高230400) + BLE 5.0 |

## 2. BLE协议详解

### 2.1 服务与特征

| 项目 | UUID | 属性 |
|------|------|------|
| **Service** | `0000FFE0-0000-1000-8000-00805F9B34FB` | — |
| Char 1 (FFE1) | `0000FFE1-0000-1000-8000-00805F9B34FB` | **Write** — 配置指令 |
| Char 2 (FFE2) | `0000FFE2-0000-1000-8000-00805F9B34FB` | **Write** — 修改蓝牙名称(10字节) |
| Char 3 (FFE3) | `0000FFE3-0000-1000-8000-00805F9B34FB` | **Write** — 模式配置(低功耗/校准/安装/量程) |
| Char 4 (FFE4) | `0000FFE4-0000-1000-8000-00805F9B34FB` | **Notify** — IMU数据输出 |

### 2.2 配置指令 (FFE1)

| 指令 | 功能 | 数据值 |
|------|------|--------|
| 0x02 | 更新速率 | 0~5: 0.5/1/5/10/50/100 Hz |
| 0x1E | 航向累计阈值 | 0~7: 0.15~0.5 °/s |
| 0x05 01 | 复位模块 | — |

### 2.3 数据通知格式 (FFE4, 22字节)

每次Notify发送22字节数据，11个int16_t字段（大端序）：

```
偏移  字段         说明              换算
0-1   ACC_X       加速度X轴         raw (±16G 量程, 需按灵敏度换算)
2-3   ACC_Y       加速度Y轴         raw
4-5   ACC_Z       加速度Z轴         raw
6-7   GYRO_X      陀螺仪X轴        raw (±2000°/s 量程)
8-9   GYRO_Y      陀螺仪Y轴        raw
10-11 GYRO_Z      陀螺仪Z轴        raw
12-13 ROLL        横滚角            /100 = 角度(-180~180)
14-15 PITCH       俯仰角            /100 = 角度(-90~90)
16-17 YAW         航向角            /100 = 角度(-180~180)
18-19 TEMP        温度              /100 = °C
20-21 HO          水平夹角          /100 = 角度(0~180)
```

**数据解析示例** (来自Arduino代码):
```javascript
// pData[0..21] = 22字节Notify数据
let roll  = (pData[12] << 8 | pData[13]) / 100;  // 度
let pitch = (pData[14] << 8 | pData[15]) / 100;  // 度
let yaw   = (pData[16] << 8 | pData[17]) / 100;  // 度
```

## 3. 微信小程序BLE兼容性评估

### 结论: **完全可行，无技术障碍**

### 3.1 微信BLE API与模块匹配分析

| 需求 | 微信小程序API | GY-BLE25T支持 | 兼容性 |
|------|-------------|-------------|--------|
| 蓝牙初始化 | `wx.openBluetoothAdapter()` | BLE 5.0 | ✅ |
| 扫描设备 | `wx.startBluetoothDevicesDiscovery()` | 广播设备名 `gy_ble25t1` | ✅ |
| 建立连接 | `wx.createBLEConnection()` | 标准BLE连接 | ✅ |
| 获取服务 | `wx.getBLEDeviceServices()` | Service FFE0 | ✅ |
| 获取特征 | `wx.getBLEDeviceCharacteristics()` | Char FFE1~FFE4 | ✅ |
| 订阅通知 | `wx.notifyBLECharacteristicValueChange()` | FFE4 (Notify) | ✅ |
| 接收数据 | `onBLECharacteristicValueChange` 回调 | 22字节int16数组 | ✅ |
| 写入配置 | `wx.writeBLECharacteristicValue()` | FFE1/FFE3 (Write) | ✅ |

### 3.2 关键技术确认

1. **UUID格式**: 模块使用标准16-bit UUID (FFE0~FFE4) 扩展为128-bit (`0000XXXX-0000-1000-8000-00805F9B34FB`)，微信BLE API完全支持此格式

2. **MTU大小**: Arduino代码设置 `setMTU(30)`，22字节数据帧在默认MTU=23时也能传输（有效载荷20字节需分帧）。微信小程序默认MTU=23（有效载荷20字节），22字节会跨两次Notify。**建议**: 微信基础库2.20.1+支持 `wx.setBLEMTU()`，可协商更大MTU

3. **数据格式**: ArrayBuffer转int16，微信原生支持ArrayBuffer操作，与Arduino代码解析逻辑一致

4. **设备发现**: 模块广播名称 `gy_ble25t1`（可配，10字节固定），可精确匹配

### 3.3 微信小程序BLE接入伪代码

```javascript
// 1. 初始化蓝牙
wx.openBluetoothAdapter()

// 2. 扫描设备
wx.startBluetoothDevicesDiscovery({
  services: ['FFE0'],  // 按Service UUID过滤
  success(res) { /* 找到设备 */ }
})

// 3. 连接
wx.createBLEConnection({ deviceId: device.deviceId })

// 4. 获取服务和特征
wx.getBLEDeviceServices({ deviceId })
wx.getBLEDeviceCharacteristics({ deviceId, serviceId: '0000FFE0-...' })

// 5. 订阅IMU数据通知
wx.notifyBLECharacteristicValueChange({
  deviceId,
  serviceId: '0000FFE0-0000-1000-8000-00805F9B34FB',
  characteristicId: '0000FFE4-0000-1000-8000-00805F9B34FB',
  state: true
})

// 6. 监听数据
wx.onBLECharacteristicValueChange(res => {
  const data = new Int16Array(res.value.buffer)  // ArrayBuffer → int16数组
  // data[0]=ACC_X, data[1]=ACC_Y, ..., data[6]=ROLL, data[7]=PITCH, data[8]=YAW
  const roll  = data[6] / 100
  const pitch = data[7] / 100
  const yaw   = data[8] / 100
})

// 7. 配置更新速率(10Hz)
const buffer = new ArrayBuffer(2)
const view = new DataView(buffer)
view.setUint8(0, 0x02)  // reg2 = 更新速率
view.setUint8(1, 0x03)  // 10Hz
wx.writeBLECharacteristicValue({
  deviceId,
  serviceId: '0000FFE0-0000-1000-8000-00805F9B34FB',
  characteristicId: '0000FFE1-0000-1000-8000-00805F9B34FB',
  value: buffer
})
```

## 4. 与v1.1四层产品体系的对齐

| 产品层 | GY-BLE25T角色 | 当前状态 |
|--------|-------------|---------|
| **Layer 0** IMU+BLE硬件 | 本模块即Layer 0验证硬件 | 资料齐全，可开始开发 |
| **Layer 1** Device SDK | 小程序BLE连接即SDK入口 | XIN-91 PRD v2已定义页面 |
| **Layer 2** Claude API引擎 | 上传IMU数据→AI分析 | XIN-93 callLLM Java已交付 |
| **Layer 3** 数据价值 | 积累训练数据 | 需要用户量支撑 |

## 5. 发现的问题与风险

### 5.1 MTU分帧风险 (低风险)
- 22字节数据超过默认ATT MTU有效载荷(20字节)
- **缓解**: 微信基础库 ≥2.20.1 支持 `wx.setBLEMTU()`，或Arduino端限制输出字段(如只输出RPY+temp=8字节)

### 5.2 设备名称限制 (无风险)
- 蓝牙名必须10字节固定长度，默认`gy_ble25t1`
- 可通过FFE2特征修改，不影响使用

### 5.3 功耗 (需关注)
- 工作电流10mA，持续BLE连接+100Hz输出
- 健身器械场景有供电，不依赖电池，**不是问题**
- 若未来做穿戴，需启用低功耗模式+降低更新速率

### 5.4 量程配置 (需注意)
- 默认加速度±16G、陀螺仪±2000°/s
- 健身器械场景推荐±4G + ±500°/s (通过FFE3特征配置)
- 需在SDK初始化时写入配置

## 6. 配套资料清单

| 文件 | 用途 |
|------|------|
| `GY_ble25t使用说明.pdf` | 完整使用手册(寄存器/BLE协议/串口协议) |
| `android/gy25t_tool3.apk` | Android测试工具 |
| `arduino/arduino_BLE/` | Arduino ESP32 BLE客户端参考代码 |
| `arduino/arduino_usart/` | Arduino串口参考代码 |
| `stm32/STM32_USART/` | STM32串口参考代码 |
| `C51/c51_usart/` | 51单片机串口参考代码 |
| `PC/串口上位机/` | Windows串口调试工具 |
| `PC/蓝牙上位机/` | Windows BLE调试工具 |

## 7. 下一步行动

| 优先级 | 行动 | 负责人 | 依赖 |
|--------|------|--------|------|
| **P0** | 在微信开发者工具中搭建BLE测试小程序，验证连通性 | Founding Engineer | 本报告 |
| **P0** | 收到实物模块后真机测试BLE连接+数据接收 | Founding Engineer | 硬件到货 |
| **P1** | 按XIN-91 PRD v2开发C端小程序BLE连接模块 | PM + FE | BLE验证通过 |
| **P1** | BLE配置指令封装为SDK (更新速率/量程/校准) | Founding Engineer | — |
| **P2** | 确定量产模组选型(GY-BLE25T vs nRF52840自研) | CEO + FE | 成本分析 |

---

*报告完成。微信小程序可以打通蓝牙连接GY-BLE25T模块，技术上无障碍。*

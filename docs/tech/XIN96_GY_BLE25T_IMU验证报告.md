# XIN-96 GY_BLE25T IMU 模块验证报告

**创建时间**: 2026-04-14
**状态**: 完成
**负责人**: PM

---

## 任务概述

验证 GY_BLE25T IMU 模块与微信小程序的蓝牙连接可行性。

**文件夹路径**: `/media/ai-no1/workspace/Xindong_Corp/Board/GY_ble25t配套资料/`

---

## 模块技术规格

### BLE 服务与特征值

| 项目 | UUID |
|------|------|
| BLE 服务 | `0000FFE0-0000-1000-8000-00805F9B34FB` |
| 数据特征值 | `0000FFE4-0000-1000-8000-00805F9B34FB` |
| 客户端特征值描述符 | `00002902-0000-1000-8000-00805F9B34FB` |
| 设备广播名 | `gy_ble25t1` |

### 数据包格式 (22字节，10Hz)

| 字节 | 数据 |
|------|------|
| 0-1 | 加速度 AX (int16, ×1000 g) |
| 2-3 | 加速度 AY (int16) |
| 4-5 | 加速度 AZ (int16) |
| 6-7 | 角速度 GX (int16, ×1000 °/s) |
| 8-9 | 角速度 GY (int16) |
| 10-11 | 角速度 GZ (int16) |
| 12-13 | 翻滚角 Roll (int16, ×100 °) |
| 14-15 | 俯仰角 Pitch (int16, ×100 °) |
| 16-17 | 偏航角 Yaw (int16, ×100 °) |
| 18-19 | 温度 Temp (int16, ×100 °C) |
| 20-21 | 磁场 Ho (int16) |

### 支持平台

- Arduino (完整 BLE client 示例)
- STM32 (USART 接口)
- C51 (USART 接口)
- Android (APK: `gy25t_tool3.apk`)
- PC 串口/蓝牙上位机

---

## 微信小程序集成可行性分析

### 当前 device-integration 状态

现有 `BLEAdapter` (`device-integration/src/protocols/ble.adapter.ts`) **不支持** GY_BLE25T 的自定义 UUID，不适用于 IMU 设备。

### 建议方案

需要在 device-integration 中新增 `IMUAdapter`，或扩展现有 BLEAdapter 以支持：

1. **新增适配器**: `src/protocols/imu.adapter.ts`，专门处理 GY_BLE25T 的 22 字节数据包格式
2. **UUID 配置化**: 将 GY_BLE25T 的服务/特征值 UUID 加入配置白名单
3. **数据解析**: 按照 int16 格式解析加速度、角速度、姿态角

### 微信 BLE 限制

微信小程序 BLE API (`wx.createBLEConnection` 等) 存在：
- iOS 上需使用 CoreBluetooth 私有 API（企业账号）
- Android 兼容性良好
- 建议使用 `wx.onBLECharacteristicValueChange` 监听 10Hz 数据通知

---

## 关键文件清单

| 文件 | 说明 |
|------|------|
| `GY_ble25t使用说明.pdf` | 官方说明文档 |
| `arduino/arduino_BLE/BLE_client/gy_ble25t_client.ino` | Arduino BLE Client 示例（完整可用） |
| `android/gy25t_tool3.apk` | Android 配置工具 |
| `stm32/STM32_USART/` | STM32 参考代码 |
| `PC/蓝牙上位机/` | PC 端调试工具 |

---

## 结论

GY_BLE25T 可通过扩展 device-integration 的 BLE 适配器接入微信小程序。核心工作：
1. 在 device-integration 中新增 IMU/运动传感器适配器
2. 配置 GY_BLE25T 的服务 UUID (`0000FFE0`)
3. 实现 22 字节数据包解析（加速度、角速度、姿态角）
4. 建议配合 Arduino 示例验证协议栈

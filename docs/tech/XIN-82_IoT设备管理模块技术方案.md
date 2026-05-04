# XIN-82: RuoYi IoT 设备管理模块

> 任务 ID: XIN-82  
> 负责人: Founding Engineer  
> 状态: in_progress  
> 创建日期: 2026-04-12  
> 目标: 用 Spring Boot + MyBatis 替换 Node.js device-gateway，实现设备管理、厂商管理、MQTT 数据接入

---

## 1. 背景与目标

Board v1.1 确定 HealthHub 四层产品体系，第零层（IMU 硬件）和第一层（设备数字化 SDK）需要统一的设备管理后端。现有的 `device-integration`（Node.js/TypeScript）运行独立进程，不便于纳入 RuoYi 的统一权限、日志和 UI 体系。

**目标**: 在 RuoYi 后端（Spring Boot）内新增 `ruoyi-iot` 模块，提供：

- 设备注册、查询、管理（BLE/MQTT 协议）
- 厂商（Manufacturer）管理
- 设备实时状态（在线/离线/告警）
- 与现有 device-integration 的数据 Schema 对齐

**非目标**: 不实现 MQTT Broker（用外部 Mosquitto），不复制 device-integration 的协议解析逻辑（那部分留在 Node.js 层）。

---

## 2. 技术架构

### 2.1 模块结构

```
RuoYi-Backend/
  ruoyi-iot/                    # 新增 Maven 模块
    pom.xml
    src/main/java/com/ruoyi/iot/
      domain/
        entity/
          IoTDevice.java         # 设备实体
          Manufacturer.java      # 厂商实体
        dto/
          IoTDeviceDTO.java      # 设备数据传输对象
      mapper/
        IoTDeviceMapper.java
        ManufacturerMapper.java
      service/
        IIoTDeviceService.java
        IoTDeviceServiceImpl.java
        IManufacturerService.java
        ManufacturerServiceImpl.java
      controller/
        IoTDeviceController.java
        ManufacturerController.java
    src/main/resources/
      mapper/iot/
        IoTDeviceMapper.xml
        ManufacturerMapper.xml
      sql/
        iot_module.sql            # 数据库迁移脚本
```

### 2.2 依赖关系

```
ruoyi-iot  →  ruoyi-common（基础实体、JSON序列化）
ruoyi-iot  →  Spring Boot Starter（JDBC、Web）
ruoyi-iot  →  MyBatis（数据访问）
ruoyi-iot  →  MQTT Client（设备数据订阅）
ruoyi-admin  →  ruoyi-iot（Web 层暴露）
```

---

## 3. 数据模型

### 3.1 IoT_DEVICE 表（健身器械设备）

| 字段 | 类型 | 说明 |
|------|------|------|
| device_id | BIGINT PK AUTO_INCREMENT | 设备主键 |
| device_code | VARCHAR(64) UNIQUE | 设备编号（MAC 或 IMEI） |
| device_name | VARCHAR(100) | 设备名称 |
| device_type | VARCHAR(50) | 设备类型（treadmill/elliptical/pilates_bed/strength_machine） |
| protocol | VARCHAR(20) | 协议（ble/mqtt/wifi） |
| manufacturer_id | BIGINT | 所属厂商 FK |
| tenant_id | BIGINT | 租户（厂商账号）FK |
| status | VARCHAR(20) | 状态（online/offline/error/maintenance） |
| firmware_version | VARCHAR(50) | 固件版本 |
| last_seen_at | DATETIME | 最后在线时间 |
| metadata | JSON | 扩展字段（设备规格、安装位置等） |
| create_by | VARCHAR(64) | 创建人 |
| create_time | DATETIME | 创建时间 |
| update_by | VARCHAR(64) | 更新人 |
| update_time | DATETIME | 更新时间 |
| del_flag | CHAR(1) | 删除标志（0=正常，1=删除） |

### 3.2 IoT_MANUFACTURER 表（健身器械厂商）

| 字段 | 类型 | 说明 |
|------|------|------|
| manufacturer_id | BIGINT PK AUTO_INCREMENT | 厂商主键 |
| manufacturer_name | VARCHAR(100) | 厂商名称 |
| contact_person | VARCHAR(64) | 联系人 |
| contact_phone | VARCHAR(20) | 联系电话 |
| address | VARCHAR(255) | 地址 |
| business_license | VARCHAR(255) | 营业执照号 |
| status | CHAR(1) | 状态（0=正常，1=停用） |
| create_by | VARCHAR(64) | 创建人 |
| create_time | DATETIME | 创建时间 |
| update_by | VARCHAR(64) | 更新人 |
| update_time | DATETIME | 更新时间 |
| del_flag | CHAR(1) | 删除标志 |

### 3.3 IoT_DEVICE_LOG 表（设备事件日志）

| 字段 | 类型 | 说明 |
|------|------|------|
| log_id | BIGINT PK AUTO_INCREMENT | 日志主键 |
| device_id | BIGINT | 设备 FK |
| event_type | VARCHAR(50) | 事件类型（online/offline/error/ota/heartbeat） |
| event_data | JSON | 事件详情 |
| create_time | DATETIME | 创建时间 |

---

## 4. API 接口

### 4.1 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iot/device/list | 分页查询设备列表 |
| GET | /iot/device/{deviceId} | 查询设备详情 |
| POST | /iot/device | 新增设备 |
| PUT | /iot/device | 修改设备 |
| DELETE | /iot/device/{deviceId} | 删除设备 |
| GET | /iot/device/{deviceId}/status | 获取设备实时状态 |
| GET | /iot/device/{deviceId}/logs | 获取设备事件日志 |
| PUT | /iot/device/{deviceId}/maintenance | 设置设备为维护状态 |

### 4.2 厂商管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /iot/manufacturer/list | 分页查询厂商列表 |
| GET | /iot/manufacturer/{manufacturerId} | 查询厂商详情 |
| POST | /iot/manufacturer | 新增厂商 |
| PUT | /iot/manufacturer | 修改厂商 |
| DELETE | /iot/manufacturer/{manufacturerId} | 删除厂商 |

---

## 5. MQTT 数据接入

MQTT Client 内嵌在 `ruoyi-iot` 模块，订阅设备上报 Topic：

- Topic 格式: `devices/{deviceCode}/telemetry`
- Payload: JSON（与 device.model.ts 的 DeviceDataSchema 对齐）
- 处理逻辑: 解析 → 验证 → 写入 timeseries 数据（暂存 device-integration 的 PostgreSQL）

**配置项**（application.yml）:

```yaml
iot:
  mqtt:
    broker-url: tcp://localhost:1883
    client-id: ruoyi-iot-{hostname}
    username: device-robot
    password: ***
    topic-prefix: devices
    qos: 1
```

---

## 6. 实现计划

| 步骤 | 内容 | 产出 |
|------|------|------|
| 1 | 创建 ruoyi-iot Maven 模块，配置 pom.xml | ruoyi-iot/pom.xml |
| 2 | 编写 SQL 迁移脚本 | iot_module.sql |
| 3 | 实现 Domain Entity（IoTDevice, Manufacturer） | Entity 类 |
| 4 | 实现 MyBatis Mapper 接口和 XML | Mapper 接口 + XML |
| 5 | 实现 Service 层 | Service 接口 + Impl |
| 6 | 实现 Controller 层 | Controller |
| 7 | 验证 Maven 编译通过 | BUILD SUCCESS |

---

## 7. 参考现有实现

- 模块结构参考: `ruoyi-system`
- 实体风格参考: `ruoyi-common/core/domain/entity/SysDept.java`
- Controller 风格参考: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysDeptController.java`
- SQL 迁移参考: `RuoYi-Backend/sql/quartz.sql`

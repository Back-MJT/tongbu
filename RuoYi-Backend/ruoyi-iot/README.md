# ruoyi-iot - 昕动智能 IoT 设备管理模块

昕动智能 HealthHub 健身器械数字底座的核心后端模块，基于 RuoYi-Vue 3.9.2 构建。

## 模块概述

本模块实现健身器械 IMU 数据采集、设备管理和数据管道，完全替换原有的 Node.js device-gateway。

## 技术栈

| 层次 | 技术 |
|------|------|
| 运行时 | Java 17, Spring Boot 4.0.3 |
| ORM | MyBatis 4.0.1 + MyBatis-Plus |
| 消息 | Eclipse Paho MQTT Client 1.2.5 |
| 数据库 | PostgreSQL 16 + TimescaleDB (时序) |
| 缓存 | Redis 7 |
| 构建 | Maven |

## 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        EMQX MQTT Broker                          │
│                   (xindong-mqtt:1883)                           │
│                                                                 │
│  device/{code}/data       ──►  ┌──────────────────────────┐     │
│  device/{code}/status     ──►  │    MqttListener          │     │
│  device/{code}/heartbeat  ──►  │  (Eclipse Paho v5)       │     │
└────────────────────────────────►│                          │     │
                                  │  1. ImuData → Redis缓存   │     │
                                  │  2. 设备在线状态更新       │     │
                                  │  3. 缓冲区批量写入PG       │     │
                                  └──────────┬───────────────┘     │
                                             │                     │
                   ┌─────────────────────────┼──────────────────┐  │
                   │                         ▼                  │  │
                   │              ┌─────────────────────┐       │  │
                   │              │ ImuDataServiceImpl  │       │  │
                   │              │  • 内存缓冲 (100条)  │       │  │
                   │              │  • 5秒定时刷新       │       │  │
                   │              │  • 关闭时强制持久化  │       │  │
                   │              └──────────┬──────────┘       │  │
                   │                         │                  │  │
                   ▼                         ▼                  ▼  │
          ┌─────────────────┐      ┌─────────────────┐            │
          │  Redis          │      │  PostgreSQL     │            │
          │  iot:imu:latest │      │  device_imu_data│            │
          │  iot:device:    │      │  (TimescaleDB   │            │
          │    status:{code}│      │   hypertable)   │            │
          └─────────────────┘      └─────────────────┘            │
                                                                 │
┌─────────────────────────────────────────────────────────────────┐
│                     REST API (RuoYi Controller)                  │
│                                                                 │
│  /iot/device/*   - 设备 CRUD + 状态管理                         │
│  /iot/manufacturer/* - 厂商管理                                  │
│  /iot/group/*   - 设备分组管理                                   │
│  /iot/log/*     - 设备事件日志                                   │
│  /iot/imu/*     - IMU 数据查询 (实时/历史/统计)                   │
│  /iot/imu/mqtt/health - MQTT连接状态                            │
└─────────────────────────────────────────────────────────────────┘
```

## 目录结构

```
ruoyi-iot/
├── pom.xml
└── src/main/
    ├── java/com/ruoyi/iot/
    │   ├── controller/
    │   │   ├── IoTDeviceController.java    # 设备管理
    │   │   ├── ImuDataController.java      # IMU数据查询
    │   │   ├── ManufacturerController.java  # 厂商管理
    │   │   ├── DeviceGroupController.java   # 设备分组
    │   │   └── DeviceLogController.java     # 设备日志
    │   ├── domain/
    │   │   ├── entity/
    │   │   │   ├── IoTDevice.java
    │   │   │   ├── ImuDataRecord.java
    │   │   │   ├── Manufacturer.java
    │   │   │   ├── DeviceGroup.java
    │   │   │   └── DeviceLog.java
    │   │   └── model/
    │   │       └── ImuData.java            # MQTT消息格式
    │   ├── mapper/
    │   │   ├── IoTDeviceMapper.java
    │   │   ├── ImuDataMapper.java
    │   │   ├── ManufacturerMapper.java
    │   │   ├── DeviceGroupMapper.java
    │   │   └── DeviceLogMapper.java
    │   ├── service/
    │   │   ├── IIoTDeviceService.java
    │   │   ├── IImuDataService.java
    │   │   ├── IManufacturerService.java
    │   │   ├── IDeviceGroupService.java
    │   │   ├── IDeviceLogService.java
    │   │   └── impl/
    │   │       ├── IoTDeviceServiceImpl.java
    │   │       ├── ImuDataServiceImpl.java   # 批量写入缓冲
    │   │       ├── ManufacturerServiceImpl.java
    │   │       ├── DeviceGroupServiceImpl.java
    │   │       └── DeviceLogServiceImpl.java
    │   └── mqtt/
    │       ├── MqttConfig.java               # MQTT配置
    │       └── MqttListener.java             # 消息监听器
    └── resources/
        ├── application-iot.yml              # 模块配置
        └── mapper/iot/
            ├── IoTDeviceMapper.xml
            ├── ImuDataMapper.xml
            ├── ManufacturerMapper.xml
            ├── DeviceGroupMapper.xml
            └── DeviceLogMapper.xml
```

## 数据库表

| 表名 | 说明 |
|------|------|
| `iot_manufacturer` | 厂商表 |
| `iot_device` | 设备表 (支持JSONB扩展字段) |
| `iot_device_group` | 设备分组表 |
| `iot_device_group_rel` | 设备-分组关联表 |
| `iot_device_log` | 设备事件日志表 |
| `device_imu_data` | IMU时序数据表 (TimescaleDB hypertable) |

## MQTT Topic 约定

| Topic | 方向 | 说明 |
|-------|------|------|
| `device/{code}/data` | 设备→服务器 | IMU数据上报 |
| `device/{code}/status` | 设备→服务器 | 状态变更 |
| `device/{code}/heartbeat` | 设备→服务器 | 心跳保活 |

## IMU 数据格式 (device/{code}/data)

```json
{
  "accelX": 0.123,
  "accelY": -0.456,
  "accelZ": 9.81,
  "gyroX": 0.01,
  "gyroY": -0.02,
  "gyroZ": 0.005,
  "sequence": 12345,
  "batteryLevel": 85,
  "motionType": "running",
  "stepCount": 1234
}
```

## 启动配置

在 `ruoyi-admin` 的 `application.yml` 中激活 iot profile：

```yaml
spring:
  profiles:
    active: druid,iot
```

或通过环境变量：

```bash
SPRING_PROFILES_ACTIVE=druid,iot
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MQTT_USERNAME` | (空) | MQTT 用户名 |
| `MQTT_PASSWORD` | (空) | MQTT 密码 |
| `DB_USERNAME` | ruoyi | PostgreSQL 用户名 |
| `DB_PASSWORD` | ruoyi123 | PostgreSQL 密码 |
| `REDIS_PASSWORD` | (空) | Redis 密码 |

## API 列表

### 设备管理 `/iot/device`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /list | 分页查询设备 |
| GET | /{deviceId} | 设备详情 |
| GET | /code/{deviceCode} | 按编号查设备 |
| POST | / | 新增设备 |
| PUT | / | 修改设备 |
| DELETE | /{deviceIds} | 删除设备 |
| PUT | /status/{deviceId} | 更新状态 |

### IMU数据 `/iot/imu`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /latest/{deviceCode} | 获取最新IMU (Redis) |
| GET | /status/{deviceCode} | 获取设备状态 (Redis) |
| GET | /mqtt/health | MQTT连接状态 |
| GET | /history/{deviceCode} | 查询最近N条历史 |
| GET | /history/{deviceCode}/range | 按时间范围查询 |
| GET | /history/{deviceCode}/stats | 数据统计 |

### 厂商管理 `/iot/manufacturer`

CRUD 接口，路径同上。

### 设备分组 `/iot/group`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /list | 分页查询分组 |
| GET | /{groupId} | 分组详情 |
| POST | / | 新增分组 |
| PUT | / | 修改分组 |
| DELETE | /{groupIds} | 删除分组 |
| POST | /device/{groupId}/{deviceId} | 添加设备到分组 |
| DELETE | /device/{groupId}/{deviceId} | 从分组移除设备 |
| GET | /devices/{groupId} | 查询分组下所有设备ID |
| GET | /device/{deviceId}/groups | 查询设备所属分组 |

### 设备日志 `/iot/log`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /list | 分页查询日志 |
| GET | /latest/{deviceId}/{limit} | 最新N条日志 |
| DELETE | /{logIds} | 删除日志 |
| DELETE | /clean/{deviceId} | 清空设备日志 |

## 高频写入优化

IMU 数据采样率可达 100Hz，瞬时写入会对 DB 造成压力。实现策略：

1. **内存缓冲**: `ConcurrentLinkedQueue<ImuDataRecord>` 无界队列
2. **批量持久化**: 缓冲达到 100 条或 5 秒超时，批量 INSERT
3. **关闭保护**: `@PreDestroy flushBuffer()` 确保不丢数据
4. **失败重试**: 写入失败数据回退队列，稍后重试

```
高频写入: 100条/秒
  └─► 内存队列缓冲
        └─► 每5秒或100条批量写入 PG
```

## 权限配置

需要在 RuoYi 菜单表中插入 IoT 模块路由：

```sql
-- 设备管理菜单 (pid=父菜单ID)
INSERT INTO sys_menu (menu_name, perms, path, ...) VALUES ('IoT设备管理', 'iot:device:list', '/iot/device', ...);

-- IMU数据菜单
INSERT INTO sys_menu (menu_name, perms, path, ...) VALUES ('IMU数据', 'iot:imu:query', '/iot/imu', ...);
```

## 相关文档

- SQL Schema: `sql/iot/iot_schema.sql`
- 父项目: `RuoYi-Vue3-master`
- 产品需求: `B2B_BACKEND_MVP_SPEC.md`

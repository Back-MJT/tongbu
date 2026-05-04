-- ===============================================================
-- Legacy IoT schema for PostgreSQL / TimescaleDB
-- 说明:
-- 1. 当前默认运行方案已经统一到若依 + 本地 MySQL + 本地 Redis
-- 2. 小程序扫码器械、BLE 连接 GY-BLE25T、训练会话和训练组明细
--    请优先使用 sql/iot/iot_equipment_mysql.sql
-- 3. 本文件仅保留为早期 TimescaleDB 版本参考
-- ===============================================================

-- IoT 厂商表
CREATE TABLE IF NOT EXISTS iot_manufacturer (
    manufacturer_id    BIGSERIAL PRIMARY KEY,
    manufacturer_name VARCHAR(100) NOT NULL UNIQUE,
    contact_person    VARCHAR(50),
    contact_phone     VARCHAR(20),
    address           VARCHAR(255),
    business_license  VARCHAR(50),
    status            CHAR(1) DEFAULT '0' COMMENT '0=正常 1=停用',
    del_flag          CHAR(1) DEFAULT '0' COMMENT '0=存在 2=删除',
    create_by         VARCHAR(64) DEFAULT '',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64) DEFAULT '',
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(500)
);
CREATE INDEX idx_manufacturer_name ON iot_manufacturer(manufacturer_name);
CREATE INDEX idx_manufacturer_del ON iot_manufacturer(del_flag);

-- IoT 设备表
CREATE TABLE IF NOT EXISTS iot_device (
    device_id         BIGSERIAL PRIMARY KEY,
    device_code       VARCHAR(64) NOT NULL UNIQUE COMMENT 'MAC或IMEI',
    device_name       VARCHAR(100),
    device_type       VARCHAR(30) COMMENT 'treadmill/elliptical/pilates_bed/strength_machine/bike',
    protocol          VARCHAR(20) DEFAULT 'mqtt' COMMENT 'ble/mqtt/wifi',
    manufacturer_id   BIGINT REFERENCES iot_manufacturer(manufacturer_id),
    tenant_id         BIGINT,
    status            VARCHAR(20) DEFAULT 'offline' COMMENT 'online/offline/error/maintenance',
    firmware_version  VARCHAR(30),
    last_seen_at      TIMESTAMP,
    metadata          JSONB DEFAULT '{}',
    del_flag          CHAR(1) DEFAULT '0' COMMENT '0=存在 2=删除',
    create_by         VARCHAR(64) DEFAULT '',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64) DEFAULT '',
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(500)
);
CREATE INDEX idx_device_code ON iot_device(device_code);
CREATE INDEX idx_device_manufacturer ON iot_device(manufacturer_id);
CREATE INDEX idx_device_tenant ON iot_device(tenant_id);
CREATE INDEX idx_device_status ON iot_device(status);
CREATE INDEX idx_device_del ON iot_device(del_flag);

-- IoT 设备事件日志表
CREATE TABLE IF NOT EXISTS iot_device_log (
    log_id            BIGSERIAL PRIMARY KEY,
    device_id         BIGINT NOT NULL,
    event_type        VARCHAR(30) COMMENT 'online/offline/error/ota/heartbeat/config',
    event_data        JSONB DEFAULT '{}',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_devicelog_device ON iot_device_log(device_id);
CREATE INDEX idx_devicelog_type ON iot_device_log(event_type);
CREATE INDEX idx_devicelog_time ON iot_device_log(create_time DESC);

-- IMU 原始数据时序表 (TimescaleDB hypertable)
CREATE TABLE IF NOT EXISTS device_imu_data (
    time              TIMESTAMPTZ NOT NULL,
    device_id         BIGINT NOT NULL,
    device_code       VARCHAR(64) NOT NULL,
    accel_x           DOUBLE PRECISION,
    accel_y           DOUBLE PRECISION,
    accel_z           DOUBLE PRECISION,
    gyro_x            DOUBLE PRECISION,
    gyro_y            DOUBLE PRECISION,
    gyro_z            DOUBLE PRECISION,
    sequence          INTEGER,
    battery_level     SMALLINT,
    motion_type       VARCHAR(20),
    step_count        INTEGER
);
SELECT create_hypertable('device_imu_data', 'time', if_not_exists => TRUE);

-- 设备分组表
CREATE TABLE IF NOT EXISTS iot_device_group (
    group_id          BIGSERIAL PRIMARY KEY,
    group_name        VARCHAR(100) NOT NULL,
    manufacturer_id   BIGINT REFERENCES iot_manufacturer(manufacturer_id),
    description       VARCHAR(255),
    del_flag          CHAR(1) DEFAULT '0',
    create_by         VARCHAR(64) DEFAULT '',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64) DEFAULT '',
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_devicegroup_manufacturer ON iot_device_group(manufacturer_id);

-- 设备-分组关联表
CREATE TABLE IF NOT EXISTS iot_device_group_rel (
    rel_id            BIGSERIAL PRIMARY KEY,
    device_id         BIGINT NOT NULL,
    group_id          BIGINT NOT NULL,
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_devgrouprel_device ON iot_device_group_rel(device_id);
CREATE INDEX idx_devgrouprel_group ON iot_device_group_rel(group_id);

-- ===============================================================
-- 权限菜单数据 (需要插入RuoYi sys_menu表)
-- 对应路由: /iot/device/*, /iot/manufacturer/*, /iot/log/*, /iot/imu/*
-- 注意: 实际部署时需根据现有菜单ID调整 parent_id
-- ===============================================================
-- IoT一级菜单 (pid=0 或 根据实际父菜单调整)
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('IoT设备管理', '', '/iot', NULL, 'M', '0', '0', '0', 'iot', 200, 'admin', NOW(), '', NULL, 'IoT设备管理目录菜单');

-- 设备管理菜单
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备管理', 'iot:device:list', '/iot/device', 'iot/device/index', 'C', '0', '0', '1', 'device', 1, 'admin', NOW(), '', NULL, 'IoT设备管理菜单');

-- 设备查询
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备查询', 'iot:device:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), '', NULL, '');

-- 设备新增
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备新增', 'iot:device:add', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), '', NULL, '');

-- 设备修改
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备修改', 'iot:device:edit', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), '', NULL, '');

-- 设备删除
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备删除', 'iot:device:remove', '', NULL, 'F', '0', '0', '1', '#', 4, 'admin', NOW(), '', NULL, '');

-- 设备导出
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备导出', 'iot:device:export', '', NULL, 'F', '0', '0', '1', '#', 5, 'admin', NOW(), '', NULL, '');

-- 厂商管理菜单
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('厂商管理', 'iot:manufacturer:list', '/iot/manufacturer', 'iot/manufacturer/index', 'C', '0', '0', '1', 'enterprise', 2, 'admin', NOW(), '', NULL, 'IoT厂商管理菜单');

-- 厂商查询
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('厂商查询', 'iot:manufacturer:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), '', NULL, '');

-- 厂商新增
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('厂商新增', 'iot:manufacturer:add', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), '', NULL, '');

-- 厂商修改
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('厂商修改', 'iot:manufacturer:edit', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), '', NULL, '');

-- 厂商删除
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('厂商删除', 'iot:manufacturer:remove', '', NULL, 'F', '0', '0', '1', '#', 4, 'admin', NOW(), '', NULL, '');

-- 设备分组菜单
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备分组', 'iot:group:list', '/iot/group', 'iot/group/index', 'C', '0', '0', '1', 'tree', 3, 'admin', NOW(), '', NULL, 'IoT设备分组菜单');

-- 设备分组查询
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('分组查询', 'iot:group:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), '', NULL, '');

-- 设备分组新增
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('分组新增', 'iot:group:add', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), '', NULL, '');

-- 设备分组修改
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('分组修改', 'iot:group:edit', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), '', NULL, '');

-- 设备分组删除
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('分组删除', 'iot:group:remove', '', NULL, 'F', '0', '0', '1', '#', 4, 'admin', NOW(), '', NULL, '');

-- IMU数据菜单
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('IMU数据', 'iot:imu:query', '/iot/imu', 'iot/imu/index', 'C', '0', '0', '1', 'chart', 4, 'admin', NOW(), '', NULL, 'IMU时序数据查询菜单');

-- IMU数据查询
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('IMU查询', 'iot:imu:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), '', NULL, '');

-- 设备日志菜单
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('设备日志', 'iot:log:list', '/iot/log', 'iot/log/index', 'C', '0', '0', '1', 'log', 5, 'admin', NOW(), '', NULL, 'IoT设备事件日志菜单');

-- 设备日志查询
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('日志查询', 'iot:log:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), '', NULL, '');

-- 设备日志删除
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('日志删除', 'iot:log:remove', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), '', NULL, '');

-- 设备日志导出
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, update_by, update_time, remark)
VALUES ('日志导出', 'iot:log:export', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), '', NULL, '');

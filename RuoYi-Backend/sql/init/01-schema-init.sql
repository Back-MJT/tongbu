-- ===============================================================
-- Legacy PostgreSQL / TimescaleDB 初始化脚本
-- 说明:
-- 1. 当前项目重构后的默认主链路为 本地 MySQL + 本地 Redis + RuoYi + ruoyi-iot
-- 2. 本文件仅保留为历史迁移参考, 不再作为默认初始化脚本
-- 3. 当前推荐使用:
--    - sql/ry_20260321.sql
--    - sql/iot/iot_equipment_mysql.sql
-- ===============================================================

-- 启用 TimescaleDB extension (hypertable 需要)
CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;

-- ===============================================================
-- RuoYi 核心表 (MySQL语法 → PostgreSQL)
-- ===============================================================

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    dept_id       BIGSERIAL PRIMARY KEY,
    parent_id     BIGINT DEFAULT 0,
    ancestors     VARCHAR(50) DEFAULT '',
    dept_name     VARCHAR(30) DEFAULT '',
    order_num     INT DEFAULT 0,
    leader        VARCHAR(20),
    phone         VARCHAR(11),
    email         VARCHAR(50),
    status        CHAR(1) DEFAULT '0',
    del_flag      CHAR(1) DEFAULT '0',
    create_by     VARCHAR(64) DEFAULT '',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64) DEFAULT '',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_sys_dept_parent ON sys_dept(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_dept_del ON sys_dept(del_flag);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id       BIGSERIAL PRIMARY KEY,
    dept_id       BIGINT,
    user_name     VARCHAR(30) NOT NULL UNIQUE,
    nick_name     VARCHAR(30) NOT NULL,
    user_type     VARCHAR(2) DEFAULT '00',
    email         VARCHAR(50) DEFAULT '',
    phonenumber   VARCHAR(11) DEFAULT '',
    sex           CHAR(1) DEFAULT '0',
    avatar        VARCHAR(100) DEFAULT '',
    password      VARCHAR(100) DEFAULT '',
    status        CHAR(1) DEFAULT '0',
    del_flag      CHAR(1) DEFAULT '0',
    login_ip      VARCHAR(128) DEFAULT '',
    login_date    TIMESTAMP,
    create_by     VARCHAR(64) DEFAULT '',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64) DEFAULT '',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark        VARCHAR(500),
    CONSTRAINT fk_user_dept FOREIGN KEY (dept_id) REFERENCES sys_dept(dept_id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_sys_user_dept ON sys_user(dept_id);
CREATE INDEX IF NOT EXISTS idx_sys_user_del ON sys_user(del_flag);
CREATE INDEX IF NOT EXISTS idx_sys_user_name ON sys_user(user_name);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id       BIGSERIAL PRIMARY KEY,
    role_name     VARCHAR(30) NOT NULL,
    role_key      VARCHAR(100) NOT NULL,
    role_sort     INT NOT NULL,
    data_scope    CHAR(1) DEFAULT '1',
    menu_check_strictly BOOLEAN DEFAULT TRUE,
    dept_check_strictly BOOLEAN DEFAULT TRUE,
    status        CHAR(1) DEFAULT '0',
    del_flag      CHAR(1) DEFAULT '0',
    create_by     VARCHAR(64) DEFAULT '',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64) DEFAULT '',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark        VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_sys_role_del ON sys_role(del_flag);
CREATE INDEX IF NOT EXISTS idx_sys_role_key ON sys_role(role_key);

-- 菜单权限表
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id       BIGSERIAL PRIMARY KEY,
    menu_name     VARCHAR(50) NOT NULL,
    parent_id     BIGINT DEFAULT 0,
    order_num     INT DEFAULT 0,
    path          VARCHAR(200) DEFAULT '',
    component     VARCHAR(255),
    menu_type     CHAR(1) DEFAULT '' COMMENT 'M=目录 C=菜单 F=按钮',
    visible       CHAR(1) DEFAULT '0',
    status        CHAR(1) DEFAULT '0',
    perms         VARCHAR(100),
    perms_type    CHAR(1) DEFAULT '1' COMMENT '1=perms 2=regex',
    icon          VARCHAR(100) DEFAULT '#',
    sort          INT DEFAULT 0,
    create_by     VARCHAR(64) DEFAULT '',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by     VARCHAR(64) DEFAULT '',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark        VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_sys_menu_parent ON sys_menu(parent_id);
CREATE INDEX IF NOT EXISTS idx_sys_menu_del ON sys_menu(del_flag);

-- 角色和菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id  BIGINT NOT NULL,
    menu_id  BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);

-- 用户和角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 角色和部门关联表
CREATE TABLE IF NOT EXISTS sys_role_dept (
    role_id  BIGINT NOT NULL,
    dept_id  BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id)
);

-- ===============================================================
-- IoT 模块表 (TimescaleDB hypertable)
-- ===============================================================

-- IoT 厂商表
CREATE TABLE IF NOT EXISTS iot_manufacturer (
    manufacturer_id    BIGSERIAL PRIMARY KEY,
    manufacturer_name VARCHAR(100) NOT NULL UNIQUE,
    contact_person    VARCHAR(50),
    contact_phone     VARCHAR(20),
    address           VARCHAR(255),
    business_license  VARCHAR(50),
    status            CHAR(1) DEFAULT '0',
    del_flag          CHAR(1) DEFAULT '0',
    create_by         VARCHAR(64) DEFAULT '',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64) DEFAULT '',
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_manufacturer_name ON iot_manufacturer(manufacturer_name);
CREATE INDEX IF NOT EXISTS idx_manufacturer_del ON iot_manufacturer(del_flag);

-- IoT 设备表
CREATE TABLE IF NOT EXISTS iot_device (
    device_id         BIGSERIAL PRIMARY KEY,
    device_code       VARCHAR(64) NOT NULL UNIQUE,
    device_name       VARCHAR(100),
    device_type       VARCHAR(30),
    protocol          VARCHAR(20) DEFAULT 'mqtt',
    manufacturer_id   BIGINT,
    tenant_id         BIGINT,
    status            VARCHAR(20) DEFAULT 'offline',
    firmware_version  VARCHAR(30),
    last_seen_at      TIMESTAMP,
    metadata          JSONB DEFAULT '{}',
    del_flag          CHAR(1) DEFAULT '0',
    create_by         VARCHAR(64) DEFAULT '',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64) DEFAULT '',
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(500)
);
CREATE INDEX IF NOT EXISTS idx_device_code ON iot_device(device_code);
CREATE INDEX IF NOT EXISTS idx_device_manufacturer ON iot_device(manufacturer_id);
CREATE INDEX IF NOT EXISTS idx_device_tenant ON iot_device(tenant_id);
CREATE INDEX IF NOT EXISTS idx_device_status ON iot_device(status);
CREATE INDEX IF NOT EXISTS idx_device_del ON iot_device(del_flag);

-- IoT 设备事件日志表
CREATE TABLE IF NOT EXISTS iot_device_log (
    log_id            BIGSERIAL PRIMARY KEY,
    device_id         BIGINT NOT NULL,
    event_type        VARCHAR(30),
    event_data        JSONB DEFAULT '{}',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_devicelog_device ON iot_device_log(device_id);
CREATE INDEX IF NOT EXISTS idx_devicelog_type ON iot_device_log(event_type);
CREATE INDEX IF NOT EXISTS idx_devicelog_time ON iot_device_log(create_time DESC);

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
    manufacturer_id   BIGINT,
    description       VARCHAR(255),
    del_flag          CHAR(1) DEFAULT '0',
    create_by         VARCHAR(64) DEFAULT '',
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by         VARCHAR(64) DEFAULT '',
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_devicegroup_manufacturer ON iot_device_group(manufacturer_id);

-- 设备-分组关联表
CREATE TABLE IF NOT EXISTS iot_device_group_rel (
    rel_id            BIGSERIAL PRIMARY KEY,
    device_id         BIGINT NOT NULL,
    group_id          BIGINT NOT NULL,
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_devgrouprel_device ON iot_device_group_rel(device_id);
CREATE INDEX IF NOT EXISTS idx_devgrouprel_group ON iot_device_group_rel(group_id);

-- ===============================================================
-- 初始化数据
-- ===============================================================

-- 默认管理员用户 (密码: admin123)
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, password, status, del_flag, create_by, create_time)
VALUES (1, 100, 'admin', '管理员', '00', 'admin@xindong.com', '15888888888', '0',
        '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE/TU0SwsoF6Ve',
        '0', '0', 'admin', NOW())
ON CONFLICT (user_id) DO NOTHING;

-- 部门数据
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
VALUES
  (100, 0, '0', '昕动智能', 0, 'CEO', '15888888888', 'hi@xindong.com', '0', '0', 'admin', NOW()),
  (101, 100, '0,100', '研发部', 1, '研发负责人', '15888888888', 'dev@xindong.com', '0', '0', 'admin', NOW())
ON CONFLICT (dept_id) DO NOTHING;

-- 超级管理员角色
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, del_flag, create_by, create_time)
VALUES (1, '超级管理员', 'admin', 1, '1', '0', '0', 'admin', NOW())
ON CONFLICT (role_id) DO NOTHING;

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1) ON CONFLICT DO NOTHING;

-- 默认IoT菜单权限
INSERT INTO sys_menu (menu_name, perms, path, component, menu_type, visible, status, perms_type, icon, sort, create_by, create_time, remark)
VALUES
  ('IoT设备管理', '', '/iot', NULL, 'M', '0', '0', '0', 'iot', 200, 'admin', NOW(), 'IoT设备管理目录菜单'),
  ('设备管理', 'iot:device:list', '/iot/device', 'iot/device/index', 'C', '0', '0', '1', 'device', 1, 'admin', NOW(), 'IoT设备管理菜单'),
  ('设备查询', 'iot:device:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), ''),
  ('设备新增', 'iot:device:add', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), ''),
  ('设备修改', 'iot:device:edit', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), ''),
  ('设备删除', 'iot:device:remove', '', NULL, 'F', '0', '0', '1', '#', 4, 'admin', NOW(), ''),
  ('设备导出', 'iot:device:export', '', NULL, 'F', '0', '0', '1', '#', 5, 'admin', NOW(), ''),
  ('厂商管理', 'iot:manufacturer:list', '/iot/manufacturer', 'iot/manufacturer/index', 'C', '0', '0', '1', 'enterprise', 2, 'admin', NOW(), 'IoT厂商管理菜单'),
  ('厂商查询', 'iot:manufacturer:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), ''),
  ('厂商新增', 'iot:manufacturer:add', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), ''),
  ('厂商修改', 'iot:manufacturer:edit', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), ''),
  ('厂商删除', 'iot:manufacturer:remove', '', NULL, 'F', '0', '0', '1', '#', 4, 'admin', NOW(), ''),
  ('设备分组', 'iot:group:list', '/iot/group', 'iot/group/index', 'C', '0', '0', '1', 'tree', 3, 'admin', NOW(), 'IoT设备分组菜单'),
  ('分组查询', 'iot:group:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), ''),
  ('分组新增', 'iot:group:add', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), ''),
  ('分组修改', 'iot:group:edit', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), ''),
  ('分组删除', 'iot:group:remove', '', NULL, 'F', '0', '0', '1', '#', 4, 'admin', NOW(), ''),
  ('IMU数据', 'iot:imu:query', '/iot/imu', 'iot/imu/index', 'C', '0', '0', '1', 'chart', 4, 'admin', NOW(), 'IMU时序数据查询菜单'),
  ('IMU查询', 'iot:imu:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), ''),
  ('设备日志', 'iot:log:list', '/iot/log', 'iot/log/index', 'C', '0', '0', '1', 'log', 5, 'admin', NOW(), 'IoT设备事件日志菜单'),
  ('日志查询', 'iot:log:query', '', NULL, 'F', '0', '0', '1', '#', 1, 'admin', NOW(), ''),
  ('日志删除', 'iot:log:remove', '', NULL, 'F', '0', '0', '1', '#', 2, 'admin', NOW(), ''),
  ('日志导出', 'iot:log:export', '', NULL, 'F', '0', '0', '1', '#', 3, 'admin', NOW(), '')
ON CONFLICT DO NOTHING;

-- 给超级管理员所有菜单权限
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE del_flag = '0'
ON CONFLICT DO NOTHING;

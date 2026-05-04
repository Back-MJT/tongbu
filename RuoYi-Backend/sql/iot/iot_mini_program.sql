-- ===============================================================
-- Legacy 小程序 SQL Schema (PostgreSQL)
-- 说明:
-- 1. 当前默认运行主链路为 MySQL 8 + Redis + RuoYi + ruoyi-iot
-- 2. 本文件仅保留为早期 PostgreSQL 方案参考
-- 3. 当前请优先使用:
--    - sql/ry_20260321.sql
--    - sql/iot/iot_equipment_mysql.sql
-- ===============================================================

-- ===============================================================
-- 设备绑定表 (用户 ↔ 设备 多对多关系)
-- ===============================================================
CREATE TABLE IF NOT EXISTS iot_device_binding (
    binding_id       BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    device_id        BIGINT NOT NULL,
    device_code      VARCHAR(64) NOT NULL,
    device_name      VARCHAR(100),
    tenant_id        BIGINT NOT NULL DEFAULT 1,
    status           VARCHAR(20) DEFAULT 'active' COMMENT 'active=已激活 unbound=已解绑',
    bound_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    unbound_at       TIMESTAMP,
    del_flag         CHAR(1) DEFAULT '0' COMMENT '0=存在 2=删除',
    create_by        VARCHAR(64) DEFAULT '',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64) DEFAULT '',
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark           VARCHAR(500)
);

COMMENT ON TABLE iot_device_binding IS '用户-设备绑定表';
COMMENT ON COLUMN iot_device_binding.user_id IS '用户ID (sys_user.user_id)';
COMMENT ON COLUMN iot_device_binding.device_id IS '设备ID (iot_device.device_id)';
COMMENT ON COLUMN iot_device_binding.device_code IS '设备编号/MAC地址';
COMMENT ON COLUMN iot_device_binding.status IS 'active=已激活 unbound=已解绑';

CREATE INDEX idx_binding_user ON iot_device_binding(user_id);
CREATE INDEX idx_binding_device ON iot_device_binding(device_id);
CREATE INDEX idx_binding_tenant ON iot_device_binding(tenant_id);
CREATE INDEX idx_binding_code ON iot_device_binding(device_code);
CREATE INDEX idx_binding_status ON iot_device_binding(status);
CREATE INDEX idx_binding_del ON iot_device_binding(del_flag);

-- ===============================================================
-- 训练会话记录表 (小程序训练完成上报)
-- ===============================================================
CREATE TABLE IF NOT EXISTS interv_session (
    session_id       BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL,
    tenant_id        BIGINT NOT NULL DEFAULT 1,
    device_id        BIGINT,
    device_code      VARCHAR(64),
    exercise_type    VARCHAR(30) COMMENT 'bench_press/lat_pulldown/seated_row等',
    completed_sets   INTEGER DEFAULT 0,
    total_reps       INTEGER DEFAULT 0,
    total_volume_kg  DECIMAL(10,2) DEFAULT 0 COMMENT '总训练量 kg',
    duration_minutes INTEGER DEFAULT 0,
    avg_heart_rate   INTEGER,
    calories_burned  INTEGER,
    session_date     DATE NOT NULL,
    session_time     TIMESTAMP NOT NULL,
    prescription_id  BIGINT COMMENT '关联 IntervPrescription',
    stage            VARCHAR(20) COMMENT 'beginner/growth/plateau/advanced',
    create_by        VARCHAR(64) DEFAULT '',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64) DEFAULT '',
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    del_flag         CHAR(1) DEFAULT '0' COMMENT '0=存在 2=删除',
    remark           VARCHAR(500)
);

COMMENT ON TABLE interv_session IS '训练会话记录表';
COMMENT ON COLUMN interv_session.exercise_type IS '运动类型 (来自AE算法exercise_type)';
COMMENT ON COLUMN interv_session.total_volume_kg IS '总训练量 = sum(重量kg × 组数 × 次数)';
COMMENT ON COLUMN interv_session.prescription_id IS '关联 IntervPrescription.prescription_id';

CREATE INDEX idx_session_user ON interv_session(user_id);
CREATE INDEX idx_session_tenant ON interv_session(tenant_id);
CREATE INDEX idx_session_date ON interv_session(session_date DESC);
CREATE INDEX idx_session_time ON interv_session(session_time DESC);
CREATE INDEX idx_session_device ON interv_session(device_code);
CREATE INDEX idx_session_prescription ON interv_session(prescription_id);
CREATE INDEX idx_session_del ON interv_session(del_flag);

-- ===============================================================
-- 租户配置表 (多租户 SaaS 配置)
-- ===============================================================
CREATE TABLE IF NOT EXISTS sys_tenant_config (
    config_id        BIGSERIAL PRIMARY KEY,
    tenant_id        BIGINT NOT NULL,
    config_key       VARCHAR(100) NOT NULL,
    config_value     VARCHAR(500),
    config_type      VARCHAR(20) DEFAULT 'string' COMMENT 'string/number/boolean/json',
    is_visible       CHAR(1) DEFAULT '1' COMMENT '1=对用户可见 0=仅管理员',
    del_flag         CHAR(1) DEFAULT '0',
    create_by        VARCHAR(64) DEFAULT '',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by        VARCHAR(64) DEFAULT '',
    update_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_id, config_key)
);

COMMENT ON TABLE sys_tenant_config IS '租户配置表';
CREATE INDEX idx_tenant_config_tenant ON sys_tenant_config(tenant_id);

-- ===============================================================
-- 示例数据 (演示用)
-- ===============================================================
-- 绑定演示账号 demo-user 到设备 HB-3412 (租户1)
INSERT INTO iot_device_binding (user_id, device_id, device_code, device_name, tenant_id, status, bound_at)
SELECT 1, 1, 'HB-3412', '智能力量站 Pro', 1, 'active', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM iot_device_binding WHERE device_code = 'HB-3412' AND del_flag = '0');

-- 演示训练记录
INSERT INTO interv_session (user_id, tenant_id, device_code, exercise_type, completed_sets, total_reps, total_volume_kg, duration_minutes, session_date, session_time)
SELECT 1, 1, 'HB-3412', 'bench_press', 4, 40, 1600.00, 45, CURRENT_DATE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM interv_session WHERE session_time > CURRENT_TIMESTAMP - INTERVAL '1 minute');

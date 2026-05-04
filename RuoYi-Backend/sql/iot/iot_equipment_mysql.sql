-- ===============================================================
-- 昕动智能器械扫码与 GY-BLE25T 接入表结构 (MySQL 8)
-- ===============================================================

CREATE TABLE IF NOT EXISTS iot_manufacturer (
  manufacturer_id    BIGINT NOT NULL AUTO_INCREMENT COMMENT '厂商ID',
  manufacturer_name  VARCHAR(100) NOT NULL COMMENT '厂商名称',
  contact_person     VARCHAR(50) DEFAULT '' COMMENT '联系人',
  contact_phone      VARCHAR(20) DEFAULT '' COMMENT '联系电话',
  address            VARCHAR(255) DEFAULT '' COMMENT '地址',
  business_license   VARCHAR(50) DEFAULT '' COMMENT '营业执照',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  status             CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  del_flag           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by          VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by          VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark             VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (manufacturer_id),
  UNIQUE KEY uk_iot_manufacturer_name (manufacturer_name),
  KEY idx_iot_manufacturer_tenant (tenant_id),
  KEY idx_iot_manufacturer_status (status),
  KEY idx_iot_manufacturer_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT厂商表';

CREATE TABLE IF NOT EXISTS iot_device (
  device_id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  device_code        VARCHAR(64) NOT NULL COMMENT '设备编号',
  device_name        VARCHAR(100) DEFAULT '' COMMENT '设备名称',
  device_type        VARCHAR(30) DEFAULT '' COMMENT '设备类型',
  protocol           VARCHAR(20) DEFAULT 'ble' COMMENT '协议 ble/mqtt/wifi',
  manufacturer_id    BIGINT DEFAULT NULL COMMENT '厂商ID',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  status             VARCHAR(20) NOT NULL DEFAULT 'offline' COMMENT '状态',
  firmware_version   VARCHAR(30) DEFAULT '' COMMENT '固件版本',
  bluetooth_name     VARCHAR(100) DEFAULT '' COMMENT '蓝牙广播名称',
  service_uuid       VARCHAR(64) DEFAULT '' COMMENT 'BLE服务UUID',
  notify_char_uuid   VARCHAR(64) DEFAULT '' COMMENT 'BLE通知特征UUID',
  last_seen_at       DATETIME DEFAULT NULL COMMENT '最后在线时间',
  metadata           JSON DEFAULT NULL COMMENT '扩展字段',
  del_flag           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by          VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by          VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark             VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (device_id),
  UNIQUE KEY uk_iot_device_code_tenant (device_code, tenant_id),
  KEY idx_iot_device_manufacturer (manufacturer_id),
  KEY idx_iot_device_tenant (tenant_id),
  KEY idx_iot_device_status (status),
  KEY idx_iot_device_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备表';

CREATE TABLE IF NOT EXISTS iot_device_log (
  log_id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  device_id          BIGINT NOT NULL COMMENT '设备ID',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  event_type         VARCHAR(30) DEFAULT '' COMMENT '事件类型',
  event_data         JSON DEFAULT NULL COMMENT '事件数据',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (log_id),
  KEY idx_iot_device_log_device (device_id),
  KEY idx_iot_device_log_tenant (tenant_id),
  KEY idx_iot_device_log_type (event_type),
  KEY idx_iot_device_log_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备事件日志表';

CREATE TABLE IF NOT EXISTS iot_device_group (
  group_id           BIGINT NOT NULL AUTO_INCREMENT COMMENT '分组ID',
  group_name         VARCHAR(100) NOT NULL COMMENT '分组名称',
  manufacturer_id    BIGINT DEFAULT NULL COMMENT '厂商ID',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  description        VARCHAR(255) DEFAULT '' COMMENT '描述',
  del_flag           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by          VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by          VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (group_id),
  KEY idx_iot_device_group_manufacturer (manufacturer_id),
  KEY idx_iot_device_group_tenant (tenant_id),
  KEY idx_iot_device_group_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备分组表';

CREATE TABLE IF NOT EXISTS iot_device_group_rel (
  rel_id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  device_id          BIGINT NOT NULL COMMENT '设备ID',
  group_id           BIGINT NOT NULL COMMENT '分组ID',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (rel_id),
  UNIQUE KEY uk_iot_device_group_rel (device_id, group_id),
  KEY idx_iot_device_group_rel_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IoT设备分组关联表';

CREATE TABLE IF NOT EXISTS interv_session (
  session_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '训练会话ID',
  user_id            BIGINT NOT NULL COMMENT '用户ID',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  device_id          BIGINT DEFAULT NULL COMMENT '设备ID',
  equipment_code     VARCHAR(64) DEFAULT '' COMMENT '器械编号',
  device_code        VARCHAR(64) DEFAULT '' COMMENT '设备编号',
  exercise_type      VARCHAR(64) DEFAULT '' COMMENT '训练类型',
  completed_sets     INT NOT NULL DEFAULT 0 COMMENT '完成组数',
  total_reps         INT NOT NULL DEFAULT 0 COMMENT '总次数',
  total_volume_kg    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '总训练量',
  duration_minutes   INT NOT NULL DEFAULT 0 COMMENT '训练时长(分钟)',
  avg_heart_rate     INT DEFAULT NULL COMMENT '平均心率',
  calories_burned    INT DEFAULT NULL COMMENT '消耗热量',
  session_date       DATE DEFAULT NULL COMMENT '训练日期',
  session_time       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '训练时间',
  prescription_id    BIGINT DEFAULT NULL COMMENT '处方ID',
  stage              VARCHAR(30) DEFAULT '' COMMENT '阶段',
  del_flag           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by          VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by          VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark             VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (session_id),
  KEY idx_interv_session_user (user_id),
  KEY idx_interv_session_tenant (tenant_id),
  KEY idx_interv_session_equipment (equipment_code),
  KEY idx_interv_session_device (device_code),
  KEY idx_interv_session_date (session_date),
  KEY idx_interv_session_time (session_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练会话记录表';

CREATE TABLE IF NOT EXISTS xd_equipment (
  equipment_id      BIGINT NOT NULL AUTO_INCREMENT COMMENT '器械ID',
  equipment_code    VARCHAR(64) NOT NULL COMMENT '器械编号',
  equipment_name    VARCHAR(100) NOT NULL COMMENT '器械名称',
  equipment_type    VARCHAR(64) NOT NULL COMMENT '器械类型',
  location          VARCHAR(255) DEFAULT '' COMMENT '安装位置',
  qr_content        VARCHAR(255) DEFAULT '' COMMENT '二维码内容',
  tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  status            CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  del_flag          CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark            VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (equipment_id),
  UNIQUE KEY uk_xd_equipment_code_tenant (equipment_code, tenant_id),
  KEY idx_xd_equipment_type (equipment_type),
  KEY idx_xd_equipment_status (status),
  KEY idx_xd_equipment_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器械主数据表';

CREATE TABLE IF NOT EXISTS xd_equipment_device (
  id                BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  equipment_id      BIGINT NOT NULL COMMENT '器械ID',
  device_id         BIGINT NOT NULL COMMENT 'IMU设备ID',
  tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  status            VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '绑定状态',
  bind_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  unbind_time       DATETIME DEFAULT NULL COMMENT '解绑时间',
  del_flag          CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark            VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_xd_equipment_device_equipment (equipment_id),
  KEY idx_xd_equipment_device_device (device_id),
  KEY idx_xd_equipment_device_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器械-IMU绑定表';

CREATE TABLE IF NOT EXISTS xd_equipment_counting_config (
  config_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  equipment_type    VARCHAR(64) NOT NULL COMMENT '器械类型',
  tenant_id         BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  main_axis         VARCHAR(16) NOT NULL DEFAULT 'pitch' COMMENT '主判断轴',
  up_threshold      DECIMAL(10,2) NOT NULL DEFAULT 20.00 COMMENT '上行阈值',
  down_threshold    DECIMAL(10,2) NOT NULL DEFAULT 5.00 COMMENT '下行阈值',
  min_interval_ms   INT NOT NULL DEFAULT 600 COMMENT '最短计数间隔',
  min_range         DECIMAL(10,2) NOT NULL DEFAULT 15.00 COMMENT '有效动作最小幅度',
  smoothing_window  INT NOT NULL DEFAULT 5 COMMENT '平滑窗口',
  status            CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态 0正常 1停用',
  del_flag          CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by         VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by         VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark            VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (config_id),
  UNIQUE KEY uk_xd_equipment_counting_type_tenant (equipment_type, tenant_id),
  KEY idx_xd_equipment_counting_status (status),
  KEY idx_xd_equipment_counting_del_flag (del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='器械计数配置表';

CREATE TABLE IF NOT EXISTS xd_training_set (
  set_id             BIGINT NOT NULL AUTO_INCREMENT COMMENT '训练组ID',
  session_id         BIGINT NOT NULL COMMENT '训练会话ID',
  user_id            BIGINT NOT NULL COMMENT '用户ID',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  equipment_code     VARCHAR(64) DEFAULT '' COMMENT '器械编号',
  device_code        VARCHAR(64) DEFAULT '' COMMENT '设备编号',
  set_no             INT NOT NULL COMMENT '组序号',
  reps               INT NOT NULL DEFAULT 0 COMMENT '本组次数',
  duration_sec       INT NOT NULL DEFAULT 0 COMMENT '本组时长(秒)',
  started_at         DATETIME DEFAULT NULL COMMENT '本组开始时间',
  ended_at           DATETIME DEFAULT NULL COMMENT '本组结束时间',
  del_flag           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by          VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by          VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (set_id),
  KEY idx_xd_training_set_session (session_id),
  KEY idx_xd_training_set_user (user_id),
  KEY idx_xd_training_set_equipment (equipment_code),
  KEY idx_xd_training_set_device (device_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='训练组明细表';

CREATE TABLE IF NOT EXISTS iot_device_binding (
  binding_id         BIGINT NOT NULL AUTO_INCREMENT COMMENT '绑定ID',
  user_id            BIGINT NOT NULL COMMENT '用户ID',
  device_id          BIGINT DEFAULT NULL COMMENT '设备ID',
  device_code        VARCHAR(64) NOT NULL COMMENT '设备编号',
  device_name        VARCHAR(100) DEFAULT '' COMMENT '设备名称',
  tenant_id          BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  status             VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '绑定状态 active/unbound',
  bound_at           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  unbound_at         DATETIME DEFAULT NULL COMMENT '解绑时间',
  del_flag           CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志 0存在 2删除',
  create_by          VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time        DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by          VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark             VARCHAR(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (binding_id),
  KEY idx_iot_device_binding_user (user_id),
  KEY idx_iot_device_binding_device (device_id),
  KEY idx_iot_device_binding_code (device_code),
  KEY idx_iot_device_binding_tenant (tenant_id),
  KEY idx_iot_device_binding_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-设备绑定表';

INSERT INTO iot_manufacturer (
  manufacturer_name, contact_person, contact_phone, tenant_id, status, create_by, remark
)
SELECT 'GY-BLE25T', 'system', '', 1, '0', 'system', 'GY-BLE25T IMU 演示厂商'
WHERE NOT EXISTS (
  SELECT 1 FROM iot_manufacturer WHERE manufacturer_name = 'GY-BLE25T'
);

INSERT INTO iot_device (
  device_code, device_name, device_type, protocol, manufacturer_id, tenant_id, status,
  firmware_version, bluetooth_name, service_uuid, notify_char_uuid, metadata, create_by, remark
)
SELECT 'HB-3412', 'GY-BLE25T 演示设备', 'imu', 'ble', m.manufacturer_id, 1, 'offline',
       '', 'gy_ble25t1', '0000FFE0-0000-1000-8000-00805F9B34FB', '0000FFE4-0000-1000-8000-00805F9B34FB',
       JSON_OBJECT('model', 'GY-BLE25T'), 'system', '扫码器械绑定演示 IMU'
FROM iot_manufacturer m
WHERE m.manufacturer_name = 'GY-BLE25T'
  AND NOT EXISTS (
    SELECT 1 FROM iot_device WHERE device_code = 'HB-3412' AND tenant_id = 1
  );

INSERT INTO xd_equipment (
  equipment_code, equipment_name, equipment_type, location, qr_content, tenant_id, status, create_by, remark
)
SELECT 'EQ-000001', '坐姿推胸训练器', 'chest_press', 'A区-01', 'xindong://equipment?code=EQ-000001', 1, '0', 'system', 'GY-BLE25T 演示器械'
WHERE NOT EXISTS (
  SELECT 1 FROM xd_equipment WHERE equipment_code = 'EQ-000001' AND tenant_id = 1
);

INSERT INTO xd_equipment_counting_config (
  equipment_type, tenant_id, main_axis, up_threshold, down_threshold, min_interval_ms, min_range, smoothing_window, status, create_by, remark
)
SELECT 'chest_press', 1, 'pitch', 20.00, 5.00, 600, 15.00, 5, '0', 'system', '推胸器械默认计数参数'
WHERE NOT EXISTS (
  SELECT 1 FROM xd_equipment_counting_config WHERE equipment_type = 'chest_press' AND tenant_id = 1
);

UPDATE iot_device
SET bluetooth_name = CASE WHEN bluetooth_name = '' OR bluetooth_name IS NULL THEN 'gy_ble25t1' ELSE bluetooth_name END,
    service_uuid = CASE WHEN service_uuid = '' OR service_uuid IS NULL THEN '0000FFE0-0000-1000-8000-00805F9B34FB' ELSE service_uuid END,
    notify_char_uuid = CASE WHEN notify_char_uuid = '' OR notify_char_uuid IS NULL THEN '0000FFE4-0000-1000-8000-00805F9B34FB' ELSE notify_char_uuid END
WHERE device_code = 'HB-3412';

INSERT INTO xd_equipment_device (
  equipment_id, device_id, tenant_id, status, create_by, remark
)
SELECT e.equipment_id, d.device_id, 1, 'active', 'system', '演示绑定'
FROM xd_equipment e
JOIN iot_device d ON d.device_code = 'HB-3412' AND d.del_flag = '0'
WHERE e.equipment_code = 'EQ-000001'
  AND NOT EXISTS (
    SELECT 1
    FROM xd_equipment_device ed
    WHERE ed.equipment_id = e.equipment_id
      AND ed.device_id = d.device_id
      AND ed.status = 'active'
      AND ed.del_flag = '0'
  );

INSERT INTO iot_device_binding (
  user_id, device_id, device_code, device_name, tenant_id, status, bound_at, create_by, remark
)
SELECT 1, d.device_id, d.device_code, COALESCE(NULLIF(d.device_name, ''), 'GY-BLE25T 演示设备'), 1, 'active', CURRENT_TIMESTAMP, 'system', '演示用户绑定'
FROM iot_device d
WHERE d.device_code = 'HB-3412'
  AND d.del_flag = '0'
  AND NOT EXISTS (
    SELECT 1
    FROM iot_device_binding b
    WHERE b.user_id = 1
      AND b.device_code = d.device_code
      AND b.tenant_id = 1
      AND b.status = 'active'
      AND b.del_flag = '0'
  );

-- =============================================================================
-- 为 xd_equipment 表添加 bluetooth_name 字段
-- 用于存储器械绑定的蓝牙设备名称
-- =============================================================================

-- 添加 bluetooth_name 字段
ALTER TABLE xd_equipment
ADD COLUMN bluetooth_name VARCHAR(100) DEFAULT NULL COMMENT '蓝牙名称';

-- 添加索引以提高查询性能
CREATE INDEX idx_xd_equipment_bluetooth ON xd_equipment(bluetooth_name);

-- 验证字段是否添加成功
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'xd_equipment'
  AND COLUMN_NAME = 'bluetooth_name';

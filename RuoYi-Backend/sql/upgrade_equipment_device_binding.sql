-- ============================================
-- 昕动智能 - 器械与传感器绑定功能优化
-- ============================================
-- 功能说明：
-- 1. 为 iot_device 表添加 MAC 地址字段
-- 2. 为 xd_equipment 表添加设备关联字段
-- 3. 实现一个器械对应一个蓝牙传感器的绑定关系
-- ============================================

-- 1. 为 iot_device 表添加 MAC 地址字段
ALTER TABLE iot_device
ADD COLUMN mac_address VARCHAR(17) NULL COMMENT '蓝牙MAC地址（格式：AA:BB:CC:DD:EE:FF）' AFTER bluetooth_name;

-- 为 MAC 地址添加唯一索引（防止重复）
ALTER TABLE iot_device
ADD UNIQUE INDEX uk_mac_address (mac_address);

-- 2. 为 xd_equipment 表添加设备关联字段
ALTER TABLE xd_equipment
ADD COLUMN device_id BIGINT NULL COMMENT '绑定的蓝牙设备ID' AFTER equipment_type,
ADD COLUMN device_code VARCHAR(64) NULL COMMENT '绑定的设备编号（冗余字段）' AFTER device_id;

-- 添加外键索引
ALTER TABLE xd_equipment
ADD INDEX idx_device_id (device_id);

-- 3. 插入示例数据（用于测试）
-- 插入一个蓝牙传感器
INSERT INTO iot_device (
  device_code,
  device_name,
  device_type,
  protocol,
  bluetooth_name,
  mac_address,
  service_uuid,
  notify_char_uuid,
  status,
  firmware_version,
  tenant_id
) VALUES (
  'HB-3412',
  'GY-BLE25T 传感器 #001',
  'imu_sensor',
  'ble',
  'gy_ble25t1',
  'AA:BB:CC:DD:EE:01',
  '0000FFE0-0000-1000-8000-00805F9B34FB',
  '0000FFE4-0000-1000-8000-00805F9B34FB',
  'online',
  'v1.2.0',
  1
) ON DUPLICATE KEY UPDATE device_name = VALUES(device_name);

-- 插入一个器械
INSERT INTO xd_equipment (
  equipment_code,
  equipment_name,
  equipment_type,
  location,
  qr_content,
  status,
  tenant_id
) VALUES (
  'EQ-000001',
  '坐姿推胸训练器',
  'chest_press',
  'A区-01',
  'EQ-000001',
  '0',
  1
) ON DUPLICATE KEY UPDATE equipment_name = VALUES(equipment_name);

-- 绑定器械和传感器
UPDATE xd_equipment e
INNER JOIN iot_device d ON d.device_code = 'HB-3412'
SET e.device_id = d.device_id,
    e.device_code = d.device_code
WHERE e.equipment_code = 'EQ-000001';

-- 4. 验证数据
SELECT
  e.equipment_code,
  e.equipment_name,
  e.device_code,
  d.bluetooth_name,
  d.mac_address,
  d.service_uuid,
  d.notify_char_uuid
FROM xd_equipment e
LEFT JOIN iot_device d ON e.device_id = d.device_id
WHERE e.equipment_code = 'EQ-000001';

-- 修复数据库编码问题

-- 1. 检查数据库字符集
SHOW VARIABLES LIKE 'character_set%';

-- 2. 检查表的字符集
SELECT TABLE_NAME, TABLE_COLLATION
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'xindong'
AND TABLE_NAME IN ('iot_device', 'iot_equipment', 'iot_equipment_sensor');

-- 3. 查看当前的设备数据
SELECT device_id, device_code, device_name, HEX(device_name) as hex_name
FROM iot_device
WHERE device_code = 'HB-3412';

-- 4. 修复设备名称的编码
UPDATE iot_device
SET device_name = 'GY-BLE25T 传感器 #001'
WHERE device_code = 'HB-3412';

-- 5. 验证修复结果
SELECT device_id, device_code, device_name
FROM iot_device
WHERE device_code = 'HB-3412';

-- 6. 如果需要，修改表的字符集为 utf8mb4
-- ALTER TABLE iot_device CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE iot_equipment CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- ALTER TABLE iot_equipment_sensor CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

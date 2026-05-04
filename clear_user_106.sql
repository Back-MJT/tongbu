-- 清空微信用户（userId: 106）的数据

-- 1. 查看用户信息
SELECT '=== 用户信息 ===' as info;
SELECT user_id, user_name, nick_name, phonenumber, create_time
FROM sys_user
WHERE user_id = 106;

-- 2. 查看训练记录数量
SELECT '=== 训练记录数量 ===' as info;
SELECT COUNT(*) as training_count
FROM interv_session
WHERE user_id = 106;

-- 3. 删除训练记录
DELETE FROM interv_session WHERE user_id = 106;

-- 4. 删除设备绑定
DELETE FROM iot_device_binding WHERE user_id = 106;

-- 5. 查看删除后的结果
SELECT '=== 删除后的训练记录数量 ===' as info;
SELECT COUNT(*) as training_count
FROM interv_session
WHERE user_id = 106;

SELECT '=== 删除后的设备绑定数量 ===' as info;
SELECT COUNT(*) as binding_count
FROM iot_device_binding
WHERE user_id = 106;

-- 6. 重置用户的统计数据（可选）
UPDATE sys_user
SET
  remark = NULL
WHERE user_id = 106;

SELECT '=== 清空完成 ===' as info;

# MySQL 初始化顺序

当前项目重构后的默认后端运行方案为:

- `RuoYi-Backend`
- 本地 `MySQL 8`
- 本地 `Redis`
- `ruoyi-iot` 集成在若依内
- 小程序负责扫码器械、连接 `GY-BLE25T`、本地计数、上传训练结果

## 推荐初始化顺序

### 1. 导入若依基础表

先执行:

- [ry_20260321.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/ry_20260321.sql)

作用:

- 初始化 `sys_user`、`sys_role`、`sys_menu`、`sys_dict_*` 等若依基础表
- 提供后台登录、权限、菜单、代码生成、监控等能力

### 2. 导入器械扫码与 IMU 扩展表

再执行:

- [iot_equipment_mysql.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/iot/iot_equipment_mysql.sql)

作用:

- 新增 `xd_equipment`
- 新增 `xd_equipment_device`
- 新增 `xd_equipment_counting_config`
- 新增 `xd_training_set`
- 新增 `iot_device_binding`
- 为 `iot_device` 增加 BLE 字段:
  - `bluetooth_name`
  - `service_uuid`
  - `notify_char_uuid`
- 为 `interv_session` 增加:
  - `equipment_code`

### 3. 按需补充演示或业务数据

如果 `iot_device` 里还没有真实 IMU 设备，需要先补设备主数据，再执行器械-IMU绑定相关 SQL。

当前 `iot_equipment_mysql.sql` 内置了一组演示数据，默认依赖设备编码:

- `HB-3412`

如果现场真实设备不是这个编码，需要把脚本里的演示 `UPDATE/INSERT` 调整成真实设备编码。

## 一键初始化脚本

如果本地已经安装 `mysql` 命令行工具，可以直接执行:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/init_local_mysql.sh
```

可选环境变量:

```bash
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DB=xindong
MYSQL_USER=root
MYSQL_PASSWORD=your_password
```

示例:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
MYSQL_USER=xindong MYSQL_PASSWORD=xindong123 bash sql/init_local_mysql.sh
```

## 当前主线使用的表

### 若依基础表

- `sys_user`
- `sys_role`
- `sys_menu`
- `sys_dict_type`
- `sys_dict_data`
- `sys_config`
- `sys_tenant` 等

### IoT / 小程序主线表

- `iot_device`
- `iot_manufacturer`
- `iot_device_log`
- `xd_equipment`
- `xd_equipment_device`
- `xd_equipment_counting_config`
- `iot_device_binding`
- `interv_session`
- `xd_training_set`

## 当前不作为默认初始化入口的脚本

以下文件保留为历史参考，不建议在当前本地 MySQL 方案中直接执行:

- [init/01-schema-init.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/init/01-schema-init.sql)
- [pg-compat.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/pg-compat.sql)
- [iot/iot_schema.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/iot/iot_schema.sql)
- [iot/iot_mini_program.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/iot/iot_mini_program.sql)
- [iot/iot_training_data.sql](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/iot/iot_training_data.sql)

原因:

- 这些脚本主要面向早期 `PostgreSQL / TimescaleDB`
- 字段、注释、索引和函数写法与当前 MySQL 主链路不一致
- 其中部分表已经被当前若依版 `interv_session + xd_training_set + xd_equipment_*` 结构替代

## 本地环境建议

- 数据库名: `xindong`
- MySQL 地址: `localhost:3306`
- Redis 地址: `localhost:6379`
- 默认 profile: `mysql,iot`

## 后续建议

当前 MySQL 主脚本已经覆盖 `iot_device_binding`，后续可以继续把租户侧的小程序配置表也统一迁到 MySQL 主链路。

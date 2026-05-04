# 本地启动与自检

当前推荐后端主链路:

- `RuoYi-Backend`
- 本地 `MySQL 8`
- 本地 `Redis`
- Profile: `mysql,iot`

## 1. 准备数据库

先初始化本地 MySQL:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
MYSQL_USER=root MYSQL_PASSWORD=your_password bash sql/init_local_mysql.sh
```

说明:

- 默认数据库名: `xindong`
- 默认地址: `127.0.0.1:3306`
- 如果你已经有业务数据，可以只参考 [sql/mysql-init-order.md](/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/mysql-init-order.md) 手动执行增量脚本

## 2. 准备 Redis

确认本地 Redis 已启动:

```bash
redis-cli -h 127.0.0.1 -p 6379 ping
```

预期返回:

```text
PONG
```

## 3. 启动若依后端

在后端目录执行:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
mvn -pl ruoyi-admin -am spring-boot:run
```

如果你希望显式指定数据库参数:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=xindong DB_USER=root DB_PASSWORD=your_password \
mvn -pl ruoyi-admin -am spring-boot:run
```

## 4. 启动成功标志

看到以下特征说明后端已正常起来:

- 端口监听在 `8080`
- 日志包含激活 profile:
  - `mysql`
  - `iot`

## 5. 接口自检

项目内已经准备了一个轻量自检脚本:

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/verify_local_backend.sh
```

它会用 demo token 依次检查:

- `/api/user/current`
- `/api/mini/equipment/resolve`
- `/api/device/my`
- `/api/training/progress/today`
- `/api/training/session`

## 6. 当前核心接口

### 扫码解析器械

```http
GET /api/mini/equipment/resolve?code=EQ-000001
Authorization: Bearer mp_demo_token_2026
```

### 获取我的设备

```http
GET /api/device/my
Authorization: Bearer mp_demo_token_2026
```

### 提交训练结果

```http
POST /api/training/session
Authorization: Bearer mp_demo_token_2026
Content-Type: application/json
```

请求体示例:

```json
{
  "equipmentCode": "EQ-000001",
  "deviceCode": "HB-3412",
  "exerciseType": "chest_press",
  "completedSets": 2,
  "totalReps": 24,
  "durationMin": 8,
  "totalVolumeKg": 0,
  "sets": [
    {
      "setNo": 1,
      "reps": 12,
      "durationSec": 40,
      "startedAt": 1777647000000,
      "endedAt": 1777647040000
    },
    {
      "setNo": 2,
      "reps": 12,
      "durationSec": 38,
      "startedAt": 1777647070000,
      "endedAt": 1777647108000
    }
  ]
}
```

## 7. 常见问题

### 1) 后端启动时报 MySQL 连接失败

优先检查:

- MySQL 是否已启动
- 数据库 `xindong` 是否存在
- `DB_HOST / DB_PORT / DB_USER / DB_PASSWORD` 是否正确

### 2) 启动时报 Redis 连接失败

检查:

- 本地 `6379` 是否可访问
- `application.yml` 中 Redis 是否被改动

### 3) 小程序扫码后提示器械未绑定 IMU

检查:

- `xd_equipment`
- `xd_equipment_device`
- `iot_device`

是否已经存在对应的器械、设备和绑定关系。

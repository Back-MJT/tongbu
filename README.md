# 昕动智能 HealthHub 当前系统 README

本文档按当前仓库代码和本地联调结果整理，重点说明前端、小程序、后端、数据链路和已知现况，便于继续开发、验收和排查问题。

## 1. 当前结论

当前主流程已经打通：

```text
微信小程序
  -> https://api.black-mjt.cn
  -> Cloudflare Tunnel
  -> 本机 RuoYi 后端 127.0.0.1:8080
  -> Docker MySQL / Redis
```

已验证链路：

- 微信登录接口可用，本地未配置 `WECHAT_SECRET` 时会走开发 openid 逻辑；真实小程序登录必须配置真实 AppSecret。
- 器械码 `EQ-000001` 可解析到器械、蓝牙设备和计数参数。
- 蓝牙训练页现有连接和计数逻辑保留，前端增加了实时数据曲线、组数、次数、时长等展示。
- 训练记录可通过 `/api/training/session` 写入数据库。
- 今日进度 `/api/training/progress/today` 和训练历史 `/api/training/history` 可读取真实训练数据。
- 后台管理前端可启动，训练记录、器械、设备等管理接口走 RuoYi 后端。
- 公网接口 `https://api.black-mjt.cn/api/mini/equipment/resolve?code=EQ-000001` 已验证可返回 `code: 200`。

本地已跑通的访问地址：

```text
后端 API: http://127.0.0.1:8080
后台管理: http://127.0.0.1:3000
公网 API:  https://api.black-mjt.cn
```

演示器械：

```text
器械编号: EQ-000001
设备编码: HB-3412
蓝牙名称: gy_ble25t1
小程序 AppID: wx71e4f887a62bccaa
```

## 2. 项目结构

| 路径 | 模块 | 当前作用 |
|---|---|---|
| `RuoYi-Backend` | Java 后端 | RuoYi / Spring Boot，提供后台管理 API、小程序 API、用户登录、器械解析、训练记录、训练计划 |
| `RuoYi-Backend/ruoyi-admin` | 后端启动模块 | 端口 `8080`，包含小程序入口 `MiniProgramController` 和 RuoYi 管理端接口 |
| `RuoYi-Backend/ruoyi-iot` | IoT 业务模块 | 器械、设备、IMU、训练记录、训练统计等管理能力 |
| `RuoYi-Backend/ruoyi-intervention` | 干预/训练计划模块 | 规则处方、训练统计、干预引擎客户端等 |
| `RuoYi-Vue3-master` | 后台管理前端 | Vue3 + Element Plus + Vite，端口 `3000` |
| `mini-program` | 微信小程序 | Taro + Vue3，登录、扫码、蓝牙连接、训练计数、训练提交、进度展示 |
| `docker-compose.local.yml` | 本地数据服务 | MySQL 8.4 + Redis 7 |
| `scripts` | 本地脚本 | 启动、停止、环境检查脚本 |
| `logs` | 本地日志 | 后端和后台管理前端运行日志 |

## 3. 后端现况

后端主入口：

```text
RuoYi-Backend/ruoyi-admin
```

核心配置：

```text
RuoYi-Backend/ruoyi-admin/src/main/resources/application.yml
RuoYi-Backend/ruoyi-admin/src/main/resources/application-mysql.yml
RuoYi-Backend/ruoyi-admin/src/main/resources/application-druid.yml
RuoYi-Backend/ruoyi-iot/src/main/resources/application-iot.yml
```

当前激活 profile：

```yaml
spring:
  profiles:
    active: mysql,iot
```

关键环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | MySQL 地址 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_NAME` | `xindong` | 数据库名 |
| `DB_USER` | `xindong` | 数据库用户 |
| `DB_PASSWORD` | `xindong123` | 数据库密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `WECHAT_APPID` | `wx71e4f887a62bccaa` | 小程序 AppID |
| `WECHAT_SECRET` | 空 | 真实微信登录必须配置 |

重要接口：

| 接口 | 方法 | 作用 |
|---|---|---|
| `/api/mini/auth/wechat-login` | `POST` | 微信登录，按 openid 查找或创建 `sys_user` |
| `/api/user/current` | `GET` | 当前登录用户信息 |
| `/api/user/training-stats` | `GET` | 当前用户训练统计 |
| `/api/mini/equipment/resolve` | `GET` | 扫码解析器械和蓝牙设备 |
| `/api/device/my` | `GET` | 我的绑定设备 |
| `/api/device/bind` | `POST` | 绑定设备 |
| `/api/device/unbind/{bindingId}` | `DELETE` | 解绑设备 |
| `/api/training/prescription` | `POST` | 生成小程序训练处方 |
| `/api/training/session` | `POST` | 提交训练记录 |
| `/api/training/progress/today` | `GET` | 今日训练进度 |
| `/api/training/history` | `GET` | 训练历史 |

### 用户 ID 逻辑

微信登录代码位于：

```text
RuoYi-Backend/ruoyi-admin/src/main/java/com/ruoyi/web/controller/MiniProgramController.java
```

当前逻辑：

```text
wx.login() 返回 code
  -> 后端调用微信 jscode2session 获取 openid
  -> 根据 openid 查找 sys_user
  -> 找不到则创建新的 sys_user
  -> 返回 userId 和 RuoYi token
```

所以小程序和训练记录里看到的 `userId` 来自 `sys_user.user_id`，不是微信 openid 本身。`user_id` 是数据库自增主键，因此会出现 `104、105、106、107、108、110` 这种不连续编号，这是正常现象，常见原因包括：

- 历史测试用户被删除或未展示；
- 自增 ID 已经分配但事务回滚；
- 不同登录方式或测试数据创建过用户；
- 表里存在系统用户、管理员用户或演示用户。

训练记录里的用户摘要目前按 `interv_session.user_id` 统计，并通过真实登录 token 解析当前用户，避免把 demo token 固定归到 `userId=1`。

### 器械 ID 逻辑

器械管理里的 `equipmentId` 来自数据库器械表的自增主键，器械业务识别优先使用稳定编码：

```text
equipmentId   数据库内部主键，可能不连续
equipmentCode 业务编码，例如 EQ-000001，扫码和小程序流程优先使用它
deviceCode    蓝牙/硬件设备编码，例如 HB-3412
```

因此后台看到器械 ID 为 `1、5、6` 不连续是正常的，说明中间可能有测试器械被删除、初始化数据不同步、或历史导入占用了 ID。小程序扫码不依赖连续 ID，而是依赖 `equipmentCode`。

### 训练记录和摘要

训练记录主要写入：

```text
interv_session
xd_training_set
```

后台训练记录列表读取：

```text
RuoYi-Backend/ruoyi-iot/src/main/resources/mapper/iot/TrainingSessionMapper.xml
```

当前摘要口径：

- `totalSessions`: `interv_session` 记录数；
- `totalSets`: `completed_sets` 汇总；
- `totalReps`: `total_reps` 汇总；
- `totalDurationMin`: `duration_minutes` 汇总；
- `latestTrainingTime`: 最新 `session_time`；
- 最近设备类型取最近一条训练记录的 `exercise_type`，不是用 `MAX(exercise_type)`。

## 4. 小程序现况

小程序路径：

```text
mini-program
```

技术栈：

```text
Taro 4 + Vue 3 + TypeScript
```

核心页面：

| 页面 | 文件 | 当前状态 |
|---|---|---|
| 登录 | `mini-program/src/pages/login/index.vue` | 微信登录和演示模式入口 |
| 首页 | `mini-program/src/pages/home/index.vue` | 今日进度、训练入口、用户状态 |
| 每日任务 | `mini-program/src/pages/daily-task/index.vue` | 训练处方展示 |
| 蓝牙训练 | `mini-program/src/pages/device-binding/index.vue` | 扫码、连接 BLE、接收 IMU、计数、提交训练 |
| 进度 | `mini-program/src/pages/progress/index.vue` | 训练记录和空状态，美化过“去开始训练”按钮位置 |
| 我的 | `mini-program/src/pages/profile/index.vue` | 用户信息和统计 |

API 配置：

```text
mini-program/src/config/env.ts
mini-program/src/services/api.ts
```

当前小程序三个环境都默认请求：

```text
https://api.black-mjt.cn
```

小程序真实模式不再默认展示假训练进度。接口失败时会抛错并提示，避免真实用户看到 demo 数据。

### 蓝牙训练页

当前蓝牙训练页保留原有流程：

```text
扫码器械
  -> 解析设备信息
  -> 搜索并连接 gy_ble25t1
  -> 监听 notify 特征值
  -> 解析 IMU 数据
  -> 本地计数 reps / sets
  -> 提交训练记录
```

前端美化新增：

- 连接成功后的实时状态面板；
- IMU 数据曲线；
- 已完成组数、次数、训练时长展示；
- 每组训练明细；
- 更统一的页面背景、卡片、按钮和空状态样式。

这部分只做展示层增强，核心蓝牙连接、数据解析、计数和提交逻辑不应随意改动。

## 5. 后台管理前端现况

后台路径：

```text
RuoYi-Vue3-master
```

技术栈：

```text
Vue 3 + Element Plus + Vite 6 + Pinia + ECharts
```

主要用途：

- 系统用户管理；
- 器械管理；
- 设备管理；
- 器械与蓝牙设备绑定关系管理；
- 训练记录列表；
- 训练统计和 IoT 相关看板。

训练记录页面相关文件：

```text
RuoYi-Vue3-master/src/views/iot/trainingSession/index.vue
RuoYi-Vue3-master/src/api/iot/trainingSession.js
```

后台管理前端启动后访问：

```text
http://127.0.0.1:3000
```

注意：后台展示的 `用户ID`、`器械ID` 是数据库主键，不要求连续。业务排查时应优先看：

```text
用户: openid / userId / nickName
器械: equipmentCode
设备: deviceCode / bluetoothName
训练: sessionId / userId / equipmentCode / deviceCode
```

## 6. 本地启动

### 6.1 启动数据服务

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
docker compose -f docker-compose.local.yml up -d
```

本地数据服务：

```text
MySQL: 127.0.0.1:3306
Redis: 127.0.0.1:6379
```

默认数据库：

```text
database: xindong
user:     xindong
password: xindong123
root:     root123456
```

### 6.2 一键启动

真实微信登录联调：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
WECHAT_SECRET=你的真实小程序AppSecret ./scripts/start_xindong_local.sh --with-admin
```

只做本地开发接口自测：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
./scripts/start_xindong_local.sh --with-admin
```

未配置 `WECHAT_SECRET` 时，后端会用 `code` 派生 `dev openid`，方便开发者工具联调；真机真实微信登录不能依赖这个模式。

停止本地环境：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
./scripts/stop_xindong_local.sh
```

日志：

```text
logs/local-backend.log
logs/local-admin.log
```

### 6.3 手动启动后端

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
mvn -pl ruoyi-admin -am -Dmaven.test.skip=true package

DB_HOST=127.0.0.1 \
DB_PORT=3306 \
DB_NAME=xindong \
DB_USER=xindong \
DB_PASSWORD=xindong123 \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6379 \
WECHAT_APPID=wx71e4f887a62bccaa \
WECHAT_SECRET=你的真实小程序AppSecret \
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

说明：`mvn package -DskipTests` 仍会编译 test sources，当前仓库测试源码存在历史依赖问题，打包运行 jar 建议使用：

```bash
mvn -pl ruoyi-admin -am -Dmaven.test.skip=true package
```

### 6.4 手动启动后台

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Vue3-master
npm run dev -- --host 127.0.0.1 --port 3000
```

### 6.5 小程序构建

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/mini-program
npm run build:weapp
```

构建产物：

```text
mini-program/dist
```

微信开发者工具导入 `mini-program`，AppID 使用：

```text
wx71e4f887a62bccaa
```

## 7. 验证命令

本地器械解析：

```bash
curl 'http://127.0.0.1:8080/api/mini/equipment/resolve?code=EQ-000001'
```

公网器械解析：

```bash
curl 'https://api.black-mjt.cn/api/mini/equipment/resolve?code=EQ-000001'
```

期望返回包含：

```json
{
  "code": 200,
  "data": {
    "equipmentCode": "EQ-000001",
    "deviceCode": "HB-3412",
    "bluetoothName": "gy_ble25t1"
  }
}
```

查看最新训练记录：

```bash
docker compose -f docker-compose.local.yml exec -T mysql \
  mysql -uroot -proot123456 --default-character-set=utf8mb4 \
  -e "SELECT session_id,user_id,equipment_code,device_code,exercise_type,completed_sets,total_reps,duration_minutes,session_date,create_time FROM xindong.interv_session ORDER BY session_id DESC LIMIT 10;"
```

检查端口：

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
lsof -nP -iTCP:3000 -sTCP:LISTEN
```

## 8. Cloudflare 和公网访问

体验版小程序请求固定域名：

```text
https://api.black-mjt.cn
```

公网链路依赖 Cloudflare Tunnel 转发到：

```text
http://localhost:8080
```

如果本地接口正常但公网接口失败，优先检查：

```bash
pgrep -f 'cloudflared tunnel run'
curl 'http://127.0.0.1:8080/api/mini/equipment/resolve?code=EQ-000001'
curl 'https://api.black-mjt.cn/api/mini/equipment/resolve?code=EQ-000001'
```

常见现象：

- `curl: (35) TLS connect error`：Cloudflare Tunnel 或公网转发异常；
- `502`：Tunnel 未连到本机后端，或后端 8080 未运行；
- 小程序提示域名不合法：微信公众平台没有配置 `request 合法域名`。

## 9. 已知限制和注意事项

- `WECHAT_SECRET` 不能写进仓库或截图，必须通过环境变量传入。
- 本地无 `WECHAT_SECRET` 时只适合开发验证，不代表真实微信登录成功。
- 外部干预引擎默认地址是 `http://localhost:4001`，当前小程序主训练处方已经由 RuoYi 后端规则生成兜底，不依赖 `4001` 才能跑通。
- 数据库自增 ID 不保证连续，后台看到用户 ID、器械 ID 不连续不是错误。
- 真机 BLE 必须在手机微信里测试，微信开发者工具模拟器不能真实连接蓝牙硬件。
- 小程序上传体验版后，如果改了合法域名或接口配置，需要重新上传/扫码体验版。

## 10. 技术验收清单

- Docker MySQL / Redis 运行正常；
- 后端 `8080` 正常监听；
- 后台管理 `3000` 正常访问；
- `https://api.black-mjt.cn/api/mini/equipment/resolve?code=EQ-000001` 返回 `code: 200`；
- 微信登录返回真实 `userId` 和 token；
- 真机扫码能识别 `EQ-000001`；
- 真机能连接 `gy_ble25t1`；
- 蓝牙训练页能显示实时数据、曲线、组数、次数；
- 提交训练后 `interv_session` 新增记录；
- 今日进度会随训练记录变化；
- 训练历史能看到当前登录用户的数据；
- 后台训练记录列表能查到对应 `sessionId`、`userId`、`equipmentCode`、`deviceCode`。

## 11. 常见问题

### 微信登录提示找不到 openid

检查：

```text
WECHAT_APPID 是否等于 wx71e4f887a62bccaa
WECHAT_SECRET 是否是真实 AppSecret
code 是否来自当前 AppID 的 wx.login()
后端是否能访问微信 jscode2session
```

### 后台用户 ID 看起来重复或奇怪

训练记录按 `user_id` 关联真实登录用户。多个记录显示同一个 `userId` 是正常的，表示同一用户完成了多次训练。不同用户 ID 不连续也是正常的数据库自增表现。

### 器械 ID 不连续

`equipmentId` 是内部自增主键，不作为扫码业务编号。扫码和小程序流程使用 `equipmentCode`，例如 `EQ-000001`。

### 公网接口不可用

先确认本地后端：

```bash
curl 'http://127.0.0.1:8080/api/mini/equipment/resolve?code=EQ-000001'
```

本地正常、公网异常时，检查 cloudflared 是否运行，以及 Cloudflare Tunnel 是否转发到 `http://localhost:8080`。

### 小程序蓝牙连不上

检查：

- 是否真机运行；
- 手机蓝牙和微信蓝牙权限是否开启；
- 传感器是否通电并广播；
- 数据库里的 `bluetoothName`、`serviceUuid`、`notifyCharUuid` 是否和真实设备一致。

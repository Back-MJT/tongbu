# 昕动智能项目交接说明

> 给接手的大模型/工程师快速理解当前项目。更新日期：2026-05-02。

## 1. 当前目标

当前主线不是旧版 Docker 全家桶，而是：

- 微信小程序：扫码器械、连接 GY-BLE25T 蓝牙 IMU、实时计数、上传训练结果。
- 若依后端：统一 API 入口，负责用户、设备、器械、训练记录。
- 管理端：若依 Vue3 后台，负责 IoT/器械/训练等后台管理页面。
- 本地基础设施：MySQL 8 + Redis 7。

本轮用户最关心的问题是：小程序连接蓝牙后能正常通讯。连接已基本可用，之前卡在通知数据/协议解析层。

## 2. 本地路径

项目根目录：

```bash
/Users/black/Desktop/try/Xindong_Platform-main
```

GY-BLE25T 厂商资料目录：

```bash
/Users/black/Desktop/GY_ble25t配套资料
```

关键厂商资料：

- `/Users/black/Desktop/GY_ble25t配套资料/GY_ble25t使用说明.pdf`
- `/Users/black/Desktop/GY_ble25t配套资料/arduino/arduino_BLE/BLE_client/gy_ble25t_client/gy_ble25t_client.ino`
- `/Users/black/Desktop/GY_ble25t配套资料/arduino/arduino_usart/arduino_usart.ino`

## 3. 项目结构

```text
Xindong_Platform-main/
  README.md
  docs/
    local_env_setup.md
    tech/
      XIN96_GY_BLE25T_IMU验证报告.md
      GY-BLE25T_IMU模块验证报告.md
      IMU器械小程序重构规划.md
  docker-compose.local.yml
  docker-compose.yml
  scripts/
    activate_dev_env.sh
  RuoYi-Backend/
    pom.xml
    docs/start-local-backend.md
    sql/
      init_local_mysql.sh
      verify_local_backend.sh
      ry_20260321.sql
      iot/iot_equipment_mysql.sql
      mysql-init-order.md
    ruoyi-admin/
    ruoyi-common/
    ruoyi-framework/
    ruoyi-generator/
    ruoyi-iot/
    ruoyi-intervention/
    ruoyi-quartz/
    ruoyi-system/
  RuoYi-Vue3-master/
    package.json
    src/
  mini-program/
    package.json
    project.config.json
    src/
      services/
        ble.ts
        api.ts
        counter.ts
      pages/device-binding/index.vue
```

说明：

- `RuoYi-Backend` 是 Java 17 + Maven + Spring Boot 若依多模块后端。
- `RuoYi-Vue3-master` 是 Vue 3 + Vite + Element Plus 管理端。
- `mini-program` 是 Taro 4 + Vue 3 微信小程序。
- 根目录 `docker-compose.yml` 是历史架构，引用了当前包中缺失的 `device-integration` / `intervention-engine` 等目录，不要把它当默认启动入口。
- 当前更推荐用 `docker-compose.local.yml` 只起 MySQL 和 Redis。

## 4. 环境要求

推荐版本：

- Node.js：20 LTS，项目要求 `>=20 <23`。
- npm：`>=10 <12`。
- Java：JDK 17。
- Maven：3.9+。
- MySQL：8.x。
- Redis：7.x。

当前机器可先执行：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
```

这个脚本会尽量切到可用的 Node 20 和 JDK 17。不要直接用系统全局 Node 25 跑依赖安装或构建。

## 5. 启动 MySQL 和 Redis

推荐使用本地 compose：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
docker compose -f docker-compose.local.yml up -d
```

默认配置：

- MySQL：`127.0.0.1:3306`
- 数据库：`xindong`
- root 密码：`root123456`
- 普通用户：`xindong`
- 普通用户密码：`xindong123`
- Redis：`127.0.0.1:6379`

检查 Redis：

```bash
redis-cli -h 127.0.0.1 -p 6379 ping
```

预期返回：

```text
PONG
```

## 6. 初始化数据库

如果是全新数据库：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DB=xindong MYSQL_USER=root MYSQL_PASSWORD=root123456 bash sql/init_local_mysql.sh
```

初始化顺序参考：

```bash
/Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend/sql/mysql-init-order.md
```

核心顺序：

1. `sql/ry_20260321.sql`：若依基础表。
2. `sql/iot/iot_equipment_mysql.sql`：器械扫码、IMU 绑定、训练记录相关表和演示数据。

注意：

- `iot_equipment_mysql.sql` 内置演示设备默认依赖 `HB-3412`。
- 如果现场 GY-BLE25T 设备编码/蓝牙名不同，需要改数据库里的设备主数据和绑定关系。
- 早期 PostgreSQL/TimescaleDB 相关脚本不要直接用于当前 MySQL 主线。

## 7. 启动后端

方式一：开发模式直接运行：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
cd RuoYi-Backend
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=xindong DB_USER=xindong DB_PASSWORD=xindong123 REDIS_HOST=127.0.0.1 REDIS_PORT=6379 \
mvn -pl ruoyi-admin -am spring-boot:run
```

方式二：先打包再运行：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
cd RuoYi-Backend
mvn -pl ruoyi-admin -am package -Dmaven.test.skip=true
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=xindong DB_USER=xindong DB_PASSWORD=xindong123 REDIS_HOST=127.0.0.1 REDIS_PORT=6379 \
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

后端成功标志：

- 监听 `8080`。
- 日志中有 `mysql`、`iot` profile。

自检：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/verify_local_backend.sh
```

核心 API：

- `GET /api/mini/equipment/resolve?code=EQ-000001`
- `GET /api/device/my`
- `POST /api/training/session`
- 小程序演示 token：`mp_demo_token_2026`

## 8. 启动管理端前端

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
cd RuoYi-Vue3-master
npm install
npm run dev
```

常见访问地址通常是：

```text
http://localhost:80
```

或以 Vite 输出为准。管理端代理/后端地址请检查：

```bash
RuoYi-Vue3-master/vite.config.js
RuoYi-Vue3-master/src/config
RuoYi-Vue3-master/src/utils/request.js
```

## 9. 启动小程序

安装依赖：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
cd mini-program
npm ci
```

构建微信小程序：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
cd mini-program
CI=1 npm run build:weapp
```

然后用微信开发者工具打开：

```text
/Users/black/Desktop/try/Xindong_Platform-main/mini-program
```

注意：

- BLE 必须真机调试，开发者工具模拟器不能验证真实蓝牙通讯。
- `mini-program/scripts/write-local-env.mjs` 会生成本地 API base，最近构建显示为 `http://192.168.1.102:8080`。如手机访问不到后端，需要确认手机和电脑同网段、防火墙、后端监听地址。
- 小程序构建已验证通过：`source ../scripts/activate_dev_env.sh >/dev/null && CI=1 npm run build:weapp`。
- 普通沙箱中直接 `npm run build:weapp` 曾触发 macOS `system-configuration` 的 Rust panic 并卡住，建议使用激活脚本后的命令。

## 10. 当前蓝牙问题和最近修改

用户反馈：小程序只能正常连接蓝牙，连接后不能正确通讯。

关键文件：

```text
mini-program/src/services/ble.ts
mini-program/src/pages/device-binding/index.vue
docs/tech/XIN96_GY_BLE25T_IMU验证报告.md
```

厂商 BLE 示例说明：

- 服务 UUID：`0000FFE0-0000-1000-8000-00805F9B34FB`
- 通知特征：`0000FFE4-0000-1000-8000-00805F9B34FB`
- BLE 示例里通知回调直接收到裸 22 字节数据：
  - AX/AY/AZ：0-5
  - GX/GY/GZ：6-11
  - Roll/Pitch/Yaw：12-17，除以 100
  - Temp：18-19，除以 100
  - Ho：20-21
- USART 示例里是 `A4 03 start len ... checksum` 帧。

本轮已改 `mini-program/src/services/ble.ts`：

- 连接前停止扫描。
- 连接后尝试设置 MTU，再延迟 500ms 获取服务。
- UUID 匹配更宽松：优先匹配配置 UUID，找不到时 fallback 到 `FFE0` 服务和具备 `notify/indicate` 的特征。
- 没有写入特征时进入 notify-only 模式，不再把缺少 `FFE3` 当成失败。
- 解析同时兼容：
  - 裸 22 字节 IMU 包。
  - `A4 03 start len ... checksum` 帧。
- 保留写入命令：`A4 03 08 1C CB`，仅当设备暴露可写特征时轮询。

本轮已改 `mini-program/src/pages/device-binding/index.vue`：

- 每次开始连接时清空 `rawNotifyCount`、`lastRawPacket`、`latestSample`，避免旧调试状态误导。

真机验证方法：

1. 后端启动。
2. 微信开发者工具导入小程序并真机预览。
3. 进入“器械训练”。
4. 使用演示器械或扫码 `EQ-000001` 对应器械。
5. 搜索并连接 `gy_ble25t1` 或现场 GY-BLE25T 设备。
6. 看页面“IMU 实时调试”：
   - `通知包` 是否增长。
   - `最后原始包` 是否出现。
   - `roll / pitch / yaw` 是否有合理变化。

判断：

- 如果 `通知包` 不增长：大概率是 notify 特征订阅失败、设备未设置 10Hz 输出、微信权限/真机系统 BLE 兼容问题。
- 如果 `通知包` 增长但姿态为空：大概率是原始包格式与当前解析不一致，需要根据页面显示的 hex 调整 `extractImuPayload`。
- 如果姿态有值但计数不准：看 `mini-program/src/services/counter.ts` 和数据库 `xd_equipment_counting_config` 的主轴/阈值。

## 11. 重要代码入口

小程序：

- `mini-program/src/services/ble.ts`：BLE 扫描、连接、订阅、解析。
- `mini-program/src/services/counter.ts`：IMU 姿态到训练次数/组数的计数逻辑。
- `mini-program/src/services/api.ts`：小程序 API 封装。
- `mini-program/src/pages/device-binding/index.vue`：扫码、搜索 BLE、连接、调试面板、训练保存。
- `mini-program/src/config/env.ts`：API base 相关。

后端：

- `RuoYi-Backend/ruoyi-admin/src/main/java/com/ruoyi/RuoYiApplication.java`：启动入口。
- `RuoYi-Backend/ruoyi-iot/src/main/java/...`：IoT 设备、器械、训练相关业务。
- `RuoYi-Backend/ruoyi-intervention/src/main/java/...`：干预/处方相关业务。
- `RuoYi-Backend/sql/iot/iot_equipment_mysql.sql`：小程序器械/IMU 绑定主线 SQL。

管理端：

- `RuoYi-Vue3-master/src/views/iot/`：IoT 管理页面。
- `ruoyi-vue-additions/src/views/iot/`：补充/迁移中的 IoT 页面源。
- `ruoyi-vue-additions/src/api/iot/`：补充 API 文件。

## 12. 当前已知问题

1. 根目录不是 git 仓库，`Xindong_Platform-main` 也不是 git 仓库。不能依赖 `git diff/status` 判断改动，需直接读文件。
2. 根 `README.md` 中的 Legacy Docker Compose 架构已经过时，当前包缺 `device-integration` 和 `intervention-engine`，完整 `docker-compose.yml` 不适合作为默认启动入口。
3. `RuoYi-Vue3-master` 当前可能没有 `package-lock.json`，首次 `npm install` 会生成锁文件。
4. 小程序 BLE 通讯只能真机验证，无法靠本地构建完全确认。
5. 当前 GY-BLE25T 是否已经通过 Android/PC 工具设置为 10Hz 输出，需要现场确认。厂商 Arduino BLE 示例注释写了“需先设置模块输出为10hz”。
6. 数据库演示绑定依赖设备编码 `HB-3412`，真实 IMU 设备需要修正设备编码、蓝牙名、服务 UUID、通知特征 UUID。
7. 小程序 API base 依赖本机局域网 IP，手机访问失败时优先检查 `mini-program/src/config/local-dev.generated.ts` 和电脑防火墙。
8. 普通 `npm run build:weapp` 在当前环境中曾卡住，建议先 `source scripts/activate_dev_env.sh`。
9. `npx eslint src/services/ble.ts ...` 直接运行时提示找不到 ESLint 配置，不能作为有效验证；以 Taro build 为主。

## 13. 建议下一个模型继续做的事

优先级从高到低：

1. 真机连接 GY-BLE25T，记录页面调试面板里的最后原始包 hex。
2. 如果通知包不增长，检查微信 `notifyBLECharacteristicValueChange` 的实际返回和设备特征 properties。
3. 如果通知包增长但解析失败，把 hex 对照厂商协议修 `mini-program/src/services/ble.ts` 的 `extractImuPayload`。
4. 如果姿态正常但计数不准，调 `mini-program/src/services/counter.ts` 和 `xd_equipment_counting_config`。
5. 确认真实设备主数据：`iot_device.bluetooth_name`、`service_uuid`、`notify_char_uuid`、器械绑定关系。
6. 后端自检通过后，再测完整链路：扫码识别器械 -> BLE 连接 -> 训练计数 -> 结束训练保存 -> 进度页/后台查看记录。

## 14. 快速命令清单

```bash
# 进入项目并激活环境
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh

# 起 MySQL + Redis
docker compose -f docker-compose.local.yml up -d

# 初始化数据库
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
MYSQL_USER=root MYSQL_PASSWORD=root123456 bash sql/init_local_mysql.sh

# 启动后端
DB_HOST=127.0.0.1 DB_PORT=3306 DB_NAME=xindong DB_USER=xindong DB_PASSWORD=xindong123 REDIS_HOST=127.0.0.1 REDIS_PORT=6379 \
mvn -pl ruoyi-admin -am spring-boot:run

# 后端自检
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/verify_local_backend.sh

# 启动管理端
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Vue3-master
npm install
npm run dev

# 构建小程序
cd /Users/black/Desktop/try/Xindong_Platform-main
source scripts/activate_dev_env.sh
cd mini-program
npm ci
CI=1 npm run build:weapp
```


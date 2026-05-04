# 昕动智能本地启动与小程序实操教程

本文基于当前精简后的目录结构，目标是把本地后端、后台管理前端、小程序跑起来，并完成一次「扫码器械 -> 连接蓝牙传感器 -> 识别组数/次数 -> 上传后台」的闭环。

## 1. 当前目录

```text
/Users/black/Desktop/try/Xindong_Platform-main
├── RuoYi-Backend          # Spring Boot 后端
├── RuoYi-Vue3-master      # 后台管理前端
├── mini-program           # 微信小程序
├── docker-compose.local.yml
└── RuoYi-Backend/sql      # MySQL 初始化脚本
```

## 2. 依赖准备

需要本机具备：

- Docker Desktop
- JDK 17
- Maven
- Node.js 20-22 更理想
- 微信开发者工具

当前机器如果是 Node 25，`npm install` 会因为 `engines` 提示不兼容。可以临时使用：

```bash
npm install --engine-strict=false
```

## 3. 启动 MySQL 和 Redis

进入项目根目录：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
docker compose -f docker-compose.local.yml up -d
```

确认容器运行：

```bash
docker compose -f docker-compose.local.yml ps
```

本地默认服务：

```text
MySQL: 127.0.0.1:3306
数据库: xindong
用户: xindong
密码: xindong123

Redis: 127.0.0.1:6379
```

## 4. 初始化数据库

如果是第一次启动，或者你想重建本地开发数据，执行：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
MYSQL_USER=root MYSQL_PASSWORD=root123456 bash sql/init_local_mysql.sh
```

然后执行后台菜单清理脚本，让管理端更像本项目后台：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
docker compose -f docker-compose.local.yml exec -T mysql \
  mysql -uroot -proot123456 --default-character-set=utf8mb4 xindong \
  < RuoYi-Backend/sql/iot/iot_admin_cleanup_mysql.sql
```

初始化后会有演示器械：

```text
器械编号: EQ-000001
默认传感器编码: HB-3412
```

如果你的真实蓝牙传感器不是这个编码，后续需要在后台「器械管理 / 设备管理」里把真实传感器和器械绑定起来。

## 5. 启动后端

推荐先构建：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
mvn -pl ruoyi-admin -am package -Dmaven.test.skip=true
```

启动后端：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
DB_HOST=127.0.0.1 \
DB_PORT=3306 \
DB_NAME=xindong \
DB_USER=xindong \
DB_PASSWORD=xindong123 \
REDIS_HOST=127.0.0.1 \
REDIS_PORT=6379 \
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

后端默认地址：

```text
http://127.0.0.1:8080
```

可以另开一个终端做接口自检：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/verify_local_backend.sh
```

看到 `/api/mini/equipment/resolve?code=EQ-000001` 和 `/api/training/session` 有返回，就说明小程序主链路接口可用。

## 6. 启动后台管理前端

另开终端：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Vue3-master
npm install --engine-strict=false
npm run dev -- --host 127.0.0.1 --port 3000
```

打开：

```text
http://127.0.0.1:3000
```

默认登录账号通常是：

```text
账号: admin
密码: admin123
```

登录后建议先看这些页面：

- 首页：查看器械总览和今日训练数据
- 器械管理：确认 `EQ-000001` 是否存在
- 设备管理：确认蓝牙传感器设备是否存在
- 训练记录：小程序上传训练后，在这里看结果

## 7. 构建并打开小程序

另开终端：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/mini-program
npm install --engine-strict=false
npm run dev:weapp
```

`npm run dev:weapp` 会先执行 `prepare:local`，自动把你电脑的局域网 IP 写入：

```text
mini-program/src/config/local-dev.ts
```

例如：

```ts
apiBase: 'http://192.168.x.x:8080'
```

如果自动识别的 IP 不对，可以手动指定：

```bash
MINI_API_HOST=你的电脑局域网IP npm run dev:weapp
```

然后打开微信开发者工具：

1. 选择「导入项目」
2. 项目目录选择：

```text
/Users/black/Desktop/try/Xindong_Platform-main/mini-program
```

3. AppID 使用 `project.config.json` 里的 AppID，或者选择测试号
4. 开发者工具里确认「不校验合法域名」已开启；当前配置里 `urlCheck: false`

## 8. 小程序实操流程

### 8.1 登录

进入小程序后，先登录。

本地开发时，后端支持开发模式的小程序登录；如果你只是先验证流程，也可以使用页面内的演示入口。

### 8.2 扫码识别器械

进入首页，点击「未绑定设备」或「开始训练」，进入「器械训练」页面。

你有两种方式：

1. 扫真实二维码  
   二维码内容建议放器械编号，例如：

```text
EQ-000001
```

或者放包含 `EQ-000001` 的字符串。

2. 开发调试  
   点击页面里的「开发调试：使用演示器械」，直接加载 `EQ-000001`。

识别成功后，页面会显示：

- 当前器械名称
- 器械编号
- 蓝牙名称
- 连接状态
- IMU 实时调试信息

### 8.3 连接蓝牙传感器

在「蓝牙连接 IMU」区域点击：

```text
搜索附近蓝牙设备
```

注意：

- 必须用真机微信测试，电脑模拟器通常不能正常连 BLE。
- 手机蓝牙要开启。
- 微信需要允许蓝牙权限。
- 传感器要通电并处于广播状态。
- 建议手机靠近传感器。

扫描列表里找到你的 GY-BLE25T 或对应蓝牙设备，点击「连接」。

连接后页面会显示：

- 通知包数量
- 最后原始包
- roll / pitch / yaw
- ax / ay / az
- 当前次数、组数、当前组次数

### 8.4 实际训练计数

连接成功后，操作器械让传感器产生姿态变化。

小程序会在本地根据 IMU 数据识别：

- reps 次数
- sets 组数
- currentSetReps 当前组次数
- setSummaries 已识别训练组

如果暂时没有真实传感器，可以点击：

```text
开发调试：模拟一组训练
```

它会模拟一次训练上传，方便先验证后端和后台页面。

### 8.5 结束训练并上传

训练结束后点击：

```text
结束训练并保存
```

小程序会调用：

```text
POST /api/training/session
```

上传内容包括：

- equipmentCode
- deviceCode
- completedSets
- totalReps
- durationMin
- sets 明细

## 9. 在后台查看上传结果

回到后台管理前端：

```text
http://127.0.0.1:3000
```

查看：

```text
训练记录
```

应该能看到刚才小程序上传的训练会话。

再回到首页，也应该能看到对应器械小卡片里的今日组数、今日次数变化。

## 10. 常见问题

### 小程序请求失败

先确认小程序里生成的 API 地址：

```bash
cat /Users/black/Desktop/try/Xindong_Platform-main/mini-program/src/config/local-dev.ts
```

手机和电脑必须在同一个 Wi-Fi 下，并且手机能访问：

```text
http://电脑局域网IP:8080
```

### 后台登录失败

检查后端是否启动，MySQL/Redis 是否运行：

```bash
docker compose -f /Users/black/Desktop/try/Xindong_Platform-main/docker-compose.local.yml ps
```

### 找不到蓝牙设备

优先检查：

- 真机运行，不要只用模拟器
- 手机蓝牙权限
- 微信蓝牙权限
- 传感器是否通电
- 传感器是否已经被其他 App 或手机连接
- 靠近设备后重新扫描

### 数据上传了但后台没显示

确认上传接口：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main/RuoYi-Backend
bash sql/verify_local_backend.sh
```

也可以直接看数据库：

```bash
cd /Users/black/Desktop/try/Xindong_Platform-main
docker compose -f docker-compose.local.yml exec -T mysql \
  mysql -uroot -proot123456 --default-character-set=utf8mb4 -e \
  "SELECT session_id,equipment_code,device_code,completed_sets,total_reps,session_time FROM xindong.interv_session ORDER BY session_id DESC LIMIT 5;"
```

## 11. 每次开发推荐启动顺序

```text
1. docker compose 启动 MySQL/Redis
2. 启动 Spring Boot 后端
3. 启动后台管理前端
4. 构建小程序并用微信开发者工具打开
5. 真机扫码/连接蓝牙/上传训练
6. 后台首页和训练记录检查数据
```

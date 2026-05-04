# 昕动智能管理系统优化说明

## 📋 已完成的优化

### 1. 品牌信息更新

**修改的文件：**
- `.env.development` - 开发环境配置
- `.env.production` - 生产环境配置
- `src/layout/components/Navbar.vue` - 导航栏组件

**修改内容：**
- ✅ 页面标题：`若依管理系统` → `昕动智能管理系统`
- ✅ 底部版权：`Copyright © 2026 昕动智能. All Rights Reserved.`
- ✅ 删除了若依相关的外部链接（源码地址、文档地址）

---

## 🔗 与小程序的数据联通

### 数据库表结构

管理系统和小程序共享同一个数据库 `xindong`，主要数据表：

#### 1. 用户相关
- `sys_user` - 用户表（包括微信用户）
  - 微信用户示例：userId = 106

#### 2. IoT 设备相关
- `iot_device` - IoT 设备表
  - 示例：HB-3412（智能力量站 Pro）
- `iot_device_binding` - 用户设备绑定表
  - 记录用户和设备的绑定关系

#### 3. 器械相关
- `iot_equipment` - 健身器械表
  - 示例：EQ-000001（坐姿推胸训练器）
- `iot_equipment_sensor` - 器械传感器绑定表
  - 记录器械和传感器的一对一绑定关系

#### 4. 训练数据相关
- `interv_session` - 训练记录表
  - 记录用户的每次训练数据
  - 包括：训练时长、组数、次数等

---

## 📱 小程序数据流

### 1. 用户登录
```
小程序 → POST /api/mini/auth/wechat-login
       → 后端创建/查找用户
       → 返回 token 和 userId
       → 小程序保存 token
```

### 2. 获取今日进度
```
小程序 → GET /api/training/progress/today (带 token)
       → 后端查询 interv_session 表
       → 返回今日训练数据
       → 小程序显示进度（3/4）
```

### 3. 扫码解析器械
```
小程序 → GET /api/mini/equipment/resolve?code=EQ-000001
       → 后端查询 iot_equipment 和 iot_equipment_sensor 表
       → 返回器械信息和绑定的传感器信息
       → 小程序显示器械详情
```

### 4. 上传训练数据
```
小程序 → POST /api/training/session
       → 后端保存到 interv_session 表
       → 更新用户统计数据
       → 返回成功
```

---

## 🖥️ 管理系统功能

### 1. 用户管理
- **路径：** 系统管理 → 用户管理
- **功能：**
  - 查看所有用户（包括微信用户）
  - 查看用户详情
  - 编辑用户信息
  - 删除用户

### 2. IoT 设备管理
- **路径：** IoT 管理 → 设备管理
- **功能：**
  - 查看所有 IoT 设备
  - 添加新设备
  - 编辑设备信息
  - 删除设备
  - 查看设备绑定关系

### 3. 器械管理
- **路径：** IoT 管理 → 器械管理
- **功能：**
  - 查看所有健身器械
  - 添加新器械
  - 编辑器械信息
  - 删除器械
  - 管理器械-传感器绑定

### 4. 训练数据管理
- **路径：** 训练管理 → 训练记录
- **功能：**
  - 查看所有训练记录
  - 筛选用户的训练记录
  - 查看训练详情
  - 删除训练记录
  - 导出训练数据

---

## 🔍 如何查看小程序用户数据

### 方法 1：通过管理系统界面

1. **登录管理系统**
   - 访问：http://127.0.0.1:3001
   - 用户名：admin
   - 密码：admin123

2. **查看微信用户**
   - 进入：系统管理 → 用户管理
   - 筛选：用户ID = 106
   - 查看用户详情

3. **查看训练记录**
   - 进入：训练管理 → 训练记录
   - 筛选：用户ID = 106
   - 查看训练详情

### 方法 2：通过 SQL 查询

```sql
-- 查看微信用户信息
SELECT user_id, user_name, nick_name, phonenumber, create_time
FROM sys_user
WHERE user_id = 106;

-- 查看训练记录
SELECT session_id, user_id, start_time, end_time, total_sets, total_reps
FROM interv_session
WHERE user_id = 106
ORDER BY start_time DESC
LIMIT 10;

-- 查看设备绑定
SELECT binding_id, user_id, device_id, device_code, status
FROM iot_device_binding
WHERE user_id = 106;
```

---

## 🗑️ 清空用户数据

### 使用 SQL 脚本

已创建脚本：`/Users/black/Desktop/try/Xindong_Platform-main/clear_user_106.sql`

**脚本功能：**
1. 查看用户信息
2. 查看训练记录数量
3. 删除所有训练记录
4. 删除所有设备绑定
5. 重置用户统计数据
6. 显示删除后的结果

**执行方法：**
```bash
# 方法 1：使用 MySQL 客户端
mysql -h 127.0.0.1 -P 3306 -u xindong -pxindong123 xindong < clear_user_106.sql

# 方法 2：通过管理系统的 SQL 控制台
# 复制脚本内容，在管理系统中执行
```

---

## 📊 数据同步说明

### 实时同步
- 小程序和管理系统共享同一个数据库
- 小程序上传的数据会立即在管理系统中显示
- 管理系统修改的数据会立即影响小程序

### 数据一致性
- 用户数据：`sys_user` 表
- 训练数据：`interv_session` 表
- 设备数据：`iot_device` 和 `iot_device_binding` 表
- 器械数据：`iot_equipment` 和 `iot_equipment_sensor` 表

---

## 🎯 下一步优化建议

### 1. 添加小程序用户专属页面
- 创建"小程序用户"菜单
- 显示所有通过小程序注册的用户
- 显示用户的训练统计数据

### 2. 添加训练数据可视化
- 用户训练趋势图
- 器械使用统计
- 训练完成率分析

### 3. 添加实时监控
- 在线用户数量
- 实时训练数据
- 设备在线状态

### 4. 添加数据导出功能
- 导出用户训练数据
- 导出设备使用报告
- 导出统计分析报告

---

## 🔧 技术栈

### 前端
- Vue 3
- Element Plus
- Vite
- Axios

### 后端
- Spring Boot
- MyBatis
- MySQL
- Redis

### 小程序
- Taro 3
- Vue 3
- TypeScript

---

## 📝 注意事项

1. **数据库连接**
   - 管理系统和小程序使用相同的数据库
   - 数据库地址：127.0.0.1:3306
   - 数据库名称：xindong
   - 用户名：xindong
   - 密码：xindong123

2. **后端服务**
   - 地址：http://192.168.1.102:8080
   - 管理系统和小程序共享同一个后端服务

3. **数据安全**
   - 删除用户数据前请确认
   - 建议定期备份数据库
   - 敏感数据请加密存储

---

## 🆘 常见问题

### Q1: 管理系统看不到小程序用户的数据？
**A:** 检查数据库连接是否正确，确认后端服务正在运行。

### Q2: 如何区分小程序用户和管理员用户？
**A:** 小程序用户的 `user_name` 通常以 `wx_` 开头，或者 `user_id` 大于 100。

### Q3: 如何清空测试数据？
**A:** 使用提供的 SQL 脚本 `clear_user_106.sql`，或者在管理系统中手动删除。

---

## 📞 联系方式

如有问题，请联系技术支持。

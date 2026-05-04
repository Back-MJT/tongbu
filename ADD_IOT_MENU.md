# 添加 IoT 管理菜单到前端管理系统

## 问题说明

前端管理系统现在没有显示 IoT 管理菜单，这是因为菜单数据还没有添加到数据库中。

## 解决方案

### 方法 1：执行 SQL 脚本（推荐）

已经准备好了 SQL 脚本：`sql/iot/iot_admin_menu_mysql.sql`

**执行步骤：**

1. **等待 MySQL 客户端安装完成**
   ```bash
   # 正在后台安装中...
   brew install mysql-client
   ```

2. **执行 SQL 脚本**
   ```bash
   # 方法 A：使用 MySQL 客户端
   /opt/homebrew/opt/mysql-client/bin/mysql \
     -h 127.0.0.1 -P 3306 \
     -u xindong -pxindong123 \
     xindong < sql/iot/iot_admin_menu_mysql.sql

   # 方法 B：如果 MySQL 在 Docker 中
   docker exec -i <mysql-container-name> \
     mysql -u xindong -pxindong123 xindong \
     < sql/iot/iot_admin_menu_mysql.sql
   ```

3. **刷新管理系统页面**
   - 在浏览器中刷新 http://127.0.0.1:3001
   - 应该可以看到"IoT管理"菜单了

---

### 方法 2：通过管理系统界面添加

如果无法执行 SQL 脚本，可以通过管理系统界面手动添加菜单：

1. **登录管理系统**
   - 访问：http://127.0.0.1:3001
   - 用户名：admin
   - 密码：admin123

2. **进入系统管理 → 菜单管理**

3. **添加 IoT 管理一级菜单**
   - 点击"新增"按钮
   - 菜单名称：IoT管理
   - 菜单类型：目录
   - 显示排序：4
   - 路由地址：iot
   - 菜单图标：monitor
   - 点击"确定"

4. **添加器械管理子菜单**
   - 选择刚创建的"IoT管理"菜单
   - 点击"新增"按钮
   - 菜单名称：器械管理
   - 菜单类型：菜单
   - 显示排序：1
   - 路由地址：equipment
   - 组件路径：iot/equipment/index
   - 权限标识：iot:equipment:list
   - 菜单图标：guide
   - 点击"确定"

5. **添加训练记录子菜单**
   - 选择"IoT管理"菜单
   - 点击"新增"按钮
   - 菜单名称：训练记录
   - 菜单类型：菜单
   - 显示排序：2
   - 路由地址：training-session
   - 组件路径：iot/trainingSession/index
   - 权限标识：iot:training:query
   - 菜单图标：date
   - 点击"确定"

6. **刷新页面**
   - 刷新浏览器
   - 应该可以看到"IoT管理"菜单了

---

## SQL 脚本内容

`sql/iot/iot_admin_menu_mysql.sql` 脚本会添加以下菜单：

### 一级菜单
- **IoT管理** (menu_id: 2000)
  - 菜单类型：目录
  - 路由地址：iot
  - 图标：monitor

### 二级菜单
1. **器械管理** (menu_id: 2001)
   - 组件路径：iot/equipment/index
   - 权限标识：iot:equipment:list
   - 功能：查看器械列表、当前绑定的 IMU 设备

2. **训练记录** (menu_id: 2002)
   - 组件路径：iot/trainingSession/index
   - 权限标识：iot:training:query
   - 功能：查看小程序上传的训练记录

### 按钮权限
- 器械查询、新增、修改、删除、导出
- 训练记录查询、导出

---

## 验证菜单是否添加成功

### 方法 1：通过管理系统界面
1. 登录管理系统
2. 进入：系统管理 → 菜单管理
3. 查找"IoT管理"菜单（menu_id: 2000）
4. 查看是否有"器械管理"和"训练记录"子菜单

### 方法 2：通过 SQL 查询
```sql
-- 查询 IoT 管理菜单
SELECT menu_id, menu_name, parent_id, path, component, perms
FROM sys_menu
WHERE menu_id >= 2000 AND menu_id < 2100
ORDER BY menu_id;
```

---

## 关于乱码问题

器械管理页面中的乱码 `GY-BLE25T â¼ æ,,Ÿâ™ #001` 是 UTF-8 编码问题。

**修复方法：**

1. **通过管理系统界面修复**
   - 进入：IoT管理 → 设备管理（如果有的话）
   - 找到设备编号 `HB-3412`
   - 修改设备名称为：`GY-BLE25T 传感器 #001`
   - 保存

2. **通过 SQL 修复**
   ```sql
   UPDATE iot_device
   SET device_name = 'GY-BLE25T 传感器 #001'
   WHERE device_code = 'HB-3412';
   ```

---

## 常见问题

### Q1: 添加菜单后看不到？
**A:** 
1. 刷新浏览器页面
2. 清除浏览器缓存
3. 重新登录管理系统

### Q2: 点击菜单后显示 404？
**A:** 
1. 检查组件路径是否正确
2. 确认前端项目中是否有对应的 Vue 组件文件
3. 检查路径：`src/views/iot/equipment/index.vue`

### Q3: 菜单显示但没有权限？
**A:** 
1. 进入：系统管理 → 角色管理
2. 编辑 admin 角色
3. 勾选"IoT管理"相关权限
4. 保存并重新登录

---

## 下一步

1. ✅ 等待 MySQL 客户端安装完成
2. ✅ 执行 SQL 脚本添加菜单
3. ✅ 刷新管理系统页面
4. ✅ 查看 IoT 管理菜单
5. ✅ 修复器械管理页面的乱码问题

---

## 联系方式

如有问题，请告诉我具体的错误信息。

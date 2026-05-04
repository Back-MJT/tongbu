-- ===============================================================
-- 昕动智能后台菜单清理 (MySQL 8)
-- 用途：隐藏若依示例/监控/工具入口，把后台入口收敛到本项目业务。
-- 可重复执行；不删除表结构和基础权限代码，避免影响登录、用户、角色、菜单等底座。
-- ===============================================================

SET NAMES utf8mb4;

-- 项目主菜单命名
UPDATE sys_menu
SET menu_name = '器械运营',
    icon = 'dashboard',
    order_num = 1,
    remark = '昕动智能器械、蓝牙传感器与训练记录管理'
WHERE menu_id = 2000;

-- 首页/看板入口
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2003, '运营首页', 2000, 0, 'dashboard', 'iot/dashboard/index', '', 'IoTDashboard',
       1, 0, 'C', '0', '0', 'iot:dashboard:query', 'dashboard',
       'system', NOW(), '', NULL, '器械使用情况和训练概览', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2003);

-- 面向当前业务保留的入口排序
UPDATE sys_menu SET order_num = 1, menu_name = '器械管理', icon = 'guide' WHERE menu_id = 2001;
UPDATE sys_menu SET order_num = 2, menu_name = '训练记录', icon = 'date' WHERE menu_id = 2002;

-- 隐藏若依自带非业务菜单：系统监控、系统工具、测试/示例类菜单。
-- 如需恢复，把 visible 改回 '0' 即可。
UPDATE sys_menu
SET visible = '1', status = '1', update_by = 'system', update_time = NOW()
WHERE path IN ('monitor', 'tool', 'demo')
   OR component LIKE 'monitor/%'
   OR component LIKE 'tool/%'
   OR perms LIKE 'monitor:%'
   OR perms LIKE 'tool:%';

-- 系统管理保留账号/角色/菜单能力，隐藏部门、岗位、通知、参数、字典等若依管理残留。
UPDATE sys_menu
SET visible = '1', status = '1', update_by = 'system', update_time = NOW()
WHERE component IN (
  'system/dept/index',
  'system/post/index',
  'system/notice/index',
  'system/config/index',
  'system/dict/index'
);

-- 授权所有已有角色访问项目首页。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 2003
FROM sys_role r
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 2003
);

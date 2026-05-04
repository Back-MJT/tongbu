-- ===============================================================
-- 昕动智能 IoT 管理端菜单与权限 (MySQL 8)
-- 可重复执行：用于把器械管理、训练记录挂入若依左侧菜单
-- ===============================================================

SET NAMES utf8mb4;

-- IoT 一级目录
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2000, 'IoT管理', 0, 4, 'iot', NULL, '', 'IoT',
       1, 0, 'M', '0', '0', '', 'monitor',
       'system', NOW(), '', NULL, 'IoT与器械训练管理目录', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2000);

-- 器械管理页面
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2001, '器械管理', 2000, 1, 'equipment', 'iot/equipment/index', '', 'IoTEquipment',
       1, 0, 'C', '0', '0', 'iot:equipment:list', 'guide',
       'system', NOW(), '', NULL, '器械主数据与当前绑定IMU', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2001);

-- 训练记录页面
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2002, '训练记录', 2000, 2, 'training-session', 'iot/trainingSession/index', '', 'IoTTrainingSession',
       1, 0, 'C', '0', '0', 'iot:training:query', 'date',
       'system', NOW(), '', NULL, '小程序上传训练会话与组明细', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2002);

-- 器械管理按钮权限
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2010, '器械查询', 2001, 1, '', NULL, '', '',
       1, 0, 'F', '0', '0', 'iot:equipment:query', '#',
       'system', NOW(), '', NULL, '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2010);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2011, '器械新增', 2001, 2, '', NULL, '', '',
       1, 0, 'F', '0', '0', 'iot:equipment:add', '#',
       'system', NOW(), '', NULL, '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2011);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2012, '器械修改', 2001, 3, '', NULL, '', '',
       1, 0, 'F', '0', '0', 'iot:equipment:edit', '#',
       'system', NOW(), '', NULL, '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2012);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2013, '器械删除', 2001, 4, '', NULL, '', '',
       1, 0, 'F', '0', '0', 'iot:equipment:remove', '#',
       'system', NOW(), '', NULL, '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2013);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2014, '器械导出', 2001, 5, '', NULL, '', '',
       1, 0, 'F', '0', '0', 'iot:equipment:export', '#',
       'system', NOW(), '', NULL, '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2014);

-- 训练记录按钮权限
INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, update_by, update_time, remark, tenant_id
)
SELECT 2020, '训练记录查询', 2002, 1, '', NULL, '', '',
       1, 0, 'F', '0', '0', 'iot:training:query', '#',
       'system', NOW(), '', NULL, '', 1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2020);

-- 给现有角色挂载菜单。超级管理员通常不依赖 sys_role_menu，
-- 这里仍补齐，方便普通角色在本地开发环境中看到入口。
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.menu_id IN (2000, 2001, 2002, 2010, 2011, 2012, 2013, 2014, 2020)
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
);

-- B Line local runtime compatibility fixes.
-- Keep this script idempotent so local environments can rerun init safely.

ALTER TABLE sys_user
  MODIFY COLUMN avatar VARCHAR(512) DEFAULT NULL COMMENT '头像地址';

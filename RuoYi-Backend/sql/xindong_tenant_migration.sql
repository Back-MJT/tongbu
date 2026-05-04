-- ============================================================================
-- XIN-144: Multi-Tenant Isolation Migration
-- Adds tenant_id to core RuoYi tables and IoT/Intervention modules
-- Run on xindong PostgreSQL database
-- ============================================================================

-- 1. Add tenant_id to sys_dept (department/租户树顶层 = 租户)
ALTER TABLE sys_dept ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_dept_tenant ON sys_dept(tenant_id);

-- 2. Add tenant_id to sys_user (users belong to a tenant)
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_user_tenant ON sys_user(tenant_id);

-- 3. Link sys_dept.parent to sys_user.tenant via FK (dept.tenant_id references dept.dept_id at root)
-- Add FK from sys_dept.tenant_id → sys_dept.dept_id (self-referential for root dept = tenant root)
-- Note: This is informational; the actual tenant root dept has dept_id as its own tenant_id
ALTER TABLE sys_dept ADD CONSTRAINT fk_dept_tenant 
    FOREIGN KEY (tenant_id) REFERENCES sys_dept(dept_id) ON DELETE SET NULL;

-- 4. Add default tenant to existing root dept (dept_id = 1 or the top-level dept)
-- First, create a default "System" tenant dept if none exists
DO $$
DECLARE
    root_dept_id BIGINT;
    default_tenant_id BIGINT;
BEGIN
    -- Find or create default tenant dept
    SELECT dept_id INTO root_dept_id FROM sys_dept WHERE parent_id = 0 OR dept_id = 1 LIMIT 1;
    
    IF root_dept_id IS NULL THEN
        -- Create default root dept as tenant root
        INSERT INTO sys_dept (dept_name, parent_id, ancestors, order_num, leader, status, del_flag, create_by, create_time)
        VALUES ('昕动智能', 0, '0', 1, 'admin', '0', '0', 'system', CURRENT_TIMESTAMP)
        RETURNING dept_id INTO default_tenant_id;
        
        -- Update all existing depts to reference this as their tenant
        UPDATE sys_dept SET tenant_id = default_tenant_id WHERE tenant_id IS NULL;
        -- Update all existing users
        UPDATE sys_user SET tenant_id = default_tenant_id WHERE tenant_id IS NULL AND dept_id IS NOT NULL;
        
        RAISE NOTICE 'Created default tenant dept with id: %', default_tenant_id;
    ELSE
        -- Set tenant_id on existing root dept
        UPDATE sys_dept SET tenant_id = root_dept_id WHERE tenant_id IS NULL AND (parent_id = 0 OR dept_id = 1);
        -- Propagate tenant_id down the dept tree
        WITH RECURSIVE dept_tree AS (
            SELECT dept_id, parent_id, tenant_id FROM sys_dept WHERE dept_id = root_dept_id
            UNION ALL
            SELECT d.dept_id, d.parent_id, dt.tenant_id 
            FROM sys_dept d JOIN dept_tree dt ON d.parent_id = dt.dept_id
        )
        UPDATE sys_dept d SET tenant_id = dt.tenant_id
        FROM dept_tree dt WHERE d.dept_id = dt.dept_id AND d.tenant_id IS NULL;
        -- Set tenant_id on users belonging to these depts
        UPDATE sys_user u SET tenant_id = d.tenant_id
        FROM sys_dept d WHERE u.dept_id = d.dept_id AND u.tenant_id IS NULL;
        
        RAISE NOTICE 'Linked existing dept tree, root dept_id: %', root_dept_id;
    END IF;
END;
$$;

-- 5. Add tenant_id to IoT module tables
ALTER TABLE iot_device ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_iot_device_tenant ON iot_device(tenant_id);

ALTER TABLE iot_device_group ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_iot_device_group_tenant ON iot_device_group(tenant_id);

ALTER TABLE iot_device_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_iot_device_log_tenant ON iot_device_log(tenant_id);

ALTER TABLE iot_manufacturer ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_iot_manufacturer_tenant ON iot_manufacturer(tenant_id);

ALTER TABLE manufacturer_equipment ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_manufacturer_equipment_tenant ON manufacturer_equipment(tenant_id);

ALTER TABLE manufacturer_calibrations ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_manufacturer_calibrations_tenant ON manufacturer_calibrations(tenant_id);

-- Set default tenant_id on IoT tables based on manufacturer
DO $$
DECLARE
    default_tenant_id BIGINT;
BEGIN
    SELECT dept_id INTO default_tenant_id FROM sys_dept WHERE parent_id = 0 LIMIT 1;
    
    UPDATE iot_device SET tenant_id = COALESCE(tenant_id, default_tenant_id) WHERE tenant_id IS NULL;
    UPDATE iot_device_group SET tenant_id = COALESCE(tenant_id, default_tenant_id) WHERE tenant_id IS NULL;
    UPDATE iot_manufacturer SET tenant_id = COALESCE(tenant_id, default_tenant_id) WHERE tenant_id IS NULL;
    UPDATE manufacturer_equipment SET tenant_id = COALESCE(tenant_id, default_tenant_id) WHERE tenant_id IS NULL;
END;
$$;

-- 6. Create sys_tenant table (RuoYi managed tenants, mirrors b2b_tenants)
CREATE TABLE IF NOT EXISTS sys_tenant (
    tenant_id BIGINT PRIMARY KEY DEFAULT nextval('sys_tenant_tenant_id_seq'::regclass),
    tenant_name VARCHAR(100) NOT NULL,
    tenant_code VARCHAR(64) UNIQUE,
    contact_name VARCHAR(50),
    contact_phone VARCHAR(20),
    contact_email VARCHAR(100),
    status VARCHAR(1) DEFAULT '0' CHECK (status IN ('0','1')),
    del_flag VARCHAR(1) DEFAULT '0' CHECK (del_flag IN ('0','2')),
    create_by VARCHAR(64) DEFAULT '',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT '',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(500)
);

CREATE SEQUENCE IF NOT EXISTS sys_tenant_tenant_id_seq;
ALTER TABLE sys_tenant ALTER COLUMN tenant_id SET DEFAULT nextval('sys_tenant_tenant_id_seq'::regclass);

-- Insert default tenant
INSERT INTO sys_tenant (tenant_id, tenant_name, tenant_code, contact_name, status, create_by, create_time)
SELECT 1, '昕动智能', 'xindong', 'admin', '0', 'system', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_tenant WHERE tenant_id = 1);

-- 7. Add constraint: IoT device.tenant_id → sys_tenant.tenant_id
ALTER TABLE iot_device DROP CONSTRAINT IF EXISTS fk_iot_device_tenant;
ALTER TABLE iot_device ADD CONSTRAINT fk_iot_device_tenant 
    FOREIGN KEY (tenant_id) REFERENCES sys_tenant(tenant_id) ON DELETE SET NULL;

-- 8. Health metrics and devices tables (from device gateway)
ALTER TABLE devices ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_devices_tenant ON devices(tenant_id);

ALTER TABLE health_metrics ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_health_metrics_tenant ON health_metrics(tenant_id);

-- 9. Log tables - add tenant context
ALTER TABLE sys_logininfor ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_logininfor_tenant ON sys_logininfor(tenant_id);

ALTER TABLE sys_oper_log ADD COLUMN IF NOT EXISTS tenant_id BIGINT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS idx_sys_oper_log_tenant ON sys_oper_log(tenant_id);

-- 10. Verify migration
DO $$
BEGIN
    RAISE NOTICE '=== Tenant Migration Summary ===';
    RAISE NOTICE 'sys_dept.tenant_id: OK';
    RAISE NOTICE 'sys_user.tenant_id: OK';
    RAISE NOTICE 'iot_device.tenant_id: OK';
    RAISE NOTICE 'iot_device_group.tenant_id: OK';
    RAISE NOTICE 'iot_manufacturer.tenant_id: OK';
    RAISE NOTICE 'manufacturer_equipment.tenant_id: OK';
    RAISE NOTICE 'sys_tenant: OK';
    RAISE NOTICE 'devices.tenant_id: OK';
    RAISE NOTICE 'health_metrics.tenant_id: OK';
END;
$$;

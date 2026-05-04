-- =============================================================================
-- XIN-144 租户隔离迁移 SQL
-- 将干预引擎核心表加入 tenant_id 字段，实现多租户数据隔离
-- =============================================================================
-- 状态: v1.0 DRAFT — 需要与 ruoyi-system 租户模型对齐后实施
-- =============================================================================

-- =============================================================================
-- 1. 租户隔离表结构 (参考 ruoyi-system ry_tenant 模型)
-- =============================================================================

-- 租户表 (力康来=租户A, 臻木=租户B)
CREATE TABLE IF NOT EXISTS xd_tenant (
    tenant_id       BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '租户ID',
    tenant_code     VARCHAR(64) NOT NULL UNIQUE COMMENT '租户代码(likanglai/zhenmu)',
    tenant_name     VARCHAR(128) NOT NULL COMMENT '租户名称',
    tenant_type     VARCHAR(32) DEFAULT 'manufacturer' COMMENT '类型:manufacturer/distributor/admin',
    package_id      BIGINT DEFAULT 1 COMMENT '套餐ID',
    status          CHAR(1) DEFAULT '1' COMMENT '状态:0正常1停用',
    expire_time     DATETIME DEFAULT '2099-12-31' COMMENT '过期时间',
    tenant_secret   VARCHAR(64) COMMENT '租户密钥(用于小程序绑定)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '租户表';

-- 租户套餐权限表
CREATE TABLE IF NOT EXISTS xd_tenant_package (
    package_id      BIGINT PRIMARY KEY AUTO_INCREMENT,
    package_name    VARCHAR(128) NOT NULL COMMENT '套餐名称',
    features        JSON COMMENT '功能列表JSON',
    max_users       INT DEFAULT 100 COMMENT '最大用户数',
    max_devices     INT DEFAULT 50 COMMENT '最大设备数',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '租户套餐';

-- =============================================================================
-- 2. 干预引擎核心表租户字段添加
-- =============================================================================

-- 健康档案表 (HealthProfile)
ALTER TABLE xd_health_profile
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- 运动处方表 (Prescription)
ALTER TABLE xd_prescription
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- 训练计划表 (TrainingPlan)
ALTER TABLE xd_training_plan
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- IMU设备数据表
ALTER TABLE xd_imu_data
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- 依从性记录表 (ComplianceTracking)
ALTER TABLE xd_compliance_record
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- 用户阶段表 (UserStage)
ALTER TABLE xd_user_stage
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- 设备表 (Device)
ALTER TABLE xd_device
    ADD COLUMN tenant_id BIGINT DEFAULT 1 COMMENT '租户ID',
    ADD INDEX idx_tenant_id (tenant_id);

-- 用户-租户绑定表
CREATE TABLE IF NOT EXISTS xd_user_tenant (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         VARCHAR(64) NOT NULL COMMENT '用户ID',
    tenant_id       BIGINT NOT NULL COMMENT '租户ID',
    role_in_tenant  VARCHAR(32) DEFAULT 'user' COMMENT '在租户中的角色:user/admin/coach',
    bind_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tenant (user_id, tenant_id),
    INDEX idx_tenant_id (tenant_id)
) COMMENT '用户租户绑定';

-- =============================================================================
-- 3. 初始租户数据
-- =============================================================================

INSERT INTO xd_tenant (tenant_id, tenant_code, tenant_name, tenant_type, status) VALUES
    (1, 'admin', '超级管理员', 'admin', '1'),
    (2, 'likanglai', '德州力康来健身器材', 'manufacturer', '1'),
    (3, 'zhenmu', '臻木汇', 'manufacturer', '1');

INSERT INTO xd_tenant_package (package_id, package_name, features, max_users, max_devices) VALUES
    (1, '免费版', '{"coaching": true, "imu": true, "analytics": false}', 50, 10),
    (2, '标准版', '{"coaching": true, "imu": true, "analytics": true}', 200, 50),
    (3, '旗舰版', '{"coaching": true, "imu": true, "analytics": true, "white_label": true}', 1000, 200);

-- =============================================================================
-- 4. 多租户查询拦截 (应用层)
--    在 RuoYi-Backend 的 BaseEntity 或 MyBatis 拦截器中实现:
--    每次查询自动注入: WHERE tenant_id = :currentTenantId
-- =============================================================================

-- 示例 MyBatis 拦截器 SQL (仅供参考, 实际在 Java 层实现):
/*
@InterceptorIgnore(tenantLine = "true")
SELECT * FROM xd_health_profile WHERE tenant_id = #{tenantId};
*/

-- =============================================================================
-- 5. 数据迁移脚本 (从 intervention-engine 独立部署迁移)
-- =============================================================================

-- 将 intervention-engine 的 B2B 数据迁移到 xd_* 表并补充 tenant_id
-- 迁移力康来租户数据 (假设 tenant_id=2)
-- UPDATE xd_health_profile SET tenant_id = 2 WHERE user_id IN (SELECT user_id FROM xd_user_tenant WHERE tenant_id = 2);

-- =============================================================================
-- 6. 验证查询
-- =============================================================================

-- 验证租户隔离: 力康来用户看不到臻木数据
-- SELECT COUNT(*) FROM xd_health_profile WHERE tenant_id = 2;
-- 预期: 只返回租户ID=2的数据

-- 验证超级管理员能看到所有数据 (特殊处理)
-- SELECT COUNT(*) FROM xd_health_profile WHERE tenant_id IN (1,2,3);

-- =============================================================================
-- 注意事项:
-- 1. 所有新增表使用 InnoDB 引擎, UTF8MB4 字符集
-- 2. tenant_id 默认值=1 (超级管理员租户)
-- 3. 外键约束在应用层控制, 避免跨租户引用
-- 4. 实施前需备份现有数据
-- =============================================================================

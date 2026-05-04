-- =============================================================================
-- XIN-144: 创建 interv_prescription 表
-- 为 TrainingPlanService.persistPrescription() 提供持久化目标
-- 参照: IntervPrescriptionMapper.xml resultMap
-- =============================================================================
-- 状态: ACTIVE
-- 执行: docker exec -i xindong-postgres psql -U xindong -d xindong < sql/xindong_interv_prescription_table.sql
-- =============================================================================

CREATE TABLE interv_prescription (
    prescription_id       BIGSERIAL PRIMARY KEY,
    prescription_no       VARCHAR(64),
    user_id               VARCHAR(64) NOT NULL,
    profile_id            VARCHAR(64),
    intervention_type     VARCHAR(64) NOT NULL,
    status                VARCHAR(32) NOT NULL DEFAULT 'active',
    duration_days         INT,
    recommendations       TEXT,
    sleep_plan            TEXT,
    adjustment_rules      TEXT,
    notes                 TEXT,
    start_date            DATE,
    end_date              DATE,
    completion_rate       DOUBLE PRECISION DEFAULT 0.0,
    del_flag              CHAR(1) DEFAULT '0',
    create_by             VARCHAR(64),
    create_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_by             VARCHAR(64),
    update_time           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id             BIGINT DEFAULT 1,
    version               INT DEFAULT 1
);

-- PostgreSQL comments (separate statements)
COMMENT ON TABLE interv_prescription IS '干预处方表';
COMMENT ON COLUMN interv_prescription.prescription_id IS '处方ID';
COMMENT ON COLUMN interv_prescription.prescription_no IS '处方编号';
COMMENT ON COLUMN interv_prescription.user_id IS '用户ID';
COMMENT ON COLUMN interv_prescription.profile_id IS '健康档案ID';
COMMENT ON COLUMN interv_prescription.intervention_type IS '干预类型: exercise/sleep/nutrition';
COMMENT ON COLUMN interv_prescription.status IS '状态: active/completed/superseded';
COMMENT ON COLUMN interv_prescription.duration_days IS '疗程天数';
COMMENT ON COLUMN interv_prescription.recommendations IS '运动处方详情JSON (AI生成)';
COMMENT ON COLUMN interv_prescription.sleep_plan IS '睡眠干预方案JSON';
COMMENT ON COLUMN interv_prescription.adjustment_rules IS '调整规则JSON';
COMMENT ON COLUMN interv_prescription.notes IS '备注';
COMMENT ON COLUMN interv_prescription.start_date IS '开始日期';
COMMENT ON COLUMN interv_prescription.end_date IS '结束日期';
COMMENT ON COLUMN interv_prescription.completion_rate IS '完成率 0.0-1.0';
COMMENT ON COLUMN interv_prescription.del_flag IS '删除标志: 0正常 2删除';
COMMENT ON COLUMN interv_prescription.create_by IS '创建者';
COMMENT ON COLUMN interv_prescription.create_time IS '创建时间';
COMMENT ON COLUMN interv_prescription.update_by IS '更新者';
COMMENT ON COLUMN interv_prescription.update_time IS '更新时间';
COMMENT ON COLUMN interv_prescription.tenant_id IS '租户ID';
COMMENT ON COLUMN interv_prescription.version IS '版本号 (乐观锁)';

-- Indexes
CREATE INDEX idx_interv_rx_user_id     ON interv_prescription(user_id);
CREATE INDEX idx_interv_rx_tenant_id   ON interv_prescription(tenant_id);
CREATE INDEX idx_interv_rx_status      ON interv_prescription(status);
CREATE INDEX idx_interv_rx_create_time ON interv_prescription(create_time DESC);

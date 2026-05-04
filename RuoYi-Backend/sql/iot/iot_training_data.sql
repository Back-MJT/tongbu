-- ===============================================================
-- Legacy 训练数据迁移脚本 (PostgreSQL / TimescaleDB)
-- 说明:
-- 1. 当前项目默认使用 MySQL 版 `interv_session + xd_training_set`
-- 2. 本文件保留为早期数据建模参考, 不作为当前默认初始化脚本
-- ===============================================================

-- ─────────────────────────────────────────────────────────────
-- 1. 训练会话表 (Training Session)
--    记录用户每次健身训练会话 (一组套数的完整训练)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS training_session (
    session_id          BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    device_id           BIGINT,
    device_code         VARCHAR(64) NOT NULL,
    manufacturer_id     BIGINT,
    -- 训练信息
    exercise_type      VARCHAR(50) NOT NULL,          -- bench_press / squat / deadlift / bicep_curl
    start_time          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time            TIMESTAMPTZ,
    total_duration_sec  INTEGER DEFAULT 0,            -- 总时长(秒)
    total_sets          INTEGER DEFAULT 0,             -- 总组数
    total_reps          INTEGER DEFAULT 0,             -- 总次数
    -- 训练质量
    avg_rest_sec        DOUBLE PRECISION DEFAULT 0,   -- 平均组间休息(秒)
    peak_load_kg        DOUBLE PRECISION,             -- 峰值负荷(kg)
    estimated_volume_kg DOUBLE PRECISION DEFAULT 0,   -- 总训练量(kg) = sum(reps * load)
    -- AI 训练方案
    training_plan_id    BIGINT,                       -- 关联训练方案
    ai_confidence       DOUBLE PRECISION,              -- AI置信度
    -- 状态
    status              VARCHAR(20) DEFAULT 'active', -- active / completed / abandoned
    del_flag            CHAR(1) DEFAULT '0',
    create_by           VARCHAR(64) DEFAULT '',
    create_time         TIMESTAMPTZ DEFAULT NOW(),
    update_by           VARCHAR(64) DEFAULT '',
    update_time         TIMESTAMPTZ DEFAULT NOW(),
    remark              VARCHAR(500)
);
CREATE INDEX idx_session_user ON training_session(user_id);
CREATE INDEX idx_session_device ON training_session(device_code);
CREATE INDEX idx_session_time ON training_session(start_time DESC);
CREATE INDEX idx_session_exercise ON training_session(exercise_type);

-- ─────────────────────────────────────────────────────────────
-- 2. 训练组记录表 (Training Set)
--    记录每一组的详细数据 (一次训练中的一组)
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS training_set (
    set_id              BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL,
    user_id             BIGINT NOT NULL,
    device_code         VARCHAR(64) NOT NULL,
    -- 组序号
    set_number          SMALLINT NOT NULL,            -- 第几组 (1, 2, 3...)
    -- 动作质量
    rep_count           SMALLINT NOT NULL DEFAULT 0, -- 本组次数
    avg_load_kg         DOUBLE PRECISION,             -- 平均负荷(kg)
    peak_load_kg        DOUBLE PRECISION,             -- 峰值负荷(kg)
    avg_magnitude_g     DOUBLE PRECISION,              -- 平均加速度(g)
    peak_magnitude_g    DOUBLE PRECISION,             -- 峰值加速度(g)
    -- 时序
    set_start_time      TIMESTAMPTZ NOT NULL,
    set_end_time        TIMESTAMPTZ,
    rest_after_sec      INTEGER DEFAULT 0,            -- 下一组前休息时长
    -- 质量评估
    quality_score       SMALLINT,                     -- 动作质量评分(0-100)
    difficulty_level    VARCHAR(20),                  -- easy / moderate / hard / maximal
    -- 来源
    source              VARCHAR(20) DEFAULT 'imu',   -- imu / manual / ai_suggested
    del_flag            CHAR(1) DEFAULT '0',
    create_by           VARCHAR(64) DEFAULT '',
    create_time         TIMESTAMPTZ DEFAULT NOW(),
    update_by           VARCHAR(64) DEFAULT '',
    update_time         TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_set_session ON training_set(session_id);
CREATE INDEX idx_set_user ON training_set(user_id);
CREATE INDEX idx_set_device ON training_set(device_code);

-- ─────────────────────────────────────────────────────────────
-- 3. 训练方案表 (Training Plan)
--    AI生成的个性化训练方案
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS training_plan (
    plan_id             BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    device_code         VARCHAR(64),                  -- 可选，绑定设备
    -- 方案内容
    plan_type           VARCHAR(30) DEFAULT 'strength', -- strength / cardio / flexibility
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    target_sets         SMALLINT,                    -- 目标组数
    target_reps_min     SMALLINT,                    -- 目标次数范围
    target_reps_max     SMALLINT,
    target_load_kg      DOUBLE PRECISION,            -- 目标负荷
    suggested_rest_sec  INTEGER,                     -- 建议组间休息(秒)
    -- AI 相关信息
    ai_model            VARCHAR(50),                 -- claude-3-5 / deepseek / qwen / mock
    ai_prompt_tokens    INTEGER,
    ai_completion_tokens INTEGER,
    ai_latency_ms       INTEGER,
    -- 使用情况
    used_count          INTEGER DEFAULT 0,           -- 被使用次数
    last_used_at        TIMESTAMPTZ,
    -- 状态
    status              VARCHAR(20) DEFAULT 'active', -- active / archived / superseded
    del_flag            CHAR(1) DEFAULT '0',
    create_by           VARCHAR(64) DEFAULT '',
    create_time         TIMESTAMPTZ DEFAULT NOW(),
    update_by           VARCHAR(64) DEFAULT '',
    update_time         TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_plan_user ON training_plan(user_id);
CREATE INDEX idx_plan_device ON training_plan(device_code);
CREATE INDEX idx_plan_status ON training_plan(status);

-- ─────────────────────────────────────────────────────────────
-- 4. 用户-设备绑定表 (User Device Binding)
--    小程序用户绑定IMU设备
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_device_binding (
    binding_id          BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    device_id           BIGINT,
    device_code         VARCHAR(64) NOT NULL,
    device_name         VARCHAR(100),                 -- 用户给设备起的名字
    manufacturer_id     BIGINT,
    -- 绑定信息
    bind_time           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unbind_time         TIMESTAMPTZ,
    is_active           CHAR(1) DEFAULT '1',         -- 1=激活 0=解绑
    -- 设备元数据
    firmware_version    VARCHAR(30),
    sensor_position     VARCHAR(30) DEFAULT 'device_fixed', -- device_fixed / handheld / body_worn
    calibration_status  VARCHAR(20) DEFAULT 'uncalibrated',
    -- 使用统计
    total_sessions      INTEGER DEFAULT 0,            -- 累计训练次数
    total_duration_min  INTEGER DEFAULT 0,           -- 累计训练时长(分钟)
    last_session_at     TIMESTAMPTZ,
    -- 偏好设置
    reminder_enabled    CHAR(1) DEFAULT '1',
    auto_start_enabled  CHAR(1) DEFAULT '0',
    del_flag            CHAR(1) DEFAULT '0',
    create_by           VARCHAR(64) DEFAULT '',
    create_time         TIMESTAMPTZ DEFAULT NOW(),
    update_by           VARCHAR(64) DEFAULT '',
    update_time         TIMESTAMPTZ DEFAULT NOW(),
    -- 唯一约束: 同一用户同一设备同时只能有一个活跃绑定
    CONSTRAINT uk_user_device_active UNIQUE (user_id, device_code, is_active)
);
CREATE INDEX idx_binding_user ON user_device_binding(user_id);
CREATE INDEX idx_binding_device ON user_device_binding(device_code);
CREATE INDEX idx_binding_active ON user_device_binding(user_id, is_active) WHERE is_active = '1';

-- ─────────────────────────────────────────────────────────────
-- 5. 每日训练进度表 (Daily Training Progress)
--    用户每日训练汇总，供小程序打卡/进度追踪使用
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS daily_training_progress (
    progress_id         BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    report_date         DATE NOT NULL,               -- 日期(天)
    -- 完成情况
    planned_sessions    SMALLINT DEFAULT 0,         -- 计划训练次数
    completed_sessions   SMALLINT DEFAULT 0,         -- 完成次数
    total_duration_min   INTEGER DEFAULT 0,          -- 总时长(分钟)
    total_sets          INTEGER DEFAULT 0,           -- 总组数
    total_reps          INTEGER DEFAULT 0,           -- 总次数
    total_volume_kg     DOUBLE PRECISION DEFAULT 0, -- 总训练量(kg)
    -- 连续性
    streak_days         SMALLINT DEFAULT 0,          -- 连续训练天数
    last_train_date     DATE,
    -- AI 依从率
    ai_compliance_rate  DOUBLE PRECISION,            -- AI方案依从率(0-1)
    -- 状态
    status              VARCHAR(20) DEFAULT 'planned', -- planned / in_progress / completed / missed
    del_flag            CHAR(1) DEFAULT '0',
    create_by           VARCHAR(64) DEFAULT '',
    create_time         TIMESTAMPTZ DEFAULT NOW(),
    update_by           VARCHAR(64) DEFAULT '',
    update_time         TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_user_date UNIQUE (user_id, report_date)
);
CREATE INDEX idx_progress_user ON daily_training_progress(user_id);
CREATE INDEX idx_progress_date ON daily_training_progress(report_date DESC);
CREATE INDEX idx_progress_streak ON daily_training_progress(user_id, streak_days DESC);

-- ─────────────────────────────────────────────────────────────
-- 6. IMU 训练扩展数据表 (IMU Training Extension)
--    存储每条IMU数据对应的训练语义信息 (rep/set归属)
--    关联 device_imu_data (time, device_id) 实现快速查询
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS imu_training_tag (
    tag_id              BIGSERIAL PRIMARY KEY,
    device_code         VARCHAR(64) NOT NULL,
    -- 时间范围 (对应IMU数据的时间范围)
    tag_time            TIMESTAMPTZ NOT NULL,        -- 该标签对应的时间点
    -- 归属训练
    session_id          BIGINT,
    set_id              BIGINT,
    user_id             BIGINT,
    -- 运动阶段
    exercise_phase      VARCHAR(30),                -- eccentric / concentric / isometric / rest
    rep_number          SMALLINT,                   -- 第几次重复
    set_number          SMALLINT,                   -- 第几组
    -- 质量指标
    peak_acceleration_g DOUBLE PRECISION,           -- 该rep峰值加速度
    movement_velocity   DOUBLE PRECISION,           -- 动作速度
    symmetry_score      DOUBLE PRECISION,           -- 左右对称性(0-1)
    del_flag            CHAR(1) DEFAULT '0',
    create_time         TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_imu_tag_device ON imu_training_tag(device_code);
CREATE INDEX idx_imu_tag_session ON imu_training_tag(session_id);
CREATE INDEX idx_imu_tag_time ON imu_training_tag(tag_time DESC);

-- ===============================================================
-- Comments for documentation
-- ===============================================================
COMMENT ON TABLE training_session IS '训练会话表 - 记录每次完整的健身训练';
COMMENT ON TABLE training_set IS '训练组记录表 - 记录每一组的详细数据';
COMMENT ON TABLE training_plan IS '训练方案表 - AI生成的个性化训练方案';
COMMENT ON TABLE user_device_binding IS '用户设备绑定表 - 小程序用户绑定IMU设备';
COMMENT ON TABLE daily_training_progress IS '每日训练进度表 - 用户每日训练汇总';
COMMENT ON TABLE imu_training_tag IS 'IMU训练语义标签表 - 训练动作阶段/质量标注';

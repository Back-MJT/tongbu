-- H2 Schema for IoT Mapper Testing
-- Based on PostgreSQL schema from ruoyi-iot

CREATE TABLE IF NOT EXISTS iot_manufacturer (
    manufacturer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    manufacturer_name VARCHAR(100),
    contact_person VARCHAR(64),
    contact_phone VARCHAR(32),
    address VARCHAR(255),
    business_license VARCHAR(255),
    status VARCHAR(20),
    del_flag CHAR(1) DEFAULT '0',
    tenant_id BIGINT NOT NULL,
    create_by VARCHAR(64),
    create_time TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS iot_device (
    device_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(64) NOT NULL,
    device_name VARCHAR(100),
    device_type VARCHAR(50),
    protocol VARCHAR(20),
    manufacturer_id BIGINT,
    tenant_id BIGINT NOT NULL,
    status VARCHAR(20),
    firmware_version VARCHAR(50),
    metadata TEXT,
    del_flag CHAR(1) DEFAULT '0',
    create_by VARCHAR(64),
    create_time TIMESTAMP,
    update_by VARCHAR(64),
    update_time TIMESTAMP,
    remark VARCHAR(500),
    last_seen_at TIMESTAMP,
    UNIQUE (device_code, tenant_id)
);

CREATE INDEX idx_device_tenant ON iot_device(tenant_id);
CREATE INDEX idx_device_manufacturer ON iot_device(manufacturer_id);
CREATE INDEX idx_device_type ON iot_device(device_type);

-- Insert test manufacturer
INSERT INTO iot_manufacturer (manufacturer_id, manufacturer_name, contact_person, contact_phone, status, del_flag, tenant_id, create_by, create_time)
VALUES (1, 'Test Manufacturer', 'Test Contact', '1234567890', 'active', '0', 1, 'test', CURRENT_TIMESTAMP);

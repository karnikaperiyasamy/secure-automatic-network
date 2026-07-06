-- ============================================================
-- SANTMS - PostgreSQL Schema
-- Run this ONLY if you want manual schema setup.
-- Spring Boot with ddl-auto=create-drop handles this automatically.
-- ============================================================

-- Create database (run as postgres superuser)
-- CREATE DATABASE santms_db;
-- \c santms_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Organizations
CREATE TABLE IF NOT EXISTS organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    domain VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    industry VARCHAR(100),
    logo_url VARCHAR(500),
    max_devices INT DEFAULT 1000,
    max_users INT DEFAULT 50,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    profile_image VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    email_verified BOOLEAN DEFAULT FALSE,
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    otp_code VARCHAR(10),
    otp_expiry TIMESTAMP,
    reset_token VARCHAR(255),
    reset_token_expiry TIMESTAMP,
    last_login TIMESTAMP,
    login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    refresh_token TEXT,
    remember_me_token VARCHAR(255),
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100)
);

-- User Roles mapping
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Devices
CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    hostname VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    mac_address VARCHAR(17),
    vendor VARCHAR(100),
    operating_system VARCHAR(100),
    device_type VARCHAR(30) DEFAULT 'UNKNOWN',
    status VARCHAR(20) DEFAULT 'UNKNOWN',
    subnet VARCHAR(50),
    gateway VARCHAR(45),
    dns_server VARCHAR(45),
    open_ports VARCHAR(1000),
    network_speed VARCHAR(20),
    network_name VARCHAR(100),
    vlan_id INT,
    is_approved BOOLEAN DEFAULT FALSE,
    is_authorized BOOLEAN DEFAULT TRUE,
    maintenance_mode BOOLEAN DEFAULT FALSE,
    cpu_usage DOUBLE PRECISION DEFAULT 0,
    ram_usage DOUBLE PRECISION DEFAULT 0,
    storage_usage DOUBLE PRECISION DEFAULT 0,
    bandwidth_usage DOUBLE PRECISION DEFAULT 0,
    temperature DOUBLE PRECISION DEFAULT 0,
    latency_ms DOUBLE PRECISION DEFAULT 0,
    packet_loss DOUBLE PRECISION DEFAULT 0,
    uptime_seconds BIGINT DEFAULT 0,
    availability_percent DOUBLE PRECISION DEFAULT 100,
    risk_score INT DEFAULT 0,
    security_score INT DEFAULT 100,
    description VARCHAR(500),
    tags VARCHAR(500),
    qr_code TEXT,
    image_url VARCHAR(500),
    last_seen TIMESTAMP,
    first_discovered TIMESTAMP,
    ipv6_address VARCHAR(45),
    is_dhcp BOOLEAN DEFAULT TRUE,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100)
);

-- Alerts
CREATE TABLE IF NOT EXISTS alerts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(20) DEFAULT 'OPEN',
    device_id BIGINT REFERENCES devices(id) ON DELETE SET NULL,
    device_ip VARCHAR(45),
    device_name VARCHAR(100),
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_note VARCHAR(500),
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(100),
    notification_sent BOOLEAN DEFAULT FALSE,
    metric_value DOUBLE PRECISION,
    threshold_value DOUBLE PRECISION,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW(),
    created_by VARCHAR(100)
);

-- Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    entity_name VARCHAR(200),
    old_value TEXT,
    new_value TEXT,
    performed_by VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    description VARCHAR(500),
    level VARCHAR(20) DEFAULT 'INFO',
    category VARCHAR(50),
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Network Scans
CREATE TABLE IF NOT EXISTS network_scans (
    id BIGSERIAL PRIMARY KEY,
    scan_name VARCHAR(100),
    target_network VARCHAR(50) NOT NULL,
    scan_type VARCHAR(50) DEFAULT 'FULL',
    status VARCHAR(20) DEFAULT 'PENDING',
    devices_found INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_seconds BIGINT,
    initiated_by VARCHAR(100),
    scan_result TEXT,
    error_message VARCHAR(500),
    progress_percent INT DEFAULT 0,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Topology Nodes
CREATE TABLE IF NOT EXISTS topology_nodes (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id) ON DELETE CASCADE,
    x_position DOUBLE PRECISION DEFAULT 0,
    y_position DOUBLE PRECISION DEFAULT 0,
    node_group VARCHAR(50) DEFAULT 'default',
    node_color VARCHAR(20),
    node_size INT DEFAULT 30,
    is_visible BOOLEAN DEFAULT TRUE,
    label_visible BOOLEAN DEFAULT TRUE,
    topology_id BIGINT,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Topology Edges
CREATE TABLE IF NOT EXISTS topology_edges (
    id BIGSERIAL PRIMARY KEY,
    source_node_id BIGINT REFERENCES topology_nodes(id) ON DELETE CASCADE,
    target_node_id BIGINT REFERENCES topology_nodes(id) ON DELETE CASCADE,
    label VARCHAR(100),
    connection_type VARCHAR(50) DEFAULT 'ETHERNET',
    bandwidth_mbps INT,
    is_active BOOLEAN DEFAULT TRUE,
    edge_color VARCHAR(20) DEFAULT '#4ecdc4',
    topology_id BIGINT,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- IP Address Pool
CREATE TABLE IF NOT EXISTS ip_address_pool (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    subnet VARCHAR(50),
    gateway VARCHAR(45),
    status VARCHAR(20) DEFAULT 'AVAILABLE',
    type VARCHAR(20) DEFAULT 'DYNAMIC',
    assigned_to VARCHAR(100),
    assigned_device_id BIGINT,
    mac_address VARCHAR(17),
    hostname VARCHAR(100),
    lease_expiry TIMESTAMP,
    is_reserved BOOLEAN DEFAULT FALSE,
    description VARCHAR(255),
    vlan_id INT,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Security Scans
CREATE TABLE IF NOT EXISTS security_scans (
    id BIGSERIAL PRIMARY KEY,
    device_id BIGINT REFERENCES devices(id) ON DELETE CASCADE,
    scan_type VARCHAR(50),
    open_ports VARCHAR(1000),
    vulnerabilities TEXT,
    risk_score INT DEFAULT 0,
    security_score INT DEFAULT 100,
    weak_passwords_detected BOOLEAN DEFAULT FALSE,
    unauthorized_access_detected BOOLEAN DEFAULT FALSE,
    suspicious_traffic_detected BOOLEAN DEFAULT FALSE,
    arp_spoofing_detected BOOLEAN DEFAULT FALSE,
    recommendations TEXT,
    scan_summary VARCHAR(500),
    initiated_by VARCHAR(100),
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Reports
CREATE TABLE IF NOT EXISTS reports (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    type VARCHAR(50) NOT NULL,
    period VARCHAR(20) NOT NULL,
    format VARCHAR(20) DEFAULT 'PDF',
    file_path VARCHAR(500),
    file_size BIGINT,
    generated_by VARCHAR(100),
    report_data TEXT,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_scheduled BOOLEAN DEFAULT FALSE,
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(20) DEFAULT 'INFO',
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    action_url VARCHAR(255),
    icon VARCHAR(50),
    organization_id BIGINT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Configurations
CREATE TABLE IF NOT EXISTS configurations (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    description VARCHAR(255),
    category VARCHAR(50),
    is_sensitive BOOLEAN DEFAULT FALSE,
    data_type VARCHAR(20) DEFAULT 'STRING',
    organization_id BIGINT REFERENCES organizations(id),
    updated_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_devices_org ON devices(organization_id);
CREATE INDEX IF NOT EXISTS idx_devices_status ON devices(status);
CREATE INDEX IF NOT EXISTS idx_devices_type ON devices(device_type);
CREATE INDEX IF NOT EXISTS idx_devices_ip ON devices(ip_address);
CREATE INDEX IF NOT EXISTS idx_alerts_org ON alerts(organization_id);
CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts(status);
CREATE INDEX IF NOT EXISTS idx_alerts_severity ON alerts(severity);
CREATE INDEX IF NOT EXISTS idx_audit_org ON audit_logs(organization_id);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(performed_by);
CREATE INDEX IF NOT EXISTS idx_topology_org ON topology_nodes(organization_id);

-- ============================================================
-- SAMPLE DATA (optional — Spring Boot seeder handles this)
-- ============================================================
INSERT INTO roles (name, description) VALUES
  ('ROLE_SUPER_ADMIN', 'Full system control'),
  ('ROLE_NETWORK_ADMIN', 'Network management'),
  ('ROLE_SECURITY_ANALYST', 'Security monitoring'),
  ('ROLE_READ_ONLY', 'View-only access')
ON CONFLICT (name) DO NOTHING;

INSERT INTO organizations (name, description, domain, industry, city, country, status)
VALUES ('SANTMS Corp', 'Enterprise Network Management', 'santms.com', 'Technology', 'San Francisco', 'USA', 'ACTIVE')
ON CONFLICT (name) DO NOTHING;

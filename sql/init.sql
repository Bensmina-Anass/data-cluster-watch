-- ═══════════════════════════════════════════════════════════════
-- DataCluster Watch — Script d'initialisation MySQL 8
-- ═══════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS datacluster_watch
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE datacluster_watch;

-- ─────────────────────────────────────────────────────────────
-- Utilisateur applicatif
-- ─────────────────────────────────────────────────────────────
CREATE USER IF NOT EXISTS 'datacluster'@'%' IDENTIFIED BY 'datacluster123';
GRANT ALL PRIVILEGES ON datacluster_watch.* TO 'datacluster'@'%';
FLUSH PRIVILEGES;

-- ─────────────────────────────────────────────────────────────
-- Table : nodes
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nodes (
    id     VARCHAR(64)  NOT NULL,
    name   VARCHAR(128) NOT NULL,
    type   ENUM('MASTER','WORKER','STORAGE') NOT NULL,
    status ENUM('ACTIVE','IDLE','DOWN')      NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────
-- Table : metrics
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS metrics (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    node_id     VARCHAR(64)   NOT NULL,
    timestamp   DATETIME(3)   NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    cpu         DECIMAL(5,2)  NOT NULL,
    ram         DECIMAL(5,2)  NOT NULL,
    disk        DECIMAL(5,2)  NOT NULL,
    active_jobs INT           NOT NULL DEFAULT 0,
    failed_jobs INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    INDEX idx_metrics_node_ts (node_id, timestamp),
    CONSTRAINT fk_metrics_node FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────
-- Table : alerts
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alerts (
    id           VARCHAR(36)  NOT NULL,
    node_id      VARCHAR(64)  NOT NULL,
    timestamp    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    type         VARCHAR(32)  NOT NULL,
    level        ENUM('INFO','WARNING','CRITICAL') NOT NULL,
    message      TEXT         NOT NULL,
    acknowledged BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    INDEX idx_alerts_node_ts (node_id, timestamp),
    CONSTRAINT fk_alerts_node FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────
-- Table : jobs
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS jobs (
    id         VARCHAR(36)  NOT NULL,
    node_id    VARCHAR(64)  NOT NULL,
    type       VARCHAR(32)  NOT NULL,
    start_time DATETIME(3)  NOT NULL,
    end_time   DATETIME(3)  NULL,
    status     ENUM('PENDING','RUNNING','COMPLETED','FAILED','CANCELLED') NOT NULL DEFAULT 'RUNNING',
    progress   DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id),
    INDEX idx_jobs_node_status (node_id, status),
    CONSTRAINT fk_jobs_node FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────
-- Table : thresholds
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS thresholds (
    metric          VARCHAR(32)   NOT NULL,
    warning_value   DECIMAL(6,2)  NOT NULL,
    critical_value  DECIMAL(6,2)  NOT NULL,
    PRIMARY KEY (metric)
) ENGINE=InnoDB;

-- ─────────────────────────────────────────────────────────────
-- Table : users
-- ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id           INT          NOT NULL AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    role         ENUM('ADMIN','VIEWER') NOT NULL DEFAULT 'VIEWER',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════
-- Données initiales
-- ═══════════════════════════════════════════════════════════════

-- Nœuds
INSERT INTO nodes (id, name, type, status) VALUES
    ('master-01',  'Master Node 01',  'MASTER',  'ACTIVE'),
    ('worker-01',  'Worker Node 01',  'WORKER',  'ACTIVE'),
    ('worker-02',  'Worker Node 02',  'WORKER',  'ACTIVE'),
    ('worker-03',  'Worker Node 03',  'WORKER',  'ACTIVE'),
    ('storage-01', 'Storage Node 01', 'STORAGE', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Seuils par défaut
INSERT INTO thresholds (metric, warning_value, critical_value) VALUES
    ('cpu',         75.0, 90.0),
    ('ram',         70.0, 85.0),
    ('disk',        80.0, 95.0),
    ('failed_jobs',  1.0,  3.0)
ON DUPLICATE KEY UPDATE warning_value=VALUES(warning_value), critical_value=VALUES(critical_value);

-- Utilisateur admin (mot de passe : admin — haché SHA-256 à remplacer en production)
INSERT INTO users (username, password_hash, role) VALUES
    ('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'ADMIN')
ON DUPLICATE KEY UPDATE role='ADMIN';

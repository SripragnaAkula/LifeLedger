-- LifeLedger MySQL Schema
-- Run this ONCE to set up the database:
--   mysql -u root -p < src\main\resources\schema.sql

CREATE DATABASE IF NOT EXISTS lifeledger;
USE lifeledger;

-- Users
CREATE TABLE IF NOT EXISTS users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100),
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Documents
CREATE TABLE IF NOT EXISTS documents (
    doc_id            INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    title             VARCHAR(150) NOT NULL,
    category          VARCHAR(50),
    doc_number        VARCHAR(80),
    issuing_authority VARCHAR(120),
    issue_date        DATE,
    expiry_date       DATE,
    is_sensitive      TINYINT DEFAULT 0,
    file_path         VARCHAR(500),
    notes             TEXT,
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Warranties
CREATE TABLE IF NOT EXISTS warranties (
    w_id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    product_name      VARCHAR(150) NOT NULL,
    brand             VARCHAR(80),
    serial_no         VARCHAR(80),
    purchase_date     DATE,
    warranty_months   INT DEFAULT 12,
    warranty_expiry   DATE,
    purchase_price    DECIMAL(10,2),
    seller            VARCHAR(100),
    notes             TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Subscriptions
CREATE TABLE IF NOT EXISTS subscriptions (
    sub_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id       INT NOT NULL,
    service_name  VARCHAR(100) NOT NULL,
    cost          DECIMAL(10,2),
    billing_cycle VARCHAR(20),
    renewal_date  DATE,
    status        VARCHAR(20) DEFAULT 'Active',
    notes         TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Reminders
CREATE TABLE IF NOT EXISTS reminders (
    rem_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id   INT NOT NULL,
    title     VARCHAR(150) NOT NULL,
    category  VARCHAR(50),
    due_date  DATE,
    urgency   VARCHAR(20) DEFAULT 'MEDIUM',
    status    VARCHAR(20) DEFAULT 'Pending',
    ref_id    INT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Renewal Plans
CREATE TABLE IF NOT EXISTS plans (
    plan_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    title       VARCHAR(150) NOT NULL,
    category    VARCHAR(50),
    target_date DATE,
    status      VARCHAR(30) DEFAULT 'In Progress',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Plan Tasks (checklist items)
CREATE TABLE IF NOT EXISTS plan_tasks (
    task_id     INT AUTO_INCREMENT PRIMARY KEY,
    plan_id     INT NOT NULL,
    description VARCHAR(200) NOT NULL,
    due_date    DATE,
    is_done     TINYINT DEFAULT 0,
    FOREIGN KEY (plan_id) REFERENCES plans(plan_id) ON DELETE CASCADE
);

-- History / Audit Log
CREATE TABLE IF NOT EXISTS history (
    hist_id    INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT NOT NULL,
    module     VARCHAR(50),
    action     VARCHAR(50),
    detail     VARCHAR(255),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ====================================================================
-- LifeLedger — Personal Document, Warranty and Obligation Management System
-- Database Schema for MySQL 8.0+
-- ====================================================================

CREATE DATABASE IF NOT EXISTS lifeledger_db;
USE lifeledger_db;

-- 1. Users & Authentication
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    vault_pin VARCHAR(10) DEFAULT '1234',
    role VARCHAR(20) DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Personal Document Vault
CREATE TABLE IF NOT EXISTS documents (
    document_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    title VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- Identity, Education, Medical, Property, Financial, Legal, Vehicle
    document_number VARCHAR(100),
    issuing_authority VARCHAR(150),
    issue_date DATE,
    expiry_date DATE,
    is_sensitive BOOLEAN DEFAULT FALSE,
    file_path VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3. Warranty & Purchase Tracker
CREATE TABLE IF NOT EXISTS warranties (
    warranty_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    product_name VARCHAR(150) NOT NULL,
    brand_model VARCHAR(100),
    serial_number VARCHAR(100),
    purchase_date DATE NOT NULL,
    warranty_months INT NOT NULL DEFAULT 12,
    warranty_expiry_date DATE NOT NULL,
    seller_retailer VARCHAR(150),
    purchase_cost DECIMAL(10, 2) DEFAULT 0.00,
    receipt_path VARCHAR(500),
    coverage_type VARCHAR(50) DEFAULT 'Standard Manufacturer',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 3b. Warranty Repair & Service Records
CREATE TABLE IF NOT EXISTS repairs (
    repair_id INT AUTO_INCREMENT PRIMARY KEY,
    warranty_id INT NOT NULL,
    service_date DATE NOT NULL,
    issue_description TEXT NOT NULL,
    service_center VARCHAR(150),
    repair_cost DECIMAL(10, 2) DEFAULT 0.00,
    is_covered BOOLEAN DEFAULT TRUE,
    technician_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (warranty_id) REFERENCES warranties(warranty_id) ON DELETE CASCADE
);

-- 4. Subscription & Recurring-Payment Tracker
CREATE TABLE IF NOT EXISTS subscriptions (
    subscription_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    service_name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL, -- Entertainment, Utilities, Productivity, Health, Cloud, Insurance
    cost DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    billing_cycle VARCHAR(20) NOT NULL, -- Monthly, Quarterly, Semi-Annually, Annually
    next_renewal_date DATE NOT NULL,
    cancellation_notice_days INT DEFAULT 3,
    auto_renew BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50),
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, PAUSED, CANCELLED
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 5. Smart Reminder Engine
CREATE TABLE IF NOT EXISTS reminders (
    reminder_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    title VARCHAR(200) NOT NULL,
    module VARCHAR(50) NOT NULL, -- Document, Warranty, Subscription, RenewalStep, Custom
    entity_id INT DEFAULT 0,
    due_date DATE NOT NULL,
    urgency VARCHAR(20) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, CRITICAL
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SNOOZED, DISMISSED, COMPLETED
    snooze_until DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 6. Renewal Action Planner (Projects & Steps)
CREATE TABLE IF NOT EXISTS renewal_plans (
    plan_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    title VARCHAR(150) NOT NULL,
    target_deadline DATE NOT NULL,
    category VARCHAR(50),
    linked_document_id INT,
    status VARCHAR(20) DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, CANCELLED
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (linked_document_id) REFERENCES documents(document_id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS renewal_steps (
    step_id INT AUTO_INCREMENT PRIMARY KEY,
    plan_id INT NOT NULL,
    step_order INT DEFAULT 1,
    step_title VARCHAR(200) NOT NULL,
    due_date DATE,
    required_document_name VARCHAR(150),
    is_completed BOOLEAN DEFAULT FALSE,
    completion_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (plan_id) REFERENCES renewal_plans(plan_id) ON DELETE CASCADE
);

-- 7. Document Dependency Manager
CREATE TABLE IF NOT EXISTS dependency_templates (
    template_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    goal_name VARCHAR(150) NOT NULL, -- e.g. "Passport Application", "Job Onboarding", "Home Loan"
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dependency_requirements (
    requirement_id INT AUTO_INCREMENT PRIMARY KEY,
    template_id INT NOT NULL,
    required_category VARCHAR(50) NOT NULL,
    required_title VARCHAR(150) NOT NULL,
    min_validity_days INT DEFAULT 30,
    is_mandatory BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (template_id) REFERENCES dependency_templates(template_id) ON DELETE CASCADE
);

-- 8. History & Version Management (Audit Logs)
CREATE TABLE IF NOT EXISTS audit_history (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT DEFAULT 1,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    module VARCHAR(50) NOT NULL,
    entity_title VARCHAR(150) NOT NULL,
    action_type VARCHAR(50) NOT NULL, -- CREATED, UPDATED, RENEWED, REPAIRED, SNOOZED, COMPLETED, RESTORED
    old_value TEXT,
    new_value TEXT,
    details TEXT,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ====================================================================
-- Initial Seed Sample Data
-- ====================================================================

INSERT INTO users (user_id, username, password_hash, full_name, email, vault_pin, role)
VALUES (1, 'demo', 'admin123', 'Mugunthan Malig', 'malig@lifeledger.local', '1234', 'ADMIN')
ON DUPLICATE KEY UPDATE username=username;

-- Sample Documents
INSERT INTO documents (document_id, user_id, title, category, document_number, issuing_authority, issue_date, expiry_date, is_sensitive, file_path, notes) VALUES
(1, 1, 'International Passport', 'Identity', 'Z98234710', 'Regional Passport Office', '2019-05-10', '2029-05-09', TRUE, 'docs/passport.pdf', 'Primary travel and identity credential'),
(2, 1, 'Driver License (Class C)', 'Vehicle', 'DL-2022-8812', 'State Dept of Motor Vehicles', '2022-03-15', '2026-11-28', FALSE, 'docs/drivers_license.pdf', 'Expires in less than two weeks! Urgent renewal.'),
(3, 1, 'Comprehensive Auto Insurance', 'Vehicle', 'POL-AUTO-9921', 'Allied Mutual Insurance', '2025-12-01', '2026-12-01', FALSE, 'docs/auto_insurance.pdf', 'Covers collision, comprehensive & roadside assistance'),
(4, 1, 'University Degree Certificate', 'Education', 'DEG-BTECH-552', 'National Institute of Technology', '2024-06-20', NULL, FALSE, 'docs/degree_certificate.pdf', 'B.Tech in Computer Science & Engineering'),
(5, 1, 'Residential Apartment Lease', 'Property', 'LEASE-APT-402', 'Highland Realty Group', '2026-01-01', '2027-01-01', FALSE, 'docs/lease_agreement.pdf', 'Annual renewal required with 30-day notice'),
(6, 1, 'Health Insurance Card', 'Medical', 'HI-BLUE-77821', 'BlueShield Healthcare', '2026-01-01', '2026-12-31', TRUE, 'docs/health_card.pdf', 'Coverage for hospital inpatient and prescription drugs');

-- Sample Warranties
INSERT INTO warranties (warranty_id, user_id, product_name, brand_model, serial_number, purchase_date, warranty_months, warranty_expiry_date, seller_retailer, purchase_cost, receipt_path, coverage_type, notes) VALUES
(1, 1, 'MacBook Pro 16" M3 Max', 'Apple Inc.', 'C02DF08MQ05D', '2024-03-10', 36, '2027-03-10', 'Apple Store Online', 2899.00, 'receipts/apple_macbook.pdf', 'AppleCare+ Comprehensive', 'Includes accidental damage protection'),
(2, 1, 'Sony WH-1000XM5 Headphones', 'Sony Electronics', 'SN-XM5-99238', '2025-11-20', 12, '2026-11-20', 'Amazon Electronics', 398.00, 'receipts/sony_headphones.pdf', 'Standard Manufacturer', 'Expiring in 4 days! Check battery performance.'),
(3, 1, 'Samsung 55" OLED 4K Smart TV', 'Samsung Electronics', 'SAM-OLED-55-77', '2024-08-15', 24, '2026-08-15', 'BestBuy Retail', 1450.00, 'receipts/samsung_tv.pdf', 'Extended Protection', 'Warranty recently expired on August 15');

-- Sample Repairs
INSERT INTO repairs (repair_id, warranty_id, service_date, issue_description, service_center, repair_cost, is_covered, technician_notes) VALUES
(1, 1, '2025-06-12', 'Keyboard Spacebar Stickiness', 'Apple Genius Bar downtown', 0.00, TRUE, 'Replaced top case and battery under AppleCare+'),
(2, 2, '2026-04-10', 'Left ear cup audio balance distortion', 'Sony Authorized Care', 0.00, TRUE, 'Firmware updated and transducer replaced under warranty');

-- Sample Subscriptions
INSERT INTO subscriptions (subscription_id, user_id, service_name, category, cost, currency, billing_cycle, next_renewal_date, cancellation_notice_days, auto_renew, payment_method, status, notes) VALUES
(1, 1, 'Netflix Premium 4K', 'Entertainment', 22.99, 'USD', 'Monthly', '2026-11-22', 2, TRUE, 'Credit Card (ending 4088)', 'ACTIVE', 'Family streaming subscription'),
(2, 1, 'GitHub Copilot & Teams', 'Productivity', 19.00, 'USD', 'Monthly', '2026-11-30', 5, TRUE, 'Debit Card (ending 1290)', 'ACTIVE', 'Developer tools'),
(3, 1, 'Google One 2TB Cloud Storage', 'Cloud', 99.99, 'USD', 'Annually', '2026-12-15', 7, TRUE, 'PayPal', 'ACTIVE', 'Backups and Google Drive'),
(4, 1, 'Anytime Fitness Gym Membership', 'Health', 49.00, 'USD', 'Monthly', '2026-11-25', 10, FALSE, 'Direct Debit', 'ACTIVE', 'Requires 10-day cancellation notice at front desk');

-- Sample Reminders
INSERT INTO reminders (reminder_id, user_id, title, module, entity_id, due_date, urgency, status) VALUES
(1, 1, 'Driver License Expires in 12 Days', 'Document', 2, '2026-11-28', 'CRITICAL', 'PENDING'),
(2, 1, 'Sony WH-1000XM5 Warranty Expires in 4 Days', 'Warranty', 2, '2026-11-20', 'HIGH', 'PENDING'),
(3, 1, 'Netflix Premium Renewal Due', 'Subscription', 1, '2026-11-22', 'MEDIUM', 'PENDING'),
(4, 1, 'Schedule Auto Insurance Policy Review', 'Document', 3, '2026-11-25', 'LOW', 'PENDING');

-- Sample Renewal Plans
INSERT INTO renewal_plans (plan_id, user_id, title, target_deadline, category, linked_document_id, status, notes) VALUES
(1, 1, 'Driver License Renewal 2026', '2026-11-28', 'Vehicle', 2, 'IN_PROGRESS', 'Complete DMV vision test, online form, and pay renewal fee');

INSERT INTO renewal_steps (step_id, plan_id, step_order, step_title, due_date, required_document_name, is_completed, completion_date) VALUES
(1, 1, 1, 'Schedule DMV Vision Test Appointment', '2026-11-18', 'Appointment Slip', TRUE, '2026-11-15'),
(2, 1, 2, 'Download and Fill DL-44 Renewal Application', '2026-11-20', 'Driver License (Class C)', TRUE, '2026-11-16'),
(3, 1, 3, 'Pay State DMV Renewal Fee ($38)', '2026-11-24', 'Payment Receipt', FALSE, NULL),
(4, 1, 4, 'Collect Temporary License & Await Physical Card', '2026-11-28', 'Old License', FALSE, NULL);

-- Sample Dependency Templates & Requirements
INSERT INTO dependency_templates (template_id, user_id, goal_name, description) VALUES
(1, 1, 'Corporate Job Onboarding', 'Mandatory documents required by HR prior to candidate start date'),
(2, 1, 'Schengen Tourist Visa Application', 'Embassy checklist for European Schengen travel visa');

INSERT INTO dependency_requirements (requirement_id, template_id, required_category, required_title, min_validity_days, is_mandatory) VALUES
(1, 1, 'Identity', 'International Passport or Government ID', 30, TRUE),
(2, 1, 'Education', 'University Degree Certificate', 0, TRUE),
(3, 1, 'Medical', 'Health Insurance Card / Fitness Certificate', 30, FALSE),
(4, 2, 'Identity', 'International Passport (valid at least 6 months)', 180, TRUE),
(5, 2, 'Vehicle', 'Driver License (for international car rental)', 90, FALSE);

-- Sample Audit History Logs
INSERT INTO audit_history (log_id, user_id, timestamp, module, entity_title, action_type, old_value, new_value, details) VALUES
(1, 1, '2026-11-14 09:30:00', 'Document', 'Driver License (Class C)', 'CREATED', NULL, 'DL-2022-8812', 'Document added to vault'),
(2, 1, '2026-11-15 14:15:00', 'Warranty', 'Sony WH-1000XM5 Headphones', 'REPAIRED', 'Audio balance issue', 'Transducer replaced', 'Authorized warranty repair logged'),
(3, 1, '2026-11-16 02:00:00', 'RenewalPlan', 'Schedule DMV Vision Test Appointment', 'COMPLETED', 'Pending', 'Completed', 'Checklist task marked as completed');

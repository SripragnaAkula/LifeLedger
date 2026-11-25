-- ========================================================
-- LifeLedger Database Backup Export
-- User ID: 1 (Charishma)
-- Generated: 2026-11-23T00:55:13.5299237
-- ========================================================

-- Table: documents
INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (1, '1st sem Marks list', 'Education', '', '', '2025-01-10', NULL, 1);
INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (1, 'aadhar', 'Identity', '', '', '2024-06-05', NULL, 0);
INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (1, '2nd year marklist', 'Education', '', 'College', '2026-07-09', NULL, 1);
INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (1, 'caste', 'Other', '', '', '2024-11-07', '2028-11-10', 1);
INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (1, 'ID card', 'Education', '', 'College', '2024-06-13', NULL, 0);
INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (1, 'Income', 'Financial', '', '', '2024-01-04', '2026-11-30', 1);

-- Table: warranties
INSERT INTO warranties(user_id, product_name, brand, serial_no, purchase_date, warranty_months, warranty_expiry, purchase_price, seller) VALUES (1, 'phone', 'redmi', '', '2024-11-08', 24, '2026-11-08', 30000.0, '');
INSERT INTO warranties(user_id, product_name, brand, serial_no, purchase_date, warranty_months, warranty_expiry, purchase_price, seller) VALUES (1, 'laptop', 'HP', '', '2025-11-07', 24, '2027-11-07', 50000.0, '');
INSERT INTO warranties(user_id, product_name, brand, serial_no, purchase_date, warranty_months, warranty_expiry, purchase_price, seller) VALUES (1, 'pods', 'boult', '', '2025-02-13', 12, '2026-02-13', 3000.0, '');

-- Table: subscriptions
INSERT INTO subscriptions(user_id, service_name, cost, billing_cycle, renewal_date, status) VALUES (1, 'jiohotstar', 300.0, 'Monthly', '2026-12-01', 'Active');
INSERT INTO subscriptions(user_id, service_name, cost, billing_cycle, renewal_date, status) VALUES (1, 'Gemini', 40.0, 'Annual', '2027-01-01', 'Active');

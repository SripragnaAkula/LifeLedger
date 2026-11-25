package com.lifeledger;

import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Feature 10 — Secure Access & Database Backup / Recovery
 * Password management, MySQL database status, backup export, and backup recovery.
 */
public class SecureBackup {

    public BorderPane getView() {
        int uid = LifeLedgerApp.userId;

        // ── 1. Database Connection Status ────────────────────────────────────
        boolean connected = (DB.conn != null);
        Label dbStatus = new Label(connected ?
            "🟢 MySQL Database Connected: `lifeledger` (Host: localhost:3306, User: root)" :
            "🔴 MySQL Database Offline: Please check MySQL server and password in DB.java");
        dbStatus.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " +
            (connected ? "#15803d" : "#dc2626") + "; -fx-padding: 10 16; -fx-background-color: " +
            (connected ? "#dcfce7" : "#fee2e2") + "; -fx-background-radius: 8;");

        // ── 2. Password Security ─────────────────────────────────────────────
        Label pwTitle = UIHelper.subHeading("🔑  Change Password");
        PasswordField oldPw = UIHelper.pwField("Current Password");
        oldPw.setMaxWidth(260);
        PasswordField newPw = UIHelper.pwField("New Password");
        newPw.setMaxWidth(260);
        PasswordField confPw = UIHelper.pwField("Confirm New Password");
        confPw.setMaxWidth(260);
        Button updatePwBtn = UIHelper.btnAction("Update Password");

        HBox pwBox = UIHelper.toolbar(oldPw, newPw, confPw, updatePwBtn);

        updatePwBtn.setOnAction(e -> {
            if (DB.conn == null) {
                UIHelper.alert("No DB", "No database connection available.", Alert.AlertType.WARNING);
                return;
            }
            String op = oldPw.getText(), np = newPw.getText(), cp = confPw.getText();
            if (op.isEmpty() || np.isEmpty()) {
                UIHelper.alert("Missing Fields", "Please enter current and new passwords.", Alert.AlertType.WARNING);
                return;
            }
            if (!np.equals(cp)) {
                UIHelper.alert("Mismatch", "New password and confirmation do not match.", Alert.AlertType.WARNING);
                return;
            }
            try {
                ResultSet rs = DB.pQuery("SELECT COUNT(*) FROM users WHERE user_id=? AND password_hash=?", uid, op);
                if (rs == null || !rs.next() || rs.getInt(1) == 0) {
                    UIHelper.alert("Incorrect Password", "Current password does not match records.", Alert.AlertType.WARNING);
                    return;
                }
                DB.pUpdate("UPDATE users SET password_hash=? WHERE user_id=?", np, uid);
                UIHelper.logHistory("Security", "PASSWORD_UPDATE", "User updated account password");
                UIHelper.alert("Success", "Password updated successfully.", Alert.AlertType.INFORMATION);
                oldPw.clear(); newPw.clear(); confPw.clear();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ── 3. Database Backup & Recovery ────────────────────────────────────
        Label backupTitle = UIHelper.subHeading("💾  Database Backup & Recovery");
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px;");
        logArea.setPrefHeight(250);
        VBox.setVgrow(logArea, Priority.ALWAYS);

        Button exportAllBtn = UIHelper.btnAction("📥 Generate Full Backup (.sql)");
        Button exportTextBtn = UIHelper.btnWarn("📄 Export Records (Summary Text)");
        Button restoreBtn = UIHelper.btnAdd("🔄 Restore Records from File");

        HBox backupTools = UIHelper.toolbar(exportAllBtn, exportTextBtn, restoreBtn);

        // Export SQL
        exportAllBtn.setOnAction(e -> {
            if (DB.conn == null) {
                logArea.setText("[ERROR] Cannot export: Database connection is not available.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("-- ========================================================\n");
            sb.append("-- LifeLedger Database Backup Export\n");
            sb.append("-- User ID: ").append(uid).append(" (").append(LifeLedgerApp.userName).append(")\n");
            sb.append("-- Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
            sb.append("-- ========================================================\n\n");

            try {
                // Documents
                sb.append("-- Table: documents\n");
                ResultSet rsDocs = DB.pQuery("SELECT title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive FROM documents WHERE user_id=?", uid);
                while (rsDocs != null && rsDocs.next()) {
                    sb.append(String.format("INSERT INTO documents(user_id, title, category, doc_number, issuing_authority, issue_date, expiry_date, is_sensitive) VALUES (%d, '%s', '%s', '%s', '%s', %s, %s, %d);\n",
                        uid,
                        escape(rsDocs.getString(1)), escape(rsDocs.getString(2)), escape(rsDocs.getString(3)),
                        escape(rsDocs.getString(4)),
                        rsDocs.getDate(5) != null ? "'" + rsDocs.getDate(5) + "'" : "NULL",
                        rsDocs.getDate(6) != null ? "'" + rsDocs.getDate(6) + "'" : "NULL",
                        rsDocs.getInt(7)
                    ));
                }

                // Warranties
                sb.append("\n-- Table: warranties\n");
                ResultSet rsWarr = DB.pQuery("SELECT product_name, brand, serial_no, purchase_date, warranty_months, warranty_expiry, purchase_price, seller FROM warranties WHERE user_id=?", uid);
                while (rsWarr != null && rsWarr.next()) {
                    sb.append(String.format("INSERT INTO warranties(user_id, product_name, brand, serial_no, purchase_date, warranty_months, warranty_expiry, purchase_price, seller) VALUES (%d, '%s', '%s', '%s', '%s', %d, '%s', %s, '%s');\n",
                        uid,
                        escape(rsWarr.getString(1)), escape(rsWarr.getString(2)), escape(rsWarr.getString(3)),
                        rsWarr.getString(4), rsWarr.getInt(5), rsWarr.getString(6),
                        rsWarr.getObject(7) != null ? rsWarr.getDouble(7) : "0.00",
                        escape(rsWarr.getString(8))
                    ));
                }

                // Subscriptions
                sb.append("\n-- Table: subscriptions\n");
                ResultSet rsSub = DB.pQuery("SELECT service_name, cost, billing_cycle, renewal_date, status FROM subscriptions WHERE user_id=?", uid);
                while (rsSub != null && rsSub.next()) {
                    sb.append(String.format("INSERT INTO subscriptions(user_id, service_name, cost, billing_cycle, renewal_date, status) VALUES (%d, '%s', %s, '%s', %s, '%s');\n",
                        uid, escape(rsSub.getString(1)), rsSub.getDouble(2), rsSub.getString(3),
                        rsSub.getDate(4) != null ? "'" + rsSub.getDate(4) + "'" : "NULL",
                        rsSub.getString(5)
                    ));
                }

                // Save to file
                File backupFile = new File("backup_user_" + uid + ".sql");
                try (PrintWriter pw = new PrintWriter(backupFile)) {
                    pw.write(sb.toString());
                }

                logArea.setText(sb.toString());
                UIHelper.logHistory("Security", "BACKUP_EXPORT", "Exported database backup to " + backupFile.getAbsolutePath());
                UIHelper.alert("Backup Created", "Backup successfully saved to:\n" + backupFile.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                logArea.setText("[ERROR] " + ex.getMessage());
            }
        });

        // Export Text Summary
        exportTextBtn.setOnAction(e -> {
            if (DB.conn == null) {
                logArea.setText("[ERROR] Cannot export: Database connection is not available.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("========================================================================================\n");
            sb.append("                           LIFELEDGER PERSONAL RECORDS EXPORT\n");
            sb.append("========================================================================================\n");
            sb.append("User: ").append(LifeLedgerApp.userName).append("   Date: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))).append("\n\n");

            try {
                // Documents
                sb.append("--- DOCUMENTS ---\n");
                sb.append(String.format("%-30s %-15s %-20s %-12s\n", "Title", "Category", "Number", "Expiry"));
                sb.append("-".repeat(80)).append("\n");
                ResultSet r1 = DB.pQuery("SELECT title, category, doc_number, expiry_date FROM documents WHERE user_id=?", uid);
                while (r1 != null && r1.next()) {
                    sb.append(String.format("%-30s %-15s %-20s %-12s\n", r1.getString(1), r1.getString(2), r1.getString(3), r1.getString(4)));
                }

                // Warranties
                sb.append("\n--- WARRANTIES ---\n");
                sb.append(String.format("%-30s %-15s %-20s %-12s\n", "Product", "Brand", "Serial No", "Warranty Expiry"));
                sb.append("-".repeat(80)).append("\n");
                ResultSet r2 = DB.pQuery("SELECT product_name, brand, serial_no, warranty_expiry FROM warranties WHERE user_id=?", uid);
                while (r2 != null && r2.next()) {
                    sb.append(String.format("%-30s %-15s %-20s %-12s\n", r2.getString(1), r2.getString(2), r2.getString(3), r2.getString(4)));
                }

                // Subscriptions
                sb.append("\n--- SUBSCRIPTIONS ---\n");
                sb.append(String.format("%-30s %-12s %-15s %-12s\n", "Service", "Cost", "Billing", "Next Renewal"));
                sb.append("-".repeat(80)).append("\n");
                ResultSet r3 = DB.pQuery("SELECT service_name, cost, billing_cycle, renewal_date FROM subscriptions WHERE user_id=?", uid);
                while (r3 != null && r3.next()) {
                    sb.append(String.format("%-30s $%-11.2f %-15s %-12s\n", r3.getString(1), r3.getDouble(2), r3.getString(3), r3.getString(4)));
                }

                logArea.setText(sb.toString());
            } catch (Exception ex) {
                logArea.setText("[ERROR] " + ex.getMessage());
            }
        });

        // Restore Records
        restoreBtn.setOnAction(e -> {
            File backupFile = new File("backup_user_" + uid + ".sql");
            if (!backupFile.exists()) {
                UIHelper.alert("No Backup Found", "Could not find backup file: " + backupFile.getName() + " in project directory.\nPlease export first.", Alert.AlertType.WARNING);
                return;
            }
            if (!UIHelper.confirmDelete("Restore database from " + backupFile.getName())) {
                return;
            }
            try {
                List<String> lines = Files.readAllLines(backupFile.toPath());
                int count = 0;
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("INSERT INTO")) {
                        DB.update(line);
                        count++;
                    }
                }
                logArea.setText("Recovery complete: " + count + " SQL statement(s) successfully executed from " + backupFile.getName());
                UIHelper.logHistory("Security", "RECOVERY", "Restored database from " + backupFile.getName());
                UIHelper.alert("Restoration Complete", count + " record(s) restored from backup.", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                logArea.setText("[RECOVERY ERROR] " + ex.getMessage());
            }
        });

        VBox center = new VBox(12,
            UIHelper.heading("🔒  Secure Access & Database Backup"),
            UIHelper.infoLabel("Manage authentication security, export SQL backups, and recover data to prevent loss."),
            dbStatus,
            new Separator(),
            pwTitle,
            pwBox,
            new Separator(),
            backupTitle,
            backupTools,
            logArea
        );
        VBox.setVgrow(logArea, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }
}

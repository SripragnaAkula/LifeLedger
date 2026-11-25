package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;

/**
 * Feature 1 — Dashboard
 * Shows real-time KPI summary cards and a table of items needing immediate attention.
 * Automatically refreshes with fresh MySQL data whenever navigated to.
 */
public class Dashboard {

    public BorderPane getView() {

        // ── Real-time KPI Calculations ────────────────────────────────────
        int totalDocs = 0, expiring = 0, expired = 0, activeWarr = 0, pendingRem = 0;
        // int totalSubs = 0;
        double monthlySpend = 0.0;
        int uid = LifeLedgerApp.userId;

        if (DB.conn != null) {
            totalDocs  = DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid);
            expiring   = DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid +
                         " AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)");
            expired    = DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid +
                         " AND expiry_date < CURDATE()");
            activeWarr = DB.count("SELECT COUNT(*) FROM warranties WHERE user_id=" + uid +
                         " AND warranty_expiry >= CURDATE()");
            pendingRem = DB.count("SELECT COUNT(*) FROM reminders WHERE user_id=" + uid +
                         " AND status='Pending'");
            // totalSubs  = DB.count("SELECT COUNT(*) FROM subscriptions WHERE user_id=" + uid);

            try {
                ResultSet rsSpend = DB.pQuery(
                    "SELECT COALESCE(SUM(CASE WHEN billing_cycle='Monthly' THEN cost " +
                    "                         WHEN billing_cycle='Annual' THEN cost/12 " +
                    "                         WHEN billing_cycle='Weekly' THEN cost * 4.33 " +
                    "                         ELSE cost END), 0) " +
                    "FROM subscriptions WHERE user_id=? AND status='Active'", uid);
                if (rsSpend != null && rsSpend.next()) {
                    monthlySpend = rsSpend.getDouble(1);
                }
            } catch (Exception ignored) {}
        }

        HBox kpiRow = new HBox(14,
            UIHelper.kpiCard("📄 Total Documents",   String.valueOf(totalDocs),  "#3b5bdb", "#eef2ff"),
            UIHelper.kpiCard("⚠ Expiring ≤30 Days",  String.valueOf(expiring),   "#d97706", "#fef3c7"),
            UIHelper.kpiCard("🔴 Expired Items",      String.valueOf(expired),    "#dc2626", "#fee2e2"),
            UIHelper.kpiCard("🛡 Active Warranties",  String.valueOf(activeWarr), "#0369a1", "#e0f2fe"),
            UIHelper.kpiCard("🔔 Pending Reminders",  String.valueOf(pendingRem), "#7c3aed", "#f3e8ff"),
            UIHelper.kpiCard("💳 Active Spend",       String.format("$%.2f/mo", monthlySpend), "#059669", "#dcfce7")
        );
        for (javafx.scene.Node n : kpiRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // ── Urgent items table ───────────────────────────────────────────
        Label urgLabel = UIHelper.subHeading("🚨  Urgent Deadlines & Attention Required (Expiring soon or overdue)");
        TableView<String[]> table = UIHelper.table("Title", "Category", "Expiry Date", "Days Remaining", "Priority");
        ObservableList<String[]> data = FXCollections.observableArrayList();

        if (DB.conn != null) {
            try {
                ResultSet rs = DB.pQuery(
                    "SELECT title, category, expiry_date, " +
                    "DATEDIFF(expiry_date, CURDATE()) AS days_left, " +
                    "CASE WHEN expiry_date < CURDATE() THEN 'OVERDUE' " +
                    "     WHEN DATEDIFF(expiry_date, CURDATE()) <= 7 THEN 'CRITICAL' " +
                    "     ELSE 'WARNING' END AS priority " +
                    "FROM documents WHERE user_id=? " +
                    "AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
                    "ORDER BY expiry_date LIMIT 25", uid);
                while (rs != null && rs.next())
                    data.add(new String[]{
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4) + (rs.getInt(4) < 0 ? " (Overdue)" : " days left"),
                        rs.getString(5)
                    });
            } catch (Exception e) { e.printStackTrace(); }
        }
        table.setItems(data);

        // ── Layout ───────────────────────────────────────────────────────
        VBox center = new VBox(14,
            UIHelper.heading("📊  Dashboard Analytics"),
            UIHelper.infoLabel("Live summary of your personal documents, warranties, subscriptions, and urgent deadlines."),
            kpiRow,
            urgLabel,
            table
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }
}

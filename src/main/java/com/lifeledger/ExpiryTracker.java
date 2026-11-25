package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.time.LocalDate;

/**
 * Feature 3 — Expiry and Renewal Tracker
 * Automatically calculates expiry status and generates reminders.
 * Instant reactive filtering without manual "Apply Filter" button.
 */
public class ExpiryTracker {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private ComboBox<String> statusFilter;
    private TextField searchFld;
    private HBox cardsBox = new HBox(14);

    public BorderPane getView() {
        refreshCards();

        // ── Toolbar with instant reactive filters (no redundant "Apply" button) ──
        searchFld = UIHelper.field("🔍 Filter by document title…");
        searchFld.setPrefWidth(240);

        statusFilter = UIHelper.combo("All","Active","Expiring Soon","Expired");
        statusFilter.setValue("All");

        Button remindBtn = UIHelper.btnWarn("⚡ Auto-Generate Reminders");

        HBox toolbar = UIHelper.toolbar(
            searchFld,
            new Label("Status:"),
            statusFilter,
            UIHelper.spacer(),
            remindBtn
        );

        // ── Table ─────────────────────────────────────────────────────────
        table = UIHelper.table("Title","Category","Expiry Date","Days Remaining","Status","Priority");
        table.setItems(data);
        loadData();

        // ── Instant reactive listeners (updates immediately upon selection) ──
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> loadData());

        remindBtn.setOnAction(e -> autoGenerateReminders());

        // ── Layout ────────────────────────────────────────────────────────
        VBox center = new VBox(10,
            UIHelper.heading("⏰  Expiry & Renewal Tracker"),
            UIHelper.infoLabel("Track document validity. Selections update the table immediately in real time."),
            cardsBox,
            toolbar,
            table
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }

    private void refreshCards() {
        cardsBox.getChildren().clear();
        int uid = LifeLedgerApp.userId;
        int total    = DB.conn != null ? DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid + " AND expiry_date IS NOT NULL") : 0;
        int active   = DB.conn != null ? DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid + " AND expiry_date > DATE_ADD(CURDATE(), INTERVAL 30 DAY)") : 0;
        int expiring = DB.conn != null ? DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid + " AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)") : 0;
        int expired  = DB.conn != null ? DB.count("SELECT COUNT(*) FROM documents WHERE user_id=" + uid + " AND expiry_date < CURDATE()") : 0;

        cardsBox.getChildren().addAll(
            UIHelper.kpiCard("✅ Active",           String.valueOf(active),   "#15803d", "#dcfce7"),
            UIHelper.kpiCard("⚠ Expiring ≤30d",    String.valueOf(expiring), "#d97706", "#fef3c7"),
            UIHelper.kpiCard("🔴 Expired",           String.valueOf(expired),  "#dc2626", "#fee2e2"),
            UIHelper.kpiCard("📄 Total Tracked",     String.valueOf(total),    "#3b5bdb", "#eef2ff")
        );
        for (javafx.scene.Node n : cardsBox.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    }

    private void loadData() {
        data.clear();
        if (DB.conn == null) return;
        int uid = LifeLedgerApp.userId;
        String sf = statusFilter.getValue() != null ? statusFilter.getValue() : "All";
        String kw = searchFld.getText() != null ? searchFld.getText().trim() : "";

        String sql =
            "SELECT title, category, expiry_date, " +
            "DATEDIFF(expiry_date, CURDATE()) AS days_left, " +
            "CASE WHEN expiry_date < CURDATE() THEN 'Expired' " +
            "     WHEN expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 'Expiring Soon' " +
            "     ELSE 'Active' END AS status, " +
            "CASE WHEN expiry_date < CURDATE() THEN 'HIGH' " +
            "     WHEN DATEDIFF(expiry_date, CURDATE()) <= 7 THEN 'CRITICAL' " +
            "     WHEN DATEDIFF(expiry_date, CURDATE()) <= 30 THEN 'MEDIUM' " +
            "     ELSE 'LOW' END AS priority " +
            "FROM documents WHERE user_id=" + uid + " AND expiry_date IS NOT NULL";

        if ("Expiring Soon".equals(sf))
            sql += " AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)";
        else if ("Expired".equals(sf))
            sql += " AND expiry_date < CURDATE()";
        else if ("Active".equals(sf))
            sql += " AND expiry_date > DATE_ADD(CURDATE(), INTERVAL 30 DAY)";

        if (!kw.isEmpty())
            sql += " AND title LIKE '%" + kw + "%'";

        sql += " ORDER BY expiry_date";

        try {
            ResultSet rs = DB.query(sql);
            while (rs != null && rs.next())
                data.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)
                });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void autoGenerateReminders() {
        if (DB.conn == null) { UIHelper.alert("No DB", "No database connection.", Alert.AlertType.WARNING); return; }
        int count = 0;
        try {
            ResultSet rs = DB.pQuery(
                "SELECT doc_id, title, expiry_date FROM documents WHERE user_id=? " +
                "AND expiry_date IS NOT NULL AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 60 DAY)",
                LifeLedgerApp.userId);
            while (rs != null && rs.next()) {
                int docId = rs.getInt("doc_id");
                String docTitle = rs.getString("title");
                String expiry   = rs.getString("expiry_date");
                ResultSet chk = DB.pQuery(
                    "SELECT COUNT(*) FROM reminders WHERE user_id=? AND ref_id=? AND status='Pending'",
                    LifeLedgerApp.userId, docId);
                if (chk != null && chk.next() && chk.getInt(1) == 0) {
                    String urgency = LocalDate.parse(expiry).isBefore(LocalDate.now().plusDays(7))
                        ? "CRITICAL" : "HIGH";
                    DB.pUpdate(
                        "INSERT INTO reminders(user_id,title,category,due_date,ref_id,status,urgency) VALUES(?,?,?,?,?,?,?)",
                        LifeLedgerApp.userId, "Renew: " + docTitle, "Document", expiry, docId, "Pending", urgency);
                    count++;
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
        refreshCards();
        loadData();
        UIHelper.alert("Done", count + " new reminder(s) generated for expiring documents.", Alert.AlertType.INFORMATION);
    }
}

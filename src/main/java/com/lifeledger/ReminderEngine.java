package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.util.Optional;

/**
 * Feature 6 — Smart Reminder Engine
 * Displays all reminders with urgency; supports add, done, snooze, delete.
 * Immediately refreshes upon status changes or filtering.
 */
public class ReminderEngine {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private ComboBox<String> statusFilter;
    private TextField searchFld;

    public BorderPane getView() {

        // ── Toolbar with instant reactive filters ─────────────────────────
        searchFld = UIHelper.field("🔍 Search reminders…");
        searchFld.setPrefWidth(220);

        statusFilter = UIHelper.combo("All","Pending","Completed","Snoozed");
        statusFilter.setValue("Pending");

        Button addBtn    = UIHelper.btnAdd("+ Add Reminder");
        Button doneBtn   = UIHelper.btnAction("✔ Mark Done");
        Button snoozeBtn = UIHelper.btnWarn("⏰ Snooze 7d");
        Button delBtn    = UIHelper.btnDelete("Delete");

        HBox toolbar = UIHelper.toolbar(
            searchFld,
            new Label("Status:"),
            statusFilter,
            UIHelper.spacer(),
            addBtn,
            doneBtn,
            snoozeBtn,
            delBtn
        );

        // ── Table ─────────────────────────────────────────────────────────
        table = UIHelper.table("ID","Title","Category","Due Date","Urgency","Status");
        table.setItems(data);
        loadData();

        // ── Instant reactive listeners ────────────────────────────────────
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> loadData());

        addBtn.setOnAction(e -> showAddDialog());

        doneBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) {
                UIHelper.alert("No Selection", "Please select a reminder to mark as completed.", Alert.AlertType.WARNING);
                return;
            }
            DB.update("UPDATE reminders SET status='Completed' WHERE rem_id=" + row[0]);
            UIHelper.logHistory("Reminder", "COMPLETED", "Marked completed: " + row[1]);
            loadData();
        });

        snoozeBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) {
                UIHelper.alert("No Selection", "Please select a reminder to snooze.", Alert.AlertType.WARNING);
                return;
            }
            DB.update("UPDATE reminders SET status='Snoozed', due_date=DATE_ADD(due_date, INTERVAL 7 DAY) WHERE rem_id=" + row[0]);
            UIHelper.logHistory("Reminder", "SNOOZED", "Snoozed 7 days: " + row[1]);
            loadData();
        });

        delBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) {
                UIHelper.alert("No Selection", "Please select a reminder to delete.", Alert.AlertType.WARNING);
                return;
            }
            if (UIHelper.confirmDelete(row[1])) {
                DB.update("DELETE FROM reminders WHERE rem_id=" + row[0]);
                UIHelper.logHistory("Reminder", "DELETED", "Deleted: " + row[1]);
                loadData();
            }
        });

        // ── Layout ────────────────────────────────────────────────────────
        VBox center = new VBox(10,
            UIHelper.heading("🔔  Smart Reminder Engine"),
            UIHelper.infoLabel("Prioritize and manage deadline reminders. Changes update immediately."),
            toolbar,
            table
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }

    private void loadData() {
        data.clear();
        if (DB.conn == null) return;
        int uid = LifeLedgerApp.userId;
        String sf = statusFilter != null && statusFilter.getValue() != null ? statusFilter.getValue() : "Pending";
        String kw = searchFld != null && searchFld.getText() != null ? searchFld.getText().trim() : "";

        String sql = "SELECT rem_id, title, category, due_date, urgency, status " +
            "FROM reminders WHERE user_id=" + uid;
        if (!"All".equals(sf)) {
            sql += " AND status='" + sf + "'";
        }
        if (!kw.isEmpty()) {
            sql += " AND title LIKE '%" + kw + "%'";
        }
        sql += " ORDER BY CASE urgency WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, due_date";

        try {
            ResultSet rs = DB.query(sql);
            while (rs != null && rs.next())
                data.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)
                });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddDialog() {
        TextField titleF = UIHelper.field("Reminder Title *");
        ComboBox<String> catF = UIHelper.combo("Document","Warranty","Subscription","General");
        catF.setValue("Document");
        DatePicker dueDP = UIHelper.datePicker();
        ComboBox<String> urgF = UIHelper.combo("LOW","MEDIUM","HIGH","CRITICAL");
        urgF.setValue("MEDIUM");

        VBox form = UIHelper.formBox(
            UIHelper.fieldLabel("Title *"), titleF,
            UIHelper.fieldLabel("Category"), catF,
            UIHelper.fieldLabel("Due Date"), dueDP,
            UIHelper.fieldLabel("Urgency"), urgF
        );

        Optional<ButtonType> result = UIHelper.showDialog("Add Reminder", form);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (titleF.getText().trim().isEmpty()) {
                UIHelper.alert("Missing", "Title is required.", Alert.AlertType.WARNING);
                return;
            }
            DB.pUpdate(
                "INSERT INTO reminders(user_id,title,category,due_date,urgency,status) VALUES(?,?,?,?,?,?)",
                LifeLedgerApp.userId, titleF.getText().trim(), catF.getValue(),
                dueDP.getValue() != null ? dueDP.getValue().toString() : null,
                urgF.getValue(), "Pending"
            );
            UIHelper.logHistory("Reminder", "ADDED", "Added: " + titleF.getText().trim());
            loadData();
        }
    }
}

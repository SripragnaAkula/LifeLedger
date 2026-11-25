package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.util.Optional;

/**
 * Feature 5 — Subscription and Recurring-Payment Tracker
 * Records subscriptions with auto monthly/annual cost summary.
 * Instant filtering and real-time expense calculations.
 */
public class SubscriptionTracker {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private Label spendLabel = new Label();
    private TextField searchFld;
    private ComboBox<String> statusFilter;

    public BorderPane getView() {

        // ── Toolbar ───────────────────────────────────────────────────────
        searchFld = UIHelper.field("🔍 Search subscriptions…");
        searchFld.setPrefWidth(220);

        statusFilter = UIHelper.combo("All", "Active", "Paused", "Cancelled");
        statusFilter.setValue("All");

        Button addBtn = UIHelper.btnAdd("+ Add Subscription");
        Button delBtn = UIHelper.btnDelete("Delete Selected");

        HBox toolbar = UIHelper.toolbar(
            searchFld,
            new Label("Status:"),
            statusFilter,
            UIHelper.spacer(),
            addBtn,
            delBtn
        );

        // ── Table ─────────────────────────────────────────────────────────
        table = UIHelper.table("ID","Service Name","Cost ($)","Billing Cycle","Renewal Date","Status","Days to Renewal");
        table.setItems(data);
        loadData();

        spendLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #059669; " +
            "-fx-background-color: #dcfce7; -fx-padding: 8 14; -fx-background-radius: 8;");

        // ── Instant reactive listeners ────────────────────────────────────
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> loadData());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());

        addBtn.setOnAction(e -> showAddDialog());
        delBtn.setOnAction(e -> deleteSelected());

        // ── Layout ────────────────────────────────────────────────────────
        VBox center = new VBox(10,
            UIHelper.heading("💳  Subscription & Recurring-Payment Tracker"),
            UIHelper.infoLabel("Track recurring subscriptions. Monthly and annual expenses are calculated automatically."),
            spendLabel,
            toolbar,
            table
        );
        VBox.setVgrow(table, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }

    private void loadData() {
        data.clear();
        double monthly = 0.0;
        if (DB.conn != null) {
            try {
                // Real-time spend summary
                ResultSet rs2 = DB.pQuery(
                    "SELECT COALESCE(SUM(CASE WHEN billing_cycle='Monthly' THEN cost " +
                    "                         WHEN billing_cycle='Annual' THEN cost/12 " +
                    "                         WHEN billing_cycle='Weekly' THEN cost * 4.33 " +
                    "                         ELSE cost END), 0) " +
                    "FROM subscriptions WHERE user_id=? AND status='Active'", LifeLedgerApp.userId);
                if (rs2 != null && rs2.next()) {
                    monthly = rs2.getDouble(1);
                }

                // Table rows with instant search & filter
                String kw = searchFld != null && searchFld.getText() != null ? searchFld.getText().trim() : "";
                String sf = statusFilter != null && statusFilter.getValue() != null ? statusFilter.getValue() : "All";

                String sql = "SELECT sub_id, service_name, cost, billing_cycle, renewal_date, status, " +
                    "DATEDIFF(renewal_date, CURDATE()) FROM subscriptions WHERE user_id=" + LifeLedgerApp.userId;
                if (!"All".equals(sf)) {
                    sql += " AND status='" + sf + "'";
                }
                if (!kw.isEmpty()) {
                    sql += " AND service_name LIKE '%" + kw + "%'";
                }
                sql += " ORDER BY renewal_date";

                ResultSet rs = DB.query(sql);
                while (rs != null && rs.next())
                    data.add(new String[]{
                        rs.getString(1), rs.getString(2), String.format("$%.2f", rs.getDouble(3)),
                        rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7) != null ? rs.getString(7) + " days" : "—"
                    });
            } catch (Exception e) { e.printStackTrace(); }
        }
        spendLabel.setText(String.format("💰  Total Active Monthly Spend: $%.2f   |   Annual Estimate: $%.2f", monthly, monthly * 12));
    }

    private void showAddDialog() {
        TextField serviceF = UIHelper.field("Service Name *");
        TextField costF    = UIHelper.field("Cost (e.g. 9.99) *");
        ComboBox<String> cycleF = UIHelper.combo("Monthly","Annual","Weekly","One-time");
        cycleF.setValue("Monthly");
        DatePicker renewalDP = UIHelper.datePicker();
        ComboBox<String> statusF = UIHelper.combo("Active","Paused","Cancelled");
        statusF.setValue("Active");

        VBox form = UIHelper.formBox(
            UIHelper.fieldLabel("Service Name *"), serviceF,
            UIHelper.fieldLabel("Cost ($) *"), costF,
            UIHelper.fieldLabel("Billing Cycle *"), cycleF,
            UIHelper.fieldLabel("Next Renewal Date"), renewalDP,
            UIHelper.fieldLabel("Status"), statusF
        );

        Optional<ButtonType> result = UIHelper.showDialog("Add Subscription", form);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (serviceF.getText().trim().isEmpty() || costF.getText().trim().isEmpty()) {
                UIHelper.alert("Missing Info", "Service and Cost are required.", Alert.AlertType.WARNING);
                return;
            }
            try {
                double costVal = Double.parseDouble(costF.getText().trim().replace("$", ""));
                boolean ok = DB.pUpdate(
                    "INSERT INTO subscriptions(user_id,service_name,cost,billing_cycle,renewal_date,status) VALUES(?,?,?,?,?,?)",
                    LifeLedgerApp.userId, serviceF.getText().trim(), costVal,
                    cycleF.getValue(),
                    renewalDP.getValue() != null ? renewalDP.getValue().toString() : null,
                    statusF.getValue()
                );
                if (ok) {
                    UIHelper.logHistory("Subscription", "ADDED", "Added: " + serviceF.getText().trim() + " ($" + costVal + ")");
                    loadData();
                } else {
                    UIHelper.alert("Error", "Could not save subscription.", Alert.AlertType.ERROR);
                }
            } catch (NumberFormatException ex) {
                UIHelper.alert("Invalid Input", "Cost must be a valid number.", Alert.AlertType.WARNING);
            }
        }
    }

    private void deleteSelected() {
        String[] row = table.getSelectionModel().getSelectedItem();
        if (row == null) { UIHelper.alert("No Selection", "Select a row first.", Alert.AlertType.WARNING); return; }
        if (UIHelper.confirmDelete(row[1])) {
            DB.update("DELETE FROM subscriptions WHERE sub_id=" + row[0]);
            UIHelper.logHistory("Subscription", "DELETED", "Deleted: " + row[1]);
            loadData();
        }
    }
}

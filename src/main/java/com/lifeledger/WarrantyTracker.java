package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Feature 4 — Warranty and Purchase Tracker
 * Store product warranties; auto-calculate warranty expiry from purchase date + months.
 * Instant reactive search and status filtering.
 */
public class WarrantyTracker {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private TextField searchFld;
    private ComboBox<String> statusFilter;
    private HBox cardsBox = new HBox(14);

    public BorderPane getView() {
        refreshCards();

        // ── Toolbar with instant reactive filter ──────────────────────────
        searchFld = UIHelper.field("🔍 Search product, brand, or serial…");
        searchFld.setPrefWidth(260);

        statusFilter = UIHelper.combo("All", "Under Warranty", "Expiring Soon", "Expired");
        statusFilter.setValue("All");

        Button addBtn = UIHelper.btnAdd("+ Add Warranty");
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
        table = UIHelper.table("ID","Product","Brand","Serial No.","Purchase Date","Warranty (Months)","Expiry Date","Status");
        table.setItems(data);
        loadData();

        // ── Instant reactive listeners ────────────────────────────────────
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> loadData());
        statusFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());

        addBtn.setOnAction(e -> showAddDialog());
        delBtn.setOnAction(e -> deleteSelected());

        // ── Layout ────────────────────────────────────────────────────────
        VBox center = new VBox(10,
            UIHelper.heading("🛡  Warranty & Purchase Tracker"),
            UIHelper.infoLabel("Track product warranties. Status and searches update immediately as you type."),
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
        int active   = DB.conn != null ? DB.count("SELECT COUNT(*) FROM warranties WHERE user_id=" + uid + " AND warranty_expiry >= CURDATE()") : 0;
        int expiring = DB.conn != null ? DB.count("SELECT COUNT(*) FROM warranties WHERE user_id=" + uid + " AND warranty_expiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)") : 0;
        int expired  = DB.conn != null ? DB.count("SELECT COUNT(*) FROM warranties WHERE user_id=" + uid + " AND warranty_expiry < CURDATE()") : 0;

        cardsBox.getChildren().addAll(
            UIHelper.kpiCard("🛡 Under Warranty",    String.valueOf(active),   "#15803d", "#dcfce7"),
            UIHelper.kpiCard("⚠ Expiring ≤30d",     String.valueOf(expiring), "#d97706", "#fef3c7"),
            UIHelper.kpiCard("❌ Expired Warranty",   String.valueOf(expired),  "#dc2626", "#fee2e2")
        );
        for (javafx.scene.Node n : cardsBox.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
    }

    private void loadData() {
        data.clear();
        if (DB.conn == null) return;
        int uid = LifeLedgerApp.userId;
        String kw = searchFld != null && searchFld.getText() != null ? searchFld.getText().trim() : "";
        String sf = statusFilter != null && statusFilter.getValue() != null ? statusFilter.getValue() : "All";

        String sql = "SELECT w_id, product_name, brand, serial_no, purchase_date, warranty_months, warranty_expiry, " +
            "CASE WHEN warranty_expiry < CURDATE() THEN 'Expired' " +
            "     WHEN warranty_expiry <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 'Expiring Soon' " +
            "     ELSE 'Under Warranty' END AS status " +
            "FROM warranties WHERE user_id=" + uid;

        if ("Under Warranty".equals(sf)) {
            sql += " AND warranty_expiry >= CURDATE()";
        } else if ("Expiring Soon".equals(sf)) {
            sql += " AND warranty_expiry BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)";
        } else if ("Expired".equals(sf)) {
            sql += " AND warranty_expiry < CURDATE()";
        }

        if (!kw.isEmpty()) {
            sql += " AND (product_name LIKE '%" + kw + "%' OR brand LIKE '%" + kw + "%' OR serial_no LIKE '%" + kw + "%')";
        }

        sql += " ORDER BY warranty_expiry";

        try {
            ResultSet rs = DB.query(sql);
            while (rs != null && rs.next())
                data.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)
                });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddDialog() {
        TextField prodF   = UIHelper.field("Product Name *");
        TextField brandF  = UIHelper.field("Brand");
        TextField serialF = UIHelper.field("Serial Number");
        TextField priceF  = UIHelper.field("Purchase Price");
        DatePicker purchaseDP = UIHelper.datePicker();
        TextField monthsF = UIHelper.field("Warranty Period (Months) *");
        TextField sellerF = UIHelper.field("Seller / Store");

        VBox form = UIHelper.formBox(
            UIHelper.fieldLabel("Product Name *"), prodF,
            UIHelper.fieldLabel("Brand"), brandF,
            UIHelper.fieldLabel("Serial Number"), serialF,
            UIHelper.fieldLabel("Purchase Date"), purchaseDP,
            UIHelper.fieldLabel("Warranty Months *"), monthsF,
            UIHelper.fieldLabel("Purchase Price ($)"), priceF,
            UIHelper.fieldLabel("Seller"), sellerF
        );

        Optional<ButtonType> result = UIHelper.showDialog("Add Warranty Record", form);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (prodF.getText().trim().isEmpty() || monthsF.getText().trim().isEmpty()) {
                UIHelper.alert("Missing Info", "Product name and warranty months are required.", Alert.AlertType.WARNING);
                return;
            }
            try {
                LocalDate purchase = purchaseDP.getValue() != null ? purchaseDP.getValue() : LocalDate.now();
                LocalDate expiry   = purchase.plusMonths(Integer.parseInt(monthsF.getText().trim()));

                boolean ok = DB.pUpdate(
                    "INSERT INTO warranties(user_id,product_name,brand,serial_no,purchase_date,warranty_months,warranty_expiry,purchase_price,seller) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)",
                    LifeLedgerApp.userId, prodF.getText().trim(), brandF.getText().trim(),
                    serialF.getText().trim(), purchase.toString(), monthsF.getText().trim(),
                    expiry.toString(), priceF.getText().trim(), sellerF.getText().trim()
                );
                if (ok) {
                    UIHelper.logHistory("Warranty", "ADDED", "Added: " + prodF.getText().trim());
                    refreshCards();
                    loadData();
                    UIHelper.alert("Saved", "Warranty record added. Expiry: " + expiry, Alert.AlertType.INFORMATION);
                }
            } catch (NumberFormatException ex) {
                UIHelper.alert("Invalid Input", "Warranty months must be a number.", Alert.AlertType.WARNING);
            }
        }
    }

    private void deleteSelected() {
        String[] row = table.getSelectionModel().getSelectedItem();
        if (row == null) { UIHelper.alert("No Selection", "Select a row first.", Alert.AlertType.WARNING); return; }
        if (UIHelper.confirmDelete(row[1])) {
            DB.update("DELETE FROM warranties WHERE w_id=" + row[0]);
            UIHelper.logHistory("Warranty", "DELETED", "Deleted: " + row[1]);
            refreshCards();
            loadData();
        }
    }
}

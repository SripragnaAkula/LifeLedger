package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;

/**
 * Feature 9 — History and Version Management
 * Complete audit trail of user actions, status changes, and renewals.
 * Immediately updates upon search or module selection.
 */
public class HistoryLog {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private ComboBox<String> moduleFilter;
    private TextField searchFld;

    public BorderPane getView() {
        searchFld = UIHelper.field("🔍 Search activity logs…");
        searchFld.setPrefWidth(260);

        moduleFilter = UIHelper.combo("All", "Login", "Document", "Warranty", "Subscription", "Reminder", "Planner", "Security");
        moduleFilter.setValue("All");

        Button clearBtn = UIHelper.btnDelete("Clear History");

        HBox toolbar = UIHelper.toolbar(
            searchFld,
            new Label("Module:"),
            moduleFilter,
            UIHelper.spacer(),
            clearBtn
        );

        table = UIHelper.table("Log ID", "Module", "Action", "Activity Details", "Timestamp");
        table.setItems(data);
        loadData();

        // ── Real-time instant listeners (no manual "Filter" button needed) ──
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> loadData());
        moduleFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());

        clearBtn.setOnAction(e -> {
            if (UIHelper.confirmDelete("all activity history logs")) {
                DB.pUpdate("DELETE FROM history WHERE user_id=?", LifeLedgerApp.userId);
                loadData();
            }
        });

        VBox center = new VBox(10,
            UIHelper.heading("📜  History & Version Management"),
            UIHelper.infoLabel("Track history of renewals, added items, status updates, and modifications across all modules."),
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
        String mod = moduleFilter.getValue() != null ? moduleFilter.getValue() : "All";
        String kw = searchFld.getText() != null ? searchFld.getText().trim() : "";

        String sql = "SELECT hist_id, module, action, detail, created_at FROM history WHERE user_id=" + uid;
        if (!"All".equals(mod)) {
            sql += " AND module='" + mod + "'";
        }
        if (!kw.isEmpty()) {
            sql += " AND (action LIKE '%" + kw + "%' OR detail LIKE '%" + kw + "%')";
        }
        sql += " ORDER BY created_at DESC LIMIT 300";

        try {
            ResultSet rs = DB.query(sql);
            while (rs != null && rs.next()) {
                data.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5)
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

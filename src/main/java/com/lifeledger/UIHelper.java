package com.lifeledger;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Optional;

/**
 * UIHelper — Shared widget factory used by all 10 feature classes.
 * Keeps each feature class clean and focused on its own logic.
 */
public class UIHelper {

    // ── Layout Helpers ────────────────────────────────────────────────────

    /**
     * Returns a BorderPane that fills the entire tab area.
     * top = toolbar, center = main content (usually a table).
     */
    public static BorderPane page(Node toolbar, Node center) {
        BorderPane bp = new BorderPane();
        bp.setStyle("-fx-background-color: #f8fafc;");
        bp.setPadding(new Insets(20, 24, 20, 24));
        if (toolbar != null) {
            BorderPane.setMargin(toolbar, new Insets(0, 0, 10, 0));
            bp.setTop(toolbar);
        }
        bp.setCenter(center);
        return bp;
    }

    /** Section heading label */
    public static Label heading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        return l;
    }

    /** Sub-heading label */
    public static Label subHeading(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 10 0 4 0;");
        return l;
    }

    /** Small info label */
    public static Label infoLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        return l;
    }

    /** KPI stat card */
    public static VBox kpiCard(String title, String value, String fg, String bg) {
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: " + fg + ";");
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: 600;");
        VBox card = new VBox(4, titleLbl, valLbl);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: " + bg + "; -fx-border-radius: 10; -fx-background-radius: 10;" +
            "-fx-border-color: #e2e8f0; -fx-padding: 14; -fx-min-width: 130;");
        return card;
    }

    /** Horizontal toolbar container */
    public static HBox toolbar(Node... nodes) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(nodes);
        return bar;
    }

    /** Spacer that pushes items to the right */
    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ── Buttons ───────────────────────────────────────────────────────────

    public static Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-font-size: 12px; -fx-padding: 7 15; -fx-background-radius: 7; -fx-cursor: hand;");
        return b;
    }

    public static Button btnAdd(String label)    { return btn(label, "#16a34a"); }
    public static Button btnDelete(String label) { return btn(label, "#dc2626"); }
    public static Button btnAction(String label) { return btn(label, "#3b5bdb"); }
    public static Button btnWarn(String label)   { return btn(label, "#d97706"); }

    // ── Form Controls ─────────────────────────────────────────────────────

    public static TextField field(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setStyle("-fx-padding: 8 12; -fx-font-size: 12px; -fx-border-color: #cbd5e1; " +
            "-fx-border-radius: 6; -fx-background-radius: 6;");
        return f;
    }

    public static PasswordField pwField(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt);
        f.setStyle("-fx-padding: 8 12; -fx-font-size: 12px; -fx-border-color: #cbd5e1; " +
            "-fx-border-radius: 6; -fx-background-radius: 6;");
        return f;
    }

    public static DatePicker datePicker() {
        DatePicker dp = new DatePicker();
        dp.setStyle("-fx-font-size: 12px;");
        return dp;
    }

    public static ComboBox<String> combo(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setStyle("-fx-font-size: 12px;");
        return cb;
    }

    public static Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #334155;");
        return l;
    }

    // ── Table ─────────────────────────────────────────────────────────────

    /**
     * Creates a TableView<String[]> with given column headers.
     * Columns are auto-sized; grows to fill available height.
     */
    public static TableView<String[]> table(String... columns) {
        TableView<String[]> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
        for (int i = 0; i < columns.length; i++) {
            final int idx = i;
            TableColumn<String[], String> col = new TableColumn<>(columns[i]);
            col.setCellValueFactory(cd -> {
                String[] row = cd.getValue();
                String val = (row != null && idx < row.length && row[idx] != null) ? row[idx] : "";
                return new javafx.beans.property.SimpleStringProperty(val);
            });
            tv.getColumns().add(col);
        }
        tv.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        VBox.setVgrow(tv, Priority.ALWAYS);
        return tv;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    /** Simple form dialog. Returns null if cancelled. */
    public static Optional<ButtonType> showDialog(String title, Node formContent) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(title);
        dlg.setHeaderText(null);
        dlg.getDialogPane().setContent(formContent);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        return dlg.showAndWait();
    }

    public static void alert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    public static boolean confirmDelete(String itemName) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirm Delete");
        a.setHeaderText("Delete \"" + itemName + "\"?");
        a.setContentText("This cannot be undone.");
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }

    /** Wraps nodes in a vertical form box for use inside dialogs */
    public static VBox formBox(Node... nodes) {
        VBox v = new VBox(7);
        v.setPadding(new Insets(10));
        v.setPrefWidth(380);
        v.getChildren().addAll(nodes);
        return v;
    }

    /** History log shortcut */
    public static void logHistory(String module, String action, String detail) {
        DB.pUpdate("INSERT INTO history(user_id,module,action,detail) VALUES(?,?,?,?)",
            LifeLedgerApp.userId, module, action, detail);
    }
}

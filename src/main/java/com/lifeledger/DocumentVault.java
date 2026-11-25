package com.lifeledger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.util.Optional;

/**
 * Feature 2 — Personal Document Vault
 * Add, search, filter, view and delete personal documents.
 * Includes complete file attachment capability and strict password-based access control
 * for sensitive / confidential documents.
 */
public class DocumentVault {

    private TableView<String[]> table;
    private ObservableList<String[]> data = FXCollections.observableArrayList();
    private TextField searchFld;
    private ComboBox<String> catFilter;

    public BorderPane getView() {

        // ── Toolbar with instant reactive filter & file actions ───────────
        searchFld = UIHelper.field("🔍 Search title, number, or authority…");
        searchFld.setPrefWidth(240);

        catFilter = UIHelper.combo("All","Identity","Education","Medical","Property","Financial","Vehicle","Legal","Other");
        catFilter.setValue("All");

        Button viewFileBtn   = UIHelper.btn("📂 Open Document", "#3b5bdb");
        Button attachFileBtn = UIHelper.btn("📎 Attach / Change File", "#0369a1");
        Button addBtn        = UIHelper.btnAdd("+ Add Document");
        Button delBtn        = UIHelper.btnDelete("Delete");

        HBox toolbar = UIHelper.toolbar(
            searchFld,
            new Label("Category:"),
            catFilter,
            UIHelper.spacer(),
            viewFileBtn,
            attachFileBtn,
            addBtn,
            delBtn
        );

        // ── Table ────────────────────────────────────────────────────────
        table = UIHelper.table(
            "ID", "Title", "Category", "Doc Number", "Issuing Authority",
            "Issue Date", "Expiry Date", "Status", "Attached Document"
        );
        table.setItems(data);
        loadData();

        // ── Instant reactive listeners ────────────────────────────────────
        searchFld.textProperty().addListener((obs, oldVal, newVal) -> loadData());
        catFilter.valueProperty().addListener((obs, oldVal, newVal) -> loadData());

        // Double click row to open attached document
        table.setRowFactory(tv -> {
            TableRow<String[]> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openAttachedFile(row.getItem());
                }
            });
            return row;
        });

        // Button handlers
        viewFileBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) {
                UIHelper.alert("No Selection", "Please select a document from the table first.", Alert.AlertType.WARNING);
                return;
            }
            openAttachedFile(row);
        });

        attachFileBtn.setOnAction(e -> {
            String[] row = table.getSelectionModel().getSelectedItem();
            if (row == null) {
                UIHelper.alert("No Selection", "Please select a document to attach a file to.", Alert.AlertType.WARNING);
                return;
            }
            attachFileToExisting(row);
        });

        addBtn.setOnAction(e -> showAddDialog());
        delBtn.setOnAction(e -> deleteSelected());

        // ── Layout ───────────────────────────────────────────────────────
        VBox center = new VBox(10,
            UIHelper.heading("📄  Personal Document Vault"),
            UIHelper.infoLabel("Store records and attach original files. Confidential documents (🔒) require your password to unlock and open."),
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
        String kw  = searchFld != null && searchFld.getText() != null ? searchFld.getText().trim() : "";
        String cat = catFilter != null && catFilter.getValue() != null ? catFilter.getValue() : "All";

        String sql = "SELECT doc_id, title, category, doc_number, issuing_authority, " +
            "issue_date, expiry_date, " +
            "CASE WHEN expiry_date IS NULL THEN 'No Expiry' " +
            "     WHEN expiry_date < CURDATE() THEN 'Expired' " +
            "     WHEN expiry_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 'Expiring Soon' " +
            "     ELSE 'Active' END AS status, " +
            "file_path, is_sensitive " +
            "FROM documents WHERE user_id=" + uid;
        if (!kw.isEmpty())
            sql += " AND (title LIKE '%" + kw + "%' OR doc_number LIKE '%" + kw + "%' OR issuing_authority LIKE '%" + kw + "%')";
        if (!"All".equals(cat))
            sql += " AND category='" + cat + "'";
        sql += " ORDER BY expiry_date";

        try {
            ResultSet rs = DB.query(sql);
            while (rs != null && rs.next()) {
                String rawTitle = rs.getString("title");
                boolean isSensitive = rs.getInt("is_sensitive") == 1;
                String displayTitle = isSensitive ? "🔒 " + rawTitle : rawTitle;

                String filePath = rs.getString("file_path");
                String displayFile = "— None —";
                if (filePath != null && !filePath.trim().isEmpty()) {
                    File f = new File(filePath);
                    displayFile = "📎 " + f.getName();
                }

                data.add(new String[]{
                    rs.getString("doc_id"),
                    displayTitle,
                    rs.getString("category"),
                    rs.getString("doc_number"),
                    rs.getString("issuing_authority"),
                    rs.getString("issue_date") != null ? rs.getString("issue_date") : "—",
                    rs.getString("expiry_date") != null ? rs.getString("expiry_date") : "Lifetime",
                    rs.getString("status"),
                    displayFile,
                    filePath != null ? filePath : "",
                    String.valueOf(rs.getInt("is_sensitive")),
                    rawTitle
                });
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddDialog() {
        TextField titleF = UIHelper.field("Document Title (e.g. Passport, Degree, Tax Return) *");
        ComboBox<String> catF = UIHelper.combo("Identity","Education","Medical","Property","Financial","Vehicle","Legal","Other");
        catF.setValue("Identity");
        TextField numF   = UIHelper.field("Document / Certificate Number");
        TextField authF  = UIHelper.field("Issuing Authority (e.g. Passport Office, DMV)");
        DatePicker issDP = UIHelper.datePicker();
        DatePicker expDP = UIHelper.datePicker();
        CheckBox sensitive = new CheckBox("🔒 Mark as Sensitive / Confidential (Requires password to open)");
        sensitive.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // File Attachment controls
        final File[] chosenFile = new File[1];
        Button chooseFileBtn = new Button("📎 Choose Document File...");
        chooseFileBtn.setStyle("-fx-background-color: #f1f5f9; -fx-border-color: #cbd5e1; -fx-border-radius: 6; " +
            "-fx-background-radius: 6; -fx-padding: 7 14; -fx-cursor: hand; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        Label fileStatusLbl = new Label("No file attached yet (Optional)");
        fileStatusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        Button removeFileBtn = new Button("✖");
        removeFileBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 0 4;");
        removeFileBtn.setVisible(false);

        HBox fileRow = new HBox(8, chooseFileBtn, fileStatusLbl, removeFileBtn);
        fileRow.setAlignment(Pos.CENTER_LEFT);

        chooseFileBtn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select Document File to Attach");
            fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Files (*.pdf, *.png, *.jpg, *.jpeg, *.doc, *.docx, *.txt)",
                    "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.doc", "*.docx", "*.txt"),
                new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Image Files (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Word Documents (*.doc, *.docx)", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
            );
            File f = fc.showOpenDialog(table.getScene().getWindow());
            if (f != null) {
                chosenFile[0] = f;
                long sizeKb = Math.max(1, f.length() / 1024);
                fileStatusLbl.setText("📎 " + f.getName() + " (" + sizeKb + " KB)");
                fileStatusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #15803d; -fx-font-weight: bold;");
                removeFileBtn.setVisible(true);
            }
        });

        removeFileBtn.setOnAction(e -> {
            chosenFile[0] = null;
            fileStatusLbl.setText("No file attached yet (Optional)");
            fileStatusLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            removeFileBtn.setVisible(false);
        });

        VBox form = UIHelper.formBox(
            UIHelper.fieldLabel("Document Title *"), titleF,
            UIHelper.fieldLabel("Category *"), catF,
            UIHelper.fieldLabel("Document Number"), numF,
            UIHelper.fieldLabel("Issuing Authority"), authF,
            UIHelper.fieldLabel("Issue Date"), issDP,
            UIHelper.fieldLabel("Expiry Date"), expDP,
            sensitive,
            new Separator(),
            UIHelper.fieldLabel("Attach Original Document / File (PDF, Image, Doc):"),
            fileRow
        );

        Optional<ButtonType> result = UIHelper.showDialog("Add New Document & Attach File", form);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String title = titleF.getText().trim();
            if (title.isEmpty() || catF.getValue() == null) {
                UIHelper.alert("Missing Info", "Document Title and Category are required.", Alert.AlertType.WARNING);
                return;
            }

            // Copy file to vault storage if one was selected
            String savedFilePath = null;
            if (chosenFile[0] != null) {
                savedFilePath = saveFileToVault(chosenFile[0]);
            }

            boolean ok = DB.pUpdate(
                "INSERT INTO documents(user_id,title,category,doc_number,issuing_authority,issue_date,expiry_date,is_sensitive,file_path) " +
                "VALUES(?,?,?,?,?,?,?,?,?)",
                LifeLedgerApp.userId, title, catF.getValue(),
                numF.getText().trim(), authF.getText().trim(),
                issDP.getValue() != null ? issDP.getValue().toString() : null,
                expDP.getValue() != null ? expDP.getValue().toString() : null,
                sensitive.isSelected() ? 1 : 0,
                savedFilePath
            );

            if (ok) {
                String detail = "Added: " + title + (sensitive.isSelected() ? " [CONFIDENTIAL]" : "") + (savedFilePath != null ? " with attachment" : "");
                UIHelper.logHistory("Document", "ADDED", detail);
                loadData();
                UIHelper.alert("Saved", "Document successfully stored in vault!" +
                    (sensitive.isSelected() ? "\n🔒 Document is password-protected." : "") +
                    (savedFilePath != null ? "\nAttachment saved: " + chosenFile[0].getName() : ""),
                    Alert.AlertType.INFORMATION);
            } else {
                UIHelper.alert("Error", "Could not save to database. Please check connection.", Alert.AlertType.ERROR);
            }
        }
    }

    private void attachFileToExisting(String[] row) {
        String cleanTitle = row.length > 11 ? row[11] : row[1];
        String isSensitive = row.length > 10 ? row[10] : "0";

        // Security check for sensitive document before modifying attachment
        if ("1".equals(isSensitive)) {
            if (!verifyUserPassword("'" + cleanTitle + "' is marked as sensitive/confidential.\nPlease confirm your account password before attaching or replacing its file.")) {
                return;
            }
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Select Document File to Attach to " + cleanTitle);
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Supported Files (*.pdf, *.png, *.jpg, *.jpeg, *.doc, *.docx, *.txt)",
                "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.doc", "*.docx", "*.txt"),
            new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"),
            new FileChooser.ExtensionFilter("All Files (*.*)", "*.*")
        );

        File f = fc.showOpenDialog(table.getScene().getWindow());
        if (f != null) {
            String savedPath = saveFileToVault(f);
            if (savedPath != null) {
                boolean ok = DB.pUpdate("UPDATE documents SET file_path=? WHERE doc_id=?", savedPath, row[0]);
                if (ok) {
                    UIHelper.logHistory("Document", "ATTACHED_FILE", "Attached file " + f.getName() + " to " + cleanTitle);
                    loadData();
                    UIHelper.alert("File Attached", "Successfully attached " + f.getName() + " to " + cleanTitle, Alert.AlertType.INFORMATION);
                }
            }
        }
    }

    private void openAttachedFile(String[] row) {
        if (row == null) return;
        String cleanTitle = row.length > 11 ? row[11] : row[1];
        String filePath = row.length > 9 ? row[9] : "";
        String isSensitive = row.length > 10 ? row[10] : "0";

        if (filePath == null || filePath.trim().isEmpty()) {
            UIHelper.alert("No File Attached",
                "There is no file attached to '" + cleanTitle + "'.\nClick 'Attach / Change File' to link your scan or document.",
                Alert.AlertType.INFORMATION);
            return;
        }

        // Strict security verification: If marked as sensitive, require account password
        if ("1".equals(isSensitive)) {
            boolean authorized = verifyUserPassword("🔒 '" + cleanTitle + "' is marked as Sensitive / Confidential.\n\nPlease enter your account password to verify your identity and view this document:");
            if (!authorized) {
                return; // Password incorrect or cancelled; do NOT open
            }
        }

        File file = new File(filePath);
        if (!file.exists()) {
            UIHelper.alert("File Not Found",
                "The attached document could not be located at:\n" + file.getAbsolutePath() +
                "\n\nThe file may have been moved. You can attach it again using 'Attach / Change File'.",
                Alert.AlertType.ERROR);
            return;
        }

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
            } else {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
            }
            UIHelper.logHistory("Document", "VIEWED", "Opened document: " + cleanTitle);
        } catch (Exception ex) {
            try {
                new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath()).start();
                UIHelper.logHistory("Document", "VIEWED", "Opened document: " + cleanTitle);
            } catch (Exception e2) {
                UIHelper.alert("Error Opening File", "Could not open file: " + e2.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Prompts the user to enter their account password to confirm identity.
     * Validates against the MySQL users table.
     */
    private boolean verifyUserPassword(String promptMessage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Security Verification");
        dialog.setHeaderText("🔒 Confidential Access Verification");

        PasswordField pwField = new PasswordField();
        pwField.setPromptText("Enter your account password");
        pwField.setStyle("-fx-padding: 8 12; -fx-font-size: 13px; -fx-border-color: #cbd5e1; -fx-border-radius: 6;");

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));
        content.setPrefWidth(380);

        Label desc = new Label(promptMessage != null ? promptMessage : "Please enter your account password to confirm access.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #334155; -fx-line-spacing: 2px;");

        Label hintLbl = new Label("Required for documents marked as sensitive or confidential.");
        hintLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b; -fx-font-style: italic;");

        content.getChildren().addAll(desc, pwField, hintLbl);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Confirm & Unlock");
        okBtn.setStyle("-fx-background-color: #3b5bdb; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Platform.runLater(pwField::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                return pwField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().isEmpty()) {
            return false;
        }

        try {
            ResultSet rs = DB.pQuery("SELECT COUNT(*) FROM users WHERE user_id=? AND password_hash=?",
                LifeLedgerApp.userId, result.get());
            if (rs != null && rs.next() && rs.getInt(1) > 0) {
                return true; // Password matched
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        UIHelper.alert("Access Denied", "Incorrect password. Access to this confidential document was denied.", Alert.AlertType.ERROR);
        return false;
    }

    private String saveFileToVault(File sourceFile) {
        try {
            String dirPath = "vault_files/user_" + LifeLedgerApp.userId;
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Create unique clean filename to avoid overwrite
            String cleanName = sourceFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
            String targetFileName = System.currentTimeMillis() + "_" + cleanName;
            Path targetPath = Paths.get(dirPath, targetFileName);

            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            return targetPath.toAbsolutePath().toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return sourceFile.getAbsolutePath(); // Fallback to original absolute path
        }
    }

    private void deleteSelected() {
        String[] row = table.getSelectionModel().getSelectedItem();
        if (row == null) { UIHelper.alert("No Selection", "Select a document to delete.", Alert.AlertType.WARNING); return; }

        String cleanTitle = row.length > 11 ? row[11] : row[1];
        String isSensitive = row.length > 10 ? row[10] : "0";

        // Security check for sensitive document deletion
        if ("1".equals(isSensitive)) {
            if (!verifyUserPassword("'" + cleanTitle + "' is marked as sensitive/confidential.\nPlease enter your account password to confirm deletion.")) {
                return;
            }
        }

        if (UIHelper.confirmDelete(cleanTitle)) {
            DB.update("DELETE FROM documents WHERE doc_id=" + row[0]);
            UIHelper.logHistory("Document", "DELETED", "Deleted: " + cleanTitle);

            // Clean up stored file if located in vault_files
            if (row.length > 9 && row[9] != null && !row[9].isEmpty()) {
                try {
                    File f = new File(row[9]);
                    if (f.exists() && f.getAbsolutePath().contains("vault_files")) {
                        f.delete();
                    }
                } catch (Exception ignored) {}
            }

            loadData();
        }
    }
}

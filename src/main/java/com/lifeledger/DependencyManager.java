package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature 8 — Document Dependency Manager
 * Links related documents and identifies missing or expired records required for tasks / goals
 * (e.g., Job Application, Passport Renewal, Visa Application, Vehicle Registration).
 * Evaluates automatically and immediately upon selecting a goal.
 */
public class DependencyManager {

    private ComboBox<String> goalCombo;
    private TableView<String[]> resultTable;
    private ObservableList<String[]> resultData = FXCollections.observableArrayList();
    private Label summaryLbl = new Label();

    record Requirement(String category, String titleKeyword, int minValidityDays, boolean mandatory) {}

    public BorderPane getView() {
        goalCombo = UIHelper.combo(
            "Passport Renewal",
            "Job Application / Employment",
            "Visa Application",
            "Vehicle Registration Renewal",
            "Home Rental / Lease Agreement"
        );
        goalCombo.setValue("Passport Renewal");

        Button refreshBtn = UIHelper.btnAdd("🔄 Re-evaluate Vault");

        HBox toolbar = UIHelper.toolbar(
            new Label("Select Target Goal / Checklist:"),
            goalCombo,
            UIHelper.spacer(),
            refreshBtn
        );

        summaryLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b; " +
            "-fx-background-color: #f1f5f9; -fx-padding: 8 14; -fx-background-radius: 8; -fx-border-color: #cbd5e1; -fx-border-radius: 8;");

        resultTable = UIHelper.table("Required Document / Category", "Type", "Status in Vault", "Matching Vault Document", "Expiry Date", "Action Required");
        resultTable.setItems(resultData);

        // Immediate reactive evaluation on selection change
        goalCombo.valueProperty().addListener((obs, oldVal, newVal) -> runCheck());
        refreshBtn.setOnAction(e -> runCheck());

        // Run initial check immediately
        runCheck();

        VBox center = new VBox(10,
            UIHelper.heading("🔗  Document Dependency Manager"),
            UIHelper.infoLabel("Check whether required documents in your vault are present and valid for key processes."),
            toolbar,
            summaryLbl,
            resultTable
        );
        VBox.setVgrow(resultTable, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }

    private void runCheck() {
        resultData.clear();
        String goal = goalCombo.getValue();
        if (goal == null) return;

        List<Requirement> reqs = getRequirementsForGoal(goal);
        int uid = LifeLedgerApp.userId;

        int okCount = 0;
        int missingCount = 0;
        int expiredCount = 0;

        for (Requirement req : reqs) {
            String status = "❌ Missing";
            String matchedDoc = "None found in vault";
            String expiry = "—";
            String action = "Upload / Add to Document Vault";

            if (DB.conn != null) {
                try {
                    String sql = "SELECT title, expiry_date, DATEDIFF(expiry_date, CURDATE()) AS days_left " +
                        "FROM documents WHERE user_id=? AND (category=? OR title LIKE ?)";
                    ResultSet rs = DB.pQuery(sql, uid, req.category(), "%" + req.titleKeyword() + "%");
                    if (rs != null && rs.next()) {
                        matchedDoc = rs.getString("title");
                        java.sql.Date exp = rs.getDate("expiry_date");
                        if (exp == null) {
                            status = "✅ Valid (No Expiry)";
                            expiry = "Lifetime";
                            action = "Ready";
                            okCount++;
                        } else {
                            expiry = exp.toString();
                            int daysLeft = rs.getInt("days_left");
                            if (daysLeft < 0) {
                                status = "🔴 Expired";
                                action = "Renew document immediately";
                                expiredCount++;
                            } else if (daysLeft < req.minValidityDays()) {
                                status = "⚠️ Expiring Soon (< " + req.minValidityDays() + " days)";
                                action = "Renew before applying";
                                expiredCount++;
                            } else {
                                status = "✅ Valid (" + daysLeft + " days remaining)";
                                action = "Ready";
                                okCount++;
                            }
                        }
                    } else {
                        missingCount++;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            resultData.add(new String[]{
                req.titleKeyword() + " (" + req.category() + ")",
                req.mandatory() ? "Mandatory" : "Optional",
                status,
                matchedDoc,
                expiry,
                action
            });
        }

        String summaryText = String.format("Goal: %s  |  ✅ Ready: %d  |  🔴 Expired / Soon: %d  |  ❌ Missing: %d  |  Status: %s",
            goal, okCount, expiredCount, missingCount,
            (missingCount == 0 && expiredCount == 0) ? "ALL REQUIREMENTS MET (Ready to proceed!)" : "ACTION NEEDED BEFORE SUBMISSION");
        summaryLbl.setText(summaryText);
        if (missingCount == 0 && expiredCount == 0) {
            summaryLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #15803d; -fx-background-color: #dcfce7; -fx-padding: 8 14; -fx-background-radius: 8;");
        } else {
            summaryLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #b45309; -fx-background-color: #fef3c7; -fx-padding: 8 14; -fx-background-radius: 8;");
        }
    }

    private List<Requirement> getRequirementsForGoal(String goal) {
        List<Requirement> list = new ArrayList<>();
        switch (goal) {
            case "Passport Renewal" -> {
                list.add(new Requirement("Identity", "Passport", 0, true));
                list.add(new Requirement("Identity", "Driver License / Identity Proof", 30, true));
                list.add(new Requirement("Property", "Address Proof / Lease", 30, true));
            }
            case "Job Application / Employment" -> {
                list.add(new Requirement("Education", "Degree Certificate", 0, true));
                list.add(new Requirement("Identity", "Driver License / Identity Proof", 30, true));
                list.add(new Requirement("Medical", "Health Insurance / Medical Certificate", 0, false));
                list.add(new Requirement("Financial", "Bank Account Details / Cancelled Cheque", 0, true));
            }
            case "Visa Application" -> {
                list.add(new Requirement("Identity", "Passport", 180, true));
                list.add(new Requirement("Financial", "Bank Statement / Financial Proof", 30, true));
                list.add(new Requirement("Medical", "Travel Health Insurance", 15, true));
            }
            case "Vehicle Registration Renewal" -> {
                list.add(new Requirement("Vehicle", "Driver License", 30, true));
                list.add(new Requirement("Vehicle", "Auto Insurance Policy", 15, true));
                list.add(new Requirement("Vehicle", "Pollution Certificate (PUC)", 7, true));
            }
            case "Home Rental / Lease Agreement" -> {
                list.add(new Requirement("Identity", "Driver License / Passport", 30, true));
                list.add(new Requirement("Financial", "Proof of Income / Salary Slip", 30, true));
                list.add(new Requirement("Property", "Lease / Rental Agreement", 0, false));
            }
            default -> {
                list.add(new Requirement("Identity", "Identity Document", 30, true));
            }
        }
        return list;
    }
}

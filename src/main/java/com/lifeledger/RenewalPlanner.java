package com.lifeledger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.ResultSet;
import java.util.Optional;

/**
 * Feature 7 — Renewal Action Planner
 * Create renewal plans with step-by-step checklists and progress tracking.
 */
public class RenewalPlanner {

    private TableView<String[]> planTable;
    private ObservableList<String[]> planData = FXCollections.observableArrayList();
    private TableView<String[]> taskTable;
    private ObservableList<String[]> taskData = FXCollections.observableArrayList();

    public BorderPane getView() {

        // ── Plans section ─────────────────────────────────────────────────
        Button addPlanBtn = UIHelper.btnAdd("+ New Plan");
        Button delPlanBtn = UIHelper.btnDelete("Delete Plan");
        HBox planBar = UIHelper.toolbar(UIHelper.subHeading("📋  Renewal Plans"), UIHelper.spacer(), addPlanBtn, delPlanBtn);

        planTable = UIHelper.table("Plan ID","Plan Title","Category","Target Date","Status","Progress");
        planTable.setItems(planData);
        planTable.setMaxHeight(220);

        // ── Tasks section ─────────────────────────────────────────────────
        Button addTaskBtn  = UIHelper.btnAdd("+ Add Task");
        Button doneTaskBtn = UIHelper.btnAction("✔ Mark Done");
        Button delTaskBtn  = UIHelper.btnDelete("Delete Task");
        HBox taskBar = UIHelper.toolbar(UIHelper.subHeading("✅  Tasks in Selected Plan"), UIHelper.spacer(), addTaskBtn, doneTaskBtn, delTaskBtn);

        taskTable = UIHelper.table("Task ID","Task Description","Due Date","Done");
        taskTable.setItems(taskData);

        // Load plans
        loadPlans();

        // Select plan → load its tasks
        planTable.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> loadTasks());

        // ── Handlers ─────────────────────────────────────────────────────
        addPlanBtn.setOnAction(e -> showAddPlanDialog());

        delPlanBtn.setOnAction(e -> {
            String[] row = planTable.getSelectionModel().getSelectedItem();
            if (row == null) return;
            if (UIHelper.confirmDelete(row[1])) {
                DB.update("DELETE FROM plan_tasks WHERE plan_id=" + row[0]);
                DB.update("DELETE FROM plans WHERE plan_id=" + row[0]);
                loadPlans(); taskData.clear();
            }
        });

        addTaskBtn.setOnAction(e -> showAddTaskDialog());

        doneTaskBtn.setOnAction(e -> {
            String[] row = taskTable.getSelectionModel().getSelectedItem();
            if (row == null) return;
            DB.update("UPDATE plan_tasks SET is_done=1 WHERE task_id=" + row[0]);
            UIHelper.logHistory("Planner", "TASK_DONE", "Task done: " + row[1]);
            loadTasks(); loadPlans();
        });

        delTaskBtn.setOnAction(e -> {
            String[] row = taskTable.getSelectionModel().getSelectedItem();
            if (row == null) return;
            if (UIHelper.confirmDelete(row[1])) { DB.update("DELETE FROM plan_tasks WHERE task_id=" + row[0]); loadTasks(); loadPlans(); }
        });

        // ── Layout ────────────────────────────────────────────────────────
        VBox center = new VBox(10,
            UIHelper.heading("📋  Renewal Action Planner"),
            UIHelper.infoLabel("Create renewal plans with step-by-step task checklists. Click a plan to see its tasks."),
            planBar, planTable,
            taskBar, taskTable
        );
        VBox.setVgrow(taskTable, Priority.ALWAYS);

        return UIHelper.page(null, center);
    }

    private void loadPlans() {
        planData.clear();
        if (DB.conn == null) return;
        try {
            ResultSet rs = DB.pQuery(
                "SELECT p.plan_id, p.title, p.category, p.target_date, p.status, " +
                "CONCAT(COALESCE(SUM(t.is_done),0),'/', COUNT(t.task_id),' done') " +
                "FROM plans p LEFT JOIN plan_tasks t ON p.plan_id=t.plan_id " +
                "WHERE p.user_id=? GROUP BY p.plan_id ORDER BY p.target_date", LifeLedgerApp.userId);
            while (rs != null && rs.next())
                planData.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3),
                    rs.getString(4), rs.getString(5), rs.getString(6)
                });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadTasks() {
        taskData.clear();
        String[] sel = planTable.getSelectionModel().getSelectedItem();
        if (sel == null || DB.conn == null) return;
        try {
            ResultSet rs = DB.pQuery(
                "SELECT task_id, description, due_date, IF(is_done,'✅ Done','⏳ Pending') " +
                "FROM plan_tasks WHERE plan_id=? ORDER BY task_id",
                Integer.parseInt(sel[0]));
            while (rs != null && rs.next())
                taskData.add(new String[]{
                    rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)
                });
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAddPlanDialog() {
        TextField titleF = UIHelper.field("Plan Title *");
        ComboBox<String> catF = UIHelper.combo("Document","License","Tax","Insurance","Other");
        catF.setValue("Document");
        DatePicker deadlineDP = UIHelper.datePicker();

        VBox form = UIHelper.formBox(
            UIHelper.fieldLabel("Plan Title *"), titleF,
            UIHelper.fieldLabel("Category"), catF,
            UIHelper.fieldLabel("Target Deadline"), deadlineDP
        );

        Optional<ButtonType> result = UIHelper.showDialog("New Renewal Plan", form);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (titleF.getText().trim().isEmpty()) { UIHelper.alert("Missing", "Title required.", Alert.AlertType.WARNING); return; }
            DB.pUpdate("INSERT INTO plans(user_id,title,category,target_date,status) VALUES(?,?,?,?,'In Progress')",
                LifeLedgerApp.userId, titleF.getText().trim(), catF.getValue(),
                deadlineDP.getValue() != null ? deadlineDP.getValue().toString() : null);
            UIHelper.logHistory("Planner", "PLAN_ADDED", "Plan: " + titleF.getText().trim());
            loadPlans();
        }
    }

    private void showAddTaskDialog() {
        String[] sel = planTable.getSelectionModel().getSelectedItem();
        if (sel == null) { UIHelper.alert("No Plan", "Select a plan first.", Alert.AlertType.WARNING); return; }

        TextField descF = UIHelper.field("Task Description *");
        DatePicker dueDP = UIHelper.datePicker();

        VBox form = UIHelper.formBox(
            UIHelper.fieldLabel("Task *"), descF,
            UIHelper.fieldLabel("Due Date"), dueDP
        );

        Optional<ButtonType> result = UIHelper.showDialog("Add Task to: " + sel[1], form);
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (descF.getText().trim().isEmpty()) { UIHelper.alert("Missing", "Description required.", Alert.AlertType.WARNING); return; }
            DB.pUpdate("INSERT INTO plan_tasks(plan_id,description,due_date,is_done) VALUES(?,?,?,0)",
                Integer.parseInt(sel[0]), descF.getText().trim(),
                dueDP.getValue() != null ? dueDP.getValue().toString() : null);
            loadTasks(); loadPlans();
        }
    }
}

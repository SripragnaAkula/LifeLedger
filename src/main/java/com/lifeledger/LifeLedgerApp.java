package com.lifeledger;

import javafx.application.Application;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.ResultSet;

/**
 * LifeLedger — Main Application
 * Features a modern Left-Sidebar navigation where clicking each menu option
 * immediately updates and refreshes the view with real-time MySQL data.
 */
public class LifeLedgerApp extends Application {

    public static int    userId   = -1;
    public static String userName = "";

    private Stage primaryStage;
    private BorderPane mainLayout;
    private Button[] navButtons;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("LifeLedger — Personal Document, Warranty & Obligation Management");
        DB.connect();
        showLogin();
        stage.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  LOGIN & REGISTRATION SCREEN
    // ─────────────────────────────────────────────────────────────────────────
    private void showLogin() {
        primaryStage.setWidth(900);
        primaryStage.setHeight(620);
        primaryStage.centerOnScreen();

        // Left Branding Banner
        VBox left = new VBox(16);
        left.setPrefWidth(360);
        left.setAlignment(Pos.CENTER);
        left.setStyle("-fx-background-color: #3b5bdb; -fx-padding: 50;");

        Label icon = new Label("📁");
        icon.setStyle("-fx-font-size: 56px;");

        Label appName = new Label("LifeLedger");
        appName.setStyle("-fx-font-size: 30px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label sub = new Label("Personal Document, Warranty\n& Obligation Management System");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: #c5d0e6; -fx-text-alignment: center;");
        sub.setWrapText(true);

        Separator sep = new Separator();
        sep.setMaxWidth(200);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.3);");

        Label pills = new Label("🔒 Secure  •  ⏰ Reminders  •  📋 Checklists");
        pills.setStyle("-fx-font-size: 11px; -fx-text-fill: #a5b4fc; -fx-padding: 8 0 0 0;");

        left.getChildren().addAll(icon, appName, sub, sep, pills);

        // Right Form Area
        VBox right = new VBox(12);
        right.setAlignment(Pos.CENTER);
        right.setPadding(new Insets(50, 60, 50, 60));
        HBox.setHgrow(right, Priority.ALWAYS);

        // Sign In Form
        VBox signInForm = new VBox(10);
        signInForm.setMaxWidth(300);

        Label title = new Label("Sign In");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label subTitle = new Label("Enter your credentials to access your vault");
        subTitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 0 0 6 0;");

        TextField userFld = tf("Username");
        PasswordField passFld = pf("Password");
        Label statusLbl = new Label();
        statusLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: 600;");

        Button loginBtn = btn("Sign In", "#3b5bdb");
        loginBtn.setMaxWidth(300);

        Label noAccLbl = new Label("New user? ");
        noAccLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        Button regLink = new Button("Create Account");
        regLink.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b5bdb; " +
            "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 0;");
        HBox regRow = new HBox(0, noAccLbl, regLink);
        regRow.setAlignment(Pos.CENTER);

        signInForm.getChildren().addAll(title, subTitle, userFld, passFld, statusLbl, loginBtn, new Separator(), regRow);

        // Sign Up Form
        VBox signUpForm = new VBox(8);
        signUpForm.setMaxWidth(300);
        Label regTitle = new Label("Create Account");
        regTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label regSub = new Label("Set up your personal LifeLedger vault");
        regSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-padding: 0 0 4 0;");

        TextField nameF = tf("Full Name *");
        TextField userF = tf("Username *");
        TextField emailF = tf("Email");
        PasswordField pw1 = pf("Password *");
        PasswordField pw2 = pf("Confirm Password *");
        Label regStatus = new Label();
        regStatus.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px; -fx-font-weight: 600;");

        Button createBtn = btn("Create Account", "#16a34a");
        createBtn.setMaxWidth(300);
        Button backLink = new Button("← Back to Sign In");
        backLink.setStyle("-fx-background-color: transparent; -fx-text-fill: #3b5bdb; -fx-cursor: hand; -fx-font-size: 12px;");

        signUpForm.getChildren().addAll(regTitle, regSub, nameF, userF, emailF, pw1, pw2, regStatus, createBtn, backLink);

        right.getChildren().add(signInForm);

        // Handlers
        Runnable doLogin = () -> {
            String u = userFld.getText().trim();
            String p = passFld.getText();
            if (u.isEmpty() || p.isEmpty()) {
                statusLbl.setText("Please enter username and password.");
                return;
            }
            if (DB.conn == null && !DB.connect()) {
                statusLbl.setText("Cannot connect to MySQL. Ensure server is running.");
                return;
            }
            try {
                ResultSet rs = DB.pQuery("SELECT user_id, full_name FROM users WHERE username=? AND password_hash=?", u, p);
                if (rs != null && rs.next()) {
                    userId   = rs.getInt("user_id");
                    userName = rs.getString("full_name");
                    DB.pUpdate("INSERT INTO history(user_id,module,action,detail) VALUES(?,?,?,?)",
                        userId, "Login", "SIGNED_IN", u + " logged in");
                    showMainApp();
                } else {
                    statusLbl.setText("Incorrect username or password.");
                    passFld.clear();
                }
            } catch (Exception e) {
                statusLbl.setText("DB error: " + e.getMessage());
            }
        };

        loginBtn.setOnAction(e -> doLogin.run());
        passFld.setOnAction(e -> doLogin.run());
        userFld.setOnAction(e -> passFld.requestFocus());

        regLink.setOnAction(e -> right.getChildren().setAll(signUpForm));
        backLink.setOnAction(e -> right.getChildren().setAll(signInForm));

        createBtn.setOnAction(e -> {
            String nm = nameF.getText().trim(), un = userF.getText().trim(),
                   em = emailF.getText().trim(), p1 = pw1.getText(), p2 = pw2.getText();
            if (nm.isEmpty() || un.isEmpty() || p1.isEmpty()) {
                regStatus.setText("Please fill in all required fields.");
                return;
            }
            if (!p1.equals(p2)) {
                regStatus.setText("Passwords do not match.");
                return;
            }
            if (DB.conn == null && !DB.connect()) {
                regStatus.setText("Cannot connect to MySQL server.");
                return;
            }
            boolean ok = DB.pUpdate("INSERT INTO users(full_name,username,email,password_hash) VALUES(?,?,?,?)", nm, un, em, p1);
            if (ok) {
                alert("Account Created", "Welcome " + nm + "! You can now sign in.");
                right.getChildren().setAll(signInForm);
                userFld.setText(un);
                passFld.clear();
                statusLbl.setText("");
            } else {
                regStatus.setText("Username already exists or database error.");
            }
        });

        HBox root = new HBox(left, right);
        root.setStyle("-fx-background-color: #f8fafc;");
        primaryStage.setScene(new Scene(root));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MAIN APPLICATION WINDOW WITH LEFT SIDEBAR NAVIGATION
    // ─────────────────────────────────────────────────────────────────────────
    private void showMainApp() {
        primaryStage.setTitle("LifeLedger — " + userName);

        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #f8fafc;");

        // Top Header Bar
        HBox topHeader = buildTopHeader();
        mainLayout.setTop(topHeader);

        // Left Sidebar Navigation
        VBox sidebar = buildLeftSidebar();
        mainLayout.setLeft(sidebar);

        // Default initial view: Dashboard (Fresh data)
        switchView(0);

        Scene scene = new Scene(mainLayout);
        try {
            var css = getClass().getResource("/styles/light-theme.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
    }

    private HBox buildTopHeader() {
        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(12, 24, 12, 24));
        top.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        Label logo = new Label("📁  LifeLedger");
        logo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #3b5bdb;");

        Label tag = new Label("  |  Personal Document, Warranty & Obligation Management");
        tag.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label userBadge = new Label("👤  " + userName);
        userBadge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #334155; -fx-padding: 0 16 0 0;");

        Button logoutBtn = new Button("Sign Out");
        logoutBtn.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; " +
            "-fx-font-size: 11px; -fx-padding: 6 14; -fx-background-radius: 6; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> showLogin());

        top.getChildren().addAll(logo, tag, spacer, userBadge, logoutBtn);
        return top;
    }

    private VBox buildLeftSidebar() {
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(230);
        sidebar.setMinWidth(210);
        sidebar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-width: 0 1 0 0; -fx-padding: 16 10 16 10;");

        Label menuLbl = new Label("MAIN MENU");
        menuLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8; -fx-padding: 4 12 8 12;");
        sidebar.getChildren().add(menuLbl);

        String[][] items = {
            {"📊 Dashboard",     "Overview, stats & urgent alerts"},
            {"📄 Documents",     "Personal document vault"},
            {"⏰ Expiry",        "Expiry tracking & auto-reminders"},
            {"🛡 Warranty",      "Product warranty & expiry"},
            {"💳 Subscriptions", "Recurring payments & spend"},
            {"🔔 Reminders",    "Deadline alerts & urgency"},
            {"📋 Planner",      "Renewal action checklists"},
            {"🔗 Dependencies", "Goal document validation"},
            {"📜 History",      "Activity & audit log"},
            {"🔒 Backup",       "Security & database backup"}
        };

        navButtons = new Button[items.length];

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            Button btn = new Button(items[i][0]);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setAlignment(Pos.BASELINE_LEFT);
            btn.setStyle(navStyle(false));

            btn.setOnAction(e -> switchView(index));
            navButtons[i] = btn;
            sidebar.getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Sidebar Bottom Card
        VBox bottomCard = new VBox(4);
        bottomCard.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10;");
        Label dbConnected = new Label(DB.conn != null ? "🟢 MySQL Online" : "🔴 MySQL Offline");
        dbConnected.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + (DB.conn != null ? "#15803d" : "#dc2626") + ";");
        Label dbInfo = new Label("Database: lifeledger");
        dbInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748b;");
        bottomCard.getChildren().addAll(dbConnected, dbInfo);

        sidebar.getChildren().addAll(spacer, bottomCard);
        return sidebar;
    }

    /**
     * Dynamically switches view and instantiates fresh data from MySQL immediately.
     */
    private void switchView(int index) {
        for (int i = 0; i < navButtons.length; i++) {
            navButtons[i].setStyle(navStyle(i == index));
        }

        // Fresh data load on every menu selection
        BorderPane view = switch (index) {
            case 0  -> new Dashboard().getView();
            case 1  -> new DocumentVault().getView();
            case 2  -> new ExpiryTracker().getView();
            case 3  -> new WarrantyTracker().getView();
            case 4  -> new SubscriptionTracker().getView();
            case 5  -> new ReminderEngine().getView();
            case 6  -> new RenewalPlanner().getView();
            case 7  -> new DependencyManager().getView();
            case 8  -> new HistoryLog().getView();
            case 9  -> new SecureBackup().getView();
            default -> new Dashboard().getView();
        };

        mainLayout.setCenter(view);
    }

    private String navStyle(boolean active) {
        if (active) {
            return "-fx-background-color: #eef2ff; -fx-text-fill: #3b5bdb; -fx-font-weight: bold; -fx-font-size: 12px; " +
                   "-fx-border-color: #3b5bdb; -fx-border-width: 0 0 0 3; -fx-border-radius: 0 6 6 0; -fx-background-radius: 0 6 6 0; " +
                   "-fx-padding: 10 14; -fx-cursor: hand; -fx-alignment: BASELINE_LEFT;";
        } else {
            return "-fx-background-color: transparent; -fx-text-fill: #475569; -fx-font-weight: normal; -fx-font-size: 12px; " +
                   "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 10 14; -fx-cursor: hand; " +
                   "-fx-alignment: BASELINE_LEFT;";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  UTILITY METHODS
    // ─────────────────────────────────────────────────────────────────────────
    static void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private TextField tf(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt); f.setMaxWidth(300);
        f.setStyle("-fx-padding: 9 12; -fx-font-size: 13px; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6;");
        return f;
    }

    private PasswordField pf(String prompt) {
        PasswordField f = new PasswordField();
        f.setPromptText(prompt); f.setMaxWidth(300);
        f.setStyle("-fx-padding: 9 12; -fx-font-size: 13px; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6;");
        return f;
    }

    private Button btn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
            "-fx-font-size: 13px; -fx-padding: 9 0; -fx-background-radius: 7; -fx-cursor: hand;");
        return b;
    }

    public static void main(String[] args) { launch(args); }
}

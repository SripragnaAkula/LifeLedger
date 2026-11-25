# LifeLedger — Personal Document, Warranty and Obligation Management System

[![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge&logo=java&logoColor=white)](https://openjfx.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-00758F?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Apache_Maven-3.9%2B-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)

**LifeLedger** is a JavaFX and MySQL desktop application designed to help individuals organize, track, and protect critical personal documents, product warranties, recurring digital subscriptions, and renewal deadlines. It features instant reactive filtering, original document attachment (PDF, Images, Word docs), password protection for confidential records, multi-step renewal checklists, and automatic reminders.

---

## 📑 Table of Contents
1. [Key Highlights](#-key-highlights)
2. [Tech Stack & Architecture](#-tech-stack--architecture)
3. [The 10 Core Features (Separated by Class)](#-the-10-core-features-separated-by-class)
4. [Project File Structure](#-project-file-structure)
5. [Quick Start & Setup](#-quick-start--setup)
6. [Advanced Operations](#-advanced-operations)
7. [Database Schema](#-database-schema)
8. [License](#-license)

---

## ✨ Key Highlights

- **Left-Sidebar Navigation**: Full-screen responsive layout with left-hand menu, zero empty whitespace, and real-time database status.
- **Original Document Attachment**: Attach actual files (PDFs, Images, Word documents) to vault records and open them with one click or double-click in your default system viewer.
- **Confidential Document Protection**: Documents flagged as sensitive require account password authentication before opening, editing, or deleting.
- **Instant Reactive Filtering**: Search as you type and change categories or status dropdowns with instantaneous table updates (no redundant "Search" or "Apply" buttons).
- **Zero-Friction Setup**: Automated batch script provisions the database, and `DB.java` ensures all required columns are automatically synced.

---

## 🛠 Tech Stack & Architecture

| Component | Technology | Description |
|---|---|---|
| **UI Framework** | JavaFX 21 | Pure Java desktop controls, BorderPane layout, TableViews, DatePickers |
| **Database** | MySQL 8.0+ | Relational schema connected via MySQL Connector/J JDBC Driver |
| **Build Tool** | Apache Maven | Project dependency management via `javafx-maven-plugin` |
| **Language** | Java 17 / 21+ | Modern Java APIs (`java.time`, `java.nio.file`, lambda listeners) |
| **Security** | Secure Password Gate | Modal password verification for confidential document vault records |

---

## 📦 The 10 Core Features (Separated by Class)

Every feature is cleanly separated into its own dedicated class under `com.lifeledger`:

### 1. `Dashboard.java` — Life Timeline & Analytics Dashboard
- Live KPI cards: Total Documents, Expiring in ≤30 days, Overdue/Expired, Active Warranties, Pending Alerts, and Monthly Active Spend.
- Dynamic table displaying urgent deadlines with automated priority tags (`CRITICAL`, `WARNING`, `OVERDUE`).
- Real-time synchronization whenever navigated to from the sidebar.

### 2. `DocumentVault.java` — Personal Document Vault
- Add, search, filter, and delete personal documents (Passports, ID cards, Degrees, Leases, Insurance).
- **File Attachment**: Browse and attach PDFs, JPGs, PNGs, and Word docs safely into local vault storage (`vault_files/`).
- **Security**: Confidential documents marked with 🔒 require entering your account password to unlock and open.
- **Quick Actions**: Double-click any row or click "📂 Open Document" to view files in your default system application.

### 3. `ExpiryTracker.java` — Expiry & Renewal Tracker
- Real-time validity countdown calculating exact remaining days (`DATEDIFF`).
- Status categorization: Active, Expiring Soon (≤30 days), and Expired.
- **One-Click Generator**: "⚡ Auto-Generate Reminders" automatically identifies upcoming expiries and registers reminders.

### 4. `WarrantyTracker.java` — Warranty & Purchase Tracker
- Store electronics, appliances, and vehicle warranty records (Serial number, brand, purchase date, duration, invoice price).
- Automatically calculates expiration date from purchase date and warranty duration in months.
- Real-time search and status filtering (Under Warranty vs. Expired).

### 5. `SubscriptionTracker.java` — Recurring-Payment Tracker
- Track software, streaming, and cloud services (Netflix, Spotify, GitHub, Cloud Storage).
- Computes normalized **Monthly Active Outflow** and **Projected Annual Outflow** across billing cycles (Monthly, Annual, Weekly).
- Days-to-renewal countdown with instant search and status filtering.

### 6. `ReminderEngine.java` — Smart Reminder Engine
- Priority-ranked deadline alerts with urgency levels: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`.
- Quick task controls: **✔ Mark Done**, **⏰ Snooze (7 days)**, or Delete.
- Instant filtering by status (Pending, Completed, Snoozed).

### 7. `RenewalPlanner.java` — Renewal Action Planner
- Manage multi-step workflows for complex obligations (e.g., Driver License Renewal, Visa Application).
- Create structured checklists with deadlines per task.
- Live progress indicator (e.g., `3/5 done`) automatically calculated as steps are marked complete.

### 8. `DependencyManager.java` — Document Dependency Manager
- Pre-configured goal templates (Passport Renewal, Job Application, Visa Application, Vehicle Registration, Home Lease).
- Automatically inspects vault holdings to detect missing documents or records expiring before the required validity window.
- Instant readiness evaluation upon selecting a goal.

### 9. `HistoryLog.java` — History & Version Management
- Persistent audit trail logging document additions, modifications, status updates, file attachments, and renewals.
- Instant search and module filtering (Login, Document, Warranty, Subscription, Reminder, Planner, Security).
- Ability to clear or review historical activity.

### 10. `SecureBackup.java` — Secure Access & Database Backup
- Change account password with old password verification.
- Live MySQL connection status indicator.
- **Full Database Export**: Generates `.sql` backup files of all user documents, warranties, and subscriptions.
- **One-Click Restore**: Recovers data from exported backup files to prevent accidental loss.

---

## 📁 Project File Structure

```text
Lifeledger/
├── pom.xml                                 # Maven dependencies & JavaFX build config
├── setup_db.bat                            # Database provisioning script
├── run.bat                                 # Quick execution script
├── README.md                               # Project documentation
│
└── src/
    ├── main/
    │   ├── java/com/lifeledger/
    │   │   ├── DB.java                     # MySQL JDBC connection helper
    │   │   ├── UIHelper.java               # Shared UI widgets (tables, dialogs, cards)
    │   │   ├── LifeLedgerApp.java          # App GUI entry, login & left sidebar layout
    │   │   ├── Main.java                   # Standard main() launcher entry point
    │   │   │
    │   │   ├── Dashboard.java              # Feature 1: Overview & KPI Analytics
    │   │   ├── DocumentVault.java          # Feature 2: Document Vault & File Attachments
    │   │   ├── ExpiryTracker.java          # Feature 3: Expiry Tracking & Auto Reminders
    │   │   ├── WarrantyTracker.java        # Feature 4: Product Warranties & Expiry
    │   │   ├── SubscriptionTracker.java    # Feature 5: Subscriptions & Spend Tracking
    │   │   ├── ReminderEngine.java         # Feature 6: Smart Reminders & Priority
    │   │   ├── RenewalPlanner.java         # Feature 7: Action Checklists & Plans
    │   │   ├── DependencyManager.java      # Feature 8: Goal Document Dependency Checker
    │   │   ├── HistoryLog.java             # Feature 9: Audit History Logging
    │   │   └── SecureBackup.java           # Feature 10: Security, Backup & Recovery
    │   │
    │   └── resources/
    │       ├── schema.sql                  # MySQL database DDL schema
    │       └── styles/
    │           └── light-theme.css         # Modern light theme CSS
```

---

## 🚀 Quick Start & Setup

### Prerequisites
- **Java Development Kit (JDK 17 or 21+)** installed.
- **Apache Maven 3.8+** installed.
- **MySQL Server 8.0+** running locally on port `3306`.

---

### Step 1: Configure Database Password
Open `src/main/java/com/lifeledger/DB.java` and set your local MySQL root password:
```java
private static final String URL  = "jdbc:mysql://localhost:3306/lifeledger";
private static final String USER = "root";
private static final String PASS = "YOUR_MYSQL_PASSWORD"; // <-- Replace with your MySQL password
```

---

### Step 2: Set Up Database & Tables
Run the automated setup script from the project root:
```powershell
.\setup_db.bat
```
*(Alternatively on Windows Command Prompt: `setup_db.bat`). When prompted, enter your MySQL username/password. This creates the `lifeledger` database and all 8 tables.*

---

### Step 3: Run the Application
Execute via Maven in your terminal or VS Code:
```powershell
mvn javafx:run
```
*(Or double-click `run.bat`).*

---

### Step 4: Sign Up & Sign In
- On the login screen, click **"Create Account"**.
- Enter your Full Name, Username, Email, and Password.
- Log in with your new credentials to access your personal dashboard.

---

## ⚡ Advanced Operations

| # | Advanced Operation | Implementation Details |
|---|---|---|
| **1** | **Automated Expiry Tracking & Reminders** | Calculates days remaining using SQL date functions (`DATEDIFF`); generates urgent reminders within 60-day thresholds via `ExpiryTracker`. |
| **2** | **Document Dependency Checking** | Inspects holdings against goal rule-sets in `DependencyManager` to identify missing or expiring prerequisites. |
| **3** | **Smart Priority Management** | Classifies obligations into `CRITICAL`, `HIGH`, `MEDIUM`, and `LOW` urgency tags across reminders and dashboard deadlines. |
| **4** | **Advanced Search & Filtering** | Instant reactive listeners on text fields and dropdowns filter TableViews in real time as the user types. |
| **5** | **Historical Tracking** | Persistent audit table logging additions, edits, file attachments, and completion actions across all modules. |
| **6** | **Dashboard Analytics** | Aggregates live KPI statistics, active warranty totals, recurring monthly outflows, and urgent action lists. |
| **7** | **Database Backup & Recovery** | Full SQL dump generation and one-click file restoration in `SecureBackup` to safeguard records against loss. |

---

## 🗄 Database Schema

The relational schema (`schema.sql`) contains:
- `users`: User identity, authentication password hash, creation timestamp.
- `documents`: Document title, category, certificate number, authority, issue date, expiry date, sensitivity flag, and attached `file_path`.
- `warranties`: Product name, brand, serial number, purchase date, warranty months, calculated expiry date, seller, and price.
- `subscriptions`: Service name, billing cycle, cost, next renewal date, status.
- `reminders`: Title, category, due date, urgency level, completion status, document reference.
- `plans`: Multi-step renewal plan title, category, target deadline, status.
- `plan_tasks`: Step-by-step checklist tasks tied to each renewal plan with completion flags.
- `history`: Comprehensive activity log tracking user actions across all modules.

---

## 📄 License
This project is developed for educational purposes under the **MIT License**.
Feel free to use and adapt it for academic coursework and demonstrations.

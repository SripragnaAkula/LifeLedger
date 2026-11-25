package com.lifeledger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * DB — MySQL Database Connection Helper
 * Mirrors the SQLDB pattern from the AWT reference code.
 */
public class DB {

    private static final String URL  = "jdbc:mysql://localhost:3306/lifeledger";
    private static final String USER = "root";
    private static final String PASS = "Your Password"; // <-- Set your MySQL root password here

    public static Connection conn = null;

    /** Call once at application startup. Returns true if connected. */
    public static boolean connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("[DB] Connected to MySQL — lifeledger");

            // Automatically ensure file_path column exists in documents table
            try {
                conn.createStatement().executeUpdate("ALTER TABLE documents ADD COLUMN file_path VARCHAR(500)");
            } catch (Exception ignored) {}

            return true;
        } catch (Exception e) {
            System.err.println("[DB] Connection failed: " + e.getMessage());
            conn = null;
            return false;
        }
    }

    /** Run a SELECT query. Returns ResultSet or null on error. */
    public static ResultSet query(String sql) {
        try {
            Statement st = conn.createStatement();
            return st.executeQuery(sql);
        } catch (Exception e) {
            System.err.println("[DB] Query error: " + e.getMessage());
            return null;
        }
    }

    /** Run an INSERT / UPDATE / DELETE. Returns true on success. */
    public static boolean update(String sql) {
        try {
            conn.createStatement().executeUpdate(sql);
            return true;
        } catch (Exception e) {
            System.err.println("[DB] Update error: " + e.getMessage());
            return false;
        }
    }

    /** Parameterised SELECT. */
    public static ResultSet pQuery(String sql, Object... args) {
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps.executeQuery();
        } catch (Exception e) {
            System.err.println("[DB] pQuery error: " + e.getMessage());
            return null;
        }
    }

    /** Parameterised INSERT / UPDATE / DELETE. Returns true on success. */
    public static boolean pUpdate(String sql, Object... args) {
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.err.println("[DB] pUpdate error: " + e.getMessage());
            return false;
        }
    }

    /** Count rows from a query like "SELECT COUNT(*) FROM …" */
    public static int count(String sql) {
        try {
            ResultSet rs = query(sql);
            return (rs != null && rs.next()) ? rs.getInt(1) : 0;
        } catch (Exception e) { return 0; }
    }

    public static void close() {
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
}

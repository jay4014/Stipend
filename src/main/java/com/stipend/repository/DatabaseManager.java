package com.stipend.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager: creates the SQLite connection and auto-creates
 * all tables on first run.
 *
 * The database file lives in ~/.coinscious/coinscious.db so it
 * persists across app restarts on any OS.
 */
public final class DatabaseManager {

    // ~/.coinscious/coinscious.db — cross-platform
    private static final String DB_DIR  =
            System.getProperty("user.home") + File.separator + ".coinscious";
    private static final String DB_FILE = DB_DIR + File.separator + "coinscious.db";
    private static final String DB_URL  = "jdbc:sqlite:" + DB_FILE;

    // Prevent instantiation
    private DatabaseManager() {}

    // ---------------------------------------------------------------
    // Connection
    // ---------------------------------------------------------------
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    // ---------------------------------------------------------------
    // Schema auto-creation (called once at startup)
    // ---------------------------------------------------------------
    public static void initializeDatabase() {
        // Create the directory if it does not exist
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // DDL statements (CREATE IF NOT EXISTS — safe to run repeatedly)
        String[] ddl = {
            // Enable foreign keys
            "PRAGMA foreign_keys = ON",

            // students
            """
            CREATE TABLE IF NOT EXISTS students (
                student_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT NOT NULL,
                course        TEXT NOT NULL,
                username      TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                salt          TEXT NOT NULL,
                profile_image BLOB,
                created_at    TEXT NOT NULL DEFAULT (date('now'))
            )
            """,

            // wallets (Cash, GCash, Maya, Bank)
            """
            CREATE TABLE IF NOT EXISTS wallets (
                wallet_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id      INTEGER NOT NULL,
                name            TEXT NOT NULL,
                opening_balance REAL NOT NULL DEFAULT 0,
                FOREIGN KEY (student_id) REFERENCES students(student_id)
                    ON DELETE CASCADE
            )
            """,

            // transactions
            """
            CREATE TABLE IF NOT EXISTS transactions (
                transaction_id   INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id       INTEGER NOT NULL,
                wallet_id        INTEGER NOT NULL DEFAULT 1,
                type             TEXT NOT NULL CHECK (type IN ('INCOME','EXPENSE')),
                amount           REAL NOT NULL CHECK (amount > 0),
                date             TEXT NOT NULL,
                category         TEXT NOT NULL,
                note             TEXT,
                source           TEXT,
                description      TEXT,
                FOREIGN KEY (student_id) REFERENCES students(student_id)
                    ON DELETE CASCADE,
                FOREIGN KEY (wallet_id)  REFERENCES wallets(wallet_id)
            )
            """,

            // budgets
            """
            CREATE TABLE IF NOT EXISTS budgets (
                budget_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id    INTEGER NOT NULL,
                category      TEXT NOT NULL,
                monthly_limit REAL NOT NULL CHECK (monthly_limit > 0),
                month         TEXT NOT NULL,
                UNIQUE (student_id, category, month),
                FOREIGN KEY (student_id) REFERENCES students(student_id)
                    ON DELETE CASCADE
            )
            """,

            // goals
            """
            CREATE TABLE IF NOT EXISTS goals (
                goal_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id    INTEGER NOT NULL,
                name          TEXT NOT NULL,
                target_amount REAL NOT NULL CHECK (target_amount > 0),
                saved_amount  REAL NOT NULL DEFAULT 0,
                target_date   TEXT,
                FOREIGN KEY (student_id) REFERENCES students(student_id)
                    ON DELETE CASCADE
            )
            """,

            // debts (IOU tracker)
            """
            CREATE TABLE IF NOT EXISTS debts (
                debt_id      INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id   INTEGER NOT NULL,
                person_name  TEXT NOT NULL,
                amount       REAL NOT NULL CHECK (amount > 0),
                direction    TEXT NOT NULL CHECK (direction IN ('OWED_TO_ME','I_OWE')),
                date_created TEXT NOT NULL,
                settled      INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (student_id) REFERENCES students(student_id)
                    ON DELETE CASCADE
            )
            """,

            // recurring transactions (allowance / subscriptions)
            """
            CREATE TABLE IF NOT EXISTS recurring (
                recurring_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id    INTEGER NOT NULL,
                type          TEXT NOT NULL CHECK (type IN ('INCOME','EXPENSE')),
                amount        REAL NOT NULL CHECK (amount > 0),
                category      TEXT NOT NULL,
                wallet_id     INTEGER NOT NULL DEFAULT 1,
                frequency     TEXT NOT NULL CHECK (frequency IN ('WEEKLY','MONTHLY')),
                next_due_date TEXT NOT NULL,
                note          TEXT,
                FOREIGN KEY (student_id) REFERENCES students(student_id)
                    ON DELETE CASCADE
            )
            """
        };

        try (Connection conn = connect();
             Statement stmt  = conn.createStatement()) {

            for (String sql : ddl) {
                stmt.execute(sql);
            }
            ensureProfileImageColumn(conn);
            System.out.println("[Coinscious] Database initialised at: " + DB_FILE);

        } catch (SQLException e) {
            System.err.println("[Coinscious] DB init error: " + e.getMessage());
            throw new RuntimeException("Could not initialise the database.", e);
        }
    }

    private static void ensureProfileImageColumn(Connection conn) throws SQLException {
        boolean hasProfileImage = false;
        try (Statement stmt = conn.createStatement();
             java.sql.ResultSet columns = stmt.executeQuery("PRAGMA table_info(students)")) {
            while (columns.next()) {
                if ("profile_image".equalsIgnoreCase(columns.getString("name"))) {
                    hasProfileImage = true;
                    break;
                }
            }
        }
        if (!hasProfileImage) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE students ADD COLUMN profile_image BLOB");
            }
        }
    }

    /** Absolute path of the database file (used by BackupService). */
    public static String getDatabasePath() {
        return DB_FILE;
    }
}

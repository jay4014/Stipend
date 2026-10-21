package com.stipend;

import com.stipend.repository.DatabaseManager;
import com.stipend.ui.LandingView;
import com.stipend.ui.LoginView;
import com.stipend.ui.theme.CoinSciousTheme;

import javax.swing.*;

/**
 * Main: entry point for the Coinscious desktop application.
 *
 * Build order follows section 11 of the specification:
 *  1. pom.xml + SQLite dependency ✓
 *  2. Model hierarchy (Transaction → Income/Expense) ✓
 *  3. Repository layer (SQLite) ✓
 *  4. AuthService + utilities ✓
 *  5. LandingView / LoginView / RegisterView ✓
 *  6. MainFrame shell + Sidebar ✓
 *  7. DashboardPanel (KPI, donut, trend) ✓
 *  8. TransactionsPanel (add/edit/delete/filter) ✓
 *  9. BudgetsPanel (alerts, progress bars) ✓
 * 10. GoalsPanel (ring, contribute) ✓
 * 11. ReportsPanel + CSV export ✓
 * 12. InsightsPanel (heatmap, gauge) ✓
 * 13. SettingsPanel (profile, password, backup) ✓
 */
public class Main {

    public static void main(String[] args) {
        // ── 1. Initialize the database schema (idempotent) ──────────
        DatabaseManager.initializeDatabase();

        // ── 2. Use the cross-platform LAF as a base for painting ───
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // Falls back to the Metal LAF — still usable with our custom painting
        }

        // ── 3. Apply global Swing dark-theme defaults ───────────────
        CoinSciousTheme.applyGlobalDefaults();

        // ── 4. Launch the landing screen on the Event Dispatch Thread ─
        SwingUtilities.invokeLater(() -> {
            LoginView landing = new LoginView();
            landing.setVisible(true);
        });
    }
}

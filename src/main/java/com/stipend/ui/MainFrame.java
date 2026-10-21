package com.stipend.ui;

import com.stipend.model.Student;
import com.stipend.service.AuthService;
import com.stipend.service.FinancialSystem;
import com.stipend.ui.panels.*;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * MainFrame: the application shell after login.
 * Contains the Sidebar (left) and a CardLayout content area (right).
 * Navigation requests from Sidebar are handled here.
 */
public class MainFrame extends JFrame implements Sidebar.NavigationListener {

    private final Student student;
    private final JPanel  contentArea;
    private final CardLayout cardLayout;
    private final Sidebar sidebar;

    // All page panels
    private final DashboardPanel     dashboardPanel;
    private final TransactionsPanel  transactionsPanel;
    private final BudgetsPanel       budgetsPanel;
    private final GoalsPanel         goalsPanel;
    private final ReportsPanel       reportsPanel;
    private final InsightsPanel      insightsPanel;
    private final SettingsPanel      settingsPanel;

    public MainFrame() {
        this.student = AuthService.getCurrentStudent();
        if (student == null) throw new IllegalStateException("No student logged in.");

        setTitle("Stipend — " + student.getFirstName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 680));
        setSize(1200, 750);
        setLocationRelativeTo(null);

        // Build panels
        dashboardPanel    = new DashboardPanel(student, this);
        transactionsPanel = new TransactionsPanel(student, this);
        budgetsPanel      = new BudgetsPanel(student);
        goalsPanel        = new GoalsPanel(student);
        reportsPanel      = new ReportsPanel(student);
        insightsPanel     = new InsightsPanel(student);
        settingsPanel     = new SettingsPanel(student, this);

        // Sidebar
        sidebar = new Sidebar(student, this);

        // CardLayout content area
        cardLayout  = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG_MAIN);

        contentArea.add(dashboardPanel,    "Dashboard");
        contentArea.add(transactionsPanel, "Transactions");
        contentArea.add(budgetsPanel,      "Budgets");
        contentArea.add(goalsPanel,        "Goals");
        contentArea.add(reportsPanel,      "Reports");
        contentArea.add(insightsPanel,     "Insights");
        contentArea.add(settingsPanel,     "Settings");

        // Left panel = sidebar + thin divider.
        // (The divider used to be added straight to the root at LINE_START,
        //  which is the same slot as WEST and silently replaced the sidebar.)
        JSeparator div = new JSeparator(JSeparator.VERTICAL);
        div.setForeground(BORDER_SUBTLE);
        div.setBackground(BORDER_SUBTLE);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(BG_SIDEBAR);
        leftPanel.add(sidebar, BorderLayout.CENTER);
        leftPanel.add(div,     BorderLayout.EAST);

        // Root layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_MAIN);
        root.add(leftPanel,   BorderLayout.WEST);
        root.add(contentArea, BorderLayout.CENTER);

        setContentPane(root);

        // Show dashboard first
        showPage("Dashboard");

        // Load safe-to-spend value for the sidebar tip card asynchronously
        refreshSafeToSpend();
    }

    // ---------------------------------------------------------------
    // NavigationListener implementation
    // ---------------------------------------------------------------
    @Override
    public void onNavigate(String page) {
        showPage(page);
    }

    @Override
    public void onLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to log out?",
            "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            AuthService.logout();
            dispose();
            new LandingView().setVisible(true);
        }
    }

    // ---------------------------------------------------------------
    // Navigation
    // ---------------------------------------------------------------
    public void showPage(String page) {
        cardLayout.show(contentArea, page);
        sidebar.setActivePage(page);
        setTitle("Stipend — " + page);

        // Refresh the active page
        switch (page) {
            case "Dashboard"    -> dashboardPanel.refresh();
            case "Transactions" -> transactionsPanel.refresh();
            case "Budgets"      -> budgetsPanel.refresh();
            case "Goals"        -> goalsPanel.refresh();
            case "Reports"      -> reportsPanel.refresh();
            case "Insights"     -> insightsPanel.refresh();
        }
        refreshSafeToSpend();
    }

    /** Called by panels after they add/edit/delete a transaction. */
    public void onDataChanged() {
        dashboardPanel.refresh();
        refreshSafeToSpend();
    }

    public void refreshProfile() {
        sidebar.refreshProfile();
        setTitle("Stipend — " + student.getFirstName());
    }

    // ---------------------------------------------------------------
    // Sidebar safe-to-spend value (runs off the EDT for DB query)
    // ---------------------------------------------------------------
    private void refreshSafeToSpend() {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception {
                double val = FinancialSystem.safeToSpendToday(student.getStudentId());
                return MoneyFormatter.format(val) + " / day";
            }
            @Override protected void done() {
                try { sidebar.updateSafeToSpend(get()); }
                catch (Exception ignored) {}
            }
        };
        worker.execute();
    }
}
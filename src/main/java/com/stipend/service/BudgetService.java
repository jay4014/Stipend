package com.stipend.service;

import com.stipend.model.Budget;
import com.stipend.model.Expense;
import com.stipend.model.Transaction;
import com.stipend.repository.DatabaseManager;

import java.sql.*;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * BudgetService: creates / retrieves budgets and populates the
 * "spent" field by querying the transactions table.
 */
public final class BudgetService {

    private BudgetService() {}

    // ---------------------------------------------------------------
    // Load budgets for a student/month (with spent amounts)
    // ---------------------------------------------------------------
    public static List<Budget> getBudgets(int studentId, YearMonth month)
            throws SQLException {

        String sql = """
            SELECT b.budget_id, b.category, b.monthly_limit,
                   COALESCE(SUM(t.amount), 0) AS spent
            FROM budgets b
            LEFT JOIN transactions t
              ON t.student_id = b.student_id
              AND t.category  = b.category
              AND t.type      = 'EXPENSE'
              AND strftime('%Y-%m', t.date) = ?
            WHERE b.student_id = ?
              AND b.month = ?
            GROUP BY b.budget_id, b.category, b.monthly_limit
            ORDER BY b.category
            """;

        List<Budget> list = new ArrayList<>();
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, month.toString());
            ps.setInt(2, studentId);
            ps.setString(3, month.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Budget b = new Budget(
                        rs.getInt("budget_id"), studentId,
                        rs.getString("category"),
                        rs.getDouble("monthly_limit"),
                        month
                    );
                    b.setSpent(rs.getDouble("spent"));
                    list.add(b);
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // Create a budget; an existing category/month must be edited explicitly.
    // ---------------------------------------------------------------
    public static String createBudget(int studentId, String category,
                                      double limit, YearMonth month) {
        if (category == null || category.isBlank())
            return "Category must not be blank.";
        if (!Double.isFinite(limit) || limit <= 0)
            return "Monthly limit must be greater than 0.";

        String sql = """
            INSERT INTO budgets (student_id, category, monthly_limit, month)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, category.trim());
            ps.setDouble(3, limit);
            ps.setString(4, month.toString());
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            if (e.getErrorCode() == 19 &&
                    e.getMessage() != null &&
                    e.getMessage().toLowerCase().contains("unique constraint failed: budgets.student_id")) {
                return "A budget for this category already exists for " + month + ". Edit that budget instead.";
            }
            return "Database error: " + e.getMessage();
        }
    }

    public static String adjustBudgetLimit(int budgetId, double adjustment,
                                           boolean increase) {
        if (!Double.isFinite(adjustment) || adjustment <= 0)
            return "Adjustment amount must be greater than 0.";

        String operator = increase ? "+" : "-";
        String sql = "UPDATE budgets SET monthly_limit = ROUND(monthly_limit " +
            operator + " ?, 2) WHERE budget_id = ? AND ROUND(monthly_limit " +
            operator + " ?, 2) > 0";

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, adjustment);
            ps.setInt(2, budgetId);
            ps.setDouble(3, adjustment);
            if (ps.executeUpdate() == 0) {
                return increase
                    ? "This budget no longer exists."
                    : "The budget limit cannot be reduced to zero or below.";
            }
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Delete a budget
    // ---------------------------------------------------------------
    public static String deleteBudget(int budgetId) {
        String sql = "DELETE FROM budgets WHERE budget_id = ?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, budgetId);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Check if adding an expense would breach any budget
    // Returns null if OK, or a warning message for the UI
    // ---------------------------------------------------------------
    public static String checkBudgetAlert(int studentId, String category,
                                          double newExpenseAmount, YearMonth month)
            throws SQLException {

        String sql = """
            SELECT b.monthly_limit,
                   COALESCE(SUM(t.amount), 0) AS spent
            FROM budgets b
            LEFT JOIN transactions t
              ON t.student_id = b.student_id
              AND t.category  = b.category
              AND t.type      = 'EXPENSE'
              AND strftime('%Y-%m', t.date) = ?
            WHERE b.student_id = ?
              AND b.category   = ?
              AND b.month      = ?
            GROUP BY b.monthly_limit
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, month.toString());
            ps.setInt(2, studentId);
            ps.setString(3, category);
            ps.setString(4, month.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null; // no budget set for this category

                double limit = rs.getDouble("monthly_limit");
                double spent = rs.getDouble("spent");
                double after = spent + newExpenseAmount;
                double pct   = (after / limit) * 100.0;

                if (pct >= 100)
                    return String.format(
                        "⚠ Budget EXCEEDED for \"%s\"!\n" +
                        "Limit: ₱%,.2f | Already spent: ₱%,.2f | This expense: ₱%,.2f",
                        category, limit, spent, newExpenseAmount);
                if (pct >= 50)
                    return String.format(
                        "⚠ Budget TIGHT for \"%s\" (%.0f%% used after this expense).\n" +
                        "Limit: ₱%,.2f | Remaining: ₱%,.2f",
                        category, pct, limit, limit - after);
            }
        }
        return null; // all clear
    }
}

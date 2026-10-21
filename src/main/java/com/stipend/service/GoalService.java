package com.stipend.service;

import com.stipend.model.Goal;
import com.stipend.repository.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** GoalService: CRUD for savings goals and contribution recording. */
public final class GoalService {

    private GoalService() {}

    // ---------------------------------------------------------------
    // Load all goals for a student
    // ---------------------------------------------------------------
    public static List<Goal> getGoals(int studentId) throws SQLException {
        String sql = """
            SELECT goal_id, name, target_amount, saved_amount, target_date
            FROM goals WHERE student_id = ?
            ORDER BY goal_id
            """;

        List<Goal> list = new ArrayList<>();
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String dateStr = rs.getString("target_date");
                    LocalDate targetDate = dateStr != null ? LocalDate.parse(dateStr) : null;
                    list.add(new Goal(
                        rs.getInt("goal_id"), studentId,
                        rs.getString("name"),
                        rs.getDouble("target_amount"),
                        rs.getDouble("saved_amount"),
                        targetDate
                    ));
                }
            }
        }
        return list;
    }

    // ---------------------------------------------------------------
    // Create a new goal
    // ---------------------------------------------------------------
    public static String createGoal(int studentId, String name,
                                    double targetAmount, LocalDate targetDate) {
        if (name == null || name.isBlank()) return "Goal name must not be blank.";
        if (targetAmount <= 0) return "Target amount must be > 0.";

        String sql = """
            INSERT INTO goals (student_id, name, target_amount, saved_amount, target_date)
            VALUES (?, ?, ?, 0, ?)
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, name.trim());
            ps.setDouble(3, targetAmount);
            ps.setString(4, targetDate != null ? targetDate.toString() : null);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Add a contribution to an existing goal
    // ---------------------------------------------------------------
    public static String addContribution(int goalId, double amount) {
        if (amount <= 0) return "Contribution amount must be > 0.";

        String sql = """
            UPDATE goals
            SET saved_amount = saved_amount + ?
            WHERE goal_id = ?
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, goalId);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Delete a goal
    // ---------------------------------------------------------------
    public static String deleteGoal(int goalId) {
        String sql = "DELETE FROM goals WHERE goal_id = ?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, goalId);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }
}

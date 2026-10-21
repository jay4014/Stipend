package com.stipend.service;

import com.stipend.model.Student;
import com.stipend.repository.DatabaseManager;
import com.stipend.util.PasswordUtil;
import com.stipend.util.Validator;

import java.sql.*;

/**
 * AuthService: handles registration, login, and session management.
 * The currently logged-in student is held in a static field so any
 * part of the app can access it without passing it through constructors.
 */
public final class AuthService {

    // Singleton session holder — the currently logged-in student
    private static Student currentStudent = null;

    private AuthService() {}

    // ---------------------------------------------------------------
    // Session
    // ---------------------------------------------------------------
    public static Student getCurrentStudent() {
        return currentStudent;
    }

    public static boolean isLoggedIn() {
        return currentStudent != null;
    }

    public static void logout() {
        currentStudent = null;
    }

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------
    /**
     * Attempts login with the supplied credentials.
     *
     * @return null on success (currentStudent is set), or an error message.
     */
    public static String login(String username, char[] password) {
        String err = Validator.validateNonBlank(username, "Username");
        if (err != null) return err;
        if (password == null || password.length == 0)
            return "Password must not be empty.";

        String sql = """
            SELECT student_id, name, course, username, password_hash, profile_image
            FROM students WHERE username = ?
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "Invalid username or password.";

                String storedHash = rs.getString("password_hash");
                if (!PasswordUtil.verifyPassword(password, storedHash))
                    return "Invalid username or password.";

                currentStudent = new Student(
                    rs.getInt("student_id"),
                    rs.getString("name"),
                    rs.getString("course"),
                    rs.getString("username")
                );
                currentStudent.setProfileImage(rs.getBytes("profile_image"));
                return null; // success
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    // ---------------------------------------------------------------
    // Registration
    // ---------------------------------------------------------------
    /**
     * Registers a new student account.
     *
     * @return null on success, or a validation/DB error message.
     */
    public static String register(String name, String course,
                                  String username, char[] password,
                                  char[] confirmPassword) {
        // Validate all fields
        String err;
        err = Validator.validateNonBlank(name, "Full name");   if (err != null) return err;
        err = Validator.validateNonBlank(course, "Course");    if (err != null) return err;
        err = Validator.validateUsername(username);            if (err != null) return err;
        err = Validator.validatePassword(password);            if (err != null) return err;
        err = Validator.validatePasswordsMatch(password, confirmPassword); if (err != null) return err;

        String hash = PasswordUtil.hashPassword(password);
        // The hash already encodes the salt; for the DB 'salt' column we store
        // the salt segment separately for legacy compatibility (section 7).
        String salt = hash.split("\\$")[2];

        String sql = """
            INSERT INTO students (name, course, username, password_hash, salt)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            ps.setString(2, course.trim());
            ps.setString(3, username.trim());
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.executeUpdate();

            // Create the default "Cash" wallet for the new student
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    createDefaultWallet(c, keys.getInt(1));
                }
            }
            return null; // success

        } catch (SQLException e) {
            if (e.getMessage() != null &&
                e.getMessage().toLowerCase().contains("unique"))
                return "That username is already taken. Please choose another.";
            return "Registration failed: " + e.getMessage();
        }
    }

    /** Inserts the default "Cash" wallet for a newly registered student. */
    private static void createDefaultWallet(Connection c, int studentId)
            throws SQLException {
        String sql = "INSERT INTO wallets (student_id, name, opening_balance) VALUES (?, 'Cash', 0)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // Change password (from Settings)
    // ---------------------------------------------------------------
    public static String changePassword(int studentId,
                                         char[] currentPwd,
                                         char[] newPwd,
                                         char[] confirmNewPwd) {
        // Verify current password first
        String fetchSql = "SELECT password_hash FROM students WHERE student_id = ?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(fetchSql)) {

            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return "Account not found.";
                if (!PasswordUtil.verifyPassword(currentPwd, rs.getString("password_hash")))
                    return "Current password is incorrect.";
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }

        String err = Validator.validatePassword(newPwd);
        if (err != null) return err;
        err = Validator.validatePasswordsMatch(newPwd, confirmNewPwd);
        if (err != null) return err;

        String newHash = PasswordUtil.hashPassword(newPwd);
        String newSalt = newHash.split("\\$")[2];

        String updateSql = "UPDATE students SET password_hash=?, salt=? WHERE student_id=?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setInt(3, studentId);
            ps.executeUpdate();
            return null;
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }
}


package com.stipend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    private static final Color NAVY = new Color(25, 42, 70);
    private static final Color BLUE = new Color(53, 105, 180);
    private static final Color LIGHT = new Color(245, 248, 252);

    public LoginFrame() {
        setTitle("Stipend | Student Budget Tracker");
        setSize(850, 540);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(createWelcomePanel());
        root.add(createLoginPanel());

        setContentPane(root);
    }

    private JPanel createWelcomePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(NAVY);
        panel.setBorder(new EmptyBorder(65, 35, 40, 35));

        JLabel logo = new JLabel("S");
        logo.setFont(new Font("SansSerif", Font.BOLD, 48));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Stipend");
        title.setFont(new Font("SansSerif", Font.BOLD, 38));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "<html><div style='width:250px'>"
                        + "Student Budget and Expense Tracking System"
                        + "</div></html>"
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 17));
        subtitle.setForeground(new Color(220, 230, 245));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel description = new JLabel(
                "<html><div style='width:260px; line-height:1.5'>"
                        + "Track your income, manage your expenses, "
                        + "and understand your spending habits."
                        + "</div></html>"
        );
        description.setFont(new Font("SansSerif", Font.PLAIN, 14));
        description.setForeground(Color.WHITE);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(logo);
        panel.add(Box.createVerticalStrut(12));
        panel.add(title);
        panel.add(Box.createVerticalStrut(14));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(35));
        panel.add(description);

        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(55, 45, 40, 45));

        JLabel heading = new JLabel("Welcome back!");
        heading.setFont(new Font("SansSerif", Font.BOLD, 27));
        heading.setForeground(NAVY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel instruction = new JLabel("Log in to your Stipend account");
        instruction.setFont(new Font("SansSerif", Font.PLAIN, 13));
        instruction.setForeground(Color.GRAY);
        instruction.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel usernameLabel = createLabel("Username");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        styleField(usernameField);

        JLabel passwordLabel = createLabel("Password");
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        styleField(passwordField);

        JButton loginButton = new JButton("LOG IN");
        styleButton(loginButton, BLUE, Color.WHITE);
        loginButton.addActionListener(e -> login());

        JButton registerButton = new JButton("Create an account");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        registerButton.setForeground(BLUE);
        registerButton.setBorderPainted(false);
        registerButton.setContentAreaFilled(false);
        registerButton.setFocusPainted(false);
        registerButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerButton.addActionListener(e -> showRegistrationDialog());

        panel.add(heading);
        panel.add(Box.createVerticalStrut(8));
        panel.add(instruction);
        panel.add(Box.createVerticalStrut(35));
        panel.add(usernameLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(usernameField);
        panel.add(Box.createVerticalStrut(20));
        panel.add(passwordLabel);
        panel.add(Box.createVerticalStrut(8));
        panel.add(passwordField);
        panel.add(Box.createVerticalStrut(28));

        loginButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        panel.add(loginButton);

        panel.add(Box.createVerticalStrut(20));
        panel.add(registerButton);

        return panel;
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(NAVY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void styleField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 218, 230)),
                new EmptyBorder(8, 10, 8, 10)
        ));
    }

    private void styleButton(JButton button, Color background,
                             Color foreground) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();

        if (username.isEmpty() || password.length == 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter your username and password.",
                    "Missing Information",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String sql = """
                SELECT student_id, name, course, password_hash
                FROM students
                WHERE username = ?
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && verifyPassword(
                        password, result.getString("password_hash"))) {

                    String studentName = result.getString("name");
                    String studentCourse = result.getString("course");
                    int studentId = result.getInt("student_id");

                    Student student = new Student(
                            studentId,
                            studentName,
                            studentCourse,
                            username
                    );

                    dispose();

                    DashboardFrame dashboard = new DashboardFrame(student);
                    dashboard.setVisible(true);

                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Invalid username or password.",
                            "Login Failed",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        } catch (SQLException ex) {
            showDatabaseError(ex);
        } finally {
            java.util.Arrays.fill(password, '\0');
        }
    }

    private void showRegistrationDialog() {
        JTextField nameField = new JTextField();
        JTextField courseField = new JTextField();
        JTextField usernameInput = new JTextField();
        JPasswordField passwordInput = new JPasswordField();
        JPasswordField confirmInput = new JPasswordField();

        JPanel form = new JPanel(new GridLayout(0, 1, 5, 5));
        form.add(new JLabel("Full Name"));
        form.add(nameField);
        form.add(new JLabel("Course / Program"));
        form.add(courseField);
        form.add(new JLabel("Username"));
        form.add(usernameInput);
        form.add(new JLabel("Password (at least 8 characters)"));
        form.add(passwordInput);
        form.add(new JLabel("Confirm Password"));
        form.add(confirmInput);

        int choice = JOptionPane.showConfirmDialog(
                this,
                form,
                "Create a Stipend Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText().trim();
        String course = courseField.getText().trim();
        String username = usernameInput.getText().trim();
        char[] password = passwordInput.getPassword();
        char[] confirm = confirmInput.getPassword();

        try {
            if (name.isEmpty() || course.isEmpty()
                    || username.isEmpty()) {
                showMessage("Please complete all fields.");
                return;
            }

            if (username.length() < 3) {
                showMessage("Username must have at least 3 characters.");
                return;
            }

            if (password.length < 8) {
                showMessage("Password must have at least 8 characters.");
                return;
            }

            if (!MessageDigest.isEqual(
                    new String(password).getBytes(StandardCharsets.UTF_8),
                    new String(confirm).getBytes(StandardCharsets.UTF_8))) {
                showMessage("Passwords do not match.");
                return;
            }

            String passwordHash = hashPassword(password);

            String sql = """
                    INSERT INTO students
                    (name, course, username, password_hash)
                    VALUES (?, ?, ?, ?)
                    """;

            try (Connection connection = DatabaseManager.connect();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, name);
                statement.setString(2, course);
                statement.setString(3, username);
                statement.setString(4, passwordHash);
                statement.executeUpdate();

                JOptionPane.showMessageDialog(
                        this,
                        "Account created successfully!\n"
                                + "You can now log in.",
                        "Registration Complete",
                        JOptionPane.INFORMATION_MESSAGE
                );

                usernameField.setText(username);
                passwordField.setText("");

            } catch (SQLException ex) {
                if (ex.getMessage() != null
                        && ex.getMessage().toLowerCase()
                        .contains("unique")) {
                    showMessage(
                            "That username is already taken. "
                                    + "Please choose another."
                    );
                } else {
                    showDatabaseError(ex);
                }
            }

        } finally {
            java.util.Arrays.fill(password, '\0');
            java.util.Arrays.fill(confirm, '\0');
        }
    }

    private String hashPassword(char[] password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        PBEKeySpec spec = new PBEKeySpec(password, salt, 210000, 256);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hash = factory.generateSecret(spec).getEncoded();

            return "PBKDF2$210000$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not hash the password.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private boolean verifyPassword(char[] password, String storedHash) {
        if (storedHash == null) {
            return false;
        }

        try {
            String[] parts = storedHash.split("\\$");

            if (parts.length != 4 || !parts[0].equals("PBKDF2")) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);

            if (iterations < 100000 || iterations > 1000000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            PBEKeySpec spec = new PBEKeySpec(
                    password, salt, iterations, expectedHash.length * 8);

            try {
                SecretKeyFactory factory = SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA256");

                byte[] actualHash =
                        factory.generateSecret(spec).getEncoded();

                return MessageDigest.isEqual(expectedHash, actualHash);
            } finally {
                spec.clearPassword();
            }

        } catch (Exception ex) {
            return false;
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Stipend",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showDatabaseError(SQLException ex) {
        JOptionPane.showMessageDialog(
                this,
                "A database error occurred. Please try again.\n"
                        + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}

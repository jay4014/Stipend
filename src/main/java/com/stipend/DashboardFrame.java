package com.stipend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;

public class DashboardFrame extends JFrame {

    private final Student student;
    private final FinancialSystem financialSystem;
    private final int studentId;
    private final String studentName;

    private final Color navy = new Color(25, 42, 70);
    private final Color blue = new Color(53, 105, 180);
    private final Color background = new Color(245, 248, 252);

    private JLabel incomeValue;
    private JLabel expenseValue;
    private JLabel balanceValue;

    public DashboardFrame(Student student) {
        this.student = student;
        this.financialSystem = new FinancialSystem(student);
        this.studentId = student.getStudentId();
        this.studentName = student.getName();

        setTitle("Stipend | Dashboard");
        setSize(1000, 680);
        setMinimumSize(new Dimension(850, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        buildInterface();
        refreshSummary();
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(background);

        // Top navigation
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(navy);
        header.setBorder(new EmptyBorder(18, 25, 18, 25));

        JLabel logo = new JLabel("STIPEND");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 25));

        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headerRight.setOpaque(false);

        JLabel userLabel = new JLabel("Hello, " + studentName);
        userLabel.setForeground(Color.WHITE);
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JButton logoutButton = new JButton("Log Out");
        logoutButton.addActionListener(e -> logout());

        headerRight.add(userLabel);
        headerRight.add(logoutButton);

        header.add(logo, BorderLayout.WEST);
        header.add(headerRight, BorderLayout.EAST);

        // Main content
        JPanel content = new JPanel();
        content.setBackground(background);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(28, 30, 30, 30));

        JLabel title = new JLabel("Financial Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(navy);

        JLabel subtitle = new JLabel(
                "Monitor your money and manage your student budget."
        );
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(25));

        // Financial summary cards
        JPanel cards = new JPanel(new GridLayout(1, 3, 15, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 135));

        incomeValue = new JLabel("₱0.00");
        expenseValue = new JLabel("₱0.00");
        balanceValue = new JLabel("₱0.00");

        cards.add(createSummaryCard("TOTAL INCOME", incomeValue,
                new Color(30, 140, 90)));
        cards.add(createSummaryCard("TOTAL EXPENSES", expenseValue,
                new Color(200, 75, 75)));
        cards.add(createSummaryCard("REMAINING BALANCE", balanceValue,
                blue));

        content.add(cards);
        content.add(Box.createVerticalStrut(30));

        JLabel actionsTitle = new JLabel("Manage Your Finances");
        actionsTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        actionsTitle.setForeground(navy);

        content.add(actionsTitle);
        content.add(Box.createVerticalStrut(15));

        JPanel actions = new JPanel(new GridLayout(2, 2, 15, 15));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JButton addIncomeButton = createActionButton("＋ Add Income");
        JButton addExpenseButton = createActionButton("－ Add Expense");
        JButton transactionsButton = createActionButton("View Transactions");
        JButton reportButton = createActionButton("Monthly Report");

        addIncomeButton.addActionListener(e -> addTransaction("INCOME"));
        addExpenseButton.addActionListener(e -> addTransaction("EXPENSE"));
        transactionsButton.addActionListener(e -> viewTransactions());
        reportButton.addActionListener(e -> showMonthlyReport());

        actions.add(addIncomeButton);
        actions.add(addExpenseButton);
        actions.add(transactionsButton);
        actions.add(reportButton);

        content.add(actions);
        content.add(Box.createVerticalStrut(22));

        JLabel note = new JLabel(
                "Your financial summary updates when transactions are saved."
        );
        note.setForeground(Color.GRAY);
        note.setFont(new Font("SansSerif", Font.ITALIC, 12));
        content.add(note);

        root.add(header, BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel createSummaryCard(
            String title, JLabel value, Color accent) {

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                new EmptyBorder(20, 18, 18, 12)
        ));

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 11));
        heading.setForeground(Color.GRAY);

        value.setFont(new Font("SansSerif", Font.BOLD, 23));
        value.setForeground(accent);

        card.add(heading);
        card.add(Box.createVerticalStrut(18));
        card.add(value);

        return card;
    }

    private JButton createActionButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(Color.WHITE);
        button.setForeground(navy);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void refreshSummary() {
        try {
            double income = financialSystem.getTotalIncome();
            double expenses = financialSystem.getTotalExpenses();

            incomeValue.setText(String.format("₱%,.2f", income));
            expenseValue.setText(String.format("₱%,.2f", expenses));
            balanceValue.setText(
                    String.format("₱%,.2f", income - expenses)
            );
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addTransaction(String type) {
        JTextField amountField = new JTextField();
        JTextField categoryField = new JTextField();
        JTextField descriptionField = new JTextField();
        JTextField dateField = new JTextField(LocalDate.now().toString());

        JPanel form = new JPanel(new GridLayout(0, 1, 6, 6));
        form.add(new JLabel("Amount (PHP)"));
        form.add(amountField);
        form.add(new JLabel("Category (e.g. Allowance, Food, Transport)"));
        form.add(categoryField);
        form.add(new JLabel("Description (optional)"));
        form.add(descriptionField);
        form.add(new JLabel("Date (YYYY-MM-DD)"));
        form.add(dateField);

        int choice = JOptionPane.showConfirmDialog(
                this,
                form,
                "Add " + (type.equals("INCOME") ? "Income" : "Expense"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (choice != JOptionPane.OK_OPTION) {
            return;
        }

        double amount;
        LocalDate date;

        try {
            amount = Double.parseDouble(amountField.getText().trim());

            if (!Double.isFinite(amount) || amount <= 0) {
                throw new IllegalArgumentException();
            }

            date = LocalDate.parse(dateField.getText().trim());

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Enter a positive amount and a valid date (YYYY-MM-DD).",
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String category = categoryField.getText().trim();

        if (category.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this, "Please enter a category."
            );
            return;
        }

        try {
            Transaction transaction;

            if ("INCOME".equals(type)) {
                transaction = new Income(
                        amount,
                        category,
                        descriptionField.getText().trim(),
                        date.toString()
                );
            } else {
                transaction = new Expense(
                        amount,
                        category,
                        descriptionField.getText().trim(),
                        date.toString()
                );
            }

            financialSystem.addTransaction(transaction);

            JOptionPane.showMessageDialog(
                    this, "Transaction saved successfully!"
            );

            refreshSummary();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void viewTransactions() {
        String[] columns = {
                "ID", "Type", "Amount (PHP)",
                "Category", "Description", "Date"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        String sql = """
                SELECT transaction_id, type, amount,
                       category, description, transaction_date
                FROM transactions
                WHERE student_id = ?
                ORDER BY transaction_date DESC, transaction_id DESC
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    model.addRow(new Object[]{
                            result.getInt("transaction_id"),
                            result.getString("type"),
                            String.format("%.2f", result.getDouble("amount")),
                            result.getString("category"),
                            result.getString("description"),
                            result.getString("transaction_date")
                    });
                }
            }

            JTable table = new JTable(model);
            table.setRowHeight(25);
            table.setAutoCreateRowSorter(true);

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(750, 300));

            JOptionPane.showMessageDialog(
                    this,
                    scrollPane,
                    "Transaction History",
                    JOptionPane.PLAIN_MESSAGE
            );

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showMonthlyReport() {
        String input = JOptionPane.showInputDialog(
                this,
                "Enter month (YYYY-MM):",
                YearMonth.now().toString()
        );

        if (input == null) {
            return;
        }

        YearMonth month;

        try {
            month = YearMonth.parse(input.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a valid month, such as 2026-10.",
                    "Invalid Month",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);

        String sql = """
                SELECT
                    COALESCE(SUM(CASE WHEN type = 'INCOME'
                        THEN amount ELSE 0 END), 0) AS total_income,
                    COALESCE(SUM(CASE WHEN type = 'EXPENSE'
                        THEN amount ELSE 0 END), 0) AS total_expenses
                FROM transactions
                WHERE student_id = ?
                  AND transaction_date >= ?
                  AND transaction_date < ?
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, studentId);
            statement.setString(2, start.toString());
            statement.setString(3, end.toString());

            try (ResultSet result = statement.executeQuery()) {
                double income = result.getDouble("total_income");
                double expenses = result.getDouble("total_expenses");

                String report = String.format(
                        """
                        MONTHLY FINANCIAL SUMMARY
                        Month: %s

                        Total Income:    ₱%,.2f
                        Total Expenses:  ₱%,.2f
                        Remaining:       ₱%,.2f
                        """,
                        month, income, expenses, income - expenses
                );

                JTextArea area = new JTextArea(report);
                area.setEditable(false);
                area.setFont(new Font("Monospaced", Font.PLAIN, 14));
                area.setMargin(new Insets(12, 12, 12, 12));

                JOptionPane.showMessageDialog(
                        this,
                        area,
                        "Monthly Report",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void logout() {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
        }
    }

    private void showError(SQLException ex) {
        ex.printStackTrace();

        JOptionPane.showMessageDialog(
                this,
                "A database operation failed. Please try again.\n"
                        + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
        );
    }
}


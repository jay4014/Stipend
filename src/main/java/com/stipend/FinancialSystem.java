
package com.stipend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FinancialSystem {

    private final Student student;

    public FinancialSystem(Student student) {
        if (student == null) {
            throw new IllegalArgumentException(
                    "Student cannot be null."
            );
        }

        this.student = student;
    }

    public Student getStudent() {
        return student;
    }

    // Save an income or expense to the database
    public void addTransaction(Transaction transaction)
            throws SQLException {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "Transaction cannot be null."
            );
        }

        String type;

        if (transaction instanceof Income) {
            type = "INCOME";
        } else if (transaction instanceof Expense) {
            type = "EXPENSE";
        } else {
            throw new IllegalArgumentException(
                    "Unsupported transaction type."
            );
        }

        if (!Double.isFinite(transaction.getAmount())
                || transaction.getAmount() <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero."
            );
        }

        String sql = """
                INSERT INTO transactions
                (student_id, type, amount, category,
                 description, transaction_date)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, student.getStudentId());
            statement.setString(2, type);
            statement.setDouble(3, transaction.getAmount());
            statement.setString(4, transaction.getCategory());
            statement.setString(5, transaction.getDescription());
            statement.setString(6, transaction.getDate());

            statement.executeUpdate();
        }
    }

    // Load this student's transactions from SQLite
    public List<Transaction> getTransactions()
            throws SQLException {

        List<Transaction> transactions = new ArrayList<>();

        String sql = """
                SELECT type, amount, category,
                       description, transaction_date
                FROM transactions
                WHERE student_id = ?
                ORDER BY transaction_date DESC,
                         transaction_id DESC
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, student.getStudentId());

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {

                    String type = result.getString("type");
                    double amount = result.getDouble("amount");
                    String category = result.getString("category");
                    String description =
                            result.getString("description");
                    String date =
                            result.getString("transaction_date");

                    Transaction transaction;

                    if ("INCOME".equals(type)) {
                        transaction = new Income(
                                amount, category, description, date
                        );
                    } else {
                        transaction = new Expense(
                                amount, category, description, date
                        );
                    }

                    transactions.add(transaction);
                }
            }
        }

        return transactions;
    }

    public double getTotalIncome() throws SQLException {
        return getTotalByType("INCOME");
    }

    public double getTotalExpenses() throws SQLException {
        return getTotalByType("EXPENSE");
    }

    public double getBalance() throws SQLException {
        return getTotalIncome() - getTotalExpenses();
    }

    private double getTotalByType(String type)
            throws SQLException {

        String sql = """
                SELECT COALESCE(SUM(amount), 0)
                FROM transactions
                WHERE student_id = ? AND type = ?
                """;

        try (Connection connection = DatabaseManager.connect();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, student.getStudentId());
            statement.setString(2, type);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getDouble(1);
                }
            }
        }

        return 0;
    }
}

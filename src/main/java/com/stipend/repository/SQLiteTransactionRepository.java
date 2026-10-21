package com.stipend.repository;

import com.stipend.model.Expense;
import com.stipend.model.Income;
import com.stipend.model.Transaction;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * SQLite implementation of TransactionRepository.
 * All queries use PreparedStatement — never string concatenation —
 * to prevent SQL injection.
 */
public class SQLiteTransactionRepository implements TransactionRepository {

    // ---------------------------------------------------------------
    // Save
    // ---------------------------------------------------------------
    @Override
    public int save(int studentId, Transaction t) throws SQLException {
        String sql = """
            INSERT INTO transactions
              (student_id, wallet_id, type, amount, date,
               category, note, source, description)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps =
                     c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, studentId);
            ps.setInt(2, t.getWalletId() > 0 ? t.getWalletId() : 1);
            ps.setString(3, t instanceof Income ? "INCOME" : "EXPENSE");
            ps.setDouble(4, t.getAmount());
            ps.setString(5, t.getDate().toString());
            ps.setString(6, t.getCategory());
            ps.setString(7, t.getNote());
            ps.setString(8, t instanceof Income ? ((Income) t).getSource() : null);
            ps.setString(9, t instanceof Expense ? ((Expense) t).getDescription() : null);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------
    @Override
    public void update(Transaction t) throws SQLException {
        String sql = """
            UPDATE transactions
            SET wallet_id=?, type=?, amount=?, date=?,
                category=?, note=?, source=?, description=?
            WHERE transaction_id=?
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, t.getWalletId() > 0 ? t.getWalletId() : 1);
            ps.setString(2, t instanceof Income ? "INCOME" : "EXPENSE");
            ps.setDouble(3, t.getAmount());
            ps.setString(4, t.getDate().toString());
            ps.setString(5, t.getCategory());
            ps.setString(6, t.getNote());
            ps.setString(7, t instanceof Income ? ((Income) t).getSource() : null);
            ps.setString(8, t instanceof Expense ? ((Expense) t).getDescription() : null);
            ps.setInt(9, t.getTransactionId());

            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------
    @Override
    public void delete(int transactionId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, transactionId);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------
    // Find all
    // ---------------------------------------------------------------
    @Override
    public List<Transaction> findAll(int studentId) throws SQLException {
        String sql = """
            SELECT * FROM transactions
            WHERE student_id = ?
            ORDER BY date DESC, transaction_id DESC
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            return mapResultSet(ps.executeQuery());
        }
    }

    // ---------------------------------------------------------------
    // Find by month
    // ---------------------------------------------------------------
    @Override
    public List<Transaction> findByMonth(int studentId, YearMonth month) throws SQLException {
        String sql = """
            SELECT * FROM transactions
            WHERE student_id = ?
              AND strftime('%Y-%m', date) = ?
            ORDER BY date DESC, transaction_id DESC
            """;

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, month.toString());   // "YYYY-MM"
            return mapResultSet(ps.executeQuery());
        }
    }

    // ---------------------------------------------------------------
    // Filtered search
    // ---------------------------------------------------------------
    @Override
    public List<Transaction> findFiltered(int studentId,
                                          String type,
                                          String category,
                                          LocalDate from,
                                          LocalDate to,
                                          String keyword) throws SQLException {

        StringBuilder sql = new StringBuilder(
            "SELECT * FROM transactions WHERE student_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(studentId);

        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("ALL")) {
            sql.append(" AND type = ?");
            params.add(type.toUpperCase());
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (from != null) {
            sql.append(" AND date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND date <= ?");
            params.add(to.toString());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (note LIKE ? OR source LIKE ? OR description LIKE ? OR category LIKE ?)");
            String like = "%" + keyword + "%";
            params.add(like); params.add(like); params.add(like); params.add(like);
        }
        sql.append(" ORDER BY date DESC, transaction_id DESC");

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object val = params.get(i);
                if (val instanceof String s) ps.setString(i + 1, s);
                else if (val instanceof Integer iv) ps.setInt(i + 1, iv);
                else ps.setObject(i + 1, val);
            }
            return mapResultSet(ps.executeQuery());
        }
    }

    // ---------------------------------------------------------------
    // Aggregate helpers
    // ---------------------------------------------------------------
    @Override
    public double totalIncome(int studentId) throws SQLException {
        return sumByType(studentId, "INCOME", null);
    }

    @Override
    public double totalExpenses(int studentId) throws SQLException {
        return sumByType(studentId, "EXPENSE", null);
    }

    @Override
    public double totalIncomeForMonth(int studentId, YearMonth month) throws SQLException {
        return sumByType(studentId, "INCOME", month);
    }

    @Override
    public double totalExpensesForMonth(int studentId, YearMonth month) throws SQLException {
        return sumByType(studentId, "EXPENSE", month);
    }

    private double sumByType(int studentId, String type, YearMonth month) throws SQLException {
        String sql = month == null
            ? "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE student_id=? AND type=?"
            : "SELECT COALESCE(SUM(amount),0) FROM transactions WHERE student_id=? AND type=? AND strftime('%Y-%m',date)=?";

        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, type);
            if (month != null) ps.setString(3, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    // ---------------------------------------------------------------
    // Category breakdown
    // ---------------------------------------------------------------
    @Override
    public Map<String, Double> expensesByCategory(int studentId, YearMonth month)
            throws SQLException {

        String sql = """
            SELECT category, COALESCE(SUM(amount),0) AS total
            FROM transactions
            WHERE student_id = ? AND type = 'EXPENSE'
              AND strftime('%Y-%m', date) = ?
            GROUP BY category
            ORDER BY total DESC
            """;

        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("category"), rs.getDouble("total"));
                }
            }
        }
        return map;
    }

    // ---------------------------------------------------------------
    // Daily totals (for the trend chart)
    // ---------------------------------------------------------------
    @Override
    public Map<String, Double> dailyExpenses(int studentId, YearMonth month)
            throws SQLException {

        String sql = """
            SELECT date, COALESCE(SUM(amount),0) AS total
            FROM transactions
            WHERE student_id = ? AND type = 'EXPENSE'
              AND strftime('%Y-%m', date) = ?
            GROUP BY date
            ORDER BY date
            """;

        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setString(2, month.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(rs.getString("date"), rs.getDouble("total"));
                }
            }
        }
        return map;
    }

    // ---------------------------------------------------------------
    // ResultSet → Transaction list (POLYMORPHISM: creates Income or
    // Expense depending on the 'type' column; caller gets List<Transaction>)
    // ---------------------------------------------------------------
    private List<Transaction> mapResultSet(ResultSet rs) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        while (rs.next()) {
            String type = rs.getString("type");
            double amount = rs.getDouble("amount");
            LocalDate date = LocalDate.parse(rs.getString("date"));
            String category = rs.getString("category");
            String note = rs.getString("note");
            int walletId = rs.getInt("wallet_id");

            Transaction t;
            if ("INCOME".equals(type)) {
                String source = rs.getString("source");
                t = new Income(amount, date, category, note, walletId, source);
            } else {
                String desc = rs.getString("description");
                t = new Expense(amount, date, category, note, walletId, desc);
            }
            t.setTransactionId(rs.getInt("transaction_id"));
            list.add(t);
        }
        return list;
    }
}

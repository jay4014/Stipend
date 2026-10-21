package com.stipend.service;

import com.stipend.model.MonthlyReport;
import com.stipend.repository.DatabaseManager;
import com.stipend.repository.SQLiteTransactionRepository;
import com.stipend.repository.TransactionRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * FinancialSystem: central service that computes summary figures and
 * generates MonthlyReport objects.
 */
public final class FinancialSystem {

    private static final TransactionRepository REPO = new SQLiteTransactionRepository();

    private FinancialSystem() {}

    // ---------------------------------------------------------------
    // Balance helpers
    // ---------------------------------------------------------------
    public static double getTotalIncome(int studentId) throws SQLException {
        return REPO.totalIncome(studentId);
    }

    public static double getTotalExpenses(int studentId) throws SQLException {
        return REPO.totalExpenses(studentId);
    }

    public static double getBalance(int studentId) throws SQLException {
        return getTotalIncome(studentId) - getTotalExpenses(studentId);
    }

    public static double getTotalIncomeForMonth(int studentId, YearMonth month)
            throws SQLException {
        return REPO.totalIncomeForMonth(studentId, month);
    }

    public static double getTotalExpensesForMonth(int studentId, YearMonth month)
            throws SQLException {
        return REPO.totalExpensesForMonth(studentId, month);
    }

    // ---------------------------------------------------------------
    // Safe-to-spend today (section 8 rule)
    // max(0, remainingBalance) / daysLeftInMonth (including today)
    // ---------------------------------------------------------------
    public static double safeToSpendToday(int studentId) throws SQLException {
        YearMonth now = YearMonth.now();
        double income   = REPO.totalIncomeForMonth(studentId, now);
        double expenses = REPO.totalExpensesForMonth(studentId, now);
        double balance  = Math.max(0, income - expenses);

        LocalDate today   = LocalDate.now();
        LocalDate lastDay = now.atEndOfMonth();
        long daysLeft = ChronoUnit.DAYS.between(today, lastDay) + 1; // include today
        if (daysLeft <= 0) daysLeft = 1;

        return balance / daysLeft;
    }

    // ---------------------------------------------------------------
    // Monthly report generation
    // ---------------------------------------------------------------
    public static MonthlyReport generateMonthlyReport(int studentId, YearMonth month)
            throws SQLException {

        double income   = REPO.totalIncomeForMonth(studentId, month);
        double expenses = REPO.totalExpensesForMonth(studentId, month);
        Map<String, Double> catTotals = REPO.expensesByCategory(studentId, month);

        // Top category = first entry (map is ordered by total DESC)
        String top = catTotals.isEmpty() ? "—" : catTotals.keySet().iterator().next();

        return new MonthlyReport(studentId, month, income, expenses, top, catTotals);
    }

    // ---------------------------------------------------------------
    // Savings rate for current month (for the KPI card)
    // ---------------------------------------------------------------
    public static double savingsRateThisMonth(int studentId) throws SQLException {
        YearMonth now = YearMonth.now();
        double income = REPO.totalIncomeForMonth(studentId, now);
        if (income <= 0) return 0.0;
        double expenses = REPO.totalExpensesForMonth(studentId, now);
        return Math.max(0, (income - expenses) / income * 100.0);
    }

    /** Expose the shared repository for panels that need direct access. */
    public static TransactionRepository getRepo() { return REPO; }
}

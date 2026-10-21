package com.stipend.repository;

import com.stipend.model.Transaction;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * ABSTRACTION: TransactionRepository defines what operations the
 * storage layer must support.  The UI and services only talk to
 * this interface, so the backing store can be swapped (SQLite →
 * JSON → cloud) without touching a single controller.
 */
public interface TransactionRepository {

    /** Persist a new transaction and return the generated ID. */
    int save(int studentId, Transaction t) throws SQLException;

    /** Update an existing transaction by its ID. */
    void update(Transaction t) throws SQLException;

    /** Delete a transaction by ID. */
    void delete(int transactionId) throws SQLException;

    /** All transactions for a student, newest first. */
    List<Transaction> findAll(int studentId) throws SQLException;

    /** Transactions for a specific month. */
    List<Transaction> findByMonth(int studentId, YearMonth month) throws SQLException;

    /** Transactions filtered by type, category, date range, keyword. */
    List<Transaction> findFiltered(int studentId,
                                   String type,
                                   String category,
                                   LocalDate from,
                                   LocalDate to,
                                   String keyword) throws SQLException;

    /** Total income for a student (all time). */
    double totalIncome(int studentId) throws SQLException;

    /** Total expenses for a student (all time). */
    double totalExpenses(int studentId) throws SQLException;

    /** Total income for a specific month. */
    double totalIncomeForMonth(int studentId, YearMonth month) throws SQLException;

    /** Total expenses for a specific month. */
    double totalExpensesForMonth(int studentId, YearMonth month) throws SQLException;

    /** Expense totals grouped by category for a given month. */
    Map<String, Double> expensesByCategory(int studentId, YearMonth month) throws SQLException;

    /** Daily expense totals for a given month (date-string → amount). */
    Map<String, Double> dailyExpenses(int studentId, YearMonth month) throws SQLException;
}

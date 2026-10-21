package com.stipend.model;

import java.time.YearMonth;

/**
 * Budget: monthly spending limit per category.
 *
 * ENCAPSULATION: all fields private, getStatus() derives the display
 * state from the data without exposing mutable internals.
 */
public class Budget {

    public enum Status { GOOD, TIGHT, EXCEEDED }

    private int       budgetId;
    private int       studentId;
    private String    category;
    private double    monthlyLimit;
    private YearMonth month;
    private double    spent; // computed by BudgetService, not stored in DB

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public Budget(int budgetId, int studentId, String category,
                  double monthlyLimit, YearMonth month) {
        this.budgetId = budgetId;
        this.studentId = studentId;
        setCategory(category);
        setMonthlyLimit(monthlyLimit);
        setMonth(month);
        this.spent = 0.0;
    }

    // ---------------------------------------------------------------
    // Business rules (section 8)
    // ---------------------------------------------------------------

    /** Percentage of the limit already used: 0–100+. */
    public double getPercentUsed() {
        if (monthlyLimit <= 0) return 0;
        return (spent / monthlyLimit) * 100.0;
    }

    /**
     * Status drives the progress-bar colour in the UI:
     *   GOOD    → green  (< 50 %)
     *   TIGHT   → orange (50 % – 99 %)
     *   EXCEEDED→ red    (≥ 100 %)
     */
    public Status getStatus() {
        double pct = getPercentUsed();
        if (pct >= 100) return Status.EXCEEDED;
        if (pct >= 50)  return Status.TIGHT;
        return Status.GOOD;
    }

    /** How much remains before the limit is hit (can be negative). */
    public double getRemaining() {
        return monthlyLimit - spent;
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------
    public int       getBudgetId()     { return budgetId;     }
    public int       getStudentId()    { return studentId;    }
    public String    getCategory()     { return category;     }
    public double    getMonthlyLimit() { return monthlyLimit; }
    public YearMonth getMonth()        { return month;        }
    public double    getSpent()        { return spent;        }

    // ---------------------------------------------------------------
    // Setters — ENCAPSULATION with validation
    // ---------------------------------------------------------------
    public void setCategory(String category) {
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Budget category must not be blank.");
        this.category = category.trim();
    }

    public void setMonthlyLimit(double limit) {
        if (limit <= 0)
            throw new IllegalArgumentException("Monthly limit must be > 0.");
        this.monthlyLimit = Math.round(limit * 100.0) / 100.0;
    }

    public void setMonth(YearMonth month) {
        if (month == null)
            throw new IllegalArgumentException("Month must not be null.");
        this.month = month;
    }

    /** Called by BudgetService after aggregating expenses. */
    public void setSpent(double spent) {
        this.spent = Math.max(0, spent);
    }
}

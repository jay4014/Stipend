package com.stipend.model;

import com.stipend.repository.Exportable;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * MonthlyReport: aggregated summary for a calendar month.
 *
 * ABSTRACTION: implements Exportable, so ExportService can treat any
 * exportable object uniformly without knowing its concrete type.
 */
public class MonthlyReport implements Exportable {

    private final int       studentId;
    private final YearMonth month;
    private final double    totalIncome;
    private final double    totalExpenses;
    private final String    topCategory;

    // Category → total spent map (sorted by value descending)
    private final Map<String, Double> categoryTotals;

    // ---------------------------------------------------------------
    // Constructor — immutable after construction (no setters needed)
    // ---------------------------------------------------------------
    public MonthlyReport(int studentId, YearMonth month,
                         double totalIncome, double totalExpenses,
                         String topCategory,
                         Map<String, Double> categoryTotals) {
        this.studentId      = studentId;
        this.month          = month;
        this.totalIncome    = totalIncome;
        this.totalExpenses  = totalExpenses;
        this.topCategory    = topCategory;
        this.categoryTotals = categoryTotals != null
                                ? new LinkedHashMap<>(categoryTotals)
                                : new LinkedHashMap<>();
    }

    // ---------------------------------------------------------------
    // Business calculations
    // ---------------------------------------------------------------

    public double getBalance() {
        return totalIncome - totalExpenses;
    }

    /**
     * Savings rate = (income – expenses) / income × 100.
     * Returns 0 % when income is 0 (section 8 rule).
     */
    public double getSavingsRate() {
        if (totalIncome <= 0) return 0.0;
        return Math.max(0.0, (totalIncome - totalExpenses) / totalIncome * 100.0);
    }

    // ---------------------------------------------------------------
    // Exportable — ABSTRACTION
    // ---------------------------------------------------------------
    @Override
    public String getCsvHeader() {
        return "Month,TotalIncome,TotalExpenses,Balance,SavingsRate%,TopCategory";
    }

    @Override
    public String toCsvRow() {
        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%s",
            month, totalIncome, totalExpenses, getBalance(),
            getSavingsRate(), topCategory);
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------
    public int                 getStudentId()     { return studentId;     }
    public YearMonth           getMonth()          { return month;          }
    public double              getTotalIncome()    { return totalIncome;    }
    public double              getTotalExpenses()  { return totalExpenses;  }
    public String              getTopCategory()    { return topCategory;    }
    public Map<String, Double> getCategoryTotals() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(categoryTotals));
    }

    @Override
    public String toString() {
        return String.format(
            "MonthlyReport[%s | Income=₱%,.2f | Expenses=₱%,.2f | " +
            "Balance=₱%,.2f | Savings=%.1f%%]",
            month, totalIncome, totalExpenses, getBalance(), getSavingsRate());
    }
}

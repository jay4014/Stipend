package com.stipend.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Savings Goal.
 *
 * ENCAPSULATION: targetAmount and savedAmount are private; contributions
 * go through addContribution() which validates the amount.
 */
public class Goal {

    private int       goalId;
    private int       studentId;
    private String    name;
    private double    targetAmount;
    private double    savedAmount;
    private LocalDate targetDate;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public Goal(int goalId, int studentId, String name,
                double targetAmount, double savedAmount, LocalDate targetDate) {
        this.goalId    = goalId;
        this.studentId = studentId;
        setName(name);
        setTargetAmount(targetAmount);
        this.savedAmount = Math.max(0, savedAmount);
        this.targetDate  = targetDate;
    }

    // ---------------------------------------------------------------
    // Business methods
    // ---------------------------------------------------------------

    /** Adds a contribution; throws if amount ≤ 0. */
    public void addContribution(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Contribution must be > 0.");
        this.savedAmount = Math.round((savedAmount + amount) * 100.0) / 100.0;
    }

    /** 0–100 (can exceed 100 if over-saved). */
    public double getProgressPercent() {
        if (targetAmount <= 0) return 0;
        return Math.min(100.0, (savedAmount / targetAmount) * 100.0);
    }

    public boolean isReached() {
        return savedAmount >= targetAmount;
    }

    /** Days remaining until the target date (negative if overdue). */
    public long getDaysRemaining() {
        if (targetDate == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }

    /** How much more needs to be saved. */
    public double getRemainingAmount() {
        return Math.max(0, targetAmount - savedAmount);
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------
    public int       getGoalId()       { return goalId;       }
    public int       getStudentId()    { return studentId;    }
    public String    getName()         { return name;         }
    public double    getTargetAmount() { return targetAmount; }
    public double    getSavedAmount()  { return savedAmount;  }
    public LocalDate getTargetDate()   { return targetDate;   }

    // ---------------------------------------------------------------
    // Setters — ENCAPSULATION
    // ---------------------------------------------------------------
    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Goal name must not be blank.");
        this.name = name.trim();
    }

    public void setTargetAmount(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Target amount must be > 0.");
        this.targetAmount = Math.round(amount * 100.0) / 100.0;
    }

    public void setSavedAmount(double amount) {
        this.savedAmount = Math.max(0, amount);
    }

    public void setTargetDate(LocalDate date) {
        this.targetDate = date;
    }
}

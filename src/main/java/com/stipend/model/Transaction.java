package com.stipend.model;

import com.stipend.repository.Exportable;
import java.time.LocalDate;

/**
 * ABSTRACTION + INHERITANCE ROOT:
 * Transaction is abstract — it defines the shared contract for all
 * financial entries. Subclasses (Income, Expense) must implement
 * displayTxn() and getSignedAmount(), enabling polymorphic handling
 * throughout the system.
 *
 * ENCAPSULATION: All fields are private. Setters validate input and
 * throw IllegalArgumentException on bad data so invalid state is
 * impossible to construct.
 */
public abstract class Transaction implements Exportable {

    // === Private fields — encapsulation ===
    private int    transactionId;
    private double amount;
    private LocalDate date;
    private String category;
    private String note;
    private int    walletId;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    protected Transaction(double amount, LocalDate date,
                          String category, String note, int walletId) {
        setAmount(amount);
        setDate(date);
        setCategory(category);
        this.note     = note == null ? "" : note.trim();
        this.walletId = walletId;
    }

    // ---------------------------------------------------------------
    // Abstract methods — POLYMORPHISM contract
    // Each subclass must provide its own formatted description and
    // its own signed contribution to the balance calculation.
    // ---------------------------------------------------------------

    /** POLYMORPHISM: overridden differently in Income and Expense. */
    public abstract String displayTxn();

    /**
     * POLYMORPHISM: Income returns a positive value; Expense returns
     * a negative value.  The UI can sum a List<Transaction> without
     * knowing the concrete type.
     */
    public abstract double getSignedAmount();

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------
    public int       getTransactionId() { return transactionId; }
    public double    getAmount()        { return amount;        }
    public LocalDate getDate()          { return date;          }
    public String    getCategory()      { return category;      }
    public String    getNote()          { return note;          }
    public int       getWalletId()      { return walletId;      }

    // ---------------------------------------------------------------
    // Setters with validation — ENCAPSULATION
    // ---------------------------------------------------------------
    public void setTransactionId(int id) {
        if (id < 0) throw new IllegalArgumentException("Transaction ID must be >= 0.");
        this.transactionId = id;
    }

    public void setAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0)
            throw new IllegalArgumentException(
                "Amount must be a positive finite number, got: " + amount);
        // Enforce at most 2 decimal places by rounding
        this.amount = Math.round(amount * 100.0) / 100.0;
    }

    public void setDate(LocalDate date) {
        if (date == null)
            throw new IllegalArgumentException("Date must not be null.");
        this.date = date;
    }

    public void setCategory(String category) {
        if (category == null || category.isBlank())
            throw new IllegalArgumentException("Category must not be blank.");
        this.category = category.trim();
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note.trim();
    }

    public void setWalletId(int walletId) {
        this.walletId = walletId;
    }

    // ---------------------------------------------------------------
    // Exportable interface — ABSTRACTION
    // ---------------------------------------------------------------
    @Override
    public String getCsvHeader() {
        return "ID,Type,Amount,Date,Category,Note,WalletId";
    }

    // ---------------------------------------------------------------
    // toString for debug / logging
    // ---------------------------------------------------------------
    @Override
    public String toString() {
        return displayTxn();
    }
}

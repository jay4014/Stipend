package com.stipend.model;

import java.time.LocalDate;

/**
 * INHERITANCE: Expense extends Transaction.
 * POLYMORPHISM: displayTxn() returns an Expense-specific format.
 *              getSignedAmount() returns a negative value.
 *
 * Extra field: description — what the money was spent on.
 */
public class Expense extends Transaction {

    // Default expense categories
    public static final String[] CATEGORIES = {
        "Food", "Transport", "School", "Load/Internet",
        "Bills", "Shopping", "Health", "Entertainment", "Other"
    };

    // ENCAPSULATION: private, validated in setter
    private String description;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public Expense(double amount, LocalDate date,
                   String category, String note, int walletId, String description) {
        super(amount, date, category, note, walletId);
        setDescription(description);
    }

    // ---------------------------------------------------------------
    // Polymorphic implementations
    // ---------------------------------------------------------------

    /**
     * POLYMORPHISM: Expense-specific display prefixed with [-] to
     * visually distinguish from Income.
     */
    @Override
    public String displayTxn() {
        return String.format(
            "[-] EXPENSE | ₱%,.2f | %s | %s | Desc: %s | Note: %s",
            getAmount(), getDate(), getCategory(), description,
            getNote().isEmpty() ? "—" : getNote()
        );
    }

    /**
     * POLYMORPHISM: negative amount reduces the balance when the UI
     * sums List<Transaction> uniformly without instanceof checks.
     */
    @Override
    public double getSignedAmount() {
        return -getAmount();   // negative — reduces balance
    }

    // ---------------------------------------------------------------
    // Exportable (from Transaction → Exportable interface)
    // ---------------------------------------------------------------
    @Override
    public String toCsvRow() {
        return String.format("%d,EXPENSE,%.2f,%s,%s,%s,%d",
            getTransactionId(), getAmount(), getDate(),
            getCategory(), getNote(), getWalletId());
    }

    // ---------------------------------------------------------------
    // Getter / Setter — ENCAPSULATION
    // ---------------------------------------------------------------
    public String getDescription() { return description; }

    public void setDescription(String description) {
        // Description is optional for Expense; empty string is fine
        this.description = (description == null) ? "" : description.trim();
    }
}

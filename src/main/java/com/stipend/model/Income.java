package com.stipend.model;

import java.time.LocalDate;

/**
 * INHERITANCE: Income extends Transaction.
 * POLYMORPHISM: displayTxn() and getSignedAmount() give Income-specific
 * behaviour.  The UI loop never needs instanceof to sum totals.
 *
 * Extra field: source — where the money came from (Allowance, Salary, etc.)
 */
public class Income extends Transaction {

    // Default categories offered in the UI (not enforced here — Validator does that)
    public static final String[] CATEGORIES = {
        "Allowance", "Salary/Part-time", "Gift", "Scholarship", "Other"
    };

    // ENCAPSULATION: private field, validated in setter
    private String source;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public Income(double amount, LocalDate date,
                  String category, String note, int walletId, String source) {
        super(amount, date, category, note, walletId);
        setSource(source);
    }

    // ---------------------------------------------------------------
    // Polymorphic implementations
    // ---------------------------------------------------------------

    /**
     * POLYMORPHISM: Income-specific display, prefixed with [+] to
     * visually distinguish from Expense in logs and exports.
     */
    @Override
    public String displayTxn() {
        return String.format(
            "[+] INCOME | ₱%,.2f | %s | %s | Source: %s | Note: %s",
            getAmount(), getDate(), getCategory(), source,
            getNote().isEmpty() ? "—" : getNote()
        );
    }

    /**
     * POLYMORPHISM: positive amount contributes positively to the
     * balance when the UI sums List<Transaction> uniformly.
     */
    @Override
    public double getSignedAmount() {
        return getAmount();   // positive — increases balance
    }

    // ---------------------------------------------------------------
    // Exportable (from Transaction → Exportable interface)
    // ---------------------------------------------------------------
    @Override
    public String toCsvRow() {
        return String.format("%d,INCOME,%.2f,%s,%s,%s,%d",
            getTransactionId(), getAmount(), getDate(),
            getCategory(), getNote(), getWalletId());
    }

    // ---------------------------------------------------------------
    // Getter / Setter — ENCAPSULATION
    // ---------------------------------------------------------------
    public String getSource() { return source; }

    public void setSource(String source) {
        // Source is optional for Income; default to "Other" if blank
        this.source = (source == null || source.isBlank()) ? "Other" : source.trim();
    }
}

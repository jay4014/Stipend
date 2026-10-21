package com.stipend;

public class Expense extends Transaction {

    public Expense(double amount, String category,
                   String description, String date) {
        super(amount, category, description, date);
    }

    @Override
    public String displayTxn() {
        return "EXPENSE | Category: " + getCategory()
                + " | Amount: ₱" + getAmount()
                + " | Date: " + getDate()
                + " | Description: " + getDescription();
    }
}

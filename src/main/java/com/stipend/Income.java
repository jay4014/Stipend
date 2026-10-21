package com.stipend;

public class Income extends Transaction {

    public Income(double amount, String category,
                  String description, String date) {
        super(amount, category, description, date);
    }

    @Override
    public String displayTxn() {
        return "INCOME | Category: " + getCategory()
                + " | Amount: ₱" + getAmount()
                + " | Date: " + getDate()
                + " | Description: " + getDescription();
    }
}

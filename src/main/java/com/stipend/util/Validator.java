package com.stipend.util;

import java.time.LocalDate;

/**
 * Validator: static helpers for validating user input.
 * All methods return an error message String, or null if valid.
 * The UI shows the returned string as an inline error label.
 */
public final class Validator {

    private Validator() {}

    // ---------------------------------------------------------------
    // Name / text
    // ---------------------------------------------------------------
    public static String validateNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank())
            return fieldName + " must not be empty.";
        return null;
    }

    // ---------------------------------------------------------------
    // Username: 4–20 chars, letters/numbers/underscores only
    // ---------------------------------------------------------------
    public static String validateUsername(String username) {
        if (username == null || username.isBlank())
            return "Username must not be empty.";
        if (username.length() < 4)
            return "Username must be at least 4 characters.";
        if (username.length() > 20)
            return "Username must be at most 20 characters.";
        if (!username.matches("[A-Za-z0-9_]+"))
            return "Username may only contain letters, numbers, and underscores.";
        return null;
    }

    // ---------------------------------------------------------------
    // Password: ≥ 8 chars, at least one letter AND one digit
    // ---------------------------------------------------------------
    public static String validatePassword(char[] password) {
        if (password == null || password.length < 8)
            return "Password must be at least 8 characters.";
        boolean hasLetter = false, hasDigit = false;
        for (char c : password) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit  = true;
        }
        if (!hasLetter) return "Password must contain at least one letter.";
        if (!hasDigit)  return "Password must contain at least one number.";
        return null;
    }

    public static String validatePasswordsMatch(char[] a, char[] b) {
        if (a == null || b == null || a.length != b.length)
            return "Passwords do not match.";
        for (int i = 0; i < a.length; i++)
            if (a[i] != b[i]) return "Passwords do not match.";
        return null;
    }

    // ---------------------------------------------------------------
    // Amount: positive, finite, at most 2 decimal places
    // ---------------------------------------------------------------
    public static String validateAmount(String raw) {
        if (raw == null || raw.isBlank())
            return "Amount must not be empty.";
        double val;
        try {
            val = Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return "Amount must be a valid number.";
        }
        if (!Double.isFinite(val) || val <= 0)
            return "Amount must be greater than 0.";
        // Check at most 2 decimal places
        String[] parts = raw.trim().split("\\.");
        if (parts.length == 2 && parts[1].length() > 2)
            return "Amount may have at most 2 decimal places.";
        return null;
    }

    // ---------------------------------------------------------------
    // Date: valid, and not in the future for regular expenses
    // ---------------------------------------------------------------
    public static String validateDate(String raw) {
        if (raw == null || raw.isBlank())
            return "Date must not be empty.";
        try {
            LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return "Date must be in YYYY-MM-DD format.";
        }
        return null;
    }

    public static String validateNotFuture(String raw) {
        String dateErr = validateDate(raw);
        if (dateErr != null) return dateErr;
        LocalDate d = LocalDate.parse(raw.trim());
        if (d.isAfter(LocalDate.now()))
            return "Date cannot be in the future.";
        return null;
    }
}

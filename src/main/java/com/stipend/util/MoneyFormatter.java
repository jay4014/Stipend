package com.stipend.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formats monetary amounts as Philippine Peso strings. */
public final class MoneyFormatter {

    private static final DecimalFormat FMT;

    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.US);
        sym.setGroupingSeparator(',');
        sym.setDecimalSeparator('.');
        FMT = new DecimalFormat("#,##0.00", sym);
    }

    private MoneyFormatter() {}

    /** Returns "₱1,250.00" for 1250.0 */
    public static String format(double amount) {
        return "₱" + FMT.format(amount);
    }

    /** Returns "+₱500.00" for income, "-₱200.00" for expenses */
    public static String formatSigned(double signedAmount) {
        if (signedAmount >= 0) return "+" + format(signedAmount);
        return "-" + format(-signedAmount);
    }
}

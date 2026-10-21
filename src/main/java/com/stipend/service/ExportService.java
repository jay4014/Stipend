package com.stipend.service;

import com.stipend.model.MonthlyReport;
import com.stipend.model.Transaction;
import com.stipend.repository.Exportable;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

/**
 * ExportService: writes CSV files for transaction lists and monthly reports.
 * Works with the Exportable interface — does not need to know concrete types.
 */
public final class ExportService {

    private ExportService() {}

    /**
     * Exports a list of Transaction objects (POLYMORPHISM: each transaction
     * provides its own toCsvRow() implementation).
     *
     * @param filePath absolute path for the output CSV file
     * @param transactions list of transactions to export
     * @return null on success, or an error message
     */
    public static String exportTransactions(String filePath, List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty())
            return "No transactions to export.";

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            // Write header from the first item (Exportable interface)
            pw.println(transactions.get(0).getCsvHeader());
            // POLYMORPHISM: each subclass produces its own CSV row
            for (Transaction t : transactions) {
                pw.println(t.toCsvRow());
            }
            return null;
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }
    }

    /**
     * Exports a MonthlyReport to CSV (also implements Exportable).
     *
     * @param filePath absolute path for the output CSV file
     * @param report   the monthly report to export
     * @return null on success, or an error message
     */
    public static String exportReport(String filePath, MonthlyReport report) {
        if (report == null) return "No report to export.";

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println(report.getCsvHeader());
            pw.println(report.toCsvRow());
            pw.println();
            pw.println("Category,Amount");
            report.getCategoryTotals().forEach((cat, amt) ->
                pw.printf("%s,%.2f%n", cat, amt));
            return null;
        } catch (IOException e) {
            return "Export failed: " + e.getMessage();
        }
    }
}

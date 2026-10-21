package com.stipend.ui.panels;

import com.stipend.model.MonthlyReport;
import com.stipend.model.Student;
import com.stipend.model.Transaction;
import com.stipend.service.ExportService;
import com.stipend.service.FinancialSystem;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * ReportsPanel: month picker, KPI summary, category breakdown table,
 * and Export CSV button.
 */
public class ReportsPanel extends JPanel {

    private final Student student;
    private YearMonth selectedMonth = YearMonth.now();

    private final JLabel  monthLabel;
    private final JLabel  incomeVal, expensesVal, balanceVal, savingsVal;
    private final DefaultTableModel catTableModel;

    public ReportsPanel(Student student) {
        this.student = student;

        monthLabel  = label(formatMonth(selectedMonth), FONT_SUBHEAD, ACCENT_1);
        incomeVal   = kpi();
        expensesVal = kpi();
        balanceVal  = kpi();
        savingsVal  = kpi();

        catTableModel = new DefaultTableModel(
            new String[]{"Category", "Amount", "% of Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        setBackground(BG_MAIN);
        setLayout(new BorderLayout());
        buildUI();
    }

    // ---------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------
    private void buildUI() {
        JPanel wrapper = new JPanel(new BorderLayout(0, GAP));
        wrapper.setBackground(BG_MAIN);
        wrapper.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, GAP));
        center.setBackground(BG_MAIN);
        center.add(buildSummaryRow(), BorderLayout.NORTH);
        center.add(buildCategoryTable(), BorderLayout.CENTER);

        wrapper.add(center, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_MAIN);
        bar.setBorder(new EmptyBorder(0, 0, GAP, 0));

        JPanel left = new JPanel();
        left.setBackground(BG_MAIN);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Monthly Report", FONT_TITLE, TEXT_PRIMARY));
        left.add(Box.createVerticalStrut(2));
        left.add(label("Financial summary for the selected month", FONT_BODY, TEXT_SECONDARY));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(BG_MAIN);

        JButton prevBtn = CoinSciousTheme.ghostButton("◀");
        prevBtn.addActionListener(e -> changeMonth(-1));
        JButton nextBtn = CoinSciousTheme.ghostButton("▶");
        nextBtn.addActionListener(e -> changeMonth(1));
        JButton exportBtn = CoinSciousTheme.primaryButton("⬇  Export CSV");
        exportBtn.setPreferredSize(new Dimension(160, 36));
        exportBtn.addActionListener(e -> exportCsv());

        right.add(prevBtn); right.add(monthLabel); right.add(nextBtn);
        right.add(Box.createHorizontalStrut(12));
        right.add(exportBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, GAP, 0));
        row.setBackground(BG_MAIN);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        row.add(buildKpiCard("Total Income",    incomeVal,   ACCENT_TEAL));
        row.add(buildKpiCard("Total Expenses",  expensesVal, DANGER));
        row.add(buildKpiCard("Net Balance",     balanceVal,  ACCENT_1));
        row.add(buildKpiCard("Savings Rate",    savingsVal,  SUCCESS));

        return row;
    }

    private JPanel buildKpiCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BorderLayout());

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(0, 3));

        JLabel titleLbl = label(title, new Font("Segoe UI", Font.PLAIN, 12), TEXT_SECONDARY);

        card.add(stripe,    BorderLayout.NORTH);
        card.add(titleLbl,  BorderLayout.CENTER);
        card.add(valueLabel, BorderLayout.SOUTH);
        return card;
    }

    private JLabel kpi() {
        JLabel l = new JLabel("—");
        l.setFont(new Font("Segoe UI", Font.BOLD, 22));
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private JPanel buildCategoryTable() {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.add(label("Spending by Category", FONT_SUBHEAD, TEXT_PRIMARY), BorderLayout.NORTH);

        JTable catTable = buildTable(catTableModel);
        JScrollPane scroll = new JScrollPane(catTable);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setBackground(BG_CARD);
        t.setForeground(TEXT_PRIMARY);
        t.setFont(FONT_BODY);
        t.setRowHeight(32);
        t.setGridColor(BORDER_SUBTLE);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        t.getTableHeader().setBackground(BG_SIDEBAR);
        t.getTableHeader().setForeground(TEXT_SECONDARY);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.getTableHeader().setPreferredSize(new Dimension(0, 36));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBackground(BG_SIDEBAR);
                setForeground(TEXT_SECONDARY);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        t.setSelectionBackground(ACCENT_2.darker());
        t.setSelectionForeground(TEXT_PRIMARY);

        DefaultTableCellRenderer centered = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBackground(selected ? ACCENT_2.darker()
                    : (row % 2 == 0 ? BG_CARD : BG_CARD_HOVER));
                setForeground(column == 1 ? DANGER : TEXT_PRIMARY);
                setFont(column == 1 ? FONT_BOLD : FONT_BODY);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        for (int column = 0; column < t.getColumnCount(); column++) {
            t.getColumnModel().getColumn(column).setCellRenderer(centered);
        }

        return t;
    }

    // ---------------------------------------------------------------
    // Refresh
    // ---------------------------------------------------------------
    public void refresh() {
        YearMonth requestedMonth = selectedMonth;
        SwingWorker<MonthlyReport, Void> worker = new SwingWorker<>() {
            @Override protected MonthlyReport doInBackground() throws Exception {
                return FinancialSystem.generateMonthlyReport(
                    student.getStudentId(), requestedMonth);
            }
            @Override protected void done() {
                if (!requestedMonth.equals(selectedMonth)) return;
                try {
                    updateUI(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showErr("Could not load the monthly report: " + cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void updateUI(MonthlyReport report) {
        incomeVal.setText(MoneyFormatter.format(report.getTotalIncome()));
        expensesVal.setText(MoneyFormatter.format(report.getTotalExpenses()));
        balanceVal.setText(MoneyFormatter.format(report.getBalance()));
        savingsVal.setText(String.format("%.1f%%", report.getSavingsRate()));

        // Category table
        catTableModel.setRowCount(0);
        double total = report.getTotalExpenses();
        Map<String, Double> categoryTotals = report.getCategoryTotals();
        for (Map.Entry<String, Double> e : categoryTotals.entrySet()) {
            double pct = total > 0 ? (e.getValue() / total) * 100 : 0;
            catTableModel.addRow(new Object[]{
                e.getKey(),
                MoneyFormatter.format(e.getValue()),
                String.format("%.1f%%", pct)
            });
        }
        if (categoryTotals.isEmpty()) {
            catTableModel.addRow(new Object[]{"No expenses this month", "—", "—"});
        }
    }

    // ---------------------------------------------------------------
    // Month navigation
    // ---------------------------------------------------------------
    private void changeMonth(int delta) {
        selectedMonth = selectedMonth.plusMonths(delta);
        monthLabel.setText(formatMonth(selectedMonth));
        refresh();
    }

    // ---------------------------------------------------------------
    // CSV Export
    // ---------------------------------------------------------------
    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File(
            "coinscious_report_" + selectedMonth + ".csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String path = fc.getSelectedFile().getAbsolutePath();
        if (!path.endsWith(".csv")) path += ".csv";

        try {
            MonthlyReport report = FinancialSystem.generateMonthlyReport(
                student.getStudentId(), selectedMonth);
            String err = ExportService.exportReport(path, report);
            if (err != null) showErr(err);
            else JOptionPane.showMessageDialog(this,
                "Report exported to:\n" + path,
                "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            // Also export transactions for the month
            List<Transaction> txns = FinancialSystem.getRepo()
                .findByMonth(student.getStudentId(), selectedMonth);
            String txPath = path.replace(".csv", "_transactions.csv");
            ExportService.exportTransactions(txPath, txns);

        } catch (Exception ex) {
            showErr("Export failed: " + ex.getMessage());
        }
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private String formatMonth(YearMonth m) {
        return m.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
    }
}

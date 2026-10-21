package com.stipend.ui.panels;

import com.stipend.model.*;
import com.stipend.service.FinancialSystem;
import com.stipend.ui.MainFrame;
import com.stipend.ui.dialogs.TransactionDialog;
import com.stipend.ui.dialogs.TransactionDetailsDialog;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.ui.theme.DatePicker;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.EventObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * TransactionsPanel: sortable table with search, filter chips,
 * and Edit/Delete buttons per row. Income amounts are green, expenses red.
 */
public class TransactionsPanel extends JPanel {

    private final Student   student;
    private final MainFrame mainFrame;

    // Filter controls
    private final JTextField     searchField;
    private final JComboBox<String> typeFilter;
    private final JComboBox<String> categoryFilter;
    private final DatePicker     fromDate;
    private final DatePicker     toDate;

    // Table
    private final JTable         table;
    private final DefaultTableModel tableModel;

    // All loaded transactions (filtered in memory for speed)
    private List<Transaction> allTransactions = new ArrayList<>();

    private static final String[] COLUMNS =
        {"#", "Date", "Type", "Category", "Description / Source", "Note", "Amount", "Actions"};

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public TransactionsPanel(Student student, MainFrame mainFrame) {
        this.student   = student;
        this.mainFrame = mainFrame;

        setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        // Initialize controls
        searchField    = CoinSciousTheme.styledField(15);
        searchField.putClientProperty("JTextField.placeholderText", "Search transactions…");
        typeFilter     = CoinSciousTheme.styledCombo(new String[]{"ALL", "INCOME", "EXPENSE"});
        categoryFilter = CoinSciousTheme.styledCombo(buildCategoryList());
        fromDate       = new DatePicker(LocalDate.now().withDayOfMonth(1), true);
        toDate         = new DatePicker(LocalDate.now(), true);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 7; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 7 ? JPanel.class : Object.class;
            }
        };
        table = buildTable();

        buildUI();
        refresh();
    }

    // ---------------------------------------------------------------
    // Build layout
    // ---------------------------------------------------------------
    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(0, GAP));
        content.setBackground(BG_MAIN);
        content.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(buildHeader2(), BorderLayout.NORTH);
        top.add(buildFilterBar(), BorderLayout.SOUTH);

        content.add(top, BorderLayout.NORTH);
        content.add(wrapTable(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    private JPanel buildHeader2() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_MAIN);
        bar.setBorder(new EmptyBorder(0, 0, GAP, 0));

        JLabel title = new JLabel("Transactions");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        JButton addBtn = CoinSciousTheme.primaryButton("  +  Add Transaction");
        addBtn.setPreferredSize(new Dimension(180, 38));
        addBtn.addActionListener(e -> openAddDialog());

        bar.add(title, BorderLayout.WEST);
        bar.add(addBtn, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildFilterBar() {
        JPanel bar = CoinSciousTheme.cardPanel();
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        bar.add(label("Search", FONT_BOLD, TEXT_SECONDARY));
        searchField.setPreferredSize(new Dimension(150, 34));
        bar.add(searchField);

        bar.add(label("Type", FONT_BOLD, TEXT_SECONDARY));
        typeFilter.setPreferredSize(new Dimension(100, 34));
        bar.add(typeFilter);

        bar.add(label("Category", FONT_BOLD, TEXT_SECONDARY));
        categoryFilter.setPreferredSize(new Dimension(125, 34));
        bar.add(categoryFilter);

        bar.add(label("From", FONT_BOLD, TEXT_SECONDARY));
        fromDate.setPreferredSize(new Dimension(130, 36));
        bar.add(fromDate);

        bar.add(label("To", FONT_BOLD, TEXT_SECONDARY));
        toDate.setPreferredSize(new Dimension(130, 36));
        bar.add(toDate);

        JButton applyBtn = CoinSciousTheme.primaryButton("Apply");
        applyBtn.setPreferredSize(new Dimension(80, 34));
        applyBtn.addActionListener(e -> applyFilters());
        bar.add(applyBtn);

        JButton resetBtn = CoinSciousTheme.ghostButton("Reset");
        resetBtn.setPreferredSize(new Dimension(72, 34));
        resetBtn.addActionListener(e -> resetFilters());
        bar.add(resetBtn);

        return bar;
    }

    private JScrollPane wrapTable() {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));
        scroll.getViewport().setOpaque(true);
        return scroll;
    }

    // ---------------------------------------------------------------
    // Table setup
    // ---------------------------------------------------------------
    private JTable buildTable() {
        JTable t = new JTable(tableModel);
        t.setBackground(BG_CARD);
        t.setForeground(TEXT_PRIMARY);
        t.setFont(FONT_BODY);
        t.setRowHeight(38);
        t.setGridColor(BORDER_SUBTLE);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.getTableHeader().setBackground(BG_SIDEBAR);
        t.getTableHeader().setForeground(TEXT_SECONDARY);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.getTableHeader().setPreferredSize(new Dimension(0, 38));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setBackground(BG_SIDEBAR);
                setForeground(TEXT_SECONDARY);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(new EmptyBorder(0, 10, 0, 8));
                return this;
            }
        });
        t.setSelectionBackground(ACCENT_2.darker());
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setFillsViewportHeight(true);
        t.setShowHorizontalLines(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        t.setAutoCreateRowSorter(true);

        // Column widths
        int[] widths = {42, 112, 88, 130, 180, 145, 120, 148};
        for (int i = 0; i < widths.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Amount cell renderer (green/red)
        t.getColumnModel().getColumn(6).setCellRenderer(new AmountCellRenderer());
        t.getColumnModel().getColumn(0).setCellRenderer(new TransactionCellRenderer());
        t.getColumnModel().getColumn(1).setCellRenderer(new TransactionCellRenderer(SwingConstants.CENTER));
        t.getColumnModel().getColumn(2).setCellRenderer(new TypeCellRenderer());

        // Actions column renderer + editor
        t.getColumnModel().getColumn(7).setCellRenderer(new ActionCellRenderer());
        t.getColumnModel().getColumn(7).setCellEditor(new ActionCellEditor());
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                int row = t.rowAtPoint(event.getPoint());
                int column = t.columnAtPoint(event.getPoint());
                if (row < 0 || column < 0 || column == 7) return;

                int modelRow = t.convertRowIndexToModel(row);
                Object value = tableModel.getValueAt(modelRow, 7);
                if (value instanceof Transaction transaction) {
                    showTransactionDetails(transaction);
                }
            }
        });

        // Row renderer for alternating background
        t.setDefaultRenderer(Object.class, new TransactionCellRenderer());

        return t;
    }

    // ---------------------------------------------------------------
    // Data loading
    // ---------------------------------------------------------------
    public void refresh() {
        SwingWorker<List<Transaction>, Void> worker = new SwingWorker<>() {
            @Override protected List<Transaction> doInBackground() throws Exception {
                return FinancialSystem.getRepo().findAll(student.getStudentId());
            }
            @Override protected void done() {
                try {
                    allTransactions = get();
                    populateTable(allTransactions);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        worker.execute();
    }

    private void populateTable(List<Transaction> list) {
        tableModel.setRowCount(0);
        int idx = 1;
        for (Transaction t : list) {
            boolean isIncome = t instanceof Income;
            String extra = isIncome
                ? ((Income)  t).getSource()
                : ((Expense) t).getDescription();

            tableModel.addRow(new Object[]{
                idx++,
                t.getDate().toString(),
                isIncome ? "Income" : "Expense",
                t.getCategory(),
                extra,
                t.getNote(),
                MoneyFormatter.format(t.getAmount()),
                t   // store the transaction object for the action buttons
            });
        }
    }

    // ---------------------------------------------------------------
    // Filters
    // ---------------------------------------------------------------
    private void applyFilters() {
        String keyword  = searchField.getText().trim();
        String type     = (String) typeFilter.getSelectedItem();
        String category = (String) categoryFilter.getSelectedItem();
        LocalDate from  = fromDate.getDate();
        LocalDate to    = toDate.getDate();

        List<Transaction> filtered = allTransactions.stream().filter(t -> {
            if (!"ALL".equals(type)) {
                boolean wantIncome = "INCOME".equals(type);
                if (wantIncome != (t instanceof Income)) return false;
            }
            if (!"ALL".equals(category) && !category.equals(t.getCategory())) return false;
            if (from != null && t.getDate().isBefore(from)) return false;
            if (to != null && t.getDate().isAfter(to)) return false;
            if (!keyword.isBlank()) {
                String kw = keyword.toLowerCase();
                String haystack = (t.getCategory() + " " + t.getNote() + " "
                    + (t instanceof Income ? ((Income) t).getSource() : "")
                    + (t instanceof Expense ? ((Expense) t).getDescription() : "")).toLowerCase();
                if (!haystack.contains(kw)) return false;
            }
            return true;
        }).toList();

        populateTable(filtered);
    }

    private void resetFilters() {
        searchField.setText("");
        typeFilter.setSelectedIndex(0);
        categoryFilter.setSelectedIndex(0);
        fromDate.setDate(LocalDate.now().withDayOfMonth(1));
        toDate.setDate(LocalDate.now());
        populateTable(allTransactions);
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------
    private void openAddDialog() {
        TransactionDialog dlg = new TransactionDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), student);
        dlg.setVisible(true);
        if (dlg.wasConfirmed()) { refresh(); mainFrame.onDataChanged(); }
    }

    private void openEditDialog(Transaction t) {
        TransactionDialog dlg = new TransactionDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), student, t);
        dlg.setVisible(true);
        if (dlg.wasConfirmed()) { refresh(); mainFrame.onDataChanged(); }
    }

    private void showTransactionDetails(Transaction transaction) {
        TransactionDetailsDialog dialog = new TransactionDetailsDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), transaction);
        dialog.setVisible(true);
    }

    private void deleteTransaction(Transaction t) {
        int choice = JOptionPane.showConfirmDialog(this,
            "Delete this transaction?\n" + t.displayTxn(),
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;
        try {
            FinancialSystem.getRepo().delete(t.getTransactionId());
            refresh();
            mainFrame.onDataChanged();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private String[] buildCategoryList() {
        List<String> cats = new ArrayList<>();
        cats.add("ALL");
        for (String c : Expense.CATEGORIES) cats.add(c);
        for (String c : Income.CATEGORIES) {
            if (!cats.contains(c)) cats.add(c);
        }
        return cats.toArray(String[]::new);
    }

    // ---------------------------------------------------------------
    // Cell Renderers / Editors
    // ---------------------------------------------------------------

    private void styleActionButton(JButton button) {
        button.setMargin(new Insets(2, 5, 2, 5));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
            new EmptyBorder(3, 6, 3, 6)
        ));
    }

    /** Renders the amount column green for income, red for expense. */
    private static class AmountCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, focus, row, col);
            setBackground(sel ? ACCENT_2.darker() : (row % 2 == 0 ? BG_CARD : BG_CARD_HOVER));
            String typeVal = (String) table.getValueAt(row, 2);
            setForeground("Income".equals(typeVal) ? SUCCESS : DANGER);
            setFont(FONT_BOLD);
            setHorizontalAlignment(RIGHT);
            setBorder(new EmptyBorder(0, 8, 0, 12));
            return this;
        }
    }

    /** Alternating row colors for the data columns. */
    private static class TransactionCellRenderer extends DefaultTableCellRenderer {
        private final int alignment;

        private TransactionCellRenderer() {
            this(SwingConstants.LEFT);
        }

        private TransactionCellRenderer(int alignment) {
            this.alignment = alignment;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, focus, row, col);
            setBackground(sel ? ACCENT_2.darker() : (row % 2 == 0 ? BG_CARD : BG_CARD_HOVER));
            setForeground(col == 0 ? TEXT_DISABLED : TEXT_PRIMARY);
            setFont(FONT_BODY);
            setHorizontalAlignment(alignment);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    private static class TypeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, sel, focus, row, col);
            setBackground(sel ? ACCENT_2.darker() : (row % 2 == 0 ? BG_CARD : BG_CARD_HOVER));
            setForeground("Income".equals(value) ? SUCCESS : DANGER);
            setFont(FONT_BOLD);
            setBorder(new EmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    /** Renders Edit and Delete buttons in the Actions column. */
    private class ActionCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            p.setBackground(sel ? ACCENT_2.darker() : (row % 2 == 0 ? BG_CARD : BG_CARD_HOVER));
            JButton edit = CoinSciousTheme.ghostButton("Edit");
            edit.setFont(FONT_SMALL);
            JButton del  = CoinSciousTheme.dangerButton("Delete");
            del.setFont(FONT_SMALL);
            edit.setFocusable(false);
            del.setFocusable(false);
            styleActionButton(edit);
            styleActionButton(del);
            p.add(edit); p.add(del);
            return p;
        }
    }

    /** Makes the Edit/Delete buttons in the Actions column clickable. */
    private class ActionCellEditor extends AbstractCellEditor
            implements TableCellEditor {
        private JPanel panel;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean sel, int row, int col) {
            // Get the actual transaction object stored in the last column
            int modelRow = table.convertRowIndexToModel(row);
            Object cell  = tableModel.getValueAt(modelRow, 7);
            Transaction t = (cell instanceof Transaction tx) ? tx : null;

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
            panel.setBackground(ACCENT_2.darker());

            JButton edit = CoinSciousTheme.ghostButton("Edit");
            edit.setFont(FONT_SMALL);
            styleActionButton(edit);
            edit.addActionListener(e -> {
                stopCellEditing();
                if (t != null) openEditDialog(t);
            });

            JButton del = CoinSciousTheme.dangerButton("Delete");
            del.setFont(FONT_SMALL);
            styleActionButton(del);
            del.addActionListener(e -> {
                stopCellEditing();
                if (t != null) deleteTransaction(t);
            });

            panel.add(edit); panel.add(del);
            return panel;
        }

        @Override public Object getCellEditorValue()    { return null; }
        @Override public boolean isCellEditable(EventObject e) { return true; }
    }
}

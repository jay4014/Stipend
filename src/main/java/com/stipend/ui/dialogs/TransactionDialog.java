package com.stipend.ui.dialogs;

import com.stipend.model.Expense;
import com.stipend.model.Income;
import com.stipend.model.Student;
import com.stipend.model.Transaction;
import com.stipend.service.BudgetService;
import com.stipend.service.FinancialSystem;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.ui.theme.DatePicker;
import com.stipend.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * TransactionDialog: modal for adding or editing a transaction.
 * Toggles between Income and Expense mode, validates all fields,
 * and checks budget alerts before saving.
 */
public class TransactionDialog extends JDialog {

    private final Student student;
    private Transaction existing; // null if adding new

    // Controls
    private final JToggleButton incomeToggle  = new JToggleButton("Income");
    private final JToggleButton expenseToggle = new JToggleButton("Expense");
    private final JTextField    amountField   = CoinSciousTheme.styledField(10);
    private final DatePicker    datePicker    = new DatePicker(LocalDate.now());
    private final JComboBox<String> categoryBox;
    private final JTextField    noteField     = CoinSciousTheme.styledField(20);
    private final JTextField    extraField    = CoinSciousTheme.styledField(20);
    private final JLabel        extraLabel    = label("Source", FONT_BOLD, TEXT_SECONDARY);
    private final JLabel        amountErr     = errorLabel();
    private final JLabel        dateErr       = errorLabel();
    private final JLabel        categoryErr   = errorLabel();
    private int                 formRow;

    private boolean confirmed = false;

    // ---------------------------------------------------------------
    // Constructor: new transaction
    // ---------------------------------------------------------------
    public TransactionDialog(Frame parent, Student student) {
        this(parent, student, null);
    }

    // Constructor: edit existing transaction
    public TransactionDialog(Frame parent, Student student, Transaction existing) {
        super(parent, "", true);
        this.student  = student;
        this.existing = existing;

        String[] expCats = Expense.CATEGORIES;
        categoryBox = CoinSciousTheme.styledCombo(expCats);

        buildUI();
        if (existing != null) populateFields();

        pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension packedSize = getSize();
        packedSize.width = 480;
        packedSize.height = Math.min(packedSize.height, screenSize.height - 80);
        setSize(packedSize);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    // ---------------------------------------------------------------
    // UI construction
    // ---------------------------------------------------------------
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CARD);

        // Header
        JLabel title = new JLabel(existing == null ? "New Transaction" : "Edit Transaction");
        title.setFont(FONT_SUBHEAD);
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(12, 24, 6, 24));
        root.add(title, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel();
        form.setBackground(BG_CARD);
        form.setLayout(new GridBagLayout());
        form.setBorder(new EmptyBorder(2, 24, 8, 24));

        // Type toggle
        ButtonGroup typeGroup = new ButtonGroup();
        styleToggle(incomeToggle,  false);
        styleToggle(expenseToggle, true);
        typeGroup.add(incomeToggle);
        typeGroup.add(expenseToggle);
        expenseToggle.setSelected(true);

        incomeToggle.addActionListener(e  -> switchType(false));
        expenseToggle.addActionListener(e -> switchType(true));

        JPanel toggleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleRow.setOpaque(false);
        toggleRow.add(incomeToggle);
        toggleRow.add(expenseToggle);
        addFormComponent(form, toggleRow, new Insets(0, 0, 10, 0));

        // Form fields
        addRow(form, "Amount (₱)", amountField, amountErr);
        addRow(form, "Date", datePicker, dateErr);
        addRow(form, "Category", categoryBox, categoryErr);

        addRow(form, extraLabel, extraField, null);

        addRow(form, "Note (optional)", noteField, null);

        root.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        btnRow.setBackground(BG_CARD);
        btnRow.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_SUBTLE));

        JButton cancelBtn = CoinSciousTheme.ghostButton("Cancel");
        cancelBtn.addActionListener(e -> dispose());
        JButton saveBtn = CoinSciousTheme.primaryButton("  Save  ");
        saveBtn.addActionListener(e -> save());

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        root.add(btnRow, BorderLayout.SOUTH);

        setContentPane(root);

        // ESC closes dialog
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "close");
        root.getActionMap().put("close", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { dispose(); }
        });
    }

    private void addRow(JPanel parent, String labelText,
                         JComponent field, JLabel err) {
        JLabel lbl = label(labelText, FONT_BOLD, TEXT_SECONDARY);
        addRow(parent, lbl, field, err);
    }

    private void addRow(JPanel parent, JLabel lbl,
                         JComponent field, JLabel err) {
        addFormComponent(parent, lbl, new Insets(0, 0, 5, 0));
        field.setPreferredSize(new Dimension(400, 36));
        addFormComponent(parent, field, new Insets(0, 0, err == null ? 8 : 0, 0));
        if (err != null) {
            err.setFont(FONT_SMALL);
            err.setPreferredSize(new Dimension(400, 14));
            addFormComponent(parent, err, new Insets(0, 0, 7, 0));
        }
    }

    private void addFormComponent(JPanel parent, JComponent component, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = formRow++;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insets;
        parent.add(component, constraints);
    }

    private void styleToggle(JToggleButton btn, boolean expense) {
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setPreferredSize(new Dimension(110, 36));
        btn.setRolloverEnabled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
            new EmptyBorder(0, 12, 0, 12)
        ));
        Runnable updateStyle = () -> {
            boolean selected = btn.isSelected();
            boolean hovered = btn.getModel().isRollover();
            if (selected) {
                btn.setBackground(expense
                    ? new Color(0x78, 0x2D, 0x38)
                    : new Color(0x16, 0x5B, 0x3B));
                btn.setForeground(TEXT_PRIMARY);
            } else {
                btn.setBackground(hovered ? new Color(0x35, 0x35, 0x55) : BG_CARD_HOVER);
                btn.setForeground(hovered ? TEXT_PRIMARY : TEXT_SECONDARY);
            }
        };
        btn.addChangeListener(e -> updateStyle.run());
        updateStyle.run();
    }

    private void switchType(boolean isExpense) {
        String[] cats = isExpense ? Expense.CATEGORIES : Income.CATEGORIES;
        categoryBox.setModel(new DefaultComboBoxModel<>(cats));
        extraLabel.setText(isExpense ? "Description" : "Source");
    }

    private void populateFields() {
        boolean isExpense = existing instanceof Expense;
        if (isExpense) {
            expenseToggle.setSelected(true);
            switchType(true);
        } else {
            incomeToggle.setSelected(true);
            switchType(false);
        }

        amountField.setText(String.format("%.2f", existing.getAmount()));
        datePicker.setDate(existing.getDate());
        categoryBox.setSelectedItem(existing.getCategory());
        noteField.setText(existing.getNote());

        if (isExpense) extraField.setText(((Expense) existing).getDescription());
        else           extraField.setText(((Income)  existing).getSource());
    }

    // ---------------------------------------------------------------
    // Validation & save
    // ---------------------------------------------------------------
    private void save() {
        amountErr.setText(" "); dateErr.setText(" "); categoryErr.setText(" ");

        String amountStr = amountField.getText().trim();
        LocalDate selectedDate = datePicker.getDate();
        String dateStr = selectedDate == null ? "" : selectedDate.toString();
        String category  = (String) categoryBox.getSelectedItem();
        String note      = noteField.getText().trim();
        String extra     = extraField.getText().trim();
        boolean isExpense = expenseToggle.isSelected();

        boolean ok = true;

        String aErr = Validator.validateAmount(amountStr);
        if (aErr != null) { amountErr.setText(aErr); ok = false; }

        String dErr = isExpense
            ? Validator.validateNotFuture(dateStr)
            : Validator.validateDate(dateStr);
        if (dErr != null) { dateErr.setText(dErr); ok = false; }

        if (category == null || category.isBlank()) {
            categoryErr.setText("Category is required."); ok = false;
        }

        if (!ok) return;

        double amount = Double.parseDouble(amountStr);
        LocalDate date = selectedDate;

        // Budget alert check for expenses
        if (isExpense && existing == null) {
            try {
                String alert = BudgetService.checkBudgetAlert(
                    student.getStudentId(), category, amount, YearMonth.now());
                if (alert != null) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        alert + "\n\nDo you want to continue?",
                        "Budget Alert", JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                    if (choice != JOptionPane.YES_OPTION) return;
                }
            } catch (SQLException ex) {
                // Non-fatal — continue without budget check
            }
        }

        try {
            Transaction t;
            if (isExpense) {
                t = new Expense(amount, date, category, note, 1, extra);
            } else {
                t = new Income(amount, date, category, note, 1, extra);
            }

            if (existing != null) {
                t.setTransactionId(existing.getTransactionId());
                FinancialSystem.getRepo().update(t);
            } else {
                FinancialSystem.getRepo().save(student.getStudentId(), t);
            }

            confirmed = true;
            dispose();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean wasConfirmed() { return confirmed; }
}

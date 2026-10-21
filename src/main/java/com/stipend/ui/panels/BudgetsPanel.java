package com.stipend.ui.panels;

import com.stipend.model.Budget;
import com.stipend.model.Expense;
import com.stipend.model.Income;
import com.stipend.model.Student;
import com.stipend.service.BudgetService;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * BudgetsPanel: one card per budget category with an animated progress bar.
 * Status: Good < 50%, Tight 50–99%, Exceeded ≥ 100%.
 * Add/edit budget opens a small dialog.
 */
public class BudgetsPanel extends JPanel {

    private final Student   student;
    private YearMonth       selectedMonth = YearMonth.now();

    private final JPanel    cardsGrid;
    private final JLabel    monthLabel;
    private List<Budget>    currentBudgets = List.of();

    public BudgetsPanel(Student student) {
        this.student = student;
        setBackground(BG_MAIN);
        setLayout(new BorderLayout());

        cardsGrid  = new JPanel(new GridBagLayout());
        cardsGrid.setBackground(BG_MAIN);

        monthLabel = label(formatMonth(selectedMonth), FONT_SUBHEAD, ACCENT_1);

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

        JScrollPane scroll = new JScrollPane(cardsGrid);
        scroll.setBackground(BG_MAIN);
        scroll.getViewport().setBackground(BG_MAIN);
        scroll.setBorder(null);
        wrapper.add(scroll, BorderLayout.CENTER);

        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_MAIN);

        JPanel left = new JPanel();
        left.setBackground(BG_MAIN);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(label("Budgets", FONT_TITLE, TEXT_PRIMARY));
        left.add(Box.createVerticalStrut(2));
        left.add(label("Monthly category spending limits", FONT_BODY, TEXT_SECONDARY));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setBackground(BG_MAIN);

        // Month picker: previous / current / next
        JButton prevBtn = CoinSciousTheme.ghostButton("◀");
        prevBtn.addActionListener(e -> {
            selectedMonth = selectedMonth.minusMonths(1);
            monthLabel.setText(formatMonth(selectedMonth));
            refresh();
        });
        JButton nextBtn = CoinSciousTheme.ghostButton("▶");
        nextBtn.addActionListener(e -> {
            selectedMonth = selectedMonth.plusMonths(1);
            monthLabel.setText(formatMonth(selectedMonth));
            refresh();
        });

        JButton addBtn = CoinSciousTheme.primaryButton("  +  Set Budget");
        addBtn.setPreferredSize(new Dimension(150, 36));
        addBtn.addActionListener(e -> openBudgetDialog(null));

        right.add(prevBtn);
        right.add(monthLabel);
        right.add(nextBtn);
        right.add(Box.createHorizontalStrut(10));
        right.add(addBtn);

        bar.add(left,  BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------
    // Refresh
    // ---------------------------------------------------------------
    public void refresh() {
        SwingWorker<List<Budget>, Void> worker = new SwingWorker<>() {
            @Override protected List<Budget> doInBackground() throws Exception {
                return BudgetService.getBudgets(student.getStudentId(), selectedMonth);
            }
            @Override protected void done() {
                try { populateCards(get()); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        worker.execute();
    }

    private void populateCards(List<Budget> budgets) {
        cardsGrid.removeAll();
        currentBudgets = List.copyOf(budgets);

        if (budgets.isEmpty()) {
            JLabel empty = label("No budgets set for " + formatMonth(selectedMonth) +
                ". Click \"Set Budget\" to add one.", FONT_BODY, TEXT_SECONDARY);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            cardsGrid.setLayout(new BorderLayout());
            cardsGrid.add(empty, BorderLayout.CENTER);
        } else {
            cardsGrid.setLayout(new GridBagLayout());
            int gridRows = (budgets.size() + 2) / 3;
            for (int i = 0; i < gridRows * 3; i++) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = i % 3;
                constraints.gridy = i / 3;
                constraints.weightx = 1;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.anchor = GridBagConstraints.NORTH;
                constraints.insets = new Insets(0, 0, GAP, GAP);
                if (i < budgets.size()) {
                    cardsGrid.add(buildBudgetCard(budgets.get(i)), constraints);
                } else {
                    JPanel placeholder = new JPanel();
                    placeholder.setOpaque(false);
                    placeholder.setPreferredSize(new Dimension(300, 198));
                    cardsGrid.add(placeholder, constraints);
                }
            }
            GridBagConstraints filler = new GridBagConstraints();
            filler.gridx = 0;
            filler.gridy = gridRows;
            filler.weighty = 1;
            filler.gridwidth = 3;
            filler.fill = GridBagConstraints.VERTICAL;
            cardsGrid.add(Box.createVerticalGlue(), filler);
        }

        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    // ---------------------------------------------------------------
    // Single budget card
    // ---------------------------------------------------------------
    private JPanel buildBudgetCard(Budget budget) {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(300, 198));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 198));

        // Category + status badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel catLabel = label(budget.getCategory(),
            new Font("Segoe UI", Font.BOLD, 19), TEXT_PRIMARY);
        JLabel statusBadge = buildStatusBadge(budget.getStatus());
        topRow.add(catLabel,    BorderLayout.WEST);
        topRow.add(statusBadge, BorderLayout.EAST);

        // Amounts
        JLabel amounts = label(
            MoneyFormatter.format(budget.getSpent()) + " / " +
            MoneyFormatter.format(budget.getMonthlyLimit()),
            FONT_BODY, TEXT_SECONDARY);
        amounts.setHorizontalAlignment(SwingConstants.LEFT);

        // Progress bar
        JPanel progressBar = buildProgressBar(budget);

        // Percent text
        JLabel pctLabel = label(
            String.format("%.1f%% used  ·  %s remaining",
                budget.getPercentUsed(),
                MoneyFormatter.format(Math.max(0, budget.getRemaining()))),
            FONT_SMALL, TEXT_SECONDARY);
        pctLabel.setHorizontalAlignment(SwingConstants.LEFT);

        // Edit / Delete buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JButton editBtn = CoinSciousTheme.ghostButton("Edit");
        editBtn.setFont(FONT_SMALL);
        editBtn.addActionListener(e -> openBudgetDialog(budget));
        JButton delBtn = CoinSciousTheme.dangerButton("Delete");
        delBtn.setFont(FONT_SMALL);
        delBtn.addActionListener(e -> deleteBudget(budget));
        actions.add(editBtn); actions.add(delBtn);

        addCardRow(card, topRow, 0, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST, new Insets(0, 0, 8, 0));
        addCardRow(card, amounts, 1, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST, new Insets(0, 0, 8, 0));
        addCardRow(card, progressBar, 2, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST, new Insets(0, 0, 6, 0));
        addCardRow(card, pctLabel, 3, GridBagConstraints.HORIZONTAL,
            GridBagConstraints.WEST, new Insets(0, 0, 6, 0));
        addCardRow(card, actions, 4, GridBagConstraints.NONE,
            GridBagConstraints.EAST, new Insets(0, 0, 0, 0));

        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = 5;
        filler.weighty = 1;
        filler.fill = GridBagConstraints.VERTICAL;
        card.add(Box.createVerticalGlue(), filler);

        return card;
    }

    private void addCardRow(JPanel card, JComponent component, int row,
                            int fill, int anchor, Insets insets) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.weightx = 1;
        constraints.fill = fill;
        constraints.anchor = anchor;
        constraints.insets = insets;
        card.add(component, constraints);
    }

    private JLabel buildStatusBadge(Budget.Status status) {
        String text  = switch (status) {
            case GOOD     -> "Good";
            case TIGHT    -> "Tight";
            case EXCEEDED -> "Exceeded";
        };
        Color color = switch (status) {
            case GOOD     -> SUCCESS;
            case TIGHT    -> WARNING;
            case EXCEEDED -> DANGER;
        };
        JLabel badge = new JLabel(text);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(color);
        badge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color.darker(), 1, true),
            new EmptyBorder(2, 6, 2, 6)
        ));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        return badge;
    }

    private JPanel buildProgressBar(Budget budget) {
        double pct   = Math.min(100, budget.getPercentUsed());
        Color  color = switch (budget.getStatus()) {
            case GOOD     -> SUCCESS;
            case TIGHT    -> WARNING;
            case EXCEEDED -> DANGER;
        };

        JPanel track = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Track
                g2.setColor(BG_MAIN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Fill
                int fillW = (int)(getWidth() * pct / 100.0);
                if (fillW > 0) {
                    GradientPaint gp = new GradientPaint(0, 0, color.brighter(), fillW, 0, color);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, fillW, getHeight(), 8, 8);
                }
                g2.dispose();
            }
        };
        track.setPreferredSize(new Dimension(0, 10));
        track.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
        track.setOpaque(false);
        track.setAlignmentX(LEFT_ALIGNMENT);

        return track;
    }

    // ---------------------------------------------------------------
    // Add / Edit dialog
    // ---------------------------------------------------------------
    private void openBudgetDialog(Budget existing) {
        JPanel form = new JPanel();
        form.setBackground(BG_CARD);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));

        JTextField adjustmentField = CoinSciousTheme.styledField(12);

        if (existing == null) {
            Set<String> availableCategories = new LinkedHashSet<>(
                Arrays.asList(concat(Expense.CATEGORIES, Income.CATEGORIES)));
            for (Budget budget : currentBudgets) {
                availableCategories.remove(budget.getCategory());
            }
            if (availableCategories.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Every available category already has a budget for this month.",
                    "No Categories Available", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            JComboBox<String> categoryBox = CoinSciousTheme.styledCombo(
                availableCategories.toArray(String[]::new));
            addBudgetFormRow(form, "Category", categoryBox);
            addBudgetFormRow(form, "Monthly Limit (₱)", adjustmentField);

            int option = JOptionPane.showConfirmDialog(this, form, "Set Budget",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option != JOptionPane.OK_OPTION) return;

            String validationError = com.stipend.util.Validator.validateAmount(
                adjustmentField.getText().trim());
            if (validationError != null) {
                showBudgetError(validationError);
                return;
            }

            String error = BudgetService.createBudget(student.getStudentId(),
                (String) categoryBox.getSelectedItem(),
                Double.parseDouble(adjustmentField.getText().trim()), selectedMonth);
            if (error != null) showBudgetError(error);
            else refresh();
            return;
        }

        addBudgetFormRow(form, "Category",
            label(existing.getCategory(), FONT_BODY, TEXT_PRIMARY));
        addBudgetFormRow(form, "Current monthly limit",
            label(MoneyFormatter.format(existing.getMonthlyLimit()), FONT_SUBHEAD, TEXT_PRIMARY));
        addBudgetFormRow(form, "Amount to adjust (₱)", adjustmentField);

        Object[] options = {"Add to Limit", "Subtract from Limit", "Cancel"};
        int option = JOptionPane.showOptionDialog(this, form, "Adjust Budget Limit",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (option < 0 || option == 2) return;

        String validationError = com.stipend.util.Validator.validateAmount(
            adjustmentField.getText().trim());
        if (validationError != null) {
            showBudgetError(validationError);
            return;
        }

        String error = BudgetService.adjustBudgetLimit(existing.getBudgetId(),
            Double.parseDouble(adjustmentField.getText().trim()), option == 0);
        if (error != null) showBudgetError(error);
        else refresh();
    }

    private void addBudgetFormRow(JPanel form, String title, JComponent component) {
        JLabel rowLabel = label(title, FONT_BOLD, TEXT_SECONDARY);
        rowLabel.setAlignmentX(LEFT_ALIGNMENT);
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        form.add(rowLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(component);
        form.add(Box.createVerticalStrut(10));
    }

    private void showBudgetError(String message) {
        JOptionPane.showMessageDialog(this, message, "Budget", JOptionPane.WARNING_MESSAGE);
    }

    private void deleteBudget(Budget b) {
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete budget for \"" + b.getCategory() + "\"?",
            "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        String err = BudgetService.deleteBudget(b.getBudgetId());
        if (err != null) JOptionPane.showMessageDialog(this, err, "Error", JOptionPane.ERROR_MESSAGE);
        else refresh();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private static String[] concat(String[] a, String[] b) {
        String[] out = new String[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private String formatMonth(YearMonth m) {
        return m.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
    }
}

package com.stipend.ui.dialogs;

import com.stipend.model.Expense;
import com.stipend.model.Income;
import com.stipend.model.Transaction;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static com.stipend.ui.theme.CoinSciousTheme.*;

public final class TransactionDetailsDialog extends JDialog {

    public TransactionDetailsDialog(Frame parent, Transaction transaction) {
        super(parent, "Transaction Details", true);
        setContentPane(buildContent(transaction));
        setSize(520, 460);
        setMinimumSize(new Dimension(480, 420));
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private JPanel buildContent(Transaction transaction) {
        boolean isIncome = transaction instanceof Income;
        Color typeColor = isIncome ? SUCCESS : DANGER;
        String type = isIncome ? "INCOME" : "EXPENSE";
        String detailLabel = isIncome ? "Source" : "Description";
        String detail = isIncome
            ? ((Income) transaction).getSource()
            : ((Expense) transaction).getDescription();

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BG_MAIN);
        root.setBorder(new EmptyBorder(24, 28, 20, 28));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel eyebrow = label("TRANSACTION DETAILS", FONT_SMALL, TEXT_SECONDARY);
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel typeLabel = label(type, FONT_SUBHEAD, typeColor);
        typeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel amount = label(MoneyFormatter.format(transaction.getAmount()),
            FONT_KPI, TEXT_PRIMARY);
        amount.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(eyebrow);
        header.add(Box.createVerticalStrut(7));
        header.add(typeLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(amount);
        root.add(header, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(BG_CARD);
        details.setBorder(new EmptyBorder(8, 18, 8, 18));
        addDetail(details, 0, "Date", transaction.getDate().toString());
        addDetail(details, 1, "Category", transaction.getCategory());
        addDetail(details, 2, detailLabel, detail);
        addDetail(details, 3, "Note", transaction.getNote());
        addDetail(details, 4, "Transaction ID", Integer.toString(transaction.getTransactionId()));
        root.add(details, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        JButton close = CoinSciousTheme.primaryButton("Close");
        close.setPreferredSize(new Dimension(96, 36));
        close.addActionListener(e -> dispose());
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(close);
        return root;
    }

    private void addDetail(JPanel panel, int row, String name, String value) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.weightx = 0;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(10, 0, 10, 18);

        JLabel fieldName = label(name, FONT_BOLD, TEXT_SECONDARY);
        panel.add(fieldName, labelConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.NORTHWEST;
        valueConstraints.insets = new Insets(10, 0, 10, 0);

        String displayValue = value == null || value.isBlank() ? "—" : value;
        JLabel fieldValue = label(displayValue, FONT_BODY, TEXT_PRIMARY);
        fieldValue.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(fieldValue, valueConstraints);
    }
}

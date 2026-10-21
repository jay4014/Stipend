package com.stipend.ui.panels;

import com.stipend.model.Goal;
import com.stipend.model.Student;
import com.stipend.service.GoalService;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.ui.theme.DatePicker;
import com.stipend.util.MoneyFormatter;
import com.stipend.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * GoalsPanel: savings goal cards with a circular progress ring,
 * days remaining, and a Contribute button.
 */
public class GoalsPanel extends JPanel {

    private final Student student;
    private final JPanel  cardsGrid;

    public GoalsPanel(Student student) {
        this.student   = student;
        cardsGrid = new JPanel(new GridBagLayout());
        cardsGrid.setBackground(BG_MAIN);

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
        left.add(label("Savings Goals", FONT_TITLE, TEXT_PRIMARY));
        left.add(Box.createVerticalStrut(2));
        left.add(label("Track your financial targets", FONT_BODY, TEXT_SECONDARY));

        JButton addBtn = CoinSciousTheme.primaryButton("  +  New Goal");
        addBtn.setPreferredSize(new Dimension(140, 36));
        addBtn.addActionListener(e -> openCreateDialog());

        bar.add(left,   BorderLayout.WEST);
        bar.add(addBtn, BorderLayout.EAST);
        return bar;
    }

    // ---------------------------------------------------------------
    // Refresh
    // ---------------------------------------------------------------
    public void refresh() {
        SwingWorker<List<Goal>, Void> worker = new SwingWorker<>() {
            @Override protected List<Goal> doInBackground() throws Exception {
                return GoalService.getGoals(student.getStudentId());
            }
            @Override protected void done() {
                try { populateCards(get()); }
                catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        worker.execute();
    }

    private void populateCards(List<Goal> goals) {
        cardsGrid.removeAll();
        if (goals.isEmpty()) {
            cardsGrid.setLayout(new BorderLayout());
            JLabel empty = label("No savings goals yet. Click \"New Goal\" to create one.",
                FONT_BODY, TEXT_SECONDARY);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            cardsGrid.add(empty, BorderLayout.CENTER);
        } else {
            cardsGrid.setLayout(new GridBagLayout());
            int gridRows = (goals.size() + 2) / 3;
            for (int i = 0; i < gridRows * 3; i++) {
                GridBagConstraints constraints = new GridBagConstraints();
                constraints.gridx = i % 3;
                constraints.gridy = i / 3;
                constraints.weightx = 1;
                constraints.fill = GridBagConstraints.HORIZONTAL;
                constraints.anchor = GridBagConstraints.NORTH;
                constraints.insets = new Insets(0, 0, GAP, GAP);

                if (i < goals.size()) {
                    cardsGrid.add(buildGoalCard(goals.get(i)), constraints);
                } else {
                    JPanel placeholder = new JPanel();
                    placeholder.setOpaque(false);
                    placeholder.setPreferredSize(new Dimension(300, 270));
                    cardsGrid.add(placeholder, constraints);
                }
            }

            GridBagConstraints filler = new GridBagConstraints();
            filler.gridx = 0;
            filler.gridy = gridRows;
            filler.gridwidth = 3;
            filler.weighty = 1;
            filler.fill = GridBagConstraints.VERTICAL;
            cardsGrid.add(Box.createVerticalGlue(), filler);
        }
        cardsGrid.revalidate();
        cardsGrid.repaint();
    }

    // ---------------------------------------------------------------
    // Single goal card
    // ---------------------------------------------------------------
    private JPanel buildGoalCard(Goal goal) {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setPreferredSize(new Dimension(300, 270));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 270));

        // Circular progress ring
        GoalRingPanel ring = new GoalRingPanel(goal.getProgressPercent());
        ring.setAlignmentX(CENTER_ALIGNMENT);
        ring.setPreferredSize(new Dimension(100, 100));
        ring.setMaximumSize(new Dimension(100, 100));

        // Name
        JLabel nameLabel = label(goal.getName(), FONT_SUBHEAD, TEXT_PRIMARY);
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Saved / target
        JLabel amtLabel = label(
            MoneyFormatter.format(goal.getSavedAmount()) + " / " +
            MoneyFormatter.format(goal.getTargetAmount()),
            FONT_BODY, TEXT_SECONDARY);
        amtLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Days remaining
        long days = goal.getDaysRemaining();
        String daysText = goal.getTargetDate() == null ? "No deadline"
            : (days > 0 ? days + " days left" : (days == 0 ? "Due today!" : "Overdue"));
        Color daysColor = days < 0 ? DANGER : (days < 7 ? WARNING : TEXT_SECONDARY);
        JLabel daysLabel = label(daysText, FONT_SMALL, daysColor);
        daysLabel.setAlignmentX(CENTER_ALIGNMENT);

        // Status badge
        if (goal.isReached()) {
            JLabel reached = label("✓ Goal Reached!", FONT_BOLD, SUCCESS);
            reached.setAlignmentX(CENTER_ALIGNMENT);
            card.add(reached);
        }

        // Buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnRow.setOpaque(false);
        JButton contribute = CoinSciousTheme.primaryButton("Contribute");
        contribute.setFont(FONT_SMALL);
        contribute.addActionListener(e -> openContributeDialog(goal));
        JButton delete = CoinSciousTheme.dangerButton("Delete");
        delete.setFont(FONT_SMALL);
        delete.addActionListener(e -> deleteGoal(goal));
        btnRow.add(contribute); btnRow.add(delete);

        card.add(ring);
        card.add(Box.createVerticalStrut(10));
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(amtLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(daysLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(btnRow);

        return card;
    }

    // ---------------------------------------------------------------
    // Dialogs
    // ---------------------------------------------------------------
    private void openCreateDialog() {
        JTextField nameField   = CoinSciousTheme.styledField(15);
        JTextField targetField = CoinSciousTheme.styledField(10);
        DatePicker datePicker = new DatePicker(LocalDate.now().plusMonths(3), true);
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        targetField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        datePicker.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel form = buildForm(
            new String[]{"Goal Name", "Target Amount (₱)", "Target Date (optional)"},
            new JComponent[]{nameField, targetField, datePicker}
        );

        if (JOptionPane.showConfirmDialog(this, form, "New Savings Goal",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;

        String name   = nameField.getText().trim();
        String target = targetField.getText().trim();

        if (name.isBlank()) { showErr("Goal name is required."); return; }
        String vErr = Validator.validateAmount(target);
        if (vErr != null) { showErr(vErr); return; }

        LocalDate targetDate = datePicker.getDate();

        String err = GoalService.createGoal(student.getStudentId(),
            name, Double.parseDouble(target), targetDate);
        if (err != null) showErr(err);
        else refresh();
    }

    private void openContributeDialog(Goal goal) {
        JTextField amtField = CoinSciousTheme.styledField(10);
        amtField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel form = buildForm(
            new String[]{"Contribution Amount (₱)"},
            new JComponent[]{amtField}
        );

        if (JOptionPane.showConfirmDialog(this, form,
                "Contribute to: " + goal.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                != JOptionPane.OK_OPTION) return;

        String vErr = Validator.validateAmount(amtField.getText().trim());
        if (vErr != null) { showErr(vErr); return; }

        String err = GoalService.addContribution(goal.getGoalId(),
            Double.parseDouble(amtField.getText().trim()));
        if (err != null) showErr(err);
        else refresh();
    }

    private void deleteGoal(Goal goal) {
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete goal \"" + goal.getName() + "\"?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;
        String err = GoalService.deleteGoal(goal.getGoalId());
        if (err != null) showErr(err);
        else refresh();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    private JPanel buildForm(String[] labels, JComponent[] fields) {
        JPanel form = new JPanel();
        form.setBackground(BG_CARD);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        for (int i = 0; i < labels.length; i++) {
            JLabel l = label(labels[i], FONT_BOLD, TEXT_SECONDARY);
            l.setAlignmentX(LEFT_ALIGNMENT);
            fields[i].setAlignmentX(LEFT_ALIGNMENT);
            form.add(l); form.add(Box.createVerticalStrut(4));
            form.add(fields[i]); form.add(Box.createVerticalStrut(10));
        }
        return form;
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.WARNING_MESSAGE);
    }

    // ---------------------------------------------------------------
    // Circular progress ring (custom painted)
    // ---------------------------------------------------------------
    private static class GoalRingPanel extends JPanel {
        private final double percent; // 0–100

        GoalRingPanel(double percent) {
            this.percent = percent;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int pad = 10;
            int size = Math.min(w, h) - pad * 2;
            int x = (w - size) / 2, y = (h - size) / 2;

            // Background ring
            g2.setColor(BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawOval(x, y, size, size);

            // Progress arc
            if (percent > 0) {
                double sweep = (percent / 100.0) * 360.0;
                Color arcColor = percent >= 100 ? SUCCESS
                    : (percent >= 80 ? WARNING : ACCENT_1);
                g2.setColor(arcColor);
                g2.setStroke(new BasicStroke(10, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Arc2D arc = new Arc2D.Double(x, y, size, size, 90, -sweep, Arc2D.OPEN);
                g2.draw(arc);
            }

            // Center percentage text
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
            String text = String.format("%.0f%%", percent);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, w / 2 - fm.stringWidth(text) / 2, h / 2 + fm.getAscent() / 2 - 2);

            g2.dispose();
        }
    }
}

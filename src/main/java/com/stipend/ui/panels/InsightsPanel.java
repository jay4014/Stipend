package com.stipend.ui.panels;

import com.stipend.model.Student;
import com.stipend.model.Transaction;
import com.stipend.model.Income;
import com.stipend.service.FinancialSystem;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * InsightsPanel: weekday spending heatmap, spending change vs last month,
 * biggest category badge, and a semi-circular budget gauge.
 */
public class InsightsPanel extends JPanel {

    private final Student student;

    // Updatable components
    private final JLabel bigCatLabel;
    private final JLabel changeLabel;
    private final JLabel heaviestDayLabel;
    private final JLabel lightestDayLabel;
    private final HeatmapPanel heatmap;
    private final GaugePanel   gauge;

    public InsightsPanel(Student student) {
        this.student = student;

        bigCatLabel     = label("—", FONT_KPI, ACCENT_1);
        changeLabel     = label("—", FONT_KPI, TEXT_PRIMARY);
        heaviestDayLabel = label("—", FONT_SUBHEAD, DANGER);
        lightestDayLabel = label("—", FONT_SUBHEAD, SUCCESS);
        heatmap = new HeatmapPanel();
        gauge   = new GaugePanel(0);

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

        // Header
        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setBackground(BG_MAIN);
        headerRow.add(label("Insights", FONT_TITLE, TEXT_PRIMARY), BorderLayout.WEST);
        headerRow.add(label("Current month · " + YearMonth.now(), FONT_BODY, TEXT_SECONDARY), BorderLayout.EAST);
        wrapper.add(headerRow, BorderLayout.NORTH);

        // Top insight cards row
        JPanel topRow = new JPanel(new GridLayout(1, 4, GAP, 0));
        topRow.setBackground(BG_MAIN);
        topRow.add(buildStatCard("Biggest Category",  bigCatLabel,  "🏆"));
        topRow.add(buildStatCard("vs Last Month",     changeLabel,  "📊"));
        topRow.add(buildStatCard("Heaviest Day",      heaviestDayLabel, "📅"));
        topRow.add(buildStatCard("Lightest Day",      lightestDayLabel, "🗓"));

        // Bottom: heatmap + gauge
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, GAP, 0));
        bottomRow.setBackground(BG_MAIN);

        JPanel heatmapCard = CoinSciousTheme.cardPanel();
        heatmapCard.setLayout(new BorderLayout(0, 8));
        heatmapCard.add(label("Weekday Spending Heatmap", FONT_SUBHEAD, TEXT_PRIMARY), BorderLayout.NORTH);
        heatmapCard.add(heatmap, BorderLayout.CENTER);

        JPanel gaugeCard = CoinSciousTheme.cardPanel();
        gaugeCard.setLayout(new BorderLayout(0, 8));
        gaugeCard.add(label("Budget Used (This Month)", FONT_SUBHEAD, TEXT_PRIMARY), BorderLayout.NORTH);
        gaugeCard.add(gauge, BorderLayout.CENTER);

        bottomRow.add(heatmapCard);
        bottomRow.add(gaugeCard);

        JPanel center = new JPanel(new BorderLayout(0, GAP));
        center.setBackground(BG_MAIN);
        center.add(topRow,    BorderLayout.NORTH);
        center.add(bottomRow, BorderLayout.CENTER);

        wrapper.add(center, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildStatCard(String title, JLabel valueLabel, String emoji) {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel emojiLbl = new JLabel(emoji);
        emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        emojiLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel titleLbl = label(title, new Font("Segoe UI", Font.PLAIN, 12), TEXT_SECONDARY);
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(LEFT_ALIGNMENT);

        card.add(emojiLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        return card;
    }

    // ---------------------------------------------------------------
    // Refresh: load data then update all components
    // ---------------------------------------------------------------
    public void refresh() {
        SwingWorker<InsightData, Void> worker = new SwingWorker<>() {
            @Override protected InsightData doInBackground() throws Exception {
                int sid = student.getStudentId();
                YearMonth now  = YearMonth.now();
                YearMonth prev = now.minusMonths(1);

                // All transactions this month
                List<Transaction> txns = FinancialSystem.getRepo().findByMonth(sid, now);

                // Category totals
                Map<String, Double> cats = FinancialSystem.getRepo()
                    .expensesByCategory(sid, now);
                String topCat = cats.isEmpty() ? "—" : cats.keySet().iterator().next();

                // Month-over-month change
                double curExp  = FinancialSystem.getTotalExpensesForMonth(sid, now);
                double prevExp = FinancialSystem.getTotalExpensesForMonth(sid, prev);
                double change  = prevExp > 0 ? ((curExp - prevExp) / prevExp) * 100.0 : 0;

                // Weekday totals (expenses only)
                Map<DayOfWeek, Double> dayTotals = new EnumMap<>(DayOfWeek.class);
                for (DayOfWeek d : DayOfWeek.values()) dayTotals.put(d, 0.0);
                txns.stream()
                    .filter(t -> !(t instanceof Income))
                    .forEach(t -> dayTotals.merge(t.getDate().getDayOfWeek(),
                        t.getAmount(), Double::sum));

                DayOfWeek heaviest = dayTotals.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                    .orElse(DayOfWeek.MONDAY);
                DayOfWeek lightest = dayTotals.entrySet().stream()
                    .min(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
                    .orElse(DayOfWeek.SUNDAY);

                // Budget gauge: total spent / total limits this month
                double totalInc = FinancialSystem.getTotalIncomeForMonth(sid, now);

                // Heatmap data: day-of-week × week-of-month → amount
                double[][] heatData = buildHeatData(txns, now);

                return new InsightData(topCat, change, heaviest, lightest,
                    dayTotals, totalInc, curExp, heatData);
            }

            @Override protected void done() {
                try {
                    InsightData d = get();
                    bigCatLabel.setText(d.topCategory);

                    String changeTxt = d.changePercent > 0
                        ? String.format("+%.1f%%", d.changePercent)
                        : String.format("%.1f%%", d.changePercent);
                    changeLabel.setText(changeTxt);
                    changeLabel.setForeground(d.changePercent > 0 ? DANGER : SUCCESS);

                    heaviestDayLabel.setText(
                        d.heaviestDay.getDisplayName(TextStyle.FULL, Locale.getDefault()));
                    lightestDayLabel.setText(
                        d.lightestDay.getDisplayName(TextStyle.FULL, Locale.getDefault()));

                    heatmap.setData(d.heatData, d.dayTotals);

                    double budgetPct = d.totalIncome > 0
                        ? Math.min(100, (d.totalExpenses / d.totalIncome) * 100) : 0;
                    gauge.setPercent(budgetPct);

                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        worker.execute();
    }

    // ---------------------------------------------------------------
    // Build 7×5 heatmap grid data
    // ---------------------------------------------------------------
    private double[][] buildHeatData(List<Transaction> txns, YearMonth month) {
        // rows = weeks (0–4), cols = day-of-week (Mon=0 … Sun=6)
        double[][] grid = new double[5][7];
        for (Transaction t : txns) {
            if (t instanceof Income) continue;
            LocalDate d = t.getDate();
            int week = (d.getDayOfMonth() - 1) / 7;
            int dow  = d.getDayOfWeek().getValue() - 1; // Mon=0
            if (week < 5) grid[week][dow] += t.getAmount();
        }
        return grid;
    }

    // ---------------------------------------------------------------
    // Data record
    // ---------------------------------------------------------------
    private record InsightData(
        String topCategory, double changePercent,
        DayOfWeek heaviestDay, DayOfWeek lightestDay,
        Map<DayOfWeek, Double> dayTotals,
        double totalIncome, double totalExpenses,
        double[][] heatData
    ) {}

    // ---------------------------------------------------------------
    // Weekday Heatmap (custom painted)
    // ---------------------------------------------------------------
    private static class HeatmapPanel extends JPanel {
        private double[][] data   = new double[5][7];
        private Map<DayOfWeek, Double> dayTotals = new EnumMap<>(DayOfWeek.class);

        HeatmapPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 180));
        }

        void setData(double[][] data, Map<DayOfWeek, Double> dayTotals) {
            this.data      = data;
            this.dayTotals = dayTotals;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
            int cellW = (getWidth() - 40) / 7;
            int cellH = 28;
            int padL  = 40, padT = 24;

            // Day labels
            g2.setFont(FONT_SMALL);
            g2.setColor(TEXT_SECONDARY);
            for (int d = 0; d < 7; d++) {
                g2.drawString(days[d], padL + d * cellW + cellW / 2 - 10, padT - 6);
            }

            // Find max for normalizing
            double max = 1;
            for (double[] row : data)
                for (double v : row) max = Math.max(max, v);

            // Cells
            for (int w = 0; w < 5; w++) {
                g2.setColor(TEXT_DISABLED);
                g2.setFont(FONT_SMALL);
                g2.drawString("W" + (w + 1), 4, padT + w * cellH + cellH / 2 + 4);
                for (int d = 0; d < 7; d++) {
                    double val = data[w][d];
                    float  alpha = val == 0 ? 0.1f : (float) (0.2 + 0.8 * (val / max));
                    Color  cell  = new Color(
                        ACCENT_2.getRed(), ACCENT_2.getGreen(), ACCENT_2.getBlue(),
                        (int)(alpha * 255));
                    g2.setColor(cell);
                    g2.fillRoundRect(padL + d * cellW + 2, padT + w * cellH + 2,
                        cellW - 4, cellH - 4, 6, 6);

                    if (val > 0) {
                        g2.setColor(TEXT_PRIMARY);
                        g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                        String s = MoneyFormatter.format(val).replace("₱", "");
                        if (s.length() > 6) s = s.substring(0, 6);
                        g2.drawString(s, padL + d * cellW + 4, padT + w * cellH + cellH / 2 + 4);
                    }
                }
            }
            g2.dispose();
        }
    }

    // ---------------------------------------------------------------
    // Semi-circular gauge (custom painted)
    // ---------------------------------------------------------------
    private static class GaugePanel extends JPanel {
        private double percent; // 0–100

        GaugePanel(double percent) {
            this.percent = percent;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 200));
        }

        void setPercent(double pct) {
            this.percent = Math.min(100, Math.max(0, pct));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int size = Math.min(w, h) - 30;
            int x = (w - size) / 2, y = h - size / 2 - 15;

            // Background arc (bottom half)
            g2.setColor(BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(24, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(x, y, size, size, 180, 180);

            // Filled arc
            if (percent > 0) {
                double sweep = (percent / 100.0) * 180.0;
                Color  fillColor = percent >= 100 ? DANGER : (percent >= 80 ? WARNING : ACCENT_1);
                GradientPaint gp = new GradientPaint(x, y, ACCENT_1, x + size, y, fillColor);
                g2.setPaint(gp);
                g2.setStroke(new BasicStroke(24, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawArc(x, y, size, size, 180, (int) sweep);
            }

            // Percentage text (center)
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
            String pctStr = String.format("%.0f%%", percent);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pctStr, w / 2 - fm.stringWidth(pctStr) / 2, y + size / 2 + 15);

            g2.setColor(TEXT_SECONDARY);
            g2.setFont(FONT_BODY);
            String sub = "Budget used";
            fm = g2.getFontMetrics();
            g2.drawString(sub, w / 2 - fm.stringWidth(sub) / 2, y + size / 2 + 38);

            // Tick marks
            g2.setColor(TEXT_DISABLED);
            g2.setFont(FONT_SMALL);
            String[] ticks = {"0%", "25%", "50%", "75%", "100%"};
            double[] angles = {180, 135, 90, 45, 0};
            for (int i = 0; i < ticks.length; i++) {
                double rad = Math.toRadians(angles[i]);
                int tx = (int)(x + size / 2.0 + (size / 2.0 + 16) * Math.cos(rad));
                int ty = (int)(y + size / 2.0 - (size / 2.0 + 16) * Math.sin(rad));
                fm = g2.getFontMetrics();
                g2.drawString(ticks[i], tx - fm.stringWidth(ticks[i]) / 2, ty);
            }

            g2.dispose();
        }
    }
}

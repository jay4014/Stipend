package com.stipend.ui.panels;

import com.stipend.model.Student;
import com.stipend.model.Transaction;
import com.stipend.service.FinancialSystem;
import com.stipend.ui.MainFrame;
import com.stipend.ui.dialogs.TransactionDialog;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.MoneyFormatter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * DashboardPanel: the main summary screen.
 * Shows KPI cards, spending donut, trend bar chart, and safe-to-spend card.
 *
 * displayDashboard() / showSummary() / navigateOptions() correspond to
 * the Dashboard methods in section 4.3.
 */
public class DashboardPanel extends JPanel {

    private final Student    student;
    private final MainFrame  mainFrame;

    // KPI labels (updated by refresh())
    private JLabel incomeKpi, expensesKpi, balanceKpi, savingsKpi;
    private JLabel incomeBadge, expensesBadge;

    // Chart panels
    private DonutChartPanel  donutChart;
    private TrendChartPanel  trendChart;

    // For badges: store last-month values
    private double lastMonthIncome    = -1;
    private double lastMonthExpenses  = -1;

    public DashboardPanel(Student student, MainFrame mainFrame) {
        this.student   = student;
        this.mainFrame = mainFrame;
        setBackground(BG_MAIN);
        setLayout(new BorderLayout());
        buildUI();
    }

    // ---------------------------------------------------------------
    // Build the full layout
    // ---------------------------------------------------------------
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_MAIN);
        root.setBorder(new EmptyBorder(PAD, PAD, PAD, PAD));

        root.add(buildTopBar(),    BorderLayout.NORTH);
        root.add(buildContent(),   BorderLayout.CENTER);

        add(root, BorderLayout.CENTER);
    }

   private JPanel buildTopBar() {
    JPanel bar = new JPanel(new BorderLayout());
    bar.setBackground(BG_MAIN);
    bar.setBorder(new EmptyBorder(0, 0, 16, 0));

    JLabel welcome = new JLabel("Welcome, " + student.getFirstName() + "!");
    welcome.setFont(FONT_TITLE.deriveFont(Font.BOLD, 34f));
    welcome.setForeground(TEXT_PRIMARY);

    JLabel subtitle = new JLabel("Here's your financial overview.");
    subtitle.setFont(FONT_BODY);
    subtitle.setForeground(TEXT_SECONDARY);

    JPanel left = new JPanel();
    left.setBackground(BG_MAIN);
    left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
    left.add(welcome);
    left.add(Box.createVerticalStrut(2));
    left.add(subtitle);

    JButton addBtn = primaryButton("  +  Add Transaction");
    addBtn.setPreferredSize(new Dimension(180, 28));
    addBtn.addActionListener(e -> {
        TransactionDialog dlg = new TransactionDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), student);
        dlg.setVisible(true);
        if (dlg.wasConfirmed()) {
            mainFrame.onDataChanged();
            refresh();
        }
    });

    bar.add(left, BorderLayout.WEST);
    bar.add(addBtn, BorderLayout.EAST);
    return bar;
}

    private JPanel buildContent() {
        JPanel content = new JPanel();
        content.setBackground(BG_MAIN);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Row 1: KPI cards
        JPanel kpiRow = buildKpiRow();
        kpiRow.setAlignmentX(LEFT_ALIGNMENT);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        content.add(kpiRow);
        content.add(Box.createVerticalStrut(GAP));

        // Row 2: Donut + Trend
        JPanel chartRow = buildChartRow();
        chartRow.setAlignmentX(LEFT_ALIGNMENT);
        content.add(chartRow);
        content.add(Box.createVerticalStrut(GAP));

        // Row 3: recent transactions
        JPanel recentsCard = buildRecentTransactions();
        recentsCard.setAlignmentX(LEFT_ALIGNMENT);
        content.add(recentsCard);

        return content;
    }

    // ---------------------------------------------------------------
    // KPI Cards
    // ---------------------------------------------------------------
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, GAP, 0));
        row.setBackground(BG_MAIN);

        incomeKpi   = kpiValueLabel();
        expensesKpi = kpiValueLabel();
        balanceKpi  = kpiValueLabel();
        savingsKpi  = kpiValueLabel();
        incomeBadge   = badgeLabel("", SUCCESS);
        expensesBadge = badgeLabel("", DANGER);

        row.add(buildKpiCard("Total Income",       incomeKpi,   incomeBadge,   ACCENT_TEAL));
        row.add(buildKpiCard("Total Expenses",     expensesKpi, expensesBadge, DANGER));
        row.add(buildKpiCard("Remaining Balance",  balanceKpi,  new JLabel(), ACCENT_1));
        row.add(buildKpiCard("Savings Rate",       savingsKpi,  new JLabel(), SUCCESS));

        return row;
    }

    private JPanel buildKpiCard(String title, JLabel valueLabel,
                                 JLabel badge, Color accent) {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BorderLayout());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel titleLbl = label(title, new Font("Segoe UI", Font.PLAIN, 12), TEXT_SECONDARY);
        top.add(titleLbl, BorderLayout.WEST);

        // Accent stripe at top
        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(0, 3));
        stripe.setOpaque(true);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(valueLabel, BorderLayout.WEST);
        bottom.add(badge, BorderLayout.EAST);

        card.add(stripe, BorderLayout.NORTH);
        card.add(top,    BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private JLabel kpiValueLabel() {
        JLabel l = new JLabel("—");
        l.setFont(FONT_KPI);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private JLabel badgeLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(color);
        l.setBorder(new EmptyBorder(2, 6, 2, 6));
        return l;
    }

    // ---------------------------------------------------------------
    // Chart row: Donut + Trend
    // ---------------------------------------------------------------
    private JPanel buildChartRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, GAP, 0));
        row.setBackground(BG_MAIN);

        // Donut card
        JPanel donutCard = CoinSciousTheme.cardPanel();
        donutCard.setLayout(new BorderLayout(0, 10));
        JLabel donutTitle = label("Spending by Category", FONT_SUBHEAD, TEXT_PRIMARY);
        donutChart = new DonutChartPanel();
        donutCard.add(donutTitle, BorderLayout.NORTH);
        donutCard.add(donutChart, BorderLayout.CENTER);

        // Trend card
        JPanel trendCard = CoinSciousTheme.cardPanel();
        trendCard.setLayout(new BorderLayout(0, 10));
        JLabel trendTitle = label("Income vs Expenses — This Month", FONT_SUBHEAD, TEXT_PRIMARY);
        trendChart = new TrendChartPanel();
        trendCard.add(trendTitle, BorderLayout.NORTH);
        trendCard.add(trendChart, BorderLayout.CENTER);

        row.add(donutCard);
        row.add(trendCard);
        return row;
    }

    // ---------------------------------------------------------------
    // Recent transactions table (last 5)
    // ---------------------------------------------------------------
    private JPanel buildRecentTransactions() {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BorderLayout(0, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(label("Recent Transactions", FONT_SUBHEAD, TEXT_PRIMARY), BorderLayout.WEST);
        JButton viewAll = CoinSciousTheme.ghostButton("View All →");
        viewAll.addActionListener(e -> mainFrame.showPage("Transactions"));
        header.add(viewAll, BorderLayout.EAST);

        // Table columns
        String[] cols = {"Date", "Type", "Category", "Description / Source", "Amount"};
        Object[][] data = {};
        JTable table = buildTable(cols, data);
        table.setName("recentTable");

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));
        scroll.setPreferredSize(new Dimension(0, 150));

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.setName("recentCard");
        return card;
    }

    // ---------------------------------------------------------------
    // Refresh (loads data and updates all labels + charts)
    // ---------------------------------------------------------------
    public void refresh() {
        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override protected DashboardData doInBackground() throws Exception {
                int sid = student.getStudentId();
                YearMonth now  = YearMonth.now();
                YearMonth prev = now.minusMonths(1);

                double income   = FinancialSystem.getTotalIncomeForMonth(sid, now);
                double expenses = FinancialSystem.getTotalExpensesForMonth(sid, now);
                double balance  = income - expenses;
                double savings  = FinancialSystem.savingsRateThisMonth(sid);

                double prevInc = FinancialSystem.getTotalIncomeForMonth(sid, prev);
                double prevExp = FinancialSystem.getTotalExpensesForMonth(sid, prev);

                Map<String, Double> catMap = FinancialSystem.getRepo()
                        .expensesByCategory(sid, now);
                Map<String, Double> dayMap = FinancialSystem.getRepo()
                        .dailyExpenses(sid, now);
                List<Transaction> recent = FinancialSystem.getRepo()
                        .findByMonth(sid, now);

                return new DashboardData(income, expenses, balance, savings,
                    prevInc, prevExp, catMap, dayMap, recent);
            }

            @Override protected void done() {
                try {
                    DashboardData d = get();
                    updateKpis(d);
                    donutChart.setData(d.catMap);
                    trendChart.setData(d.dayMap);
                    updateRecentTable(d.recent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private void updateKpis(DashboardData d) {
        incomeKpi.setText(MoneyFormatter.format(d.income));
        expensesKpi.setText(MoneyFormatter.format(d.expenses));
        balanceKpi.setText(MoneyFormatter.format(d.balance));
        savingsKpi.setText(String.format("%.1f%%", d.savings));

        // vs. last month badges
        if (d.prevIncome > 0) {
            double pct = ((d.income - d.prevIncome) / d.prevIncome) * 100.0;
            incomeBadge.setText(String.format("%+.1f%%", pct));
            incomeBadge.setForeground(pct >= 0 ? SUCCESS : DANGER);
        }
        if (d.prevExpenses > 0) {
            double pct = ((d.expenses - d.prevExpenses) / d.prevExpenses) * 100.0;
            expensesBadge.setText(String.format("%+.1f%%", pct));
            expensesBadge.setForeground(pct <= 0 ? SUCCESS : DANGER);
        }
    }

    private void updateRecentTable(List<Transaction> list) {
        // Find the table by name in the component hierarchy
        findTable(this, "recentTable", list);
    }

    private void findTable(Container root, String name, List<Transaction> list) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && c instanceof JTable table) {
                DefaultListModel<String> dummy = new DefaultListModel<>();
                String[] cols = {"Date", "Type", "Category", "Description/Source", "Amount"};
                Object[][] rows = list.stream()
                    .limit(8)
                    .map(t -> new Object[]{
                        t.getDate().toString(),
                        t instanceof com.stipend.model.Income ? "Income" : "Expense",
                        t.getCategory(),
                        t instanceof com.stipend.model.Income
                            ? ((com.stipend.model.Income) t).getSource()
                            : ((com.stipend.model.Expense) t).getDescription(),
                        MoneyFormatter.format(t.getAmount())
                    }).toArray(Object[][]::new);

                table.setModel(new javax.swing.table.DefaultTableModel(rows, cols) {
                    @Override public boolean isCellEditable(int r, int c) { return false; }
                });
                // Color rows
                table.setDefaultRenderer(Object.class, new ColoredCellRenderer(list));
                return;
            }
            if (c instanceof Container cont) findTable(cont, name, list);
        }
    }

    private JTable buildTable(String[] cols, Object[][] data) {
        JTable t = new JTable(new javax.swing.table.DefaultTableModel(data, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        });
        t.setBackground(BG_CARD);
        t.setForeground(TEXT_PRIMARY);
        t.setFont(FONT_BODY);
        t.setRowHeight(32);
        t.setGridColor(BORDER_SUBTLE);
        t.getTableHeader().setBackground(BG_SIDEBAR);
        t.getTableHeader().setForeground(TEXT_SECONDARY);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        t.setSelectionBackground(ACCENT_2.darker());
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        return t;
    }

    // ---------------------------------------------------------------
    // Data container record
    // ---------------------------------------------------------------
    private record DashboardData(
        double income, double expenses, double balance, double savings,
        double prevIncome, double prevExpenses,
        Map<String, Double> catMap, Map<String, Double> dayMap,
        List<Transaction> recent
    ) {}

    // ---------------------------------------------------------------
    // Colored cell renderer for the recent transactions table
    // ---------------------------------------------------------------
    private static class ColoredCellRenderer
            extends javax.swing.table.DefaultTableCellRenderer {
        private final List<Transaction> transactions;
        ColoredCellRenderer(List<Transaction> transactions) {
            this.transactions = transactions;
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            setBackground(selected ? ACCENT_2.darker() : BG_CARD);
            setForeground(TEXT_PRIMARY);
            if (col == 4 && row < transactions.size()) {
                boolean isIncome = transactions.get(row) instanceof com.stipend.model.Income;
                setForeground(isIncome ? SUCCESS : DANGER);
            }
            setBorder(new EmptyBorder(0, 8, 0, 8));
            setFont(FONT_BODY);
            return this;
        }
    }

    // ---------------------------------------------------------------
    // Inner chart panels
    // ---------------------------------------------------------------

    /** Donut chart drawn on a Canvas (no external library). */
    private static class DonutChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();
        private final JPanel legendPanel = new JPanel();

        DonutChartPanel() {
            setOpaque(false);
            setLayout(new BorderLayout(8, 8));
            legendPanel.setOpaque(false);
            legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));

            JPanel donut = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    paintDonut((Graphics2D) g);
                }
            };
            donut.setOpaque(false);
            donut.setPreferredSize(new Dimension(180, 180));

            add(donut,        BorderLayout.CENTER);
            add(legendPanel,  BorderLayout.EAST);
        }

        void setData(Map<String, Double> data) {
            this.data = data != null ? data : new LinkedHashMap<>();
            legendPanel.removeAll();
            int i = 0;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                Color c = CHART_COLORS[i % CHART_COLORS.length];
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                row.setOpaque(false);
                JPanel dot = new JPanel();
                dot.setBackground(c);
                dot.setPreferredSize(new Dimension(8, 8));
                JLabel lbl = new JLabel(e.getKey() + "  " + MoneyFormatter.format(e.getValue()));
                lbl.setFont(FONT_SMALL);
                lbl.setForeground(TEXT_SECONDARY);
                row.add(dot); row.add(lbl);
                legendPanel.add(row);
                i++;
            }
            legendPanel.revalidate();
            repaint();
        }

        private void paintDonut(Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int size = Math.min(w, h) - 20;
            int x = (w - size) / 2, y = (h - size) / 2;

            if (data.isEmpty()) {
                g2.setColor(BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(28, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                g2.drawOval(x + 14, y + 14, size - 28, size - 28);
                return;
            }

            double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
            double start = -90;
            int i = 0;
            for (double val : data.values()) {
                double sweep = (val / total) * 360.0;
                g2.setColor(CHART_COLORS[i % CHART_COLORS.length]);
                g2.setStroke(new BasicStroke(26, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
                Arc2D arc = new Arc2D.Double(x + 14, y + 14,
                    size - 28, size - 28, start, sweep, Arc2D.OPEN);
                g2.draw(arc);
                start += sweep;
                i++;
            }
            // Center text: total
            g2.setColor(TEXT_PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            String totalStr = MoneyFormatter.format(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalStr, w/2 - fm.stringWidth(totalStr)/2, h/2 + 4);
            g2.setColor(TEXT_SECONDARY);
            g2.setFont(FONT_SMALL);
            String spent = "total spent";
            fm = g2.getFontMetrics();
            g2.drawString(spent, w/2 - fm.stringWidth(spent)/2, h/2 + 18);
        }
    }

    /** Bar chart for daily income vs expense trend. */
    private static class TrendChartPanel extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        TrendChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 200));
        }

        void setData(Map<String, Double> data) {
            this.data = data != null ? data : new LinkedHashMap<>();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL = 50, padB = 30, padR = 10, padT = 20;
            int chartW = w - padL - padR;
            int chartH = h - padB - padT;

            if (data.isEmpty()) {
                g2.setColor(TEXT_DISABLED);
                g2.setFont(FONT_BODY);
                String msg = "No expense data for this month";
                g2.drawString(msg, w/2 - g2.getFontMetrics().stringWidth(msg)/2, h/2);
                g2.dispose();
                return;
            }

            double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
            if (max == 0) max = 1;

            // Gridlines
            g2.setColor(BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
            for (int i = 1; i <= 4; i++) {
                int y = padT + chartH - (i * chartH / 4);
                g2.drawLine(padL, y, w - padR, y);
            }
            g2.setStroke(new BasicStroke(1));

            // Bars
            int n = data.size();
            int barW = Math.max(4, chartW / (n * 2));
            int slot = chartW / n;
            int idx = 0;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                int barH = (int) ((e.getValue() / max) * chartH);
                int x = padL + idx * slot + (slot - barW) / 2;
                int y = padT + chartH - barH;

                GradientPaint gp = new GradientPaint(
                    x, y, ACCENT_ORANGE,
                    x, padT + chartH, ACCENT_ORANGE.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barW, barH, 4, 4);
                idx++;
            }

            // X-axis labels (every 5th day)
            g2.setColor(TEXT_DISABLED);
            g2.setFont(FONT_SMALL);
            idx = 0;
            for (String dateStr : data.keySet()) {
                if (idx % 5 == 0) {
                    String day = dateStr.substring(8); // DD
                    int x = padL + idx * slot + slot / 2;
                    g2.drawString(day, x - 5, h - 8);
                }
                idx++;
            }
            g2.dispose();
        }
    }
}

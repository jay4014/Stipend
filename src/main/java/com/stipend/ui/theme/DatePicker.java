package com.stipend.ui.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

public final class DatePicker extends JPanel {

    private static final String[] WEEKDAYS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private final boolean allowClear;
    private final JButton dateButton;
    private LocalDate date;

    public DatePicker(LocalDate initialDate) {
        this(initialDate, false);
    }

    public DatePicker(LocalDate initialDate, boolean allowClear) {
        super(new BorderLayout());
        this.allowClear = allowClear;
        this.date = initialDate;
        if (!allowClear && initialDate == null) {
            throw new IllegalArgumentException("A required date picker needs an initial date.");
        }

        setOpaque(false);
        dateButton = CoinSciousTheme.ghostButton("");
        dateButton.setHorizontalAlignment(SwingConstants.LEFT);
        dateButton.setPreferredSize(new Dimension(142, 36));
        dateButton.addActionListener(e -> showCalendar(
            YearMonth.from(date == null ? LocalDate.now() : date)));
        add(dateButton, BorderLayout.CENTER);
        updateButton();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        if (!allowClear && date == null) {
            throw new IllegalArgumentException("This date picker does not allow an empty date.");
        }
        this.date = date;
        updateButton();
    }

    private void updateButton() {
        dateButton.setText(date == null ? "Select date" : date.toString());
        dateButton.getAccessibleContext().setAccessibleName(
            date == null ? "Select date" : "Selected date " + date);
    }

    private void showCalendar(YearMonth visibleMonth) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(CoinSciousTheme.BORDER_SUBTLE));
        showMonth(popup, visibleMonth);
        popup.show(this, 0, getHeight());
    }

    private void showMonth(JPopupMenu popup, YearMonth month) {
        JPanel calendar = new JPanel(new BorderLayout(0, 8));
        calendar.setBackground(CoinSciousTheme.BG_CARD);
        calendar.setBorder(new EmptyBorder(10, 10, 8, 10));

        JButton previous = CoinSciousTheme.ghostButton("‹");
        previous.setPreferredSize(new Dimension(38, 30));
        previous.addActionListener(e -> showMonth(popup, month.minusMonths(1)));
        JButton next = CoinSciousTheme.ghostButton("›");
        next.setPreferredSize(new Dimension(38, 30));
        next.addActionListener(e -> showMonth(popup, month.plusMonths(1)));

        JLabel monthLabel = CoinSciousTheme.label(
            month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.getYear(),
            CoinSciousTheme.FONT_BOLD, CoinSciousTheme.TEXT_PRIMARY);
        monthLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel heading = new JPanel(new BorderLayout(6, 0));
        heading.setOpaque(false);
        heading.add(previous, BorderLayout.WEST);
        heading.add(monthLabel, BorderLayout.CENTER);
        heading.add(next, BorderLayout.EAST);
        calendar.add(heading, BorderLayout.NORTH);

        JPanel days = new JPanel(new GridLayout(7, 7, 2, 2));
        days.setOpaque(false);
        for (String weekday : WEEKDAYS) {
            JLabel label = CoinSciousTheme.label(
                weekday, CoinSciousTheme.FONT_SMALL, CoinSciousTheme.TEXT_SECONDARY);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            days.add(label);
        }

        int firstDayOffset = month.atDay(1).getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int daysInMonth = month.lengthOfMonth();
        for (int cell = 0; cell < 42; cell++) {
            int day = cell - firstDayOffset + 1;
            if (day < 1 || day > daysInMonth) {
                days.add(new JLabel());
                continue;
            }

            LocalDate candidate = month.atDay(day);
            JButton dayButton = new JButton(Integer.toString(day));
            dayButton.setFont(CoinSciousTheme.FONT_BODY);
            dayButton.setForeground(candidate.equals(date)
                ? CoinSciousTheme.TEXT_PRIMARY : CoinSciousTheme.TEXT_SECONDARY);
            dayButton.setBackground(candidate.equals(date)
                ? CoinSciousTheme.ACCENT_2 : CoinSciousTheme.BG_CARD);
            dayButton.setOpaque(true);
            dayButton.setFocusPainted(false);
            dayButton.setBorder(BorderFactory.createLineBorder(CoinSciousTheme.BORDER_SUBTLE));
            dayButton.setPreferredSize(new Dimension(34, 30));
            dayButton.getAccessibleContext().setAccessibleName(candidate.toString());
            dayButton.addActionListener(e -> {
                setDate(candidate);
                popup.setVisible(false);
            });
            days.add(dayButton);
        }
        calendar.add(days, BorderLayout.CENTER);

        if (allowClear) {
            JButton clear = CoinSciousTheme.ghostButton("Clear date");
            clear.addActionListener(e -> {
                setDate(null);
                popup.setVisible(false);
            });
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            footer.setOpaque(false);
            footer.add(clear);
            calendar.add(footer, BorderLayout.SOUTH);
        }

        popup.removeAll();
        popup.add(calendar);
        popup.setPopupSize(calendar.getPreferredSize());
        popup.revalidate();
        popup.repaint();
    }
}

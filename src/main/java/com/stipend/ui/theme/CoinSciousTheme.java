package com.stipend.ui.theme;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

/**
 * CoinSciousTheme: all design tokens (colors, fonts, borders) in one place.
 * Every UI class imports from here — changing a color here changes it everywhere.
 */
public final class CoinSciousTheme {

    // =========================================================
    // COLOR TOKENS (dark theme — section 6.6)
    // =========================================================
    public static final Color BG_MAIN        = new Color(0x0D, 0x0D, 0x1A); // #0D0D1A
    public static final Color BG_SIDEBAR     = new Color(0x11, 0x11, 0x1F); // #11111F
    public static final Color BG_CARD        = new Color(0x16, 0x16, 0x2B); // #16162B
    public static final Color BG_CARD_HOVER  = new Color(0x1D, 0x1D, 0x38); // #1D1D38
    public static final Color BORDER_SUBTLE  = new Color(0x24, 0x24, 0x3D); // #24243D

    public static final Color ACCENT_1       = new Color(0xA7, 0x8B, 0xFA); // #A78BFA (purple start)
    public static final Color ACCENT_2       = new Color(0x7C, 0x3A, 0xED); // #7C3AED (purple end)
    public static final Color ACCENT_TEAL    = new Color(0x2D, 0xD4, 0xBF); // #2DD4BF
    public static final Color ACCENT_ORANGE  = new Color(0xFB, 0x92, 0x3C); // #FB923C
    public static final Color ACCENT_PINK    = new Color(0xF4, 0x72, 0xB6); // #F472B6

    public static final Color SUCCESS        = new Color(0x4A, 0xDE, 0x80); // #4ADE80
    public static final Color WARNING        = new Color(0xFB, 0xBF, 0x24); // #FBBF24
    public static final Color DANGER         = new Color(0xF8, 0x71, 0x71); // #F87171

    public static final Color TEXT_PRIMARY   = new Color(0xFF, 0xFF, 0xFF); // #FFFFFF
    public static final Color TEXT_SECONDARY = new Color(0xA1, 0xA1, 0xBC); // #A1A1BC
    public static final Color TEXT_DISABLED  = new Color(0x5B, 0x5B, 0x78); // #5B5B78

    /** Chart category colors in order (section 6.6). */
    public static final Color[] CHART_COLORS = {
        ACCENT_1,
        ACCENT_TEAL,
        ACCENT_ORANGE,
        ACCENT_PINK,
        new Color(0x60, 0xA5, 0xFA), // blue
        WARNING,
        SUCCESS,
        DANGER
    };

    // =========================================================
    // FONTS
    // =========================================================
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD,  26);
    public static final Font FONT_KPI     = new Font("Segoe UI", Font.BOLD,  30);
    public static final Font FONT_SUBHEAD = new Font("Segoe UI", Font.BOLD,  15);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD,  13);
    public static final Font FONT_SIDEBAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN, 13);

    // =========================================================
    // SPACING
    // =========================================================
    public static final int GAP    = 16; // gap between cards
    public static final int PAD    = 24; // page padding
    public static final int CARDPAD = 20; // padding inside a card
    public static final int RADIUS  = 14; // card corner radius (painted manually)

    // =========================================================
    // BORDERS
    // =========================================================
    public static final Border CARD_BORDER =
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(CARDPAD, CARDPAD, CARDPAD, CARDPAD)
            );

    // =========================================================
    // FACTORY HELPERS
    // =========================================================
    private CoinSciousTheme() {}

    /** Applies global LAF defaults to all Swing components. */
    public static void applyGlobalDefaults() {
        UIManager.put("Panel.background",      BG_MAIN);
        UIManager.put("Label.foreground",      TEXT_PRIMARY);
        UIManager.put("TextField.background",  BG_CARD);
        UIManager.put("TextField.foreground",  TEXT_PRIMARY);
        UIManager.put("TextField.caretForeground", TEXT_PRIMARY);
        UIManager.put("PasswordField.background",  BG_CARD);
        UIManager.put("PasswordField.foreground",  TEXT_PRIMARY);
        UIManager.put("PasswordField.caretForeground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background",   BG_CARD);
        UIManager.put("ComboBox.foreground",   TEXT_PRIMARY);
        UIManager.put("Table.background",      BG_CARD);
        UIManager.put("Table.foreground",      TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", ACCENT_2);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("Table.gridColor",        BORDER_SUBTLE);
        UIManager.put("TableHeader.background", BG_SIDEBAR);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("ScrollPane.background", BG_MAIN);
        UIManager.put("Viewport.background",   BG_MAIN);
        UIManager.put("ScrollBar.background",  BG_SIDEBAR);
        UIManager.put("ScrollBar.thumb",       BORDER_SUBTLE);
        UIManager.put("ScrollBar.width",       12);
        UIManager.put("ScrollBarUI", ModernScrollBarUI.class.getName());
        UIManager.put("OptionPane.background", BG_CARD);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
    }

    // ---------------------------------------------------------------
    // Styled label factory
    // ---------------------------------------------------------------
    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    // ---------------------------------------------------------------
    // Primary gradient button (purple)
    // ---------------------------------------------------------------
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, ACCENT_1,
                    getWidth(), 0, ACCENT_2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(FONT_BOLD);
        b.setForeground(TEXT_PRIMARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setOpaque(false);
        return b;
    }

    // ---------------------------------------------------------------
    // Ghost (secondary) button
    // ---------------------------------------------------------------
    public static JButton ghostButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY);
        b.setForeground(TEXT_SECONDARY);
        b.setBackground(BG_CARD);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------------------------------------------------------
    // Danger button (red)
    // ---------------------------------------------------------------
    public static JButton dangerButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY);
        b.setForeground(DANGER);
        b.setBackground(new Color(0x3D, 0x16, 0x16));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DANGER.darker(), 1, true),
            new EmptyBorder(6, 14, 6, 14)
        ));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ---------------------------------------------------------------
    // Styled text field
    // ---------------------------------------------------------------
    public static JTextField styledField(int columns) {
        JTextField f = new JTextField(columns);
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        return f;
    }

    // ---------------------------------------------------------------
    // Styled password field
    // ---------------------------------------------------------------
    public static JPasswordField styledPasswordField(int columns) {
        JPasswordField f = new JPasswordField(columns);
        f.setBackground(BG_CARD);
        f.setForeground(TEXT_PRIMARY);
        f.setCaretColor(TEXT_PRIMARY);
        f.setFont(FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_SUBTLE, 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        return f;
    }

    // ---------------------------------------------------------------
    // Styled combo box
    // ---------------------------------------------------------------
    public static JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(BG_CARD);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(FONT_BODY);
        ((JComponent) cb.getRenderer()).setBackground(BG_CARD);
        return cb;
    }

    // ---------------------------------------------------------------
    // Card panel (rounded dark card)
    // ---------------------------------------------------------------
    public static JPanel cardPanel() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS * 2, RADIUS * 2);
                g2.setColor(BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RADIUS * 2, RADIUS * 2);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(CARDPAD, CARDPAD, CARDPAD, CARDPAD));
        return p;
    }

    // ---------------------------------------------------------------
    // Error label (inline validation)
    // ---------------------------------------------------------------
    public static JLabel errorLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(FONT_SMALL);
        l.setForeground(DANGER);
        return l;
    }

    // ---------------------------------------------------------------
    // Section header label
    // ---------------------------------------------------------------
    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_DISABLED);
        return l;
    }
}

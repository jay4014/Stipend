package com.stipend.ui.theme;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public final class ModernScrollBarUI extends BasicScrollBarUI {

    private static final int THICKNESS = 12;

    public static ComponentUI createUI(JComponent component) {
        return new ModernScrollBarUI();
    }

    @Override
    protected void configureScrollBarColors() {
        thumbColor = CoinSciousTheme.BORDER_SUBTLE;
        thumbDarkShadowColor = thumbColor;
        thumbHighlightColor = thumbColor;
        thumbLightShadowColor = thumbColor;
        trackColor = CoinSciousTheme.BG_MAIN;
        trackHighlightColor = trackColor;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createInvisibleButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createInvisibleButton();
    }

    private JButton createInvisibleButton() {
        JButton button = new JButton();
        button.setFocusable(false);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
            ? new Dimension(THICKNESS, 32)
            : new Dimension(32, THICKNESS);
    }

    @Override
    protected Dimension getMaximumThumbSize() {
        return scrollbar.getOrientation() == JScrollBar.VERTICAL
            ? new Dimension(THICKNESS, Integer.MAX_VALUE)
            : new Dimension(Integer.MAX_VALUE, THICKNESS);
    }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setColor(trackColor);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.dispose();
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) return;

        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(isThumbRollover() ? CoinSciousTheme.ACCENT_1 : thumbColor);

        int inset = 3;
        int x = bounds.x + inset;
        int y = bounds.y + inset;
        int width = Math.max(0, bounds.width - inset * 2);
        int height = Math.max(0, bounds.height - inset * 2);
        g.fillRoundRect(x, y, width, height, 8, 8);
        g.dispose();
    }
}

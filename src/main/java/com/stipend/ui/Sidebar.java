package com.stipend.ui;

import com.stipend.model.Student;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * Sidebar: left navigation panel (~220 px wide).
 * Implements the layout from section 6.2.
 */
public class Sidebar extends JPanel {

    public interface NavigationListener {
        void onNavigate(String page);
        void onLogout();
    }

    private static final int WIDTH = 220;

    private final Student           student;
    private final NavigationListener listener;
    private final List<JPanel>       navItems = new ArrayList<>();
    private String                   activePage = "Dashboard";
    private JLabel nameLabel;
    private JLabel courseLabel;
    private JPanel avatar;
    private BufferedImage avatarImage;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public Sidebar(Student student, NavigationListener listener) {
        this.student  = student;
        this.listener = listener;

        setPreferredSize(new Dimension(WIDTH, 0));
        setBackground(BG_SIDEBAR);
        setLayout(new BorderLayout());

        JPanel inner = new JPanel();
        inner.setBackground(BG_SIDEBAR);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBorder(new EmptyBorder(20, 12, 20, 12));

        inner.add(buildAvatar());
        inner.add(Box.createVerticalStrut(28));
        inner.add(buildSection(
            "Dashboard", "Transactions", "Budgets", "Goals", "Reports", "Insights"));
        inner.add(Box.createVerticalStrut(8));
        inner.add(buildSection("Settings"));
        inner.add(Box.createVerticalGlue());
        inner.add(buildSafeSpendCard());
        inner.add(buildLogoutButton());

        add(inner, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // Avatar + name + course
    // ---------------------------------------------------------------
    private JPanel buildAvatar() {
        JPanel p = new JPanel();
        p.setBackground(BG_SIDEBAR);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0, 4, 0, 4));

        updateAvatarImage();
        avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (avatarImage == null) {
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT_1, getWidth(), getHeight(), ACCENT_2);
                    g2.setPaint(gp);
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    g2.setColor(TEXT_PRIMARY);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                    FontMetrics metrics = g2.getFontMetrics();
                    String initials = student.getInitials();
                    g2.drawString(initials, (getWidth() - metrics.stringWidth(initials)) / 2,
                            (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
                } else {
                    g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, getWidth(), getHeight()));
                    g2.drawImage(avatarImage, 0, 0, getWidth(), getHeight(), null);
                }
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(80, 80));
        avatar.setMaximumSize(new Dimension(80, 80));

        nameLabel = new JLabel(student.getName(), SwingConstants.CENTER);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);

        courseLabel = new JLabel(student.getCourse(), SwingConstants.CENTER);
        courseLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        courseLabel.setForeground(TEXT_SECONDARY);
        courseLabel.setAlignmentX(CENTER_ALIGNMENT);

        avatar.setAlignmentX(CENTER_ALIGNMENT);

        p.add(avatar);
        p.add(Box.createVerticalStrut(12));
        p.add(nameLabel);
        p.add(Box.createVerticalStrut(2));
        p.add(courseLabel);
        return p;
    }

    // ---------------------------------------------------------------
    // Nav section block
    // ---------------------------------------------------------------
    private JPanel buildSection(String... pages) {
        JPanel p = new JPanel();
        p.setBackground(BG_SIDEBAR);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(0, 4, 0, 4));

        for (String page : pages) {
            JPanel item = buildNavItem(page);
            navItems.add(item);
            p.add(item);
            p.add(Box.createVerticalStrut(2));
        }
        return p;
    }

    // ---------------------------------------------------------------
    // Single text-only nav item, with active/hover state
    // ---------------------------------------------------------------
    private JPanel buildNavItem(String page) {
        JPanel item = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                if (page.equals(activePage)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT_1, getWidth(), 0, ACCENT_2);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setBorder(new EmptyBorder(8, 12, 8, 10));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel textLabel = new JLabel(page);
        textLabel.setFont(FONT_SIDEBAR);
        textLabel.setForeground(page.equals(activePage) ? TEXT_PRIMARY : TEXT_SECONDARY);

        item.add(textLabel, BorderLayout.WEST);

        // Hover effect
        item.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!page.equals(activePage)) { item.setOpaque(true); item.setBackground(BG_CARD_HOVER); }
            }
            @Override public void mouseExited(MouseEvent e) {
                item.setOpaque(false); item.repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                setActivePage(page);
                listener.onNavigate(page);
            }
        });

        return item;
    }

    // ---------------------------------------------------------------
    // Safe-to-spend tip card at the bottom
    // ---------------------------------------------------------------
    private JPanel buildSafeSpendCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0,
                    new Color(0x2D, 0x1B, 0x69),
                    getWidth(), getHeight(),
                    new Color(0x1E, 0x10, 0x4A));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel tipTitle = new JLabel("Safe to Spend");
        tipTitle.setFont(FONT_BOLD);
        tipTitle.setForeground(ACCENT_1);
        tipTitle.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tipValue = new JLabel("Loading...");
        tipValue.setName("safeToSpend"); // accessed by Dashboard to update
        tipValue.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tipValue.setForeground(TEXT_PRIMARY);
        tipValue.setAlignmentX(LEFT_ALIGNMENT);

        JLabel tipSub = new JLabel("per day remaining this month");
        tipSub.setFont(FONT_SMALL);
        tipSub.setForeground(TEXT_SECONDARY);
        tipSub.setAlignmentX(LEFT_ALIGNMENT);

        card.add(tipTitle);
        card.add(Box.createVerticalStrut(4));
        card.add(tipValue);
        card.add(Box.createVerticalStrut(2));
        card.add(tipSub);
        card.setName("safeSpendCard");
        Dimension cardSize = card.getPreferredSize();
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, cardSize.height));
        card.setAlignmentX(LEFT_ALIGNMENT);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_SIDEBAR);
        wrapper.setBorder(new EmptyBorder(0, 4, 6, 4));
        wrapper.add(card, BorderLayout.CENTER);
        Dimension wrapperSize = wrapper.getPreferredSize();
        Dimension controlSize = new Dimension(WIDTH - 24, wrapperSize.height);
        wrapper.setMinimumSize(controlSize);
        wrapper.setPreferredSize(controlSize);
        wrapper.setMaximumSize(controlSize);
        wrapper.setAlignmentX(RIGHT_ALIGNMENT);
        return wrapper;
    }

    // ---------------------------------------------------------------
    // Log out button
    // ---------------------------------------------------------------
    private JPanel buildLogoutButton() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_SIDEBAR);
        p.setBorder(new EmptyBorder(0, 4, 6, 4));

        JButton btn = new JButton("Log Out");
        btn.setFont(FONT_BODY);
        btn.setForeground(DANGER);
        btn.setBackground(BG_SIDEBAR);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(DANGER.darker(), 2, true),
            new EmptyBorder(8, 12, 8, 10)
        ));
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setPreferredSize(new Dimension(0, 38));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.addActionListener(e -> listener.onLogout());

        p.add(btn, BorderLayout.CENTER);
        Dimension preferredSize = p.getPreferredSize();
        Dimension controlSize = new Dimension(WIDTH - 24, preferredSize.height);
        p.setMinimumSize(controlSize);
        p.setPreferredSize(controlSize);
        p.setMaximumSize(controlSize);
        p.setAlignmentX(RIGHT_ALIGNMENT);
        return p;
    }

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------
    public void setActivePage(String page) {
        this.activePage = page;
        navItems.forEach(item -> {
            item.setOpaque(false);
            item.repaint();
        });
        repaint();
    }

    public void refreshProfile() {
        nameLabel.setText(student.getName());
        courseLabel.setText(student.getCourse());
        updateAvatarImage();
        avatar.repaint();
    }

    private void updateAvatarImage() {
        byte[] imageData = student.getProfileImage();
        if (imageData == null) {
            avatarImage = null;
            return;
        }
        try {
            avatarImage = ImageIO.read(new ByteArrayInputStream(imageData));
            if (avatarImage == null) {
                throw new java.io.IOException("Unsupported profile image data.");
            }
        } catch (java.io.IOException ex) {
            avatarImage = null;
            System.err.println("[Coinscious] Could not load profile image: " + ex.getMessage());
        }
    }

    /** Called by MainFrame after the safe-to-spend value is computed. */
    public void updateSafeToSpend(String formatted) {
        // Walk the component tree to find the label by name
        updateLabel(this, "safeToSpend", formatted);
    }

    private void updateLabel(Container root, String name, String text) {
        for (Component c : root.getComponents()) {
            if (name.equals(c.getName()) && c instanceof JLabel l) {
                l.setText(text);
                return;
            }
            if (c instanceof Container cont) updateLabel(cont, name, text);
        }
    }

}

package com.stipend.ui;

import com.stipend.ui.theme.CoinSciousTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * LandingView: the first screen users see.
 * Centered dark card with the STIPEND logo, tagline and a Log In button.
 * Account creation now lives on the login screen ("Sign up" link).
 */
public class LandingView extends JFrame {

    public LandingView() {
        setTitle("Stipend — Student Budget & Expense Tracker");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(540, 440);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BG_MAIN);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // faint radial glow in top-left
                RadialGradientPaint rg = new RadialGradientPaint(
                    new Point(0, 0), 300,
                    new float[]{0f, 1f},
                    new Color[]{new Color(0x7C, 0x3A, 0xED, 60), new Color(0, 0, 0, 0)}
                );
                g2.setPaint(rg);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(60, 60, 60, 60));

        JPanel card = buildCard();
        root.add(card, BorderLayout.CENTER);

        setContentPane(root);
    }

    private JPanel buildCard() {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Logo
        JLabel logo = new JLabel("STIPEND");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 34));
        logo.setForeground(ACCENT_1);
        logo.setAlignmentX(CENTER_ALIGNMENT);

        // Tagline
        JLabel tagline = new JLabel("Your smart money companion for student life.");
        tagline.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        tagline.setForeground(TEXT_SECONDARY);
        tagline.setAlignmentX(CENTER_ALIGNMENT);

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_SUBTLE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        // Description
        JLabel desc = new JLabel(
            "<html><center>Track your income, set budgets, reach your savings goals,"
            + "<br>and understand your spending—all in one place.</center></html>");
        desc.setFont(FONT_BODY);
        desc.setForeground(TEXT_SECONDARY);
        desc.setAlignmentX(CENTER_ALIGNMENT);

        // Button
        JButton loginBtn = primaryButton("  Log In  ");
        loginBtn.setPreferredSize(new Dimension(200, 44));
        loginBtn.setMaximumSize(new Dimension(200, 44));
        loginBtn.setAlignmentX(CENTER_ALIGNMENT);
        loginBtn.addActionListener(e -> openLogin());

        // Version tag
        JLabel version = new JLabel("v1.0 · OOP Final Project");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        version.setForeground(TEXT_DISABLED);
        version.setAlignmentX(CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(6));
        card.add(tagline);
        card.add(Box.createVerticalStrut(20));
        card.add(sep);
        card.add(Box.createVerticalStrut(20));
        card.add(desc);
        card.add(Box.createVerticalStrut(28));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(24));
        card.add(version);

        return card;
    }

    private void openLogin() {
        dispose();
        new LoginView().setVisible(true);
    }
}
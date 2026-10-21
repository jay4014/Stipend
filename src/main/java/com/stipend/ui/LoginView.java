package com.stipend.ui;

import com.stipend.service.AuthService;
import com.stipend.ui.theme.CoinSciousTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * LoginView: split-panel layout matching the reference design.
 * Left: gradient dune art with STIPEND logo.
 * Right: login form (username + password).
 * No social buttons, no decorative text.
 */
public class LoginView extends JFrame {

    // Dark card colors
    private static final Color CARD_BG    = new Color(0x1A, 0x16, 0x2E);
    private static final Color OUTER_BG   = new Color(0x2C, 0x26, 0x42);
    private static final Color FIELD_BG   = new Color(0x24, 0x1F, 0x3A);
    private static final Color FIELD_BORDER = new Color(0x3A, 0x34, 0x56);

    private final JTextField     usernameField = makeField("Username");
    private final JPasswordField passwordField = makePasswordField("Enter your password");
    private final JLabel         errorLabel    = errorLabel();

    public LoginView() {
        setTitle("Stipend — Log In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(OUTER_BG);
        root.add(buildMainCard());
        setContentPane(root);

        // Enter to submit
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) login();
            }
        };
        usernameField.addKeyListener(enter);
        passwordField.addKeyListener(enter);
    }

    // ===============================================================
    // Main rounded card (left art + right form)
    // ===============================================================
    private JPanel buildMainCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 28, 28));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(840, 490));

        card.add(new ArtPanel(), BorderLayout.WEST);
        card.add(buildFormPanel(), BorderLayout.CENTER);
        return card;
    }

    // ===============================================================
    // Right-side form
    // ===============================================================
    private JPanel buildFormPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(56, 48, 40, 48));

        // "Welcome back"
        JLabel heading = new JLabel("Welcome back");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        // "Don't have an account?  Sign up"
        JPanel sub = linkRow("Don't have an account? ", "Sign up",
            () -> { dispose(); new RegisterView().setVisible(true); });

        // Fields
        JLabel uLbl = fieldLabel("Username");
        JLabel pLbl = fieldLabel("Password");
        errorLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Button
        JButton loginBtn = purpleButton("Log In");
        loginBtn.addActionListener(e -> login());

        // Back link


        // Assembly
        p.add(heading);
        p.add(Box.createVerticalStrut(6));
        p.add(sub);
        p.add(Box.createVerticalStrut(36));
        p.add(uLbl);
        p.add(Box.createVerticalStrut(8));
        p.add(usernameField);
        p.add(Box.createVerticalStrut(20));
        p.add(pLbl);
        p.add(Box.createVerticalStrut(8));
        p.add(passwordField);
        p.add(Box.createVerticalStrut(6));
        p.add(errorLabel);
        p.add(Box.createVerticalStrut(28));
        p.add(loginBtn);
        p.add(Box.createVerticalStrut(18));


        return p;
    }

    private void login() {
        String username = usernameField.getText().trim();
        char[] password = passwordField.getPassword();
        String err = AuthService.login(username, password);
        java.util.Arrays.fill(password, '\0');
        if (err != null) {
            errorLabel.setText(err);
            passwordField.setText("");
        } else {
            dispose();
            new MainFrame().setVisible(true);
        }
    }

    // ===============================================================
    // Shared styled components (also used by RegisterView)
    // ===============================================================

    static JTextField makeField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0x70, 0x6A, 0x88));
                    g2.setFont(getFont());
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                    g2.dispose();
                }
            }
        };
        styleField(f);
        return f;
    }

    static JPasswordField makePasswordField(String placeholder) {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(new Color(0x70, 0x6A, 0x88));
                    g2.setFont(getFont());
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left, getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                    g2.dispose();
                }
            }
        };
        styleField(f);
        return f;
    }

    private static void styleField(JComponent f) {
        f.setBackground(FIELD_BG);
        f.setForeground(Color.WHITE);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(FIELD_BORDER, 1, true),
            new EmptyBorder(10, 14, 10, 14)
        ));
        if (f instanceof JTextField tf) tf.setCaretColor(Color.WHITE);
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        f.setPreferredSize(new Dimension(0, 46));
    }

    static JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(new Color(0xA0, 0x9A, 0xB8));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    static JButton purpleButton(String text) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(0x8B, 0x5C, 0xF6),
                    getWidth(), 0, new Color(0x6D, 0x38, 0xE0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        b.setPreferredSize(new Dimension(0, 48));
        return b;
    }

    static JPanel linkRow(String prefix, String linkText, Runnable action) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel pre = new JLabel(prefix);
        pre.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pre.setForeground(new Color(0x90, 0x8A, 0xA8));
        JLabel link = makeLink(linkText, action);
        row.add(pre);
        row.add(link);
        return row;
    }

    static JLabel makeLink(String text, Runnable action) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(ACCENT_1);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
            @Override public void mouseEntered(MouseEvent e) {
                l.setText("<html><u>" + text + "</u></html>");
            }
            @Override public void mouseExited(MouseEvent e) { l.setText(text); }
        });
        return l;
    }

    // ===============================================================
    // Left-side art panel (gradient + dune shapes + logo)
    // Shared by LoginView and RegisterView
    // ===============================================================
    static class ArtPanel extends JPanel {
        private final BufferedImage illustration = loadIllustration();

        ArtPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(370, 0));
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(12, 12, 12, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int w = getWidth(), h = getHeight();

            Shape clip = new RoundRectangle2D.Double(0, 0, w + 24, h, 24, 24);
            g2.setClip(clip);
            g2.setColor(new Color(0x1E, 0x0A, 0x3A));
            g2.fillRect(0, 0, w, h);

            double scale = Math.max(
                (double) w / illustration.getWidth(),
                (double) h / illustration.getHeight());
            int imageWidth = (int) Math.ceil(illustration.getWidth() * scale);
            int imageHeight = (int) Math.ceil(illustration.getHeight() * scale);
            int x = (w - imageWidth) / 2;
            int y = (h - imageHeight) / 2;
            g2.drawImage(illustration, x, y, imageWidth, imageHeight, null);

            g2.dispose();
        }

        private static BufferedImage loadIllustration() {
            try (InputStream stream = LoginView.class.getResourceAsStream(
                    "/images/login-illustration.png")) {
                if (stream == null) {
                    throw new IllegalStateException("Login illustration resource is missing.");
                }
                BufferedImage image = ImageIO.read(stream);
                if (image == null) {
                    throw new IllegalStateException("Login illustration is not a supported image.");
                }
                return image;
            } catch (IOException ex) {
                throw new IllegalStateException("Could not load the login illustration.", ex);
            }
        }
    }
}

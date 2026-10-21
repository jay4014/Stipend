package com.stipend.ui;

import com.stipend.service.AuthService;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.util.Validator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import static com.stipend.ui.LoginView.*;          // shared styled components
import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * RegisterView: split-panel layout matching the reference design.
 * Left: same gradient dune art panel with STIPEND logo.
 * Right: registration form (name, course dropdown, username, password, confirm).
 * No Google/Apple buttons, no decorative text.
 */
public class RegisterView extends JFrame {

    private static final Color CARD_BG  = new Color(0x1A, 0x16, 0x2E);
    private static final Color OUTER_BG = new Color(0x2C, 0x26, 0x42);

    // Dropdown styling (same palette as the text fields in LoginView)
    private static final Color FIELD_BG     = new Color(0x24, 0x1F, 0x3A);
    private static final Color FIELD_BORDER = new Color(0x3A, 0x34, 0x56);
    private static final Color ITEM_HOVER   = new Color(0x3A, 0x34, 0x56);
    private static final Color PLACEHOLDER  = new Color(0x70, 0x6A, 0x88);

    // Course options
    private static final String COURSE_PLACEHOLDER = "Select course";
    private static final String[] COURSES = {"BSIT", "BSHM", "BSTM", "BSED", "BEED", "CRIM"};

    // Form fields
    private final JTextField     nameField     = makeField("Full name");
    private final JComboBox<String> courseBox  = makeCourseBox();
    private final JTextField     usernameField = makeField("Username");
    private final JPasswordField passwordField = makePasswordField("Enter your password");
    private final JPasswordField confirmField  = makePasswordField("Confirm password");

    // Per-field error labels
    private final JLabel nameErr     = errLbl();
    private final JLabel courseErr   = errLbl();
    private final JLabel usernameErr = errLbl();
    private final JLabel passwordErr = errLbl();
    private final JLabel confirmErr  = errLbl();
    private final JLabel generalErr  = errLbl();

    public RegisterView() {
        setTitle("Stipend");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 600);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(OUTER_BG);
        root.add(buildMainCard());
        setContentPane(root);
    }

    // ===============================================================
    // Main rounded card
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
        card.setPreferredSize(new Dimension(840, 540));

        // Reuse the same art panel from LoginView
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
        p.setBorder(new EmptyBorder(36, 48, 28, 48));

        // "Create an account"
        JLabel heading = new JLabel("Create an account");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(Color.WHITE);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        // "Already have an account?  Log in"
        JPanel sub = linkRow("Already have an account? ", "Log in",
            () -> { dispose(); new LoginView().setVisible(true); });

        // ── Name + Course side-by-side ──
        JPanel nameRow = new JPanel(new GridLayout(1, 2, 12, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        nameRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        nameRow.add(nameField);
        nameRow.add(courseBox);

        JPanel nameErrRow = new JPanel(new GridLayout(1, 2, 12, 0));
        nameErrRow.setOpaque(false);
        nameErrRow.setAlignmentX(LEFT_ALIGNMENT);
        nameErrRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        nameErrRow.add(nameErr);
        nameErrRow.add(courseErr);

        // ── Username ──
        usernameField.setAlignmentX(LEFT_ALIGNMENT);
        usernameErr.setAlignmentX(LEFT_ALIGNMENT);

        // ── Password ──
        passwordField.setAlignmentX(LEFT_ALIGNMENT);
        passwordErr.setAlignmentX(LEFT_ALIGNMENT);

        // ── Confirm ──
        confirmField.setAlignmentX(LEFT_ALIGNMENT);
        confirmErr.setAlignmentX(LEFT_ALIGNMENT);

        generalErr.setAlignmentX(LEFT_ALIGNMENT);

        // Button
        JButton createBtn = purpleButton("Create account");
        createBtn.addActionListener(e -> register());

        // ── Assembly ──
        p.add(heading);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(24));

        p.add(nameRow);
        p.add(nameErrRow);
        p.add(Box.createVerticalStrut(10));

        p.add(fieldLabel("Username"));
        p.add(Box.createVerticalStrut(6));
        p.add(usernameField);
        p.add(usernameErr);
        p.add(Box.createVerticalStrut(10));

        p.add(fieldLabel("Password"));
        p.add(Box.createVerticalStrut(6));
        p.add(passwordField);
        p.add(passwordErr);
        p.add(Box.createVerticalStrut(10));

        p.add(fieldLabel("Confirm password"));
        p.add(Box.createVerticalStrut(6));
        p.add(confirmField);
        p.add(confirmErr);
        p.add(Box.createVerticalStrut(6));

        p.add(generalErr);
        p.add(Box.createVerticalStrut(18));
        p.add(createBtn);

        return p;
    }

    // ===============================================================
    // Course dropdown
    // ===============================================================
    private static JComboBox<String> makeCourseBox() {
        String[] items = new String[COURSES.length + 1];
        items[0] = COURSE_PLACEHOLDER;
        System.arraycopy(COURSES, 0, items, 1, COURSES.length);

        JComboBox<String> box = new JComboBox<>(items);
        box.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton b = new JButton() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
                        Path2D tri = new Path2D.Double();
                        tri.moveTo(cx - 5, cy - 2);
                        tri.lineTo(cx + 5, cy - 2);
                        tri.lineTo(cx, cy + 3);
                        tri.closePath();
                        g2.setColor(new Color(0xA0, 0x9A, 0xB8));
                        g2.fill(tri);
                        g2.dispose();
                    }
                };
                b.setBorder(new EmptyBorder(0, 0, 0, 10));
                b.setContentAreaFilled(false);
                b.setFocusPainted(false);
                b.setOpaque(false);
                return b;
            }

            @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                g.setColor(FIELD_BG);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });

        box.setBackground(FIELD_BG);
        box.setForeground(Color.WHITE);
        box.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        box.setBorder(BorderFactory.createLineBorder(FIELD_BORDER, 1, true));
        box.setFocusable(true);
        box.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        box.setPreferredSize(new Dimension(0, 46));

        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                l.setBorder(new EmptyBorder(10, 14, 10, 14));
                l.setBackground(isSelected && index >= 0 ? ITEM_HOVER : FIELD_BG);
                l.setForeground(COURSE_PLACEHOLDER.equals(value) ? PLACEHOLDER : Color.WHITE);
                l.setOpaque(true);
                return l;
            }
        });

        box.setSelectedIndex(0);
        return box;
    }

    // ===============================================================
    // Register logic
    // ===============================================================
    private void register() {
        // Clear all errors
        nameErr.setText(" "); courseErr.setText(" "); usernameErr.setText(" ");
        passwordErr.setText(" "); confirmErr.setText(" "); generalErr.setText(" ");

        String name     = nameField.getText().trim();
        String course   = courseBox.getSelectedIndex() > 0
                ? (String) courseBox.getSelectedItem() : "";
        String username = usernameField.getText().trim();
        char[] pass     = passwordField.getPassword();
        char[] confirm  = confirmField.getPassword();

        boolean ok = true;

        if (name.isBlank())   { nameErr.setText("Full name is required.");   ok = false; }
        if (course.isBlank()) { courseErr.setText("Please select a course."); ok = false; }

        String uErr = Validator.validateUsername(username);
        if (uErr != null) { usernameErr.setText(uErr); ok = false; }

        String pErr = Validator.validatePassword(pass);
        if (pErr != null) { passwordErr.setText(pErr); ok = false; }

        String cErr = Validator.validatePasswordsMatch(pass, confirm);
        if (cErr != null) { confirmErr.setText(cErr); ok = false; }

        if (!ok) {
            java.util.Arrays.fill(pass, '\0');
            java.util.Arrays.fill(confirm, '\0');
            return;
        }

        String err = AuthService.register(name, course, username, pass, confirm);
        java.util.Arrays.fill(pass, '\0');
        java.util.Arrays.fill(confirm, '\0');

        if (err != null) {
            generalErr.setText(err);
        } else {
            JOptionPane.showMessageDialog(this,
                "Account created successfully!\nYou can now log in.",
                "Registration Complete", JOptionPane.INFORMATION_MESSAGE);
            dispose();
            new LoginView().setVisible(true);
        }
    }

    // Small error label (red, compact)
    private static JLabel errLbl() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(DANGER);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }
}
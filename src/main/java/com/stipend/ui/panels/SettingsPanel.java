package com.stipend.ui.panels;

import com.stipend.model.Student;
import com.stipend.service.AuthService;
import com.stipend.ui.MainFrame;
import com.stipend.ui.LandingView;
import com.stipend.ui.theme.CoinSciousTheme;
import com.stipend.repository.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import javax.imageio.ImageIO;

import static com.stipend.ui.theme.CoinSciousTheme.*;

/**
 * SettingsPanel: profile edit, change password, backup/restore,
 * and delete account.
 */
public class SettingsPanel extends JPanel {

    private final Student    student;
    private final MainFrame  mainFrame;

    private final JTextField nameField;
    private final JTextField courseField;
    private final JLabel profilePreview;
    private byte[] pendingProfileImage;

    public SettingsPanel(Student student, MainFrame mainFrame) {
        this.student   = student;
        this.mainFrame = mainFrame;

        nameField   = CoinSciousTheme.styledField(20);
        courseField = CoinSciousTheme.styledField(20);
        profilePreview = new JLabel();
        pendingProfileImage = student.getProfileImage();
        nameField.setText(student.getName());
        courseField.setText(student.getCourse());
        updateProfilePreview();

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

        JLabel title = label("Settings", FONT_TITLE, TEXT_PRIMARY);
        wrapper.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, GAP, GAP));
        grid.setBackground(BG_MAIN);

        grid.add(buildProfileCard());
        grid.add(buildPasswordCard());
        grid.add(buildBackupCard());
        grid.add(buildDangerZoneCard());

        JScrollPane scrollPane = new JScrollPane(grid);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------
    // Profile card
    // ---------------------------------------------------------------
    private JPanel buildProfileCard() {
        JPanel card = sectionCard("👤  Profile");

        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        courseField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        addRow(card, "Full Name",       nameField);
        addRow(card, "Course / Program", courseField);
        addRow(card, "Username",         styledReadOnly(student.getUsername()));
        addRow(card, "Profile Picture", buildProfilePictureControls());

        JButton saveBtn = CoinSciousTheme.primaryButton("Save Changes");
        saveBtn.setAlignmentX(LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> saveProfile());

        card.add(Box.createVerticalStrut(8));
        card.add(saveBtn);
        return card;
    }

    private JPanel buildProfilePictureControls() {
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        controls.setAlignmentX(LEFT_ALIGNMENT);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        profilePreview.setPreferredSize(new Dimension(64, 64));
        profilePreview.setHorizontalAlignment(SwingConstants.CENTER);
        profilePreview.setBorder(BorderFactory.createLineBorder(BORDER_SUBTLE));

        JButton chooseButton = CoinSciousTheme.ghostButton("Choose Image");
        chooseButton.addActionListener(e -> chooseProfileImage());
        JButton removeButton = CoinSciousTheme.ghostButton("Remove");
        removeButton.addActionListener(e -> {
            pendingProfileImage = null;
            updateProfilePreview();
        });

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        chooseButton.setAlignmentX(LEFT_ALIGNMENT);
        removeButton.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(chooseButton);
        buttons.add(Box.createVerticalStrut(6));
        buttons.add(removeButton);

        controls.add(profilePreview);
        controls.add(buttons);
        return controls;
    }

    private void chooseProfileImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image files (PNG, JPG, GIF, BMP)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            BufferedImage selected = ImageIO.read(chooser.getSelectedFile());
            if (selected == null) {
                showWarn("The selected file is not a supported image.");
                return;
            }
            if (selected.getWidth() > 4096 || selected.getHeight() > 4096) {
                showWarn("Choose an image no larger than 4096 × 4096 pixels.");
                return;
            }

            BufferedImage normalized = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = normalized.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 256, 256);
            double scale = Math.min(256.0 / selected.getWidth(), 256.0 / selected.getHeight());
            int width = (int) Math.round(selected.getWidth() * scale);
            int height = (int) Math.round(selected.getHeight() * scale);
            graphics.drawImage(selected, (256 - width) / 2, (256 - height) / 2,
                    width, height, null);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(normalized, "png", output);
            pendingProfileImage = output.toByteArray();
            updateProfilePreview();
        } catch (IOException ex) {
            showErr("Could not load the selected image: " + ex.getMessage());
        }
    }

    private void updateProfilePreview() {
        if (pendingProfileImage == null) {
            profilePreview.setIcon(null);
            profilePreview.setText(student.getInitials());
            profilePreview.setForeground(TEXT_SECONDARY);
            profilePreview.setFont(FONT_BOLD);
            return;
        }

        ImageIcon imageIcon = new ImageIcon(pendingProfileImage);
        Image scaled = imageIcon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        profilePreview.setIcon(new ImageIcon(scaled));
        profilePreview.setText("");
    }

    private void saveProfile() {
        String name   = nameField.getText().trim();
        String course = courseField.getText().trim();

        if (name.isBlank() || course.isBlank()) {
            showWarn("Name and course must not be empty.");
            return;
        }

        String sql = "UPDATE students SET name=?, course=?, profile_image=? WHERE student_id=?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, course);
            if (pendingProfileImage == null) {
                ps.setNull(3, Types.BLOB);
            } else {
                ps.setBytes(3, pendingProfileImage);
            }
            ps.setInt(4, student.getStudentId());
            if (ps.executeUpdate() != 1) {
                showErr("Profile could not be saved because the account was not found.");
                return;
            }
            student.setName(name);
            student.setCourse(course);
            student.setProfileImage(pendingProfileImage);
            mainFrame.refreshProfile();
            showInfo("Profile updated successfully.");
        } catch (SQLException ex) {
            showErr("Update failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Change password card
    // ---------------------------------------------------------------
    private JPanel buildPasswordCard() {
        JPanel card = sectionCard("🔒  Change Password");

        JPasswordField currPwd    = CoinSciousTheme.styledPasswordField(15);
        JPasswordField newPwd     = CoinSciousTheme.styledPasswordField(15);
        JPasswordField confirmPwd = CoinSciousTheme.styledPasswordField(15);
        currPwd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        newPwd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        confirmPwd.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        addRow(card, "Current Password", currPwd);
        addRow(card, "New Password",     newPwd);
        addRow(card, "Confirm New",      confirmPwd);

        JLabel errLbl = errorLabel();
        errLbl.setAlignmentX(LEFT_ALIGNMENT);
        card.add(errLbl);

        JButton changeBtn = CoinSciousTheme.primaryButton("Change Password");
        changeBtn.setAlignmentX(LEFT_ALIGNMENT);
        changeBtn.addActionListener(e -> {
            errLbl.setText(" ");
            char[] curr    = currPwd.getPassword();
            char[] newP    = newPwd.getPassword();
            char[] confirm = confirmPwd.getPassword();

            String err = AuthService.changePassword(
                student.getStudentId(), curr, newP, confirm);

            java.util.Arrays.fill(curr, '\0');
            java.util.Arrays.fill(newP, '\0');
            java.util.Arrays.fill(confirm, '\0');

            if (err != null) errLbl.setText(err);
            else {
                currPwd.setText(""); newPwd.setText(""); confirmPwd.setText("");
                showInfo("Password changed successfully.");
            }
        });

        card.add(Box.createVerticalStrut(8));
        card.add(changeBtn);
        return card;
    }

    // ---------------------------------------------------------------
    // Backup / Restore card
    // ---------------------------------------------------------------
    private JPanel buildBackupCard() {
        JPanel card = sectionCard("💾  Backup & Restore");

        JLabel info = label(
            "Back up your Coinscious data file or restore from a previous backup.",
            FONT_BODY, TEXT_SECONDARY);
        info.setAlignmentX(LEFT_ALIGNMENT);
        card.add(info);
        card.add(Box.createVerticalStrut(12));

        JButton backupBtn = CoinSciousTheme.primaryButton("⬇  Backup Database");
        backupBtn.setAlignmentX(LEFT_ALIGNMENT);
        backupBtn.addActionListener(e -> backupDatabase());

        JButton restoreBtn = CoinSciousTheme.ghostButton("⬆  Restore Database");
        restoreBtn.setAlignmentX(LEFT_ALIGNMENT);
        restoreBtn.addActionListener(e -> restoreDatabase());

        card.add(backupBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(restoreBtn);
        return card;
    }

    private void backupDatabase() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("coinscious_backup.db"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.copy(Path.of(DatabaseManager.getDatabasePath()),
                fc.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            showInfo("Backup saved to:\n" + fc.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            showErr("Backup failed: " + ex.getMessage());
        }
    }

    private void restoreDatabase() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Restoring will REPLACE all current data. Continue?",
            "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "SQLite DB", "db", "sqlite"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Files.copy(fc.getSelectedFile().toPath(),
                Path.of(DatabaseManager.getDatabasePath()),
                StandardCopyOption.REPLACE_EXISTING);
            showInfo("Database restored. Please restart Coinscious.");
        } catch (IOException ex) {
            showErr("Restore failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Danger zone card
    // ---------------------------------------------------------------
    private JPanel buildDangerZoneCard() {
        JPanel card = sectionCard("⚠  Danger Zone");

        JLabel warn = label(
            "Deleting your account is permanent and cannot be undone.",
            FONT_BODY, DANGER);
        warn.setAlignmentX(LEFT_ALIGNMENT);
        card.add(warn);
        card.add(Box.createVerticalStrut(12));

        JButton deleteBtn = CoinSciousTheme.dangerButton("Delete My Account");
        deleteBtn.setAlignmentX(LEFT_ALIGNMENT);
        deleteBtn.addActionListener(e -> deleteAccount());
        card.add(deleteBtn);

        return card;
    }

    private void deleteAccount() {
        String input = JOptionPane.showInputDialog(this,
            "Type DELETE to confirm permanent account deletion:",
            "Delete Account", JOptionPane.WARNING_MESSAGE);
        if (!"DELETE".equals(input)) return;

        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection c = DatabaseManager.connect();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, student.getStudentId());
            ps.executeUpdate();
            AuthService.logout();
            mainFrame.dispose();
            new LandingView().setVisible(true);
        } catch (SQLException ex) {
            showErr("Delete failed: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------
    public void refresh() {} // nothing to refresh dynamically

    private JPanel sectionCard(String title) {
        JPanel card = CoinSciousTheme.cardPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        JLabel titleLbl = label(title, FONT_SUBHEAD, TEXT_PRIMARY);
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(14));
        return card;
    }

    private void addRow(JPanel parent, String labelText, JComponent field) {
        JLabel lbl = label(labelText, FONT_BOLD, TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(4));
        parent.add(field);
        parent.add(Box.createVerticalStrut(10));
    }

    private JTextField styledReadOnly(String text) {
        JTextField f = CoinSciousTheme.styledField(15);
        f.setText(text);
        f.setEditable(false);
        f.setForeground(TEXT_DISABLED);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return f;
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Coinscious", JOptionPane.INFORMATION_MESSAGE);
    }
    private void showWarn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation", JOptionPane.WARNING_MESSAGE);
    }
    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}

// File: src/ui/LoginView.java
package ui;

import dao.AdminDAO;
import models.Admin;

import javax.swing.*;
import javax.swing.border.EmptyBorder; 
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginView extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private AdminDAO adminDAO;

    public LoginView() {
        this.adminDAO = new AdminDAO();
        setTitle("Library Management System - Login");
        setMinimumSize(new Dimension(400, 450)); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new BorderLayout()); 
        getContentPane().setBackground(new Color(245, 248, 250)); 

        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 20)); 
        mainContentPanel.setBorder(new EmptyBorder(30, 50, 30, 50)); 
        mainContentPanel.setOpaque(false); 

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);
        topPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = new JLabel("\uD83D\uDCD6", SwingConstants.CENTER); 
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 50));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setBorder(new EmptyBorder(0, 0, 10, 0)); 

        JLabel titleLabel = new JLabel("Library Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setForeground(new Color(50, 50, 50)); 

        JLabel subtitleLabel = new JLabel("Please login to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setForeground(new Color(100, 100, 100)); 
        subtitleLabel.setBorder(new EmptyBorder(5, 0, 20, 0)); 

        topPanel.add(iconLabel);
        topPanel.add(titleLabel);
        topPanel.add(subtitleLabel);
        mainContentPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(20, 0, 20, 0)); 
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 5, 8, 5); 

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.LINE_END; gbc.weightx = 0.1;
        centerPanel.add(userLabel, gbc);

        usernameField = new JTextField(20);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(250, 35)); 
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(5, 8, 5, 8) 
        ));
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.LINE_START; gbc.weightx = 0.9;
        centerPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.LINE_END; gbc.weightx = 0.1;
        centerPanel.add(passLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(250, 35));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(5, 8, 5, 8)
        ));
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.LINE_START; gbc.weightx = 0.9;
        centerPanel.add(passwordField, gbc);

        JLabel forgetPasswordLabel = new JLabel("<html><u>Lupa password?</u></html>");
        forgetPasswordLabel.setForeground(new Color(0, 123, 255)); 
        forgetPasswordLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgetPasswordLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        forgetPasswordLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                JOptionPane.showMessageDialog(LoginView.this, "Fitur 'Lupa Password' belum diimplementasikan.", "Informasi", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.LINE_END; gbc.insets = new Insets(0, 5, 15, 5); 
        centerPanel.add(forgetPasswordLabel, gbc);
        
        mainContentPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 0, 0, 0)); 
        loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(200, 45)); 
        loginButton.setBackground(new Color(0, 123, 255)); 
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20)); 
        loginButton.addActionListener(e -> performLogin());
        bottomPanel.add(loginButton);
        mainContentPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainContentPanel, BorderLayout.CENTER);
        pack(); 
        setLocationRelativeTo(null); 
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan Password tidak boleh kosong!", "Error Login", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Admin admin = adminDAO.validateLogin(username, password);

        if (admin != null) {
            JOptionPane.showMessageDialog(this, "Login Berhasil! Selamat datang, " + admin.getUsername() + ".", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            MainAppView mainApp = new MainAppView(admin.getUsername(), admin.getId());
            mainApp.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Username atau Password salah, atau terjadi masalah koneksi database.", "Error Login", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                boolean nimbusFound = false;
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        nimbusFound = true;
                        break;
                    }
                }
                if (!nimbusFound) {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                }
            } catch (Exception e) {
                try {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            new LoginView().setVisible(true);
        });
    }
}

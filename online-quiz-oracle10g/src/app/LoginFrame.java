package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField = new JTextField();
    private JPasswordField passwordField = new JPasswordField();
    private JButton loginBtn = new JButton("Login");

    public interface LoginListener {
        void onLoginSuccess(long userId, String username);
    }

    public LoginFrame(LoginListener listener) {
        super("Online Quiz - Login");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx=0; gc.gridy=0; p.add(new JLabel("Username"), gc);
        gc.gridx=1; gc.gridy=0; p.add(usernameField, gc);
        gc.gridx=0; gc.gridy=1; p.add(new JLabel("Password"), gc);
        gc.gridx=1; gc.gridy=1; p.add(passwordField, gc);
        gc.gridx=1; gc.gridy=2; p.add(loginBtn, gc);

        loginBtn.addActionListener(e -> doLogin(listener));

        setContentPane(p);
    }

    private void doLogin(LoginListener listener) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter username and password");
            return;
        }
        String hash = HashUtil.sha256Hex(password);
        Connection c = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            c = Db.get();
            ps = c.prepareStatement("SELECT user_id, username FROM users WHERE username = ? AND password_hash = ?");
            ps.setString(1, username);
            ps.setString(2, hash);
            rs = ps.executeQuery();
            if (rs.next()) {
                long userId = rs.getLong("user_id");
                listener.onLoginSuccess(userId, username);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        } finally {
            Db.closeQuietly(rs); Db.closeQuietly(ps);
        }
    }
}

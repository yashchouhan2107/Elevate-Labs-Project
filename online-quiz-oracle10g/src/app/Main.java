package app;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame((userId, username) -> {
                new QuizSetupFrame(userId, username).setVisible(true);
            }).setVisible(true);
        });
    }
}

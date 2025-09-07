package app;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class QuizSetupFrame extends JFrame {
    private long userId;
    private String username;

    private JComboBox<Item> quizBox = new JComboBox<>();
    private JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
    private JTextField durationField = new JTextField(); // seconds from DB
    private JButton startBtn = new JButton("Start Quiz");

    static class Item {
        long id; String label;
        Item(long id, String label){ this.id=id; this.label=label; }
        public String toString(){ return label; }
    }

    public QuizSetupFrame(long userId, String username) {
        super("Online Quiz - Setup");
        this.userId = userId;
        this.username = username;

        setSize(500, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8,8,8,8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        // Load quizzes
        loadQuizzes();

        gc.gridx=0; gc.gridy=0; p.add(new JLabel("User"), gc);
        gc.gridx=1; gc.gridy=0; p.add(new JLabel(username), gc);
        gc.gridx=0; gc.gridy=1; p.add(new JLabel("Quiz"), gc);
        gc.gridx=1; gc.gridy=1; p.add(quizBox, gc);
        gc.gridx=0; gc.gridy=2; p.add(new JLabel("Number of Questions"), gc);
        gc.gridx=1; gc.gridy=2; p.add(countSpinner, gc);
        gc.gridx=0; gc.gridy=3; p.add(new JLabel("Duration (sec)"), gc);
        gc.gridx=1; gc.gridy=3; p.add(durationField, gc);

        startBtn.addActionListener(e -> startQuiz());
        gc.gridx=1; gc.gridy=4; p.add(startBtn, gc);

        setContentPane(p);
    }

    private void loadQuizzes() {
        Connection c=null; Statement st=null; ResultSet rs=null;
        try{
            c = Db.get();
            st = c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, // allows scrolling
ResultSet.CONCUR_READ_ONLY);
            rs = st.executeQuery("SELECT quiz_id, title, duration_sec FROM quizzes ORDER BY quiz_id");
            while (rs.next()) {
                long id = rs.getLong(1);
                String title = rs.getString(2);
                int duration = rs.getInt(3);
                quizBox.addItem(new Item(id, title + " (" + duration + "s)"));
            }
            if (quizBox.getItemCount() > 0) {
                // default duration from first quiz
                rs.beforeFirst();
                // can't rewind; just query again for first selected item
                Item it = (Item)quizBox.getItemAt(0);
                PreparedStatement ps = c.prepareStatement("SELECT duration_sec FROM quizzes WHERE quiz_id=?");
                ps.setLong(1, it.id);
                ResultSet r2 = ps.executeQuery();
                if (r2.next()) durationField.setText(Integer.toString(r2.getInt(1)));
                r2.close(); ps.close();
            }
            quizBox.addActionListener(e -> {
                try {
                    Item it = (Item)quizBox.getSelectedItem();
                    if (it != null) {
                        PreparedStatement ps = Db.get().prepareStatement("SELECT duration_sec FROM quizzes WHERE quiz_id=?");
                        ps.setLong(1, it.id);
                        ResultSet r2 = ps.executeQuery();
                        if (r2.next()) durationField.setText(Integer.toString(r2.getInt(1)));
                        r2.close(); ps.close();
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            });
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        } finally {
            Db.closeQuietly(rs); Db.closeQuietly(st);
        }
    }

    private void startQuiz() {
        Item it = (Item)quizBox.getSelectedItem();
        if (it == null) {
            JOptionPane.showMessageDialog(this, "No quiz available");
            return;
        }
        int n = ((Integer)countSpinner.getValue()).intValue();
        int durationSec;
        try { durationSec = Integer.parseInt(durationField.getText().trim()); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Invalid duration"); return; }

        new QuizFrame(userId, it.id, n, durationSec).setVisible(true);
        dispose();
    }
}

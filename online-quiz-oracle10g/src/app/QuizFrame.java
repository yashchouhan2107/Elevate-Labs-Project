package app;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import app.Models.*;

public class QuizFrame extends JFrame {
    private long userId;
    private long quizId;
    private int totalQuestions;
    private int durationSec;

    private java.util.List<Question> questions = new ArrayList<>();
    private int idx = 0;
    private Map<Long, Long> selected = new HashMap<>(); // questionId -> optionId
    private JLabel timerLabel = new JLabel();
    private javax.swing.Timer swingTimer;
    private long endMillis;

    private JTextArea questionArea = new JTextArea(4, 40);
    private ButtonGroup optionsGroup = new ButtonGroup();
    private JPanel optionsPanel = new JPanel();
    private JButton nextBtn = new JButton("Next");
    private JButton submitBtn = new JButton("Submit");

    private long attemptId = -1;

    public QuizFrame(long userId, long quizId, int totalQuestions, int durationSec) {
        super("Online Quiz - Taking Quiz");
        this.userId = userId;
        this.quizId = quizId;
        this.totalQuestions = totalQuestions;
        this.durationSec = durationSec;

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        questionArea.setEditable(false);

        JPanel top = new JPanel(new BorderLayout());
        top.add(timerLabel, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout());
        center.add(new JScrollPane(questionArea), BorderLayout.NORTH);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        center.add(optionsPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.add(nextBtn);
        bottom.add(submitBtn);

        setLayout(new BorderLayout());
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        nextBtn.addActionListener(e -> { saveSelection(); nextQuestion(); });
        submitBtn.addActionListener(e -> { saveSelection(); submitQuiz(); });

        loadQuestions();
        startAttempt();
        startTimer();
        renderQuestion();
    }

    /** Start a new attempt and fetch the generated attempt_id */
    private void startAttempt() {
        Connection c = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            c = Db.get();
            ps = c.prepareStatement(
                "INSERT INTO quiz_attempts (user_id, quiz_id, total_questions, duration_sec, started_at) " +
                "VALUES (?, ?, ?, ?, SYSTIMESTAMP)",
                new String[] {"ATTEMPT_ID"}   // tell JDBC to return the PK
            );
            ps.setLong(1, userId);
            ps.setLong(2, quizId);
            ps.setInt(3, totalQuestions);
            ps.setInt(4, durationSec);
            ps.executeUpdate();

            // ✅ Fetch generated ATTEMPT_ID
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                attemptId = rs.getLong(1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error creating attempt: " + ex.getMessage());
        } finally {
            Db.closeQuietly(rs); Db.closeQuietly(ps);
        }
    }

    /** Load random questions for this quiz */
    private void loadQuestions() {
        Connection c=null; PreparedStatement ps=null; ResultSet rs=null;
        try {
            c = Db.get();
            String sql = "SELECT * FROM (SELECT q.question_id, q.question_text FROM questions q " +
                         "WHERE q.quiz_id = ? ORDER BY DBMS_RANDOM.VALUE) WHERE ROWNUM <= ?";
            ps = c.prepareStatement(sql);
            ps.setLong(1, quizId);
            ps.setInt(2, totalQuestions);
            rs = ps.executeQuery();
            while (rs.next()) {
                Question q = new Question();
                q.questionId = rs.getLong("question_id");
                q.text = rs.getString("question_text");
                // load options
                PreparedStatement ps2 = c.prepareStatement(
                    "SELECT option_id, option_text, is_correct FROM options WHERE question_id = ? ORDER BY option_id"
                );
                ps2.setLong(1, q.questionId);
                ResultSet r2 = ps2.executeQuery();
                while (r2.next()) {
                    Models.Option op = new Models.Option();
                    op.optionId = r2.getLong(1);
                    op.text = r2.getString(2);
                    op.isCorrect = "Y".equalsIgnoreCase(r2.getString(3));
                    q.options.add(op);
                }
                r2.close(); ps2.close();
                questions.add(q);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error loading questions: " + ex.getMessage());
        } finally {
            Db.closeQuietly(rs); Db.closeQuietly(ps);
        }
    }

    /** Start countdown timer */
    private void startTimer() {
        endMillis = System.currentTimeMillis() + (durationSec * 1000L);
        swingTimer = new javax.swing.Timer(250, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                long remain = endMillis - System.currentTimeMillis();
                if (remain <= 0) {
                    timerLabel.setText("00:00");
                    swingTimer.stop();
                    saveSelection();
                    submitQuiz();
                } else {
                    long s = remain / 1000;
                    long m = s / 60;
                    long sec = s % 60;
                    timerLabel.setText(String.format("%02d:%02d", m, sec));
                }
            }
        });
        swingTimer.start();
    }

    /** Show current question */
    private void renderQuestion() {
        if (idx >= questions.size()) {
            submitQuiz();
            return;
        }
        Question q = questions.get(idx);
        questionArea.setText("Q" + (idx+1) + ") " + q.text);
        optionsPanel.removeAll();
        optionsGroup = new ButtonGroup();
        Long selectedOption = selected.get(q.questionId);
        for (Models.Option op : q.options) {
            JRadioButton rb = new JRadioButton(op.text);
            rb.setActionCommand(Long.toString(op.optionId));
            optionsGroup.add(rb);
            optionsPanel.add(rb);
            if (selectedOption != null && selectedOption.longValue() == op.optionId) {
                rb.setSelected(true);
            }
        }
        optionsPanel.revalidate();
        optionsPanel.repaint();
    }

    /** Save selected option for current question */
    private void saveSelection() {
        if (idx < questions.size()) {
            Question q = questions.get(idx);
            ButtonModel bm = optionsGroup.getSelection();
            if (bm != null) {
                long optionId = Long.parseLong(bm.getActionCommand());
                selected.put(q.questionId, optionId);
            }
        }
    }

    private void nextQuestion() {
        idx++;
        renderQuestion();
    }

    /** Submit quiz, compute score, save answers */
    private void submitQuiz() {
        // compute score
        int score = 0;
        for (Question q : questions) {
            Long opt = selected.get(q.questionId);
            if (opt != null) {
                for (Models.Option op : q.options) {
                    if (op.optionId == opt.longValue() && op.isCorrect) {
                        score++;
                    }
                }
            }
        }

        Connection c=null; PreparedStatement ps=null;
        try {
            c = Db.get();
            // update attempt with score
            ps = c.prepareStatement("UPDATE quiz_attempts SET ended_at = SYSTIMESTAMP, score = ? WHERE attempt_id = ?");
            ps.setInt(1, score);
            ps.setLong(2, attemptId);
            ps.executeUpdate();
            Db.closeQuietly(ps);

            // ✅ batch insert answers
            ps = c.prepareStatement(
                "INSERT INTO attempt_answers (attempt_id, question_id, selected_option_id, is_correct) VALUES (?, ?, ?, ?)"
            );
            for (Question q : questions) {
                Long opt = selected.get(q.questionId);
                String correct = "N";
                if (opt != null) {
                    for (Models.Option op : q.options) {
                        if (op.optionId == opt.longValue() && op.isCorrect) {
                            correct = "Y";
                        }
                    }
                }
                ps.setLong(1, attemptId);
                ps.setLong(2, q.questionId);
                if (opt == null) ps.setNull(3, java.sql.Types.NUMERIC);
                else ps.setLong(3, opt.longValue());
                ps.setString(4, correct);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error saving attempt: " + ex.getMessage());
        } finally {
            Db.closeQuietly(ps);
        }
        // Show result
        new ResultFrame(attemptId).setVisible(true);
        dispose();
    }
}

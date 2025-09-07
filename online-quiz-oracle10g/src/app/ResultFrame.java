package app;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.sql.*;

public class ResultFrame extends JFrame {
    private long attemptId;
    private JTextArea area = new JTextArea(20, 70);
    private JButton exportBtn = new JButton("Export CSV");

    public ResultFrame(long attemptId) {
        super("Quiz Result");
        this.attemptId = attemptId;
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        area.setEditable(false);
        JPanel bottom = new JPanel();
        bottom.add(exportBtn);
        exportBtn.addActionListener(e -> exportCsv());

        setLayout(new BorderLayout());
        add(new JScrollPane(area), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        loadAndRender();
    }

    private void loadAndRender() {
        try (Connection c = Db.get()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT a.attempt_id, u.username, q.title, a.started_at, a.ended_at, a.score, a.total_questions " +
                "FROM quiz_attempts a JOIN users u ON u.user_id=a.user_id " +
                "JOIN quizzes q ON q.quiz_id=a.quiz_id WHERE a.attempt_id=?"
            );
            ps.setLong(1, attemptId);
            ResultSet rs = ps.executeQuery();
            String header = "";
            if (rs.next()) {
                header = String.format("User: %s\nQuiz: %s\nStarted: %s\nEnded: %s\nScore: %d / %d\n\n",
                        rs.getString("username"),
                        rs.getString("title"),
                        rs.getTimestamp("started_at"),
                        rs.getTimestamp("ended_at"),
                        rs.getInt("score"),
                        rs.getInt("total_questions"));
            }
            rs.close(); ps.close();

            StringBuilder sb = new StringBuilder(header);
            sb.append("Answers:\n");

            PreparedStatement ps2 = c.prepareStatement(
                "SELECT qs.question_id, qs.question_text, aa.selected_option_id, aa.is_correct " +
                "FROM attempt_answers aa " +
                "JOIN questions qs ON qs.question_id = aa.question_id " +
                "WHERE aa.attempt_id = ? ORDER BY qs.question_id"
            );
            ps2.setLong(1, attemptId);
            rs = ps2.executeQuery();
            while (rs.next()) {
                long qid = rs.getLong(1);
                String qtext = rs.getString(2);

                // ✅ Fix BigDecimal cast issue
                Long selectedOpt = null;
                Object obj = rs.getObject(3);
                if (obj != null) {
                    selectedOpt = ((java.math.BigDecimal) obj).longValue();
                }

                String isCorrect = rs.getString(4);

                String correctText = "";
                PreparedStatement ps3 = c.prepareStatement(
                    "SELECT option_text FROM options WHERE question_id=? AND is_correct='Y'"
                );
                ps3.setLong(1, qid);
                ResultSet r3 = ps3.executeQuery();
                if (r3.next()) correctText = r3.getString(1);
                r3.close(); ps3.close();

                String selectedText = "(no answer)";
                if (selectedOpt != null) {
                    PreparedStatement ps4 = c.prepareStatement("SELECT option_text FROM options WHERE option_id=?");
                    ps4.setLong(1, selectedOpt);
                    ResultSet r4 = ps4.executeQuery();
                    if (r4.next()) selectedText = r4.getString(1);
                    r4.close(); ps4.close();
                }

                sb.append("Q: ").append(qtext).append("\n");
                sb.append("Your answer: ").append(selectedText).append("\n");
                sb.append("Correct answer: ").append(correctText).append("\n");
                sb.append("Result: ").append("Y".equals(isCorrect) ? "Correct" : "Wrong").append("\n\n");
            }
            rs.close(); ps2.close();

            area.setText(sb.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
        }
    }

    private void exportCsv() {
        try (Connection c = Db.get()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT u.username, q.title, a.started_at, a.ended_at, a.score, a.total_questions " +
                "FROM quiz_attempts a JOIN users u ON u.user_id=a.user_id " +
                "JOIN quizzes q ON q.quiz_id=a.quiz_id WHERE a.attempt_id=?"
            );
            ps.setLong(1, attemptId);
            ResultSet rs = ps.executeQuery();
            String username="", title=""; Timestamp st=null,en=null; int score=0, total=0;
            if (rs.next()) {
                username=rs.getString(1); title=rs.getString(2);
                st=rs.getTimestamp(3); en=rs.getTimestamp(4);
                score=rs.getInt(5); total=rs.getInt(6);
            }
            rs.close(); ps.close();

            PreparedStatement ps2 = c.prepareStatement(
                "SELECT qs.question_text, aa.selected_option_id, aa.is_correct, " +
                "(SELECT option_text FROM options WHERE question_id=qs.question_id AND is_correct='Y') AS correct_text, " +
                "(SELECT option_text FROM options WHERE option_id=aa.selected_option_id) AS selected_text " +
                "FROM attempt_answers aa JOIN questions qs ON qs.question_id=aa.question_id " +
                "WHERE aa.attempt_id=? ORDER BY qs.question_id"
            );
            ps2.setLong(1, attemptId);
            rs = ps2.executeQuery();

            java.nio.file.Path out = java.nio.file.Paths.get("exports", "quiz_result_" + attemptId + ".csv");
            out.toFile().getParentFile().mkdirs();
            FileWriter fw = new FileWriter(out.toFile());
            fw.write("username,quiz,started_at,ended_at,score,total\n");
            fw.write(String.format("%s,%s,%s,%s,%d,%d\n", username, title, st, en, score, total));
            fw.write("\nquestion,selected,correct,is_correct\n");
            while (rs.next()) {
                String q = rs.getString(1);
                String sel = rs.getString(5);
                String cor = rs.getString(4);
                String ic = rs.getString(3);

                // escape quotes
                q = q == null ? "" : q.replace("\"","\"\"");
                sel = sel == null ? "" : sel.replace("\"","\"\"");
                cor = cor == null ? "" : cor.replace("\"","\"\"");

                fw.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n", q, sel, cor, ic));
            }
            fw.close();
            rs.close(); ps2.close();

            JOptionPane.showMessageDialog(this, "Exported to " + out.toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export error: " + ex.getMessage());
        }
    }
}

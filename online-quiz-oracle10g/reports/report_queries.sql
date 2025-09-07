-- Reports for Online Quiz System (Oracle)

-- 1) Latest attempts per user
SELECT a.attempt_id, u.username, q.title, a.started_at, a.ended_at, a.score, a.total_questions
FROM quiz_attempts a
JOIN users u ON u.user_id = a.user_id
JOIN quizzes q ON q.quiz_id = a.quiz_id
ORDER BY a.started_at DESC;

-- 2) Top scores by quiz
SELECT q.title, u.username, a.score, a.total_questions, a.started_at
FROM quiz_attempts a
JOIN quizzes q ON q.quiz_id = a.quiz_id
JOIN users u ON u.user_id = a.user_id
WHERE a.score IS NOT NULL
ORDER BY q.title, a.score DESC, a.started_at DESC;

-- 3) Per-question correctness rate for a quiz
SELECT qz.title,
       qs.question_id,
       SUBSTR(qs.question_text,1,80) AS question_snippet,
       SUM(CASE WHEN aa.is_correct='Y' THEN 1 ELSE 0 END) AS correct_count,
       COUNT(*) AS total_answers,
       ROUND( (SUM(CASE WHEN aa.is_correct='Y' THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0)) * 100, 2 ) AS correct_pct
FROM attempt_answers aa
JOIN quiz_attempts a ON a.attempt_id = aa.attempt_id
JOIN questions qs ON qs.question_id = aa.question_id
JOIN quizzes qz ON qz.quiz_id = a.quiz_id
GROUP BY qz.title, qs.question_id, qs.question_text
ORDER BY qz.title, qs.question_id;

-- 4) User history
SELECT u.username, q.title, a.started_at, a.ended_at, a.score, a.total_questions, a.duration_sec
FROM quiz_attempts a
JOIN users u ON u.user_id = a.user_id
JOIN quizzes q ON q.quiz_id = a.quiz_id
WHERE u.username = :USERNAME
ORDER BY a.started_at DESC;

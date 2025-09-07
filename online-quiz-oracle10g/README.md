# Online Quiz System (Java Swing + Oracle 10g XE)

A minimal, self-contained **timed quiz** application with login, per-user randomized questions,
score tracking, persistence, and result display with correct answers.

## Tech
- Java 8+
- **Swing** (desktop UI)
- **Oracle 10g XE** (tested via SQL compatible syntax)
- JDBC (ojdbc14.jar for 10g)
- No build tool required (compile via `javac`), but instructions for Maven users are included.

---

## 1) Database Setup (Oracle 10g XE)
1. Open **SQL Developer** (or `sqlplus`) and run in order:
   - `db/oracle10g_schema.sql`
   - `db/sample_data.sql`

2. Create an application DB user (optional if you want to separate from SYSTEM):
   ```sql
   -- Run as SYSTEM or a privileged user
   CREATE USER QUIZUSER IDENTIFIED BY quizpass;
   GRANT CONNECT, RESOURCE TO QUIZUSER;
   -- You may also need: GRANT CREATE VIEW, CREATE TRIGGER, CREATE SEQUENCE TO QUIZUSER;
   -- Then run the schema scripts after changing the `ALTER SESSION SET CURRENT_SCHEMA=...` or by prefixing with QUIZUSER.
   ```

3. Verify tables: `USERS`, `QUIZZES`, `QUESTIONS`, `OPTIONS`, `QUIZ_ATTEMPTS`, `ATTEMPT_ANSWERS`.

---

## 2) Java Setup

### Oracle JDBC Driver
For Oracle 10g XE, use **ojdbc14.jar** or **ojdbc6.jar**. Place the JAR in `lib/ojdbc.jar` (create `lib/` folder).  
You can download from Oracle's site (login may be required).

### Compile & Run (CLI)
```bash
# From project root
mkdir -p lib
# put your Oracle driver at lib/ojdbc.jar

# Compile
javac -cp lib/ojdbc.jar -d out src/app/*.java

# Run
java -cp "out:lib/ojdbc.jar" app.Main
# On Windows use ; instead of :
# java -cp "out;lib/ojdbc.jar" app.Main
```

### Configure Database Connection
Edit `src/app/Config.java` if needed:
- URL: `jdbc:oracle:thin:@localhost:1521:XE`
- USER: `QUIZUSER`
- PASSWORD: `quizpass`

> If you're using the default SYSTEM account (not recommended for production), update credentials accordingly.

---

## 3) App Flow
1. **Login**: username + password (passwords are hashed with SHA-256 for demo).
2. **Select Quiz**: choose a quiz and number of questions (<= available) and duration (from DB).
3. **Timed Quiz**: questions randomized per attempt (`ORDER BY DBMS_RANDOM.VALUE`), countdown visible.
4. **Submit / Auto-submit on Timeout**: score computed; attempt & answers stored.
5. **Results**: shows per-question selection, correct answer, correctness; also saved in DB.
6. **Reports**: sample SQL queries in `reports/report_queries.sql` and simple CSV export in the app.

---

## 4) Notes
- Oracle 10g has no `IDENTITY`. We use **SEQUENCE + TRIGGER** per table.
- Oracle has no `BOOLEAN`. We use `CHAR(1)` with 'Y'/'N'.
- For randomization + limiting rows: `SELECT * FROM ( ... ORDER BY DBMS_RANDOM.VALUE ) WHERE ROWNUM <= :n`.
- Timestamps: `SYSTIMESTAMP` and `TIMESTAMP` columns used.
- **Password hashing**: SHA-256; **do not** use plain text in production.

---

## 5) Quick Demo Credentials
- user: `alice`, password: `alice123`
- user: `bob`, password: `bob123`

(Inserted by `db/sample_data.sql`)

---

## 6) Troubleshooting
- `ORA-28000` / login errors: unlock user or reset password.
- `io exception: connection refused`: confirm listener is running and SID is `XE` on port 1521.
- `ClassNotFoundException: oracle.jdbc.driver.OracleDriver`: ensure `lib/ojdbc.jar` is on the classpath.
- Fonts/UI cutoff: increase window size or system scaling.

---

## 7) License
MIT (for the sample code). Use at your own risk.

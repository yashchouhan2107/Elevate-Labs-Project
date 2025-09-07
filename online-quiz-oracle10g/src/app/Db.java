package app;

import java.sql.*;

public class Db {
    private static Connection conn;

    public static Connection get() throws Exception {
        if (conn == null || conn.isClosed()) {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            conn = DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASS);
        }
        return conn;
    }

    public static void closeQuietly(AutoCloseable c) {
        if (c != null) { try { c.close(); } catch (Exception ignored) {} }
    }
}

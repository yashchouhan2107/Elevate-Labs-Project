package app;

public class Config {
    // Update to your Oracle settings
    public static final String DB_URL  = "jdbc:oracle:thin:@localhost:1521:XE"; // 10g XE SID
    public static final String DB_USER = "ALICE";
    public static final String DB_PASS = "alice123";

    // UI
    public static final int WINDOW_WIDTH = 800;
    public static final int WINDOW_HEIGHT = 600;
}

package server.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQLite database bootstrap for leaderboard and profiles.
 */
public class Database {
    private static final String JDBC_URL = "jdbc:sqlite:leaderboard.db";
    private static volatile boolean initialized = false;

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(JDBC_URL);
        if (!initialized) {
            synchronized (Database.class) {
                if (!initialized) {
                    try (Statement st = conn.createStatement()) {
                        st.execute("PRAGMA journal_mode=WAL");
                        st.execute("CREATE TABLE IF NOT EXISTS users (\n" +
                                "  id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                                "  username TEXT UNIQUE NOT NULL,\n" +
                                "  device_id TEXT,\n" +
                                "  created_at INTEGER NOT NULL\n" +
                                ")");
                        st.execute("CREATE TABLE IF NOT EXISTS levels (\n" +
                                "  id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                                "  code TEXT UNIQUE NOT NULL,\n" +
                                "  name TEXT,\n" +
                                "  version INTEGER NOT NULL DEFAULT 1\n" +
                                ")");
                        st.execute("CREATE TABLE IF NOT EXISTS runs (\n" +
                                "  id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                                "  user_id INTEGER NOT NULL,\n" +
                                "  level_id INTEGER,\n" +
                                "  duration_ms INTEGER NOT NULL,\n" +
                                "  xp_gained INTEGER NOT NULL,\n" +
                                "  validated INTEGER NOT NULL DEFAULT 0,\n" +
                                "  created_at INTEGER NOT NULL,\n" +
                                "  FOREIGN KEY(user_id) REFERENCES users(id),\n" +
                                "  FOREIGN KEY(level_id) REFERENCES levels(id)\n" +
                                ")");
                        st.execute("CREATE INDEX IF NOT EXISTS idx_runs_level_time ON runs(level_id, duration_ms, validated)");
                        st.execute("CREATE INDEX IF NOT EXISTS idx_runs_xp ON runs(xp_gained, validated)");
                    }
                    initialized = true;
                }
            }
        }
        return conn;
    }
}








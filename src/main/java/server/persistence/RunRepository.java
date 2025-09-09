package server.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RunRepository {
    public static void upsertUser(String username, String deviceId) throws SQLException {
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO users(username, device_id, created_at) VALUES(?,?,?) ON CONFLICT(username) DO UPDATE SET device_id=excluded.device_id")) {
                ps.setString(1, username);
                ps.setString(2, deviceId);
                ps.setLong(3, System.currentTimeMillis());
                ps.executeUpdate();
            }
        }
    }

    public static void ensureLevel(String code, String name, int version) throws SQLException {
        try (Connection c = Database.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO levels(code, name, version) VALUES(?,?,?) ON CONFLICT(code) DO UPDATE SET name=excluded.name, version=excluded.version")) {
                ps.setString(1, code);
                ps.setString(2, name);
                ps.setInt(3, version);
                ps.executeUpdate();
            }
        }
    }

    public static void insertRun(String username, String levelCode, long durationMs, int xpGained, boolean validated) throws SQLException {
        try (Connection c = Database.getConnection()) {
            long now = System.currentTimeMillis();
            String userSel = "SELECT id FROM users WHERE username=?";
            String lvlSel = "SELECT id FROM levels WHERE code=?";
            Integer userId = null; Integer levelId = null;
            try (PreparedStatement ps = c.prepareStatement(userSel)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) userId = rs.getInt(1); }
            }
            try (PreparedStatement ps = c.prepareStatement(lvlSel)) {
                ps.setString(1, levelCode);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) levelId = rs.getInt(1); }
            }
            if (userId == null) throw new SQLException("user not found");
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO runs(user_id, level_id, duration_ms, xp_gained, validated, created_at) VALUES(?,?,?,?,?,?)")) {
                ps.setInt(1, userId);
                if (levelId == null) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, levelId);
                ps.setLong(3, durationMs);
                ps.setInt(4, xpGained);
                ps.setInt(5, validated ? 1 : 0);
                ps.setLong(6, now);
                ps.executeUpdate();
            }
        }
    }

    public static List<LeaderboardEntry> topTimes(String levelCode, int limit) throws SQLException {
        String sql = "SELECT u.username, r.duration_ms, r.xp_gained, r.created_at " +
                "FROM runs r JOIN users u ON r.user_id=u.id JOIN levels l ON r.level_id=l.id " +
                "WHERE l.code=? AND r.validated=1 ORDER BY r.duration_ms ASC, r.created_at ASC LIMIT ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, levelCode);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new LeaderboardEntry(rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getLong(4)));
                }
                return list;
            }
        }
    }

    public static List<LeaderboardEntry> topXpAllTime(int limit) throws SQLException {
        String sql = "SELECT u.username, r.duration_ms, r.xp_gained, r.created_at " +
                "FROM runs r JOIN users u ON r.user_id=u.id " +
                "WHERE r.validated=1 ORDER BY r.xp_gained DESC, r.created_at ASC LIMIT ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new LeaderboardEntry(rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getLong(4)));
                }
                return list;
            }
        }
    }

    public static List<LeaderboardEntry> topCampaignTimeAllTime(int limit) throws SQLException {
        String sql = "WITH best AS (\n" +
                "  SELECT user_id, level_id, MIN(duration_ms) AS duration_ms\n" +
                "  FROM runs\n" +
                "  WHERE validated=1 AND level_id IS NOT NULL\n" +
                "  GROUP BY user_id, level_id\n" +
                ")\n" +
                "SELECT u.username, SUM(best.duration_ms) AS total_time, 0 as xp, MAX(r.created_at) as when\n" +
                "FROM best\n" +
                "JOIN users u ON u.id = best.user_id\n" +
                "LEFT JOIN runs r ON r.user_id = best.user_id\n" +
                "GROUP BY u.username\n" +
                "ORDER BY total_time ASC\n" +
                "LIMIT ?";
        try (Connection c = Database.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardEntry> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new LeaderboardEntry(rs.getString(1), rs.getLong(2), rs.getInt(3), rs.getLong(4)));
                }
                return list;
            }
        }
    }

    public static class LeaderboardEntry {
        public final String username;
        public final long durationMs;
        public final int xp;
        public final long when;
        public LeaderboardEntry(String username, long durationMs, int xp, long when) {
            this.username = username;
            this.durationMs = durationMs;
            this.xp = xp;
            this.when = when;
        }
    }
}



package protocol.messages;

import java.util.List;

public class Leaderboard {
    public static class Request {
        public String type = "LeaderboardRequest";
        public String levelCode;
        public String mode; // "time" | "xp" | "campaign"
        public int limit = 10;
    }

    public static class Entry {
        public int rank;
        public String username;
        public long durationMs;
        public int xp;
        public long when;
    }

    public static class Response {
        public String type = "LeaderboardResponse";
        public String mode;
        public String levelCode;
        public List<Entry> entries;
    }
}



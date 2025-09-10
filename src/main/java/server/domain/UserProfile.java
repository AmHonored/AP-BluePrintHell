package server.domain;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    public int schemaVersion = 1;
    public String deviceId;
    public String username;
    public int xp;
    public List<String> unlockedAbilities = new ArrayList<>();
    public String activeAbility;
    public String squadId;
    public List<Score> scoreHistory = new ArrayList<>();
    public long createdAt;
    public long updatedAt;

    public static class Score {
        public String levelCode;
        public long durationMs;
        public int xpGained;
        public long when;
    }
}





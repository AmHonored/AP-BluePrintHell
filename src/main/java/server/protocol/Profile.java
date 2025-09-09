package server.protocol;

public class Profile {
    public static class Snapshot {
        public String type = "ProfileSnapshot";
        public String username;
        public String deviceId;
        public int xp;
        public java.util.List<String> unlockedAbilities;
        public String activeAbility;
        public String squadId;
    }
}




package protocol.messages;

import java.util.List;

public class Profile {
    public static class Snapshot {
        public String type;
        public String username;
        public String deviceId;
        public int xp;
        public List<String> unlockedAbilities;
        public String activeAbility;
        public String squadId;
    }
}



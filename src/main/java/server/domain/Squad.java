package server.domain;

import java.util.ArrayList;
import java.util.List;

public class Squad {
    public int schemaVersion = 1;
    public String id;
    public String name;
    public List<String> memberDeviceIds = new ArrayList<>();
    public int xp;
    public List<Battle> battles = new ArrayList<>();
    public long createdAt;
    public long updatedAt;

    public static class Battle {
        public String opponentSquadId;
        public String result; // WIN/LOSE/DRAW
        public long when;
    }
}




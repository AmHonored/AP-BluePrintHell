package server.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Server-side Leaderboard messages.
 */
public class Leaderboard {
    
    public static class Request {
        @JsonProperty("type")
        public String type = "LeaderboardRequest";
        
        @JsonProperty("levelCode")
        public String levelCode;
        
        @JsonProperty("mode")
        public String mode;
        
        @JsonProperty("limit")
        public int limit;
    }
    
    public static class Response {
        @JsonProperty("type")
        public String type = "LeaderboardResponse";
        
        @JsonProperty("levelCode")
        public String levelCode;
        
        @JsonProperty("mode")
        public String mode;
        
        @JsonProperty("entries")
        public List<Entry> entries;
    }
    
    public static class Entry {
        @JsonProperty("username")
        public String username;
        
        @JsonProperty("value")
        public long value;
        
        @JsonProperty("timestamp")
        public long timestamp;
    }
}
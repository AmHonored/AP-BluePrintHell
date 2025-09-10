package server.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server-side Profile messages.
 */
public class Profile {
    
    public static class Request {
        @JsonProperty("type")
        public String type = "ProfileRequest";
    }
    
    public static class Snapshot {
        @JsonProperty("type")
        public String type = "ProfileSnapshot";
        
        @JsonProperty("username")
        public String username;
        
        @JsonProperty("totalXp")
        public int totalXp;
        
        @JsonProperty("totalCoins")
        public int totalCoins;
        
        @JsonProperty("gamesPlayed")
        public int gamesPlayed;
    }
}
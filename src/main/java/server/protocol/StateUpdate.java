package server.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server-side StateUpdate message.
 */
public class StateUpdate {
    @JsonProperty("type")
    public String type = "StateUpdate";
    
    @JsonProperty("snapshot")
    public GameSnapshot snapshot;
    
    public static class GameSnapshot {
        @JsonProperty("coins")
        public int coins;
        
        @JsonProperty("packetsCollected")
        public int packetsCollected;
        
        @JsonProperty("packetLoss")
        public int packetLoss;
        
        @JsonProperty("packetsGenerated")
        public int packetsGenerated;
    }
}


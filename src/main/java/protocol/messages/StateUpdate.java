package protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * State update message for online multiplayer synchronization.
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


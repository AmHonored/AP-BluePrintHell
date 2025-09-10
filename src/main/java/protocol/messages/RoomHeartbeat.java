package protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client heartbeat to indicate they're still active in a room.
 */
public class RoomHeartbeat {
    @JsonProperty("type")
    public String type = "RoomHeartbeat";
    
    @JsonProperty("roomCode")
    public String roomCode;
    
    public RoomHeartbeat() {}
    
    public RoomHeartbeat(String roomCode) {
        this.roomCode = roomCode;
    }
}

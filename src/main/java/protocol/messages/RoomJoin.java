package protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client request to join an existing room.
 */
public class RoomJoin {
    @JsonProperty("type")
    public String type = "RoomJoin";
    
    @JsonProperty("roomCode")
    public String roomCode;
    
    public RoomJoin() {}
    
    public RoomJoin(String roomCode) {
        this.roomCode = roomCode;
    }
}

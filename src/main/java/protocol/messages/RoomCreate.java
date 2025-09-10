package protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client request to create a new room.
 */
public class RoomCreate {
    @JsonProperty("type")
    public String type = "RoomCreate";
    
    @JsonProperty("roomCode")
    public String roomCode; // Optional, server generates if null/empty
    
    public RoomCreate() {}
    
    public RoomCreate(String roomCode) {
        this.roomCode = roomCode;
    }
}

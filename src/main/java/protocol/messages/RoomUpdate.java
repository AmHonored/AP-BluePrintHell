package protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Server update about room state (sent to all players in room).
 */
public class RoomUpdate {
    @JsonProperty("type")
    public String type = "RoomUpdate";
    
    @JsonProperty("roomCode")
    public String roomCode;
    
    @JsonProperty("players")
    public List<String> players;
    
    @JsonProperty("logs")
    public List<String> logs;
    
    @JsonProperty("success")
    public boolean success = true;
    
    @JsonProperty("error")
    public String error; // Only set if success = false
    
    public RoomUpdate() {}
    
    public RoomUpdate(String roomCode, List<String> players, List<String> logs) {
        this.roomCode = roomCode;
        this.players = players;
        this.logs = logs;
    }
    
    public static RoomUpdate error(String error) {
        RoomUpdate update = new RoomUpdate();
        update.success = false;
        update.error = error;
        return update;
    }
}

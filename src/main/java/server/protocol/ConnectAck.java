package server.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server acknowledgment of connection request.
 */
public class ConnectAck {
    @JsonProperty("type")
    public String type = "ConnectAck";
    
    @JsonProperty("sessionId")
    public String sessionId;
    
    @JsonProperty("message")
    public String message;
    
    public ConnectAck() {}
    
    public ConnectAck(String sessionId, String message) {
        this.sessionId = sessionId;
        this.message = message;
    }
}
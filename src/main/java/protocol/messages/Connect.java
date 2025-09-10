package protocol.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Connection request message.
 */
public class Connect {
    @JsonProperty("type")
    public String type = "Connect";
    
    @JsonProperty("username")
    public String username;
    
    @JsonProperty("deviceId")
    public String deviceId;
    
    @JsonProperty("clientVersion")
    public String clientVersion;
    
    public Connect() {}
    
    public Connect(String username, String deviceId, String clientVersion) {
        this.username = username;
        this.deviceId = deviceId;
        this.clientVersion = clientVersion;
    }
}


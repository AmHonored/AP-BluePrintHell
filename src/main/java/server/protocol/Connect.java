package server.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Server-side Connect message.
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
}
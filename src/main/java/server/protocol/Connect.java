package server.protocol;

/**
 * Server-side mirror of client Connect message.
 * Keeps server independent from client code.
 */
public class Connect {
    public String type;
    public int protocolVersion;
    public String username;
    public String deviceId;
    public String clientVersion;

    public Connect() {}
}






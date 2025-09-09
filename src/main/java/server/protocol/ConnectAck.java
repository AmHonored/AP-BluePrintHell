package server.protocol;

/**
 * Server-side ConnectAck response message.
 * Independent from client code.
 */
public class ConnectAck {
    public String type = "ConnectAck";
    public int protocolVersion = 1;
    public boolean accepted;
    public String reason;
    public long serverTime;

    public ConnectAck() {}
}






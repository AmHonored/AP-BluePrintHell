package protocol.messages;

/**
 * Server -> Client: connection acknowledgement.
 */
public class ConnectAck {
    public String type = "ConnectAck";
    public int protocolVersion = 1;
    public boolean accepted;
    public String reason; // optional
    public long serverTime;

    public ConnectAck() {}
}








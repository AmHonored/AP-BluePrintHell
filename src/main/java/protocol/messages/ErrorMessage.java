package protocol.messages;

/**
 * Server -> Client: generic error report.
 */
public class ErrorMessage {
    public String type = "Error";
    public int protocolVersion = 1;
    public String code;
    public String message;
}








package protocol.messages;

import java.util.List;

/**
 * Client -> Server: frame input for a given tick.
 */
public class ClientInput {
    public String type = "Input";
    public int protocolVersion = 1;
    public long tick;
    public List<InputAction> actions;

    public static class InputAction {
        public String kind; // e.g., "WIRE_CONNECT", "WIRE_DISCONNECT", "BUTTON_PRESS"
        public String data; // JSON string or simple payload (keep minimal for now)
    }
}








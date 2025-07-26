package model.wire;

public class Connection {
    private final String wireId;
    private final String sourcePortId;
    private final String destPortId;

    public Connection(String wireId, String sourcePortId, String destPortId) {
        this.wireId = wireId;
        this.sourcePortId = sourcePortId;
        this.destPortId = destPortId;
    }

    public String getWireId() {
        return wireId;
    }
    public String getSourcePortId() {
        return sourcePortId;
    }
    public String getDestPortId() {
        return destPortId;
    }
}

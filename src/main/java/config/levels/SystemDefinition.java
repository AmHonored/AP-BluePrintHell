package config.levels;

import java.util.ArrayList;
import java.util.List;

public class SystemDefinition {
    public enum SystemType {
        START, INTERMEDIATE, END, DDOS, SPY, VPN, DISTRIBUTOR, MERGE, ANTIVIRUS
    }

    private String id;
    private SystemType type;
    private Point position;
    private List<PortDefinition> ports = new ArrayList<>();

    public SystemDefinition() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public SystemType getType() { return type; }
    public void setType(SystemType type) { this.type = type; }

    public Point getPosition() { return position; }
    public void setPosition(Point position) { this.position = position; }

    public List<PortDefinition> getPorts() { return ports; }
    public void setPorts(List<PortDefinition> ports) { this.ports = ports; }
}


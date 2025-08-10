package config.levels;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a system node defined in a level configuration file.
 */
public class SystemDefinition {
    public enum SystemType {
        START, INTERMEDIATE, END, DDOS, SPY, VPN, DISTRIBUTOR, MERGE, ANTIVIRUS
    }

    /** Unique identifier of the system within the level. */
    private String id;
    /** The system category/type to instantiate. */
    private SystemType type;
    /** Absolute position of the system center in the game pane coordinate space. */
    private Point position;
    /** Ports defined for this system. */
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


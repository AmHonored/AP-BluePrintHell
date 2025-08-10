package model.entity.systems;

import javafx.geometry.Point2D;
import java.util.List;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.entity.ports.PortType;
import java.util.ArrayList;

public abstract class System {
    protected String id; // stable id from level config
    public static final double WIDTH = 80;
    public static final double HEIGHT = 100;

    protected Point2D position;
    protected boolean ready;
    protected SystemType type;
    protected final ArrayList<Port> inPorts = new ArrayList<>();
    protected final ArrayList<Port> outPorts = new ArrayList<>();
    // Ports and wires will be added in future steps

    public System(Point2D position, SystemType type) {
        this.position = position;
        this.type = type;
        this.ready = false;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public SystemType getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addPort(Port port) {
        if (port.getType() == PortType.INPUT) {
            inPorts.add(port);
        } else {
            outPorts.add(port);
        }
    }

    public ArrayList<Port> getInPorts() {
        return inPorts;
    }

    public ArrayList<Port> getOutPorts() {
        return outPorts;
    }

    public void updateReady() {
        for (Port p : inPorts) {
            if (!p.isConnected()) {
                ready = false;
                return;
            }
        }
        for (Port p : outPorts) {
            if (!p.isConnected()) {
                ready = false;
                return;
            }
        }
        ready = true;
    }

    /**
     * Whether this system can be dragged when Sisyphus is activated.
     * Default is true; reference systems (start/end) override to false.
     */
    public boolean isDraggableWithSisyphus() {
        return true;
    }

    public Port findBestOutPort(Packet packet) {
        // Prefer an available output port compatible with the packet
        for (Port port : outPorts) {
            if (port.isConnected() && port.isCompatible(packet)) {
                return port;
            }
        }
        // Otherwise, use the first available output port
        for (Port port : outPorts) {
            if (port.isConnected()) {
                return port;
            }
        }
        return null;
    }
}

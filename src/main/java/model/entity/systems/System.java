package model.entity.systems;

import javafx.geometry.Point2D;
import java.util.List;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.entity.ports.PortType;
import java.util.ArrayList;

public abstract class System {
    protected String id;
    public static final double WIDTH = 80;
    public static final double HEIGHT = 100;

    protected Point2D position;
    protected boolean ready;
    protected SystemType type;
    protected final ArrayList<Port> inPorts = new ArrayList<>();
    protected final ArrayList<Port> outPorts = new ArrayList<>();

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

    public boolean isDraggableWithSisyphus() {
        return true;
    }

    public List<Port> getAvailableOutPorts(Packet packet) {
        List<Port> compatibleAndAvailable = new ArrayList<>();
        List<Port> available = new ArrayList<>();
        for (Port port : outPorts) {
            boolean connected = port.isConnected();
            boolean wireAvailable = port.getWire() != null && port.getWire().isActive();
            if (!connected || !wireAvailable) {
                continue;
            }
            if (packet != null && port.isCompatible(packet)) {
                compatibleAndAvailable.add(port);
            } else {
                available.add(port);
            }
        }
        if (!compatibleAndAvailable.isEmpty()) {
            return compatibleAndAvailable;
        }
        return available;
    }

    public Port findBestOutPort(Packet packet) {
        java.util.List<Port> available = getAvailableOutPorts(packet);
        return available.isEmpty() ? null : available.get(0);
    }
}

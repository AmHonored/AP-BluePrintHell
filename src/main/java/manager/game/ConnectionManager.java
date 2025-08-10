package manager.game;

import model.wire.Wire;
import model.entity.ports.Port;
import model.levels.Level;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private final List<Wire> wires = new ArrayList<>();
    private Level level;
    private double maxWireLength = 100.0;
    private double usedWireLength = 0.0;

    public ConnectionManager(Level level, double maxWireLength) {
        this.level = level;
        this.maxWireLength = maxWireLength;
    }

    public boolean canAddWire(Wire wire) {
        double wireLen = getWireLength(wire);
        return (usedWireLength + wireLen) <= maxWireLength;
    }

    public boolean addWire(Wire wire) {
        double wireLen = getWireLength(wire);
        if (!canAddWire(wire)) {
            return false;
        }
        
        wires.add(wire);
        usedWireLength += wireLen;
        
        level.subtractWireLength(wireLen);
        return true;
    }

    public void removeWire(Wire wire) {
        if (wires.remove(wire)) {
            double wireLen = getWireLength(wire);
            usedWireLength -= wireLen;
            if (usedWireLength < 0) usedWireLength = 0;
            
            level.addWireLength(wireLen);
            
        }
    }

    public double getRemainingWireLength() {
        return maxWireLength - usedWireLength;
    }

    public List<Wire> getWires() {
        return wires;
    }

    public boolean isNetworkValid() {
        for (model.entity.systems.System system : level.getSystems()) {
            for (Port port : system.getInPorts()) {
                if (!port.isConnected()) return false;
            }
            for (Port port : system.getOutPorts()) {
                if (!port.isConnected()) return false;
            }
        }
        return true;
    }

    public void recalculateWireLengths() {
        double oldUsedLength = usedWireLength;
        
        double newUsedLength = 0.0;
        for (Wire wire : wires) {
            newUsedLength += getWireLength(wire);
        }
        
        double difference = newUsedLength - oldUsedLength;
        usedWireLength = newUsedLength;
        
        if (difference > 0) {
            level.subtractWireLength(difference);
        } else if (difference < 0) {
            level.addWireLength(-difference);
        }
        
    }

    private double getWireLength(Wire wire) {
        return wire.getLength();
    }
} 
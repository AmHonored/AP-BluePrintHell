package manager.game;

import model.wire.Wire;
import model.entity.ports.Port;
import model.levels.Level;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private final List<Wire> wires = new ArrayList<>();
    private Level level;
    private double maxWireLength = 100.0; // Placeholder, can be set from level
    private double usedWireLength = 0.0;

    public ConnectionManager(Level level, double maxWireLength) {
        this.level = level;
        this.maxWireLength = maxWireLength;
        System.out.println("DEBUG: ConnectionManager constructor - maxWireLength: " + maxWireLength);
        System.out.println("DEBUG: ConnectionManager constructor - level.getWireLength(): " + level.getWireLength());
    }

    public boolean canAddWire(Wire wire) {
        // For now, use a fixed length per wire (e.g., 10 units)
        double wireLen = getWireLength(wire);
        System.out.println("DEBUG: ConnectionManager.canAddWire() - Wire length: " + wireLen);
        System.out.println("DEBUG: ConnectionManager.canAddWire() - Used wire: " + usedWireLength + "/" + maxWireLength);
        System.out.println("DEBUG: ConnectionManager.canAddWire() - Would need: " + (usedWireLength + wireLen));
        return (usedWireLength + wireLen) <= maxWireLength;
    }

    public boolean addWire(Wire wire) {
        double wireLen = getWireLength(wire);
        System.out.println("DEBUG: ConnectionManager.addWire() - Wire length: " + wireLen);
        System.out.println("DEBUG: ConnectionManager.addWire() - Used wire: " + usedWireLength + "/" + maxWireLength);
        
        if (!canAddWire(wire)) {
            System.out.println("DEBUG: ConnectionManager.addWire() - REJECTED: Not enough wire remaining");
            return false;
        }
        
        wires.add(wire);
        usedWireLength += wireLen;
        
        // Synchronize with Level's wire length
        level.subtractWireLength(wireLen);
        
        System.out.println("DEBUG: ConnectionManager.addWire() - SUCCESS: Used wire now: " + usedWireLength + "/" + maxWireLength);
        System.out.println("DEBUG: ConnectionManager.addWire() - Level remaining wire: " + level.getRemainingWireLength());
        return true;
    }

    public void removeWire(Wire wire) {
        if (wires.remove(wire)) {
            double wireLen = getWireLength(wire);
            usedWireLength -= wireLen;
            if (usedWireLength < 0) usedWireLength = 0;
            
            // Synchronize with Level's wire length
            level.addWireLength(wireLen);
            
            System.out.println("DEBUG: ConnectionManager.removeWire() - Wire length: " + wireLen);
            System.out.println("DEBUG: ConnectionManager.removeWire() - Used wire now: " + usedWireLength + "/" + maxWireLength);
            System.out.println("DEBUG: ConnectionManager.removeWire() - Level remaining wire: " + level.getRemainingWireLength());
        }
    }

    public double getRemainingWireLength() {
        return maxWireLength - usedWireLength;
    }

    public List<Wire> getWires() {
        return wires;
    }

    public boolean isNetworkValid() {
        // All ports must be filled
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

    private double getWireLength(Wire wire) {
        // Calculate actual wire length based on distance between ports
        return wire.getLength();
    }
} 
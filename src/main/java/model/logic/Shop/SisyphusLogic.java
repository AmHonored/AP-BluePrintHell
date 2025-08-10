package model.logic.Shop;

import javafx.geometry.Point2D;
import model.levels.Level;
import model.entity.systems.System;
import model.entity.systems.SystemType;
import model.entity.ports.Port;
import model.wire.Wire;

import java.util.List;
import java.util.ArrayList;

/**
 * Core logic for Sisyphus system movement functionality.
 */
public final class SisyphusLogic {
    private SisyphusLogic() {}
    
    public static final double MOVEMENT_RADIUS = 150.0; // Maximum movement radius in pixels
    
    /**
     * Check if a system can be moved (non-reference systems only)
     */
    public static boolean canMoveSystem(System system) {
        if (system == null) return false;
        
        SystemType type = system.getType();
        // Reference systems (start and end) cannot be moved
        return type != SystemType.StartSystem && type != SystemType.EndSystem;
    }
    
    /**
     * Check if moving a system to a new position is valid
     */
    public static boolean isValidMove(Level level, System system, Point2D newPosition, Point2D originalPosition) {
        if (system == null || newPosition == null || originalPosition == null) {
            return false;
        }
        
        // Check if the system can be moved at all
        if (!canMoveSystem(system)) {
            return false;
        }
        
        // Check movement radius constraint
        double distance = originalPosition.distance(newPosition);
        if (distance > MOVEMENT_RADIUS) {
            java.lang.System.out.println("DEBUG: SisyphusLogic - Movement exceeds radius: " + distance + " > " + MOVEMENT_RADIUS);
            return false;
        }
        
        // Check if the new position would cause wire length to exceed available length
        if (!isWireLengthValid(level, system, newPosition)) {
            java.lang.System.out.println("DEBUG: SisyphusLogic - Wire length would exceed available");
            return false;
        }
        
        // Check if wires would pass through other systems
        if (wouldWiresIntersectSystems(level, system, newPosition)) {
            java.lang.System.out.println("DEBUG: SisyphusLogic - Wires would intersect with other systems");
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate the total wire length change if a system is moved
     */
    private static boolean isWireLengthValid(Level level, System system, Point2D newPosition) {
        double totalLengthChange = 0.0;
        
        // Calculate length change for all connected wires
        List<Wire> connectedWires = getConnectedWires(level, system);
        
        for (Wire wire : connectedWires) {
            double currentLength = wire.getLength();
            double newLength = calculateNewWireLength(wire, system, newPosition);
            totalLengthChange += (newLength - currentLength);
        }
        
        // Check if the new total length would exceed available wire length
        double newRemainingLength = level.getRemainingWireLength() - totalLengthChange;
        return newRemainingLength >= 0;
    }
    
    /**
     * Check if moving a system would cause its wires to pass through other systems
     */
    private static boolean wouldWiresIntersectSystems(Level level, System movingSystem, Point2D newPosition) {
        List<Wire> connectedWires = getConnectedWires(level, movingSystem);
        List<System> otherSystems = new ArrayList<>();
        
        for (System sys : level.getSystems()) {
            if (sys != movingSystem) {
                otherSystems.add(sys);
            }
        }
        
        for (Wire wire : connectedWires) {
            if (wireWouldIntersectSystems(wire, movingSystem, newPosition, otherSystems)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a specific wire would intersect with any system after movement
     */
    private static boolean wireWouldIntersectSystems(Wire wire, System movingSystem, Point2D newPosition, List<System> otherSystems) {
        Point2D sourcePos, destPos;
        
        // Determine new wire endpoints
        if (wire.getSource().getSystem() == movingSystem) {
            // Source system is moving
            Port sourcePort = wire.getSource();
            Point2D portOffset = sourcePort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = newPosition.add(portOffset);
            destPos = wire.getDest().getPosition();
        } else if (wire.getDest().getSystem() == movingSystem) {
            // Destination system is moving
            Port destPort = wire.getDest();
            Point2D portOffset = destPort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = wire.getSource().getPosition();
            destPos = newPosition.add(portOffset);
        } else {
            // Wire doesn't connect to moving system (shouldn't happen)
            return false;
        }
        
        // Skip endpoint systems when checking intersections. It is valid for
        // the segment to end inside its own endpoints' rectangles.
        System sourceSystem = wire.getSource().getSystem();
        System destSystem = wire.getDest().getSystem();

        // Check intersection with each other (non-endpoint) system
        for (System system : otherSystems) {
            if (system == sourceSystem || system == destSystem) continue;
            if (lineIntersectsRectangle(sourcePos, destPos, system.getPosition(), System.WIDTH, System.HEIGHT)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a line intersects with a rectangle (system bounds)
     */
    private static boolean lineIntersectsRectangle(Point2D lineStart, Point2D lineEnd, Point2D rectCenter, double width, double height) {
        // Rectangle bounds
        double left = rectCenter.getX() - width / 2;
        double right = rectCenter.getX() + width / 2;
        double top = rectCenter.getY() - height / 2;
        double bottom = rectCenter.getY() + height / 2;
        
        // Note: do not treat endpoints inside the rectangle as an automatic
        // intersection here; callers exclude endpoint systems explicitly. We
        // only detect when the segment crosses the rectangle edges.
        
        // Check intersection with rectangle edges
        return lineIntersectsLine(lineStart, lineEnd, new Point2D(left, top), new Point2D(right, top)) ||     // top edge
               lineIntersectsLine(lineStart, lineEnd, new Point2D(right, top), new Point2D(right, bottom)) || // right edge
               lineIntersectsLine(lineStart, lineEnd, new Point2D(right, bottom), new Point2D(left, bottom)) || // bottom edge
               lineIntersectsLine(lineStart, lineEnd, new Point2D(left, bottom), new Point2D(left, top));     // left edge
    }
    
    /**
     * Check if a point is inside a rectangle
     */
    private static boolean pointInRectangle(Point2D point, double left, double right, double top, double bottom) {
        return point.getX() >= left && point.getX() <= right && 
               point.getY() >= top && point.getY() <= bottom;
    }
    
    /**
     * Check if two line segments intersect
     */
    private static boolean lineIntersectsLine(Point2D p1, Point2D p2, Point2D p3, Point2D p4) {
        double x1 = p1.getX(), y1 = p1.getY();
        double x2 = p2.getX(), y2 = p2.getY();
        double x3 = p3.getX(), y3 = p3.getY();
        double x4 = p4.getX(), y4 = p4.getY();
        
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-10) return false; // Lines are parallel
        
        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        
        return t >= 0 && t <= 1 && u >= 0 && u <= 1;
    }
    
    /**
     * Get all wires connected to a system
     */
    private static List<Wire> getConnectedWires(Level level, System system) {
        List<Wire> connectedWires = new ArrayList<>();
        
        // Check all ports of the system
        for (Port port : system.getInPorts()) {
            if (port.isConnected() && port.getWire() != null) {
                connectedWires.add(port.getWire());
            }
        }
        
        for (Port port : system.getOutPorts()) {
            if (port.isConnected() && port.getWire() != null) {
                connectedWires.add(port.getWire());
            }
        }
        
        return connectedWires;
    }
    
    /**
     * Calculate new wire length if a system is moved
     */
    private static double calculateNewWireLength(Wire wire, System movingSystem, Point2D newPosition) {
        Point2D sourcePos, destPos;
        
        if (wire.getSource().getSystem() == movingSystem) {
            // Source system is moving - calculate new source port position
            Port sourcePort = wire.getSource();
            Point2D portOffset = sourcePort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = newPosition.add(portOffset);
            destPos = wire.getDest().getPosition();
        } else if (wire.getDest().getSystem() == movingSystem) {
            // Destination system is moving - calculate new dest port position
            Port destPort = wire.getDest();
            Point2D portOffset = destPort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = wire.getSource().getPosition();
            destPos = newPosition.add(portOffset);
        } else {
            // Wire doesn't connect to moving system
            return wire.getLength();
        }
        
        // Calculate distance between new positions
        return sourcePos.distance(destPos);
    }
    
    /**
     * Actually move the system and update its ports
     */
    public static boolean moveSystem(Level level, System system, Point2D newPosition) {
        Point2D originalPosition = system.getPosition();
        
        if (!isValidMove(level, system, newPosition, originalPosition)) {
            return false;
        }
        
        // Calculate position delta
        Point2D delta = newPosition.subtract(originalPosition);
        
        // Calculate wire length changes before moving
        List<Wire> connectedWires = getConnectedWires(level, system);
        double totalLengthChange = 0.0;
        
        for (Wire wire : connectedWires) {
            double currentLength = wire.getLength();
            double newLength = calculateNewWireLength(wire, system, newPosition);
            totalLengthChange += (newLength - currentLength);
        }
        
        // Move the system
        system.setPosition(newPosition);
        
        // Update all port positions
        for (Port port : system.getInPorts()) {
            Point2D oldPortPos = port.getPosition();
            port.setPosition(oldPortPos.add(delta));
        }
        
        for (Port port : system.getOutPorts()) {
            Point2D oldPortPos = port.getPosition();
            port.setPosition(oldPortPos.add(delta));
        }
        
        // Update wire length tracking
        level.subtractWireLength(totalLengthChange);
        
        java.lang.System.out.println("DEBUG: SisyphusLogic - System moved successfully. Wire length change: " + totalLengthChange);
        java.lang.System.out.println("DEBUG: SisyphusLogic - Remaining wire length: " + level.getRemainingWireLength());
        
        return true;
    }
}

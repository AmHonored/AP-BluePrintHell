package model.logic.Shop;

import javafx.geometry.Point2D;
import model.levels.Level;
import model.entity.systems.System;
import model.entity.systems.SystemType;
import model.entity.ports.Port;
import model.wire.Wire;

import java.util.List;
import java.util.ArrayList;

public final class Sisyphus implements ShopItem {
    public Sisyphus() {}

    public static final double MOVEMENT_RADIUS = 150.0; 

    public static boolean canMoveSystem(System system) {
        if (system == null) return false;
        SystemType type = system.getType();
        return type != SystemType.StartSystem && type != SystemType.EndSystem;
    }

    public static boolean isValidMove(Level level, System system, Point2D newPosition, Point2D originalPosition) {
        if (system == null || newPosition == null || originalPosition == null) {
            return false;
        }

        if (!canMoveSystem(system)) {
            return false;
        }

        double distance = originalPosition.distance(newPosition);
        if (distance > MOVEMENT_RADIUS) {
            return false;
        }

        if (!isWireLengthValid(level, system, newPosition)) {
            return false;
        }

        if (wouldWiresIntersectSystems(level, system, newPosition)) {
            return false;
        }

        return true;
    }

    private static boolean isWireLengthValid(Level level, System system, Point2D newPosition) {
        double totalLengthChange = 0.0;

        List<Wire> connectedWires = getConnectedWires(system);
        for (Wire wire : connectedWires) {
            double currentLength = wire.getLength();
            double newLength = calculateNewWireLength(wire, system, newPosition);
            totalLengthChange += (newLength - currentLength);
        }

        double newRemainingLength = level.getRemainingWireLength() - totalLengthChange;
        return newRemainingLength >= 0;
    }

    private static boolean wouldWiresIntersectSystems(Level level, System movingSystem, Point2D newPosition) {
        List<Wire> connectedWires = getConnectedWires(movingSystem);
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

    private static boolean wireWouldIntersectSystems(Wire wire, System movingSystem, Point2D newPosition, List<System> otherSystems) {
        Point2D sourcePos, destPos;

        if (wire.getSource().getSystem() == movingSystem) {
            Port sourcePort = wire.getSource();
            Point2D portOffset = sourcePort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = newPosition.add(portOffset);
            destPos = wire.getDest().getPosition();
        } else if (wire.getDest().getSystem() == movingSystem) {
            Port destPort = wire.getDest();
            Point2D portOffset = destPort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = wire.getSource().getPosition();
            destPos = newPosition.add(portOffset);
        } else {
            return false;
        }

        System sourceSystem = wire.getSource().getSystem();
        System destSystem = wire.getDest().getSystem();

        for (System system : otherSystems) {
            if (system == sourceSystem || system == destSystem) continue;
            if (lineIntersectsRectangle(sourcePos, destPos, system.getPosition(), System.WIDTH, System.HEIGHT)) {
                return true;
            }
        }
        return false;
    }

    private static boolean lineIntersectsRectangle(Point2D lineStart, Point2D lineEnd, Point2D rectCenter, double width, double height) {
        double left = rectCenter.getX() - width / 2;
        double right = rectCenter.getX() + width / 2;
        double top = rectCenter.getY() - height / 2;
        double bottom = rectCenter.getY() + height / 2;

        return lineIntersectsLine(lineStart, lineEnd, new Point2D(left, top), new Point2D(right, top)) ||     // top edge
               lineIntersectsLine(lineStart, lineEnd, new Point2D(right, top), new Point2D(right, bottom)) || // right edge
               lineIntersectsLine(lineStart, lineEnd, new Point2D(right, bottom), new Point2D(left, bottom)) || // bottom edge
               lineIntersectsLine(lineStart, lineEnd, new Point2D(left, bottom), new Point2D(left, top));     // left edge
    }

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

    private static List<Wire> getConnectedWires(System system) {
        List<Wire> connectedWires = new ArrayList<>();
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

    private static double calculateNewWireLength(Wire wire, System movingSystem, Point2D newPosition) {
        Point2D sourcePos, destPos;
        if (wire.getSource().getSystem() == movingSystem) {
            Port sourcePort = wire.getSource();
            Point2D portOffset = sourcePort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = newPosition.add(portOffset);
            destPos = wire.getDest().getPosition();
        } else if (wire.getDest().getSystem() == movingSystem) {
            Port destPort = wire.getDest();
            Point2D portOffset = destPort.getPosition().subtract(movingSystem.getPosition());
            sourcePos = wire.getSource().getPosition();
            destPos = newPosition.add(portOffset);
        } else {
            return wire.getLength();
        }
        return sourcePos.distance(destPos);
    }

    public static boolean moveSystem(Level level, System system, Point2D newPosition) {
        Point2D originalPosition = system.getPosition();
        if (!isValidMove(level, system, newPosition, originalPosition)) {
            return false;
        }

        Point2D delta = newPosition.subtract(originalPosition);
        List<Wire> connectedWires = getConnectedWires(system);
        double totalLengthChange = 0.0;
        for (Wire wire : connectedWires) {
            double currentLength = wire.getLength();
            double newLength = calculateNewWireLength(wire, system, newPosition);
            totalLengthChange += (newLength - currentLength);
        }

        system.setPosition(newPosition);
        for (Port port : system.getInPorts()) {
            Point2D oldPortPos = port.getPosition();
            port.setPosition(oldPortPos.add(delta));
        }
        for (Port port : system.getOutPorts()) {
            Point2D oldPortPos = port.getPosition();
            port.setPosition(oldPortPos.add(delta));
        }

        level.subtractWireLength(totalLengthChange);
        return true;
    }

    @Override
    public String getName() { return "Scroll of Sisyphus"; }

    @Override
    public int getPrice() { return 15; }

    @Override
    public int getDurationSeconds() { return 0; }

    @Override
    public void apply(Level level) {
        level.addSisyphusScrolls(1);
    }
}



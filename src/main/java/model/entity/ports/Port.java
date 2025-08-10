package model.entity.ports;

import javafx.geometry.Point2D;
import model.entity.systems.System;
import model.wire.Wire;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;
import java.util.Random;

public abstract class Port {
    private static final double CONFIDENTIAL_PACKET_CHANCE = 0.2; 
    private static final double MASSIVE_PACKET_CHANCE = 0.2; 
    private static final Random random = new Random();

    protected final String id;
    protected final System system;
    protected final PortType type;
    protected Point2D position;
    protected Wire wire;

    public enum ShapeKind { SQUARE, TRIANGLE, HEXAGON }
    private ShapeKind shapeKind = ShapeKind.SQUARE;

    public Port(String id, System system, PortType type, Point2D position) {
        this.id = id;
        this.system = system;
        this.type = type;
        this.position = position;
        this.wire = null;
    }

    public String getId() {
        return id;
    }
    public System getSystem() {
        return system;
    }
    public PortType getType() {
        return type;
    }
    public Point2D getPosition() {
        return position;
    }
    public void setPosition(Point2D position) {
        this.position = position;
    }
    public Wire getWire() {
        return wire;
    }
    public void setWire(Wire wire) {
        this.wire = wire;
    }
    public boolean isConnected() {
        return wire != null;
    }

    public ShapeKind getShapeKind() {
        return shapeKind;
    }

    public void setShapeKind(ShapeKind newKind) {
        if (newKind != null) {
            this.shapeKind = newKind;
        }
    }

    public boolean shouldGenerateConfidentialPacket() {
        return random.nextDouble() < CONFIDENTIAL_PACKET_CHANCE;
    }

    public boolean shouldGenerateMassivePacket() {
        return random.nextDouble() < MASSIVE_PACKET_CHANCE;
    }

    public abstract boolean isCompatible(Packet packet);

    protected boolean isCompatibleByShapeKind(Packet packet) {
        if (packet == null) return false;

        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            switch (shapeKind) {
                case SQUARE:
                    return protectedPacket.getInheritedMovement() == ProtectedPacket.InheritedMovement.SQUARE;
                case TRIANGLE:
                    return protectedPacket.getInheritedMovement() == ProtectedPacket.InheritedMovement.TRIANGLE;
                case HEXAGON:
                    return protectedPacket.getInheritedMovement() == ProtectedPacket.InheritedMovement.HEXAGON;
            }
            return false;
        }

        switch (shapeKind) {
            case SQUARE:
                return packet instanceof model.entity.packets.SquarePacket;
            case TRIANGLE:
                return packet instanceof model.entity.packets.TrianglePacket;
            case HEXAGON:
                return packet instanceof model.entity.packets.HexagonPacket;
            default:
                return false;
        }
    }
}

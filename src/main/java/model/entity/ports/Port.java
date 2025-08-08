package model.entity.ports;

import javafx.geometry.Point2D;
import model.entity.systems.System;
import model.wire.Wire;
import model.entity.packets.Packet;
import java.util.Random;

public abstract class Port {
    public static final double SIZE = 14;
    private static final double CONFIDENTIAL_PACKET_CHANCE = 0.2; // 20% chance
    private static final double MASSIVE_PACKET_CHANCE = 0.1; // 10% chance
    private static final Random random = new Random();

    protected final String id;
    protected final System system;
    protected final PortType type;
    protected Point2D position;
    protected Wire wire;

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

    /**
     * Determines if this port should generate a confidential packet (20% chance)
     */
    public boolean shouldGenerateConfidentialPacket() {
        return random.nextDouble() < CONFIDENTIAL_PACKET_CHANCE;
    }

    /**
     * Determines if this port should generate a massive packet (10% chance)
     */
    public static boolean shouldGenerateMassivePacket() {
        return random.nextDouble() < MASSIVE_PACKET_CHANCE;
    }

    public abstract boolean isCompatible(Packet packet);
}

package model.entity.systems;

import javafx.geometry.Point2D;
import java.util.LinkedList;
import java.util.Queue;
import model.entity.packets.Packet;

public class VPNSystem extends System {
    private static final int MAX_STORAGE = 5;
    private static final double HIGH_SPEED_THRESHOLD = 75.0;
    private final Queue<Packet> storage = new LinkedList<>();
    private boolean disabled = false;

    public VPNSystem(Point2D position) {
        super(position, SystemType.VPNSystem);
    }

    public int getStorageSize() {
        return storage.size();
    }

    public void enqueuePacket(Packet packet) {
        storage.add(packet);
    }

    public Packet peekNextPacket() {
        return storage.peek();
    }

    public Packet dequeuePacket() {
        return storage.poll();
    }

    public Queue<Packet> getPackets() {
        return storage;
    }

    public boolean isFull() {
        return storage.size() >= MAX_STORAGE;
    }

    public void removeOldestPacket() {
        if (!storage.isEmpty()) {
            storage.poll();
        }
    }

    /**
     * Check if this VPN system is disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Disable this VPN system
     */
    public void disable() {
        this.disabled = true;
    }

    /**
     * Enable this VPN system (for testing or reset purposes)
     */
    public void enable() {
        this.disabled = false;
    }

    /**
     * Check if a packet should disable this VPN system
     * VPN system gets disabled when a packet with speed > 75 enters
     */
    public boolean shouldDisableFromPacket(Packet packet) {
        return packet.getSpeed() > HIGH_SPEED_THRESHOLD;
    }
} 
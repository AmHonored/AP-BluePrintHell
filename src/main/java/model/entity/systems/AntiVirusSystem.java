package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import java.util.LinkedList;
import java.util.Queue;

public class AntiVirusSystem extends System {
    private static final int MAX_STORAGE = 5;
    private static final double DETECTION_RADIUS = 200.0; 
    private static final long DISABLE_DURATION_MS = 5000;
    
    private final Queue<Packet> storage = new LinkedList<>();
    private boolean disabled = false;
    private long disableEndTime = 0;

    public AntiVirusSystem(Point2D position) {
        super(position, SystemType.AntiVirusSystem);
    }

    public double getDetectionRadius() {
        return DETECTION_RADIUS;
    }

    public boolean isDisabled() {
        if (disabled && java.lang.System.currentTimeMillis() >= disableEndTime) {
            disabled = false;
        }
        return disabled;
    }

    public void disable() {
        this.disabled = true;
        this.disableEndTime = java.lang.System.currentTimeMillis() + DISABLE_DURATION_MS;
    }

    public boolean isPacketInRange(Packet packet) {
        Point2D packetPos = packet.getPosition();
        Point2D systemPos = this.getPosition();

        double dx = packetPos.getX() - systemPos.getX();
        double dy = packetPos.getY() - systemPos.getY();
        double distanceSquared = dx * dx + dy * dy;

        return distanceSquared <= (DETECTION_RADIUS * DETECTION_RADIUS);
    }

    public boolean processActiveTrojanPacket(Packet packet) {
        if (isDisabled() || !packet.isTrojan() || !isPacketInRange(packet)) {
            return false;
        }

        packet.setTrojan(false);
        disable();
        
        return true;
    }

    public void processPacket(Packet packet) {
        if (packet.isTrojan() && !isDisabled()) {
            packet.setTrojan(false);
            disable();
        }
        
        if (!isFull()) {
            enqueuePacket(packet);
        } else {
            removeOldestPacket();
            enqueuePacket(packet);
        }
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

    public long getRemainingDisableTime() {
        if (!disabled) {
            return 0;
        }
        return Math.max(0, disableEndTime - java.lang.System.currentTimeMillis());
    }
} 
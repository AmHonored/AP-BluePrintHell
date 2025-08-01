package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import java.util.LinkedList;
import java.util.Queue;

public class AntiVirusSystem extends System {
    private static final int MAX_STORAGE = 5;
    private static final double DETECTION_RADIUS = 200.0; // Increased radius for better detection coverage
    private static final long DISABLE_DURATION_MS = 5000; // 5 seconds
    
    private final Queue<Packet> storage = new LinkedList<>();
    private boolean disabled = false;
    private long disableEndTime = 0;

    public AntiVirusSystem(Point2D position) {
        super(position, SystemType.AntiVirusSystem);
    }

    /**
     * Get the detection radius for trojan packets
     */
    public double getDetectionRadius() {
        return DETECTION_RADIUS;
    }

    /**
     * Check if this system is currently disabled
     */
    public boolean isDisabled() {
        // Check if disable period has expired
        if (disabled && java.lang.System.currentTimeMillis() >= disableEndTime) {
            disabled = false;
        }
        return disabled;
    }

    /**
     * Disable the system for the specified duration
     */
    public void disable() {
        this.disabled = true;
        this.disableEndTime = java.lang.System.currentTimeMillis() + DISABLE_DURATION_MS;
        java.lang.System.out.println("🛡️ ANTIVIRUS SYSTEM: Disabled for " + (DISABLE_DURATION_MS / 1000) + " seconds");
    }

    /**
     * Check if a packet is within detection radius of this system
     */
    public boolean isPacketInRange(Packet packet) {
        Point2D packetPos = packet.getPosition();
        Point2D systemPos = this.getPosition();
        
        double distance = Math.sqrt(
            Math.pow(packetPos.getX() - systemPos.getX(), 2) + 
            Math.pow(packetPos.getY() - systemPos.getY(), 2)
        );
        
        return distance <= DETECTION_RADIUS;
    }

    /**
     * Process a trojan packet by removing its trojan status
     */
    public boolean processActiveTrojanPacket(Packet packet) {
        if (isDisabled() || !packet.isTrojan() || !isPacketInRange(packet)) {
            return false;
        }

        // Remove trojan status
        packet.setTrojan(false);
        
        // Disable system for 5 seconds
        disable();
        
        java.lang.System.out.println("🛡️ ANTIVIRUS SYSTEM: Packet " + packet.getId() + " cleaned of trojan! System disabled for 5 seconds");
        
        return true;
    }

    /**
     * Process a packet entering the AntiVirus system through ports
     */
    public void processPacket(Packet packet) {
        // If the packet is a trojan, clean it
        if (packet.isTrojan() && !isDisabled()) {
            packet.setTrojan(false);
            disable();
            java.lang.System.out.println("🛡️ ANTIVIRUS SYSTEM: Packet " + packet.getId() + " cleaned of trojan while entering system!");
        }
        
        // Add to storage if there's space
        if (!isFull()) {
            enqueuePacket(packet);
        } else {
            // Remove oldest packet if storage is full
            removeOldestPacket();
            enqueuePacket(packet);
        }
    }

    // Storage management methods (following the pattern of other systems)
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
     * Get remaining disable time in milliseconds
     */
    public long getRemainingDisableTime() {
        if (!disabled) {
            return 0;
        }
        return Math.max(0, disableEndTime - java.lang.System.currentTimeMillis());
    }
} 
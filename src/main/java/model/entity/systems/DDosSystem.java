package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


public class DDosSystem extends System {
    private static final int MAX_STORAGE = 5;
    private static final double TROJAN_CONVERSION_PROBABILITY = 0.2; // 20% chance
    private final Queue<Packet> storage = new LinkedList<>();
    private final Random random = new Random();

    public DDosSystem(Point2D position) {
        super(position, SystemType.DDosSystem);
    }

    /**
     * Process a packet entering the DDoS system
     * @param packet The packet to process
     */
    public void processPacket(Packet packet) {
        // Apply DDoS effects
        // Apply noise (reduce health by 1)
        packet.takeDamage(1);
        
        // If packet is destroyed by noise, don't process further
        if (!packet.isAlive()) {
            return;
        }
        
        // 20% chance to convert to trojan
        if (random.nextDouble() < TROJAN_CONVERSION_PROBABILITY) {
            packet.convertToTrojan();
            java.lang.System.out.println("⚡ DDOS SYSTEM: Packet " + packet.getId() + " converted to TROJAN!");
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

    /**
     * Find the best output port for DDoS system behavior:
     * 1. Prefer incompatible ports that are available
     * 2. Fall back to compatible ports if no incompatible ports are available
     */
    @Override
    public Port findBestOutPort(Packet packet) {
        ArrayList<Port> incompatiblePorts = new ArrayList<>();
        ArrayList<Port> compatiblePorts = new ArrayList<>();
        
        // Categorize ports by compatibility
        for (Port port : outPorts) {
            if (port.isConnected()) {
                if (port.isCompatible(packet)) {
                    compatiblePorts.add(port);
                } else {
                    incompatiblePorts.add(port);
                }
            }
        }
        
        // Prefer incompatible ports (DDoS behavior)
        if (!incompatiblePorts.isEmpty()) {
            // Randomly select from available incompatible ports
            int randomIndex = random.nextInt(incompatiblePorts.size());
            return incompatiblePorts.get(randomIndex);
        }
        
        // Fall back to compatible ports if no incompatible ports available
        if (!compatiblePorts.isEmpty()) {
            int randomIndex = random.nextInt(compatiblePorts.size());
            return compatiblePorts.get(randomIndex);
        }
        
        return null; // No available ports
    }

    // Queue management methods
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
        storage.poll(); // Remove the oldest packet (FIFO)
    }
}

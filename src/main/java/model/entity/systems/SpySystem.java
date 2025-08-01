package model.entity.systems;

import javafx.geometry.Point2D;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.Random;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.levels.Level;

public class SpySystem extends System {
    private static final int MAX_STORAGE = 5;
    private final Queue<Packet> storage = new LinkedList<>();
    private final Random random = new Random();
    private Level level; // Reference to level to find other spy systems

    public SpySystem(Point2D position) {
        super(position, SystemType.SpySystem);
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
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
     * Find a random spy system output port (including this system's own ports)
     * This implements the spy system behavior where packets can exit from any spy system
     * Uses normal routing logic (FIFO and compatible priority) but from any spy system
     */
    public Port findRandomSpySystemOutPort(Packet packet) {
        if (level == null) {
            // Fallback to normal behavior if level reference is not available
            return findBestOutPort(packet);
        }

        // Collect all spy systems in the network
        List<SpySystem> spySystems = new java.util.ArrayList<>();
        for (System system : level.getSystems()) {
            if (system instanceof SpySystem) {
                spySystems.add((SpySystem) system);
            }
        }

        if (spySystems.isEmpty()) {
            // No spy systems found, fallback to normal behavior
            return findBestOutPort(packet);
        }

        // Randomly select a spy system
        SpySystem randomSpySystem = spySystems.get(random.nextInt(spySystems.size()));
        
        // Try to find available output ports from the randomly selected spy system
        Port bestPort = findBestAvailablePortFromSpySystem(randomSpySystem, packet);
        if (bestPort != null) {
            return bestPort;
        }

        // If no available ports in the randomly selected spy system, try other spy systems
        for (SpySystem spySystem : spySystems) {
            if (spySystem != randomSpySystem) {
                bestPort = findBestAvailablePortFromSpySystem(spySystem, packet);
                if (bestPort != null) {
                    return bestPort;
                }
            }
        }

        // Fallback to normal behavior if no spy system ports are available
        return findBestOutPort(packet);
    }

    /**
     * Helper method to find the best available output port from a specific spy system
     * Uses normal routing logic (compatible priority, then any available)
     */
    private Port findBestAvailablePortFromSpySystem(SpySystem spySystem, Packet packet) {
        // First pass: compatible and available
        for (Port port : spySystem.getOutPorts()) {
            if (port.isConnected() && port.isCompatible(packet) && 
                port.getWire() != null && port.getWire().isAvailable()) {
                return port;
            }
        }
        
        // Second pass: any available
        for (Port port : spySystem.getOutPorts()) {
            if (port.isConnected() && port.getWire() != null && port.getWire().isAvailable()) {
                return port;
            }
        }
        
        return null; // No available ports in this spy system
    }

    /**
     * Process a packet entering the spy system
     * @param packet The packet to process
     */
    public void processPacket(Packet packet) {
        // TODO: Check if packet is confidential - confidential packets are destroyed
        
        // Apply spy system effects
        // Add noise, random position changes, etc. (existing spy behavior)
        
        // For now, just add to storage if there's space
        if (!isFull()) {
            enqueuePacket(packet);
        } else {
            // Remove oldest packet if storage is full
            removeOldestPacket();
            enqueuePacket(packet);
        }
    }
}

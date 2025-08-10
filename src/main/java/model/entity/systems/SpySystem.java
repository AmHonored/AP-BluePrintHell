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
    private Level level; 

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

    public Port findRandomSpySystemOutPort(Packet packet) {
        if (level == null) {
            return findBestOutPort(packet);
        }

        List<SpySystem> spySystems = new java.util.ArrayList<>();
        for (System system : level.getSystems()) {
            if (system instanceof SpySystem) {
                spySystems.add((SpySystem) system);
            }
        }

        if (spySystems.isEmpty()) {
            return findBestOutPort(packet);
        }

        SpySystem randomSpySystem = spySystems.get(random.nextInt(spySystems.size()));
        
        Port bestPort = findBestAvailablePortFromSpySystem(randomSpySystem, packet);
        if (bestPort != null) {
            return bestPort;
        }

        for (SpySystem spySystem : spySystems) {
            if (spySystem != randomSpySystem) {
                bestPort = findBestAvailablePortFromSpySystem(spySystem, packet);
                if (bestPort != null) {
                    return bestPort;
                }
            }
        }

        return findBestOutPort(packet);
    }

    private Port findBestAvailablePortFromSpySystem(SpySystem spySystem, Packet packet) {
        for (Port port : spySystem.getOutPorts()) {
            if (port.isConnected() && port.isCompatible(packet) && 
                port.getWire() != null && port.getWire().isActive()) {
                return port;
            }
        }
        
        for (Port port : spySystem.getOutPorts()) {
            if (port.isConnected() && port.getWire() != null && port.getWire().isActive()) {
                return port;
            }
        }
        
        return null; 
    }

    public void processPacket(Packet packet) {
        if (!isFull()) {
            enqueuePacket(packet);
        } else {
            removeOldestPacket();
            enqueuePacket(packet);
        }
    }
}

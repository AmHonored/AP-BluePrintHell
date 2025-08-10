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
    private static final double TROJAN_CONVERSION_PROBABILITY = 0.2;
    private final Queue<Packet> storage = new LinkedList<>();
    private final Random random = new Random();

    public DDosSystem(Point2D position) {
        super(position, SystemType.DDosSystem);
    }

    public void processPacket(Packet packet) {
        packet.takeDamage(1);
        
        if (!packet.isAlive()) {
            return;
        }
        
        if (random.nextDouble() < TROJAN_CONVERSION_PROBABILITY) {
            packet.convertToTrojan();
        }
        
        if (!isFull()) {
            enqueuePacket(packet);
        } else {
            removeOldestPacket();
            enqueuePacket(packet);
        }
    }

    @Override
    public Port findBestOutPort(Packet packet) {
        ArrayList<Port> incompatiblePorts = new ArrayList<>();
        ArrayList<Port> compatiblePorts = new ArrayList<>();
        
        for (Port port : outPorts) {
            if (port.isConnected()) {
                if (port.isCompatible(packet)) {
                    compatiblePorts.add(port);
                } else {
                    incompatiblePorts.add(port);
                }
            }
        }
        
        if (!incompatiblePorts.isEmpty()) {
            int randomIndex = random.nextInt(incompatiblePorts.size());
            return incompatiblePorts.get(randomIndex);
        }
        
        if (!compatiblePorts.isEmpty()) {
            int randomIndex = random.nextInt(compatiblePorts.size());
            return compatiblePorts.get(randomIndex);
        }
        
        return null; 
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
        storage.poll(); 
    }
}

package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;

/**
 * Distributor system: no storage/capacity. When a massive packet enters,
 * it splits into bit packets of size 1 and buffers them. It has unlimited capacity
 * and forwards FIFO via available output ports.
 */
public class DistributorSystem extends System {
    private final java.util.Queue<Packet> storage = new java.util.LinkedList<>();

    public DistributorSystem(Point2D position) {
        super(position, SystemType.DistributorSystem);
    }

    /**
     * Enqueue a packet into internal storage
     */
    public void processPacket(Packet packet) {
        storage.add(packet);
    }

    public java.util.Queue<Packet> getPackets() {
        return storage;
    }

    public int getStorageSize() {
        return storage.size();
    }
}



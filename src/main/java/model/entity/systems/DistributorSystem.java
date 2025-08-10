package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;

public class DistributorSystem extends System {
    private final java.util.Queue<Packet> storage = new java.util.LinkedList<>();

    public DistributorSystem(Point2D position) {
        super(position, SystemType.DistributorSystem);
    }

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



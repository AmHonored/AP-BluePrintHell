package model.entity.systems;

import javafx.geometry.Point2D;
import java.util.LinkedList;
import java.util.Queue;
import model.entity.packets.Packet;
import model.entity.ports.Port;

public class IntermediateSystem extends System {
    private static final int MAX_STORAGE = 5;
    private final Queue<Packet> storage = new LinkedList<>();

    public IntermediateSystem(Point2D position) {
        super(position, SystemType.IntermediateSystem);
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

    public void removeLastPacket() {
        if (!storage.isEmpty()) {
            // Remove the last packet (not FIFO, but for overflow handling)
            Packet last = null;
            for (Packet p : storage) last = p;
            if (last != null) storage.remove(last);
        }
    }
}

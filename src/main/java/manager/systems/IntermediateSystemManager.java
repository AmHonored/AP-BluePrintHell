package manager.systems;

import model.entity.systems.IntermediateSystem;
import model.entity.packets.Packet;

public class IntermediateSystemManager extends SystemManager<IntermediateSystem> {
    private static final int MAX_STORAGE = 5;

    public IntermediateSystemManager(IntermediateSystem system) {
        super(system);
    }

    public void forwardPackets() {
        forwardAllFromQueue(system.getPackets());
        
        int removed = 0;
        while (system.getStorageSize() > MAX_STORAGE) {
            system.removeLastPacket();
            removed++;
        }
        if (removed > 0) {
        }
    }

    public void receivePacket(Packet packet) {
        if (!system.isFull()) {
            system.enqueuePacket(packet);
        } else {
            system.removeLastPacket();
        }
    }

}

package manager.systems;

import model.entity.systems.SpySystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;

public class SpySystemManager extends SystemManager<SpySystem> {
    private static final int MAX_STORAGE = 5;

    public SpySystemManager(SpySystem system) {
        super(system);
    }
    public void forwardPackets() {
        forwardAllFromQueue(system.getPackets(), p -> system.findRandomSpySystemOutPort(p));
        // Handle overflow by removing excess packets
        while (system.getStorageSize() > MAX_STORAGE) {
            system.removeOldestPacket();
        }
    }

    public void receivePacket(Packet packet) {

        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            Packet originalPacket = protectedPacket.convertToOriginalType();
            
            // Copy movement state
            originalPacket.setPosition(protectedPacket.getPosition());
            originalPacket.setDirection(protectedPacket.getDirection());
            originalPacket.setMoving(protectedPacket.isMoving());
            originalPacket.setCurrentWire(protectedPacket.getCurrentWire());
            originalPacket.setMovementProgress(protectedPacket.getMovementProgress());
            originalPacket.setStartPosition(protectedPacket.getStartPosition());
            originalPacket.setTargetPosition(protectedPacket.getTargetPosition());
            
            system.processPacket(originalPacket);
        } else {
            system.processPacket(packet);
        }
    }

    public SpySystem getSystem() {
        return system;
    }

    public static void forwardPacketsFromAnySpySystem(model.levels.Level level) {

        java.util.List<SpySystem> spySystemsWithPackets = new java.util.ArrayList<>();
        
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof SpySystem) {
                SpySystem spySystem = (SpySystem) system;
                if (!spySystem.getPackets().isEmpty()) {
                    spySystemsWithPackets.add(spySystem);
                }
            }
        }
        
        for (SpySystem spySystem : spySystemsWithPackets) {
            SpySystemManager manager = new SpySystemManager(spySystem);
            manager.forwardPackets();
        }
    }
}

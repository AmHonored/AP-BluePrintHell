package manager.systems;

import model.entity.systems.DDosSystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;

public class DDosSystemManager extends SystemManager<DDosSystem> {

    public DDosSystemManager(DDosSystem system) {
        super(system);
    }

    public void forwardPackets() {
        forwardAllFromQueue(system.getPackets());
        
    }

    public void receivePacket(Packet packet) {
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            Packet originalPacket = protectedPacket.convertToOriginalType();
            
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

    public DDosSystem getSystem() {
        return system;
    }
}

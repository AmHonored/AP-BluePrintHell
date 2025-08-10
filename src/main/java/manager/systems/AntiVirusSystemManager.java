package manager.systems;

import model.entity.systems.AntiVirusSystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;

public class AntiVirusSystemManager extends SystemManager<AntiVirusSystem> {

    public AntiVirusSystemManager(AntiVirusSystem system) {
        super(system);
    }

    public void forwardPackets() {
        forwardAllFromQueue(system.getPackets());
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

    public boolean processActiveTrojanPackets(java.util.List<Packet> allPackets) {
        if (system.isDisabled()) {
            return false;
        }

        for (Packet packet : allPackets) {
            if (packet.isTrojan() && system.isPacketInRange(packet)) {
                return system.processActiveTrojanPacket(packet);
            }
        }
        
        return false;
    }

    public AntiVirusSystem getSystem() {
        return system;
    }
} 
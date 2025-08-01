package manager.systems;

import model.entity.systems.DDosSystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;
import model.entity.ports.Port;
import manager.packets.PacketManager;

public class DDosSystemManager {
    private final DDosSystem system;
    private static final int MAX_STORAGE = 5;

    public DDosSystemManager(DDosSystem system) {
        this.system = system;
    }

    /**
     * Forward packets from storage to output ports using DDoS logic
     */
    public void forwardPackets() {
        // Forward packets from storage to available output ports
        while (!system.getPackets().isEmpty()) {
            Packet packet = system.peekNextPacket();
            
            // Use DDoS-specific port selection logic
            Port bestPort = system.findBestOutPort(packet);
            if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isAvailable()) {
                // Move packet to the output port position
                packet.setPosition(bestPort.getPosition());
                
                boolean sent = PacketManager.sendPacket(bestPort, packet);
                if (sent) {
                    system.dequeuePacket();
                } else {
                    break;
                }
            } else {
                // No available port, keep packet in storage
                break;
            }
        }
        
        // Handle overflow by removing excess packets
        while (system.getStorageSize() > MAX_STORAGE) {
            system.removeOldestPacket();
        }
    }

    /**
     * Receive and process a packet in the DDoS system
     */
    public void receivePacket(Packet packet) {
        // If it's a protected packet, convert it back to original type first
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
            
            System.out.println("⚡ DDOS SYSTEM: Protected packet converted back to " + 
                protectedPacket.getOriginalType() + " - " + packet.getId());
            
            // Process the converted packet
            system.processPacket(originalPacket);
        } else {
            // Process the packet through DDoS system logic
            system.processPacket(packet);
        }
    }

    /**
     * Get the DDoS system
     */
    public DDosSystem getSystem() {
        return system;
    }
}

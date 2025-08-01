package manager.systems;

import model.entity.systems.AntiVirusSystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;
import model.entity.ports.Port;
import manager.packets.PacketManager;

public class AntiVirusSystemManager {
    private final AntiVirusSystem system;
    private static final int MAX_STORAGE = 5;

    public AntiVirusSystemManager(AntiVirusSystem system) {
        this.system = system;
    }

    /**
     * Forward packets from storage to output ports
     */
    public void forwardPackets() {
        // Forward packets from storage to available output ports
        while (!system.getPackets().isEmpty()) {
            Packet packet = system.peekNextPacket();
            
            // Find an available output port
            Port bestPort = findBestOutputPort(packet);
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
     * Find the best output port for the packet (prioritize compatible ports)
     */
    private Port findBestOutputPort(Packet packet) {
        // First try to find a compatible port
        for (Port port : system.getOutPorts()) {
            if (port.isConnected() && port.isCompatible(packet)) {
                return port;
            }
        }
        
        // If no compatible port found, try any connected port
        for (Port port : system.getOutPorts()) {
            if (port.isConnected()) {
                return port;
            }
        }
        
        return null;
    }

    /**
     * Receive and process a packet in the AntiVirus system
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
            
            java.lang.System.out.println("🛡️ ANTIVIRUS SYSTEM: Protected packet converted back to " + 
                protectedPacket.getOriginalType() + " - " + packet.getId());
            
            // Process the converted packet
            system.processPacket(originalPacket);
        } else {
            // Process the packet through AntiVirus system logic
            system.processPacket(packet);
        }
    }

    /**
     * Process active trojan packets in the level within range
     * This should be called periodically to scan for trojan packets
     */
    public boolean processActiveTrojanPackets(java.util.List<Packet> allPackets) {
        if (system.isDisabled()) {
            return false;
        }

        // Check all packets in the level to see if any trojans are in range
        for (Packet packet : allPackets) {
            if (packet.isTrojan() && system.isPacketInRange(packet)) {
                return system.processActiveTrojanPacket(packet);
            }
        }
        
        return false;
    }

    /**
     * Get the AntiVirus system
     */
    public AntiVirusSystem getSystem() {
        return system;
    }
} 
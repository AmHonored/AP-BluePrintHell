package manager.systems;

import model.entity.systems.SpySystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;
import model.entity.ports.Port;
import manager.packets.PacketManager;

public class SpySystemManager {
    private final SpySystem system;
    private static final int MAX_STORAGE = 5;

    public SpySystemManager(SpySystem system) {
        this.system = system;
    }

    /**
     * Forward packets from storage to output ports using spy system logic
     * Packets can exit from any spy system in the network (randomly chosen)
     * Uses normal routing logic (FIFO and compatible priority) but from any spy system
     * 
     * For spy systems without input ports: This method will be called but won't do anything
     * since they have no packets to forward. The actual spy routing happens when packets
     * are in spy systems that have storage.
     */
    public void forwardPackets() {
        java.lang.System.out.println("DEBUG SpySystemManager.forwardPackets: Starting with " + system.getStorageSize() + " packets in storage");
        
        // Forward packets from storage to available output ports
        while (!system.getPackets().isEmpty()) {
            Packet packet = system.peekNextPacket();
            
            // Use spy-specific port selection logic (random spy system routing with normal logic)
            Port bestPort = system.findRandomSpySystemOutPort(packet);
            if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isAvailable()) {
                // Move packet to the output port position
                packet.setPosition(bestPort.getPosition());
                
                boolean sent = PacketManager.sendPacket(bestPort, packet);
                if (sent) {
                    system.dequeuePacket();
                    java.lang.System.out.println("DEBUG SpySystemManager.forwardPackets: SUCCESS - Sent packet " + packet.getId() + 
                                                 " through port " + bestPort.getId() + " (spy routing with normal logic)");
                } else {
                    java.lang.System.out.println("DEBUG SpySystemManager.forwardPackets: FAILED - Could not send packet " + packet.getId());
                    break;
                }
            } else {
                // No available port, keep packet in storage
                java.lang.System.out.println("DEBUG SpySystemManager.forwardPackets: No available spy system ports for packet " + packet.getId());
                break;
            }
        }
        
        // Handle overflow by removing excess packets
        while (system.getStorageSize() > MAX_STORAGE) {
            system.removeOldestPacket();
            java.lang.System.out.println("DEBUG SpySystemManager.forwardPackets: Removed packet due to overflow");
        }
        
        java.lang.System.out.println("DEBUG SpySystemManager.forwardPackets: Finished with " + system.getStorageSize() + " packets in storage");
    }

    /**
     * Receive and process a packet in the spy system
     */
    public void receivePacket(Packet packet) {
        java.lang.System.out.println("DEBUG SpySystemManager.receivePacket: Attempting to receive packet " + packet.getId() + 
                                     ". Current storage: " + system.getStorageSize() + "/" + MAX_STORAGE);
        
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
            
            System.out.println("🕵️ SPY SYSTEM: Protected packet converted back to " + 
                protectedPacket.getOriginalType() + " - " + packet.getId());
            
            // Process the converted packet
            system.processPacket(originalPacket);
        } else {
            // Process the packet through spy system logic
            system.processPacket(packet);
        }
        
        java.lang.System.out.println("DEBUG SpySystemManager.receivePacket: SUCCESS - Packet " + packet.getId() + 
                                     " processed. New storage: " + system.getStorageSize() + "/" + MAX_STORAGE);
    }

    /**
     * Get the spy system
     */
    public SpySystem getSystem() {
        return system;
    }

    /**
     * Static method to handle network-wide spy system packet forwarding
     * This allows packets to exit from any spy system in the network, even those without input ports
     * 
     * @param level The level containing all systems
     */
    public static void forwardPacketsFromAnySpySystem(model.levels.Level level) {
        // Find all spy systems that have packets to forward
        java.util.List<SpySystem> spySystemsWithPackets = new java.util.ArrayList<>();
        
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof SpySystem) {
                SpySystem spySystem = (SpySystem) system;
                if (!spySystem.getPackets().isEmpty()) {
                    spySystemsWithPackets.add(spySystem);
                }
            }
        }
        
        // Process each spy system that has packets
        for (SpySystem spySystem : spySystemsWithPackets) {
            SpySystemManager manager = new SpySystemManager(spySystem);
            manager.forwardPackets();
        }
    }
}

package manager.systems;

import model.entity.systems.DDosSystem;
import model.entity.packets.Packet;
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
        System.out.println("DEBUG DDosSystemManager.forwardPackets: Starting with " + system.getStorageSize() + " packets in storage");
        
        // Forward packets from storage to available output ports
        while (!system.getPackets().isEmpty()) {
            Packet packet = system.peekNextPacket();
            System.out.println("DEBUG DDosSystemManager.forwardPackets: Processing packet " + packet.getId() + 
                             " (health: " + packet.getCurrentHealth() + ", trojan: " + packet.isTrojan() + ")");
            
            // Use DDoS-specific port selection logic
            Port bestPort = system.findBestOutPort(packet);
            if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isAvailable()) {
                System.out.println("DEBUG DDosSystemManager.forwardPackets: Found available port " + bestPort.getId() + 
                                 " (compatible: " + bestPort.isCompatible(packet) + ")");
                
                // Move packet to the output port position
                packet.setPosition(bestPort.getPosition());
                
                boolean sent = PacketManager.sendPacket(bestPort, packet);
                if (sent) {
                    system.dequeuePacket();
                    System.out.println("DEBUG DDosSystemManager.forwardPackets: SUCCESS - Sent packet " + packet.getId() + 
                                     " through port " + bestPort.getId() + 
                                     " (compatible: " + bestPort.isCompatible(packet) + 
                                     ", trojan: " + packet.isTrojan() + ")");
                } else {
                    System.out.println("DEBUG DDosSystemManager.forwardPackets: FAILED - Could not send packet " + packet.getId() + 
                                     " through port " + bestPort.getId());
                    break;
                }
            } else {
                // No available port, keep packet in storage
                System.out.println("DEBUG DDosSystemManager.forwardPackets: No available port found for packet " + packet.getId());
                break;
            }
        }
        
        // Handle overflow by removing excess packets
        int removed = 0;
        while (system.getStorageSize() > MAX_STORAGE) {
            system.removeOldestPacket();
            removed++;
        }
        if (removed > 0) {
            System.out.println("DEBUG DDosSystemManager.forwardPackets: Removed " + removed + " packets due to overflow");
        }
        
        System.out.println("DEBUG DDosSystemManager.forwardPackets: Finished with " + system.getStorageSize() + " packets in storage");
    }

    /**
     * Receive and process a packet in the DDoS system
     */
    public void receivePacket(Packet packet) {
        System.out.println("DEBUG DDosSystemManager: Processing packet " + packet.getId() + 
                         " (health: " + packet.getCurrentHealth() + ", trojan: " + packet.isTrojan() + ")");
        
        // Process the packet through DDoS system logic
        system.processPacket(packet);
        
        // Log the result
        if (packet.isAlive()) {
            System.out.println("DEBUG DDosSystemManager: Packet " + packet.getId() + 
                             " processed - health: " + packet.getCurrentHealth() + 
                             ", trojan: " + packet.isTrojan() + 
                             ", storage: " + system.getStorageSize() + "/" + MAX_STORAGE);
        } else {
            System.out.println("DEBUG DDosSystemManager: Packet " + packet.getId() + 
                             " was destroyed by noise during processing");
        }
    }

    /**
     * Get the DDoS system
     */
    public DDosSystem getSystem() {
        return system;
    }
}

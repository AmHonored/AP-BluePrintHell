package manager.systems;

import model.entity.systems.IntermediateSystem;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import manager.packets.PacketManager;
import java.util.Iterator;

public class IntermediateSystemManager {
    private final IntermediateSystem system;
    private static final int MAX_STORAGE = 5;

    public IntermediateSystemManager(IntermediateSystem system) {
        this.system = system;
    }

    public void forwardPackets() {
        // Forward packets from storage to available output ports
        while (!system.getPackets().isEmpty()) {
            Packet packet = system.peekNextPacket();
            
            Port bestPort = findBestAvailableOutPort(packet);
            if (bestPort != null) {
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
        int removed = 0;
        while (system.getStorageSize() > MAX_STORAGE) {
            system.removeLastPacket();
            removed++;
        }
        if (removed > 0) {
            // Packets were removed due to overflow
        }
    }

    public void receivePacket(Packet packet) {
        java.lang.System.out.println("DEBUG receivePacket: Attempting to receive packet " + packet.getId() + 
                                     ". Current storage: " + system.getStorageSize() + "/" + MAX_STORAGE);
        
        if (!system.isFull()) {
            system.enqueuePacket(packet);
            java.lang.System.out.println("DEBUG receivePacket: SUCCESS - Packet " + packet.getId() + 
                                         " added to storage. New storage: " + system.getStorageSize() + "/" + MAX_STORAGE);
        } else {
            // Overflow: kill the last packet
            system.removeLastPacket();
            java.lang.System.out.println("DEBUG receivePacket: OVERFLOW - Removed last packet due to full storage. " +
                                         "Storage remains: " + system.getStorageSize() + "/" + MAX_STORAGE);
        }
    }

    // Prefer compatible and available port, fallback to any available port
    private Port findBestAvailableOutPort(Packet packet) {
        java.lang.System.out.println("DEBUG findBestAvailableOutPort: Looking for best port for packet " + packet.getId());
        
        // First pass: compatible and available
        for (Port port : system.getOutPorts()) {
            boolean connected = port.isConnected();
            boolean compatible = port.isCompatible(packet);
            boolean available = port.getWire() != null ? port.getWire().isAvailable() : false;
            
            java.lang.System.out.println("DEBUG findBestAvailableOutPort: Port " + port.getId() + 
                                         " - connected: " + connected + ", compatible: " + compatible + 
                                         ", wire available: " + available);
            
            if (connected && compatible && available) {
                java.lang.System.out.println("DEBUG findBestAvailableOutPort: Found COMPATIBLE port " + port.getId());
                return port;
            }
        }
        
        // Second pass: any available
        for (Port port : system.getOutPorts()) {
            boolean connected = port.isConnected();
            boolean available = port.getWire() != null ? port.getWire().isAvailable() : false;
            
            if (connected && available) {
                java.lang.System.out.println("DEBUG findBestAvailableOutPort: Found ANY AVAILABLE port " + port.getId());
                return port;
            }
        }
        
        java.lang.System.out.println("DEBUG findBestAvailableOutPort: No available ports found");
        return null;
    }
}

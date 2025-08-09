package manager.systems;

import manager.packets.PacketManager;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.entity.systems.DistributorSystem;

import java.util.List;

public class DistributorSystemManager {
    private final DistributorSystem system;
    private int lastUsedPortIndex = -1; // For round-robin distribution

    public DistributorSystemManager(DistributorSystem system) {
        this.system = system;
    }

    public void receivePacket(Packet packet) {
        if (packet instanceof MassivePacket) {
            MassivePacket massive = (MassivePacket) packet;
            // Split into bits
            List<Packet> bits = massive.splitIntoBits();
            // Remove the original from level/view
            PacketManager.removePacket(packet);

            // Enqueue all bits into distributor storage (will be handled by sendPacket when forwarded)
            for (Packet bit : bits) {
                bit.setPosition(system.getPosition());
                system.processPacket(bit);
                // Note: Bitpackets are added to level when sent via PacketManager.sendPacket()
            }
            // Attempt forwarding
            forwardPackets();
        } else {
            // Non-massive packets: store in FIFO queue and attempt immediate forwarding
            system.processPacket(packet);
            
            // Try to forward immediately if possible
            java.util.Queue<Packet> queue = system.getPackets();
            if (queue.peek() == packet) { // Only forward if it's at the front of queue
                Port best = findBestAvailableOutPort(packet);
                if (best != null && best.getWire() != null && best.getWire().isAvailable()) {
                    packet.setPosition(best.getPosition());
                    
                    // Send with preserved compatibility to maintain original speed
                    boolean sent = PacketManager.sendPacket(best, packet, true);
                    if (sent) {
                        queue.poll(); // Remove from queue after successful send
                        java.lang.System.out.println("DEBUG DistributorSystemManager: Non-massive packet " + packet.getId() + 
                                                 " forwarded immediately via port " + best.getId());
                    }
                } else {
                    java.lang.System.out.println("DEBUG DistributorSystemManager: No available port for non-massive packet " + 
                                               packet.getId() + ", queued for later");
                }
            }
        }
    }

    public void forwardPackets() {
        java.util.Queue<Packet> queue = system.getPackets();
        while (!queue.isEmpty()) {
            Packet next = queue.peek();
            Port best = findBestAvailableOutPort(next);
            if (best != null && best.getWire() != null && best.getWire().isAvailable()) {
                next.setPosition(best.getPosition());
                
                // For non-massive/non-bit packets, preserve compatibility to maintain speed
                boolean preserveCompatibility = !next.isBitFragment() && !(next instanceof MassivePacket);
                
                boolean sent = PacketManager.sendPacket(best, next, preserveCompatibility);
                if (sent) {
                    queue.poll();
                    String packetType = next.isBitFragment() ? "bitpacket" : "packet";
                    java.lang.System.out.println("DEBUG DistributorSystemManager: Successfully sent " + packetType + " " + next.getId() + " (" + next.getType() + ") through port " + best.getId());
                } else {
                    break;
                }
            } else {
                // No available port -> stop, wait for next tick
                java.lang.System.out.println("DEBUG DistributorSystemManager: No available ports for bitpacket " + next.getId());
                break;
            }
        }
    }

    // Round-robin distribution across available output ports
    private Port findBestAvailableOutPort(Packet packet) {
        java.util.List<Port> outPorts = system.getOutPorts();
        if (outPorts.isEmpty()) {
            return null;
        }

        // Find all available ports (compatible and connected)
        java.util.List<Port> availablePorts = new java.util.ArrayList<>();
        
        // First pass: compatible and available
        for (Port port : outPorts) {
            boolean connected = port.isConnected();
            boolean compatible = port.isCompatible(packet);
            boolean available = port.getWire() != null ? port.getWire().isAvailable() : false;

            if (connected && compatible && available) {
                availablePorts.add(port);
            }
        }

        // Second pass: if no compatible ports, use any available
        if (availablePorts.isEmpty()) {
            for (Port port : outPorts) {
                boolean connected = port.isConnected();
                boolean available = port.getWire() != null ? port.getWire().isAvailable() : false;

                if (connected && available) {
                    availablePorts.add(port);
                }
            }
        }

        if (availablePorts.isEmpty()) {
            return null;
        }

        // Round-robin selection from available ports
        lastUsedPortIndex = (lastUsedPortIndex + 1) % availablePorts.size();
        Port selectedPort = availablePorts.get(lastUsedPortIndex);
        
        java.lang.System.out.println("DEBUG DistributorSystemManager: Round-robin selected port " + 
            selectedPort.getId() + " (index " + lastUsedPortIndex + " of " + availablePorts.size() + " available)");
        
        return selectedPort;
    }
}



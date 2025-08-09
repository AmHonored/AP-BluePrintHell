package manager.systems;

import manager.packets.PacketManager;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;
import model.entity.ports.Port;
import model.entity.systems.MergeSystem;
import view.components.systems.MergeSystemView;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.UUID;

public class MergeSystemManager {
    private final MergeSystem system;

    public MergeSystemManager(MergeSystem system) {
        this.system = system;
    }

    /**
     * Receive a packet into the merge system
     */
    public void receivePacket(Packet packet) {
        // Only accept bit packets
        if (packet.isBitFragment()) {
            // Remove from game view since it's being stored
            PacketManager.removePacket(packet);
            
            // Add to merge system storage
            system.addBitPacket(packet);
            
            System.out.println("DEBUG MergeSystemManager: Received bit packet " + packet.getId() + 
                              " (type: " + packet.getType() + ")");
            
            // Check if we can create a massive packet
            checkAndCreateMassivePacket();
        } else {
            // Non-bit packets pass through
            forwardPacket(packet);
        }
    }

    /**
     * Check if enough bits are collected and create massive packet
     */
    private void checkAndCreateMassivePacket() {
        if (!system.canCreateMassivePacket()) {
            return;
        }

        PacketType massiveType = system.getMassivePacketType();
        if (massiveType == null) {
            return;
        }

        // Remove the bits used for creating massive packet
        List<Packet> usedBits = system.removeBitsForMassive(massiveType);
        
        // Create new massive packet at system position
        MassivePacket newMassive = null;
        String id = "MP-" + UUID.randomUUID().toString().substring(0, 8);
        Point2D position = system.getPosition();
        Point2D direction = new Point2D(1, 0); // Default direction

        if (massiveType == PacketType.MASSIVE_TYPE1) {
            newMassive = new MassivePacket.Type1(id, position, direction);
            System.out.println("DEBUG MergeSystemManager: Created Massive Type1 packet from 8 circle bits");
        } else if (massiveType == PacketType.MASSIVE_TYPE2) {
            newMassive = new MassivePacket.Type2(id, position, direction);
            System.out.println("DEBUG MergeSystemManager: Created Massive Type2 packet from 10 rect bits");
        }

        if (newMassive != null) {
            // Calculate and apply packet loss if needed
            double packetLoss = system.calculatePacketLoss();
            if (packetLoss > 0) {
                // Apply packet loss by reducing health
                int healthReduction = (int) Math.round(packetLoss);
                int newHealth = Math.max(1, newMassive.getHealth() - healthReduction);
                newMassive.setCurrentHealth(newHealth);
                System.out.println("DEBUG MergeSystemManager: Applied packet loss of " + 
                                 packetLoss + ", new health: " + newHealth);
            }

            // Forward the newly created massive packet
            forwardPacket(newMassive);
        }
    }

    /**
     * Forward a packet to the best available output port
     */
    private void forwardPacket(Packet packet) {
        Port bestPort = findBestOutputPort(packet);
        if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isAvailable()) {
            packet.setPosition(bestPort.getPosition());
            
            boolean sent = PacketManager.sendPacket(bestPort, packet);
            if (sent) {
                System.out.println("DEBUG MergeSystemManager: Forwarded packet " + packet.getId() + 
                                 " through port " + bestPort.getId());
            }
        } else {
            System.out.println("DEBUG MergeSystemManager: No available output port for packet " + 
                             packet.getId());
        }
    }

    /**
     * Find the best output port for the packet
     */
    private Port findBestOutputPort(Packet packet) {
        // First try compatible ports
        for (Port port : system.getOutPorts()) {
            if (port.isConnected() && port.isCompatible(packet) && 
                port.getWire() != null && port.getWire().isAvailable()) {
                return port;
            }
        }
        
        // Then try any available port
        for (Port port : system.getOutPorts()) {
            if (port.isConnected() && port.getWire() != null && port.getWire().isAvailable()) {
                return port;
            }
        }
        
        return null;
    }

    /**
     * Update the visual display of the merge system
     */
    public void updateView(MergeSystemView view) {
        if (view != null) {
            view.updateCounts(system.getCircleBitCount(), system.getRectBitCount());
        }
    }
}

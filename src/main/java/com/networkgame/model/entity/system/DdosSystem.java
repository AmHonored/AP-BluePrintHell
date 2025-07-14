package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.state.GameState;
import com.networkgame.model.manager.PacketManager;
import com.networkgame.model.entity.packettype.messenger.ProtectedPacket;
import java.util.Random;

/**
 * DdosSystem - A malicious system that sends packets to incompatible ports,
 * adds noise, and can convert packets to trojan packets
 */
public class DdosSystem extends BaseSystem {
    
    private static final Random random = new Random();
    private static final double TROJAN_CONVERSION_PROBABILITY = 0.3; // 30% chance to convert to trojan
    
    public DdosSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
        this.label = "DDoS System";
    }
    
    @Override
    protected void initializeSystem() {
        // TODO: Initialize DDoS system specific components
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // TODO: Implement DDoS system specific update logic
    }
    
    @Override
    public void handleOutputPortAvailable(Port port) {
        // TODO: Handle output port availability for DDoS system
    }
    
    @Override
    public void receivePacket(Packet packet) {
        System.out.println("=== DDoS SYSTEM: Packet " + packet.getId() + " (" + packet.getType() + ") ENTERING ===");
        System.out.println("DDoS: Packet initial health: " + packet.getHealth() + ", noise level: " + packet.getNoiseLevel());
        
        // Skip if this is a reference system
        if (isReference) {
            System.out.println("DDoS: Skipping reference system");
            return;
        }
        
        // Clear the connection reference
        if (packet.getCurrentConnection() != null) {
            System.out.println("DDoS: Clearing connection reference");
            packet.setCurrentConnection(null);
        }
        
        // Check if this is a protected packet and reveal it
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            if (!protectedPacket.isRevealed()) {
                System.out.println("DDoS: REVEALING protected packet " + packet.getId() + 
                                 " - true type: " + protectedPacket.getUnderlyingType());
                protectedPacket.revealTrueType();
            }
        }
        
        // Apply DDoS system effects to the packet
        System.out.println("DDoS: Applying DDoS effects...");
        processDdosEffects(packet);
        
        // If packet is critically damaged, remove it completely
        if (packet.hasProperty("criticallyDamaged")) {
            System.out.println("DDoS: Packet " + packet.getId() + " is critically damaged - REMOVING from game");
            if (gameState != null) {
                gameState.getActivePackets().remove(packet);
            }
            return;
        }
        
        // Try to send packet to an INCOMPATIBLE port (opposite of normal behavior)
        System.out.println("DDoS: Attempting to send packet " + packet.getId() + " to incompatible port...");
        boolean packetSent = sendToIncompatiblePort(packet);
        
        if (packetSent) {
            System.out.println("DDoS: Successfully sent packet " + packet.getId() + " to incompatible port");
        } else {
            System.out.println("DDoS: Could not send to incompatible port, storing packet");
            // If no incompatible port available, store the packet
            PacketManager packetManager = managerRegistry.get(PacketManager.class);
            if (packetManager != null) {
                packetManager.receivePacket(packet);
            }
        }
        System.out.println("=== DDoS SYSTEM: Packet " + packet.getId() + " processing COMPLETE ===");
    }
    
    /**
     * Apply DDoS system effects to the packet
     */
    private void processDdosEffects(Packet packet) {
        System.out.println("DDoS: Processing effects for packet " + packet.getId());
        System.out.println("DDoS: Before - Health: " + packet.getHealth() + ", Noise: " + packet.getNoiseLevel());
        
        // Add 1 noise to the packet (always, not just if it has no noise)
        packet.addNoise(1);
        System.out.println("DDoS: After adding noise - Health: " + packet.getHealth() + ", Noise: " + packet.getNoiseLevel());
        
        // Check if packet should be removed based on noise tolerance after adding noise
        boolean shouldRemove = shouldRemovePacket(packet);
        System.out.println("DDoS: Should remove packet? " + shouldRemove);
        
        if (shouldRemove) {
            System.out.println("DDoS: Marking packet " + packet.getId() + " as critically damaged");
            packet.setProperty("criticallyDamaged", true);
        }
        
        // Convert to trojan packet with certain probability
        if (random.nextDouble() < TROJAN_CONVERSION_PROBABILITY) {
            System.out.println("DDoS: Converting packet " + packet.getId() + " to trojan");
            convertToTrojanPacket(packet);
        }
    }
    
    /**
     * Check if packet should be removed based on noise tolerance
     */
    private boolean shouldRemovePacket(Packet packet) {
        // For protected packets, use the underlying type for noise tolerance calculation
        Packet.PacketType typeToCheck = packet.getType();
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            typeToCheck = protectedPacket.getUnderlyingType();
            System.out.println("DDoS: Using underlying type " + typeToCheck + " for protected packet noise tolerance");
        }
        
        switch (typeToCheck) {
            case SQUARE:
                boolean squareRemove = packet.getNoiseLevel() > 2; // Square packets can take 2 noises
                System.out.println("DDoS: Square packet noise check - Level: " + packet.getNoiseLevel() + ", Remove: " + squareRemove);
                return squareRemove;
            case TRIANGLE:
                boolean triangleRemove = packet.getNoiseLevel() > 3; // Triangle packets can take 3 noises
                System.out.println("DDoS: Triangle packet noise check - Level: " + packet.getNoiseLevel() + ", Remove: " + triangleRemove);
                return triangleRemove;
            case HEXAGON:
                // FIXED: Hexagon packets have 2 health points, so they can take 1 noise and should be removed when they reach 2 noise
                boolean hexagonRemove = packet.getNoiseLevel() > 2; // Hexagon packets can take 2 noises (was >= 1, now > 2)
                System.out.println("DDoS: Hexagon packet noise check - Level: " + packet.getNoiseLevel() + ", Remove: " + hexagonRemove);
                return hexagonRemove;
            case PROTECTED:
                // This should not happen since we check for ProtectedPacket above, but handle gracefully
                System.out.println("DDoS: WARNING - PROTECTED type reached switch statement, treating as safe");
                return false;
            default:
                return false;
        }
    }
    
    /**
     * Convert packet to trojan packet
     */
    private void convertToTrojanPacket(Packet packet) {
        // Skip if packet is already a trojan
        if (packet.isTrojan()) {
            System.out.println("DDoS: Packet " + packet.getId() + " is already a trojan, skipping conversion");
            return;
        }
        
        // Convert the packet to trojan using the new functionality
        packet.convertToTrojan();
        
        // Add additional DDoS-specific trojan properties
        packet.setProperty("ddosInfected", true);
        packet.setProperty("ddosInfectionTime", System.currentTimeMillis());
        
        System.out.println("DDoS: 🦠 Successfully converted packet " + packet.getId() + " to TROJAN");
        System.out.println("DDoS:   - Original type: " + packet.getOriginalType());
        System.out.println("DDoS:   - Current type: " + packet.getType());
        System.out.println("DDoS:   - Health: " + packet.getHealth() + "/" + packet.getSize());
    }
    
    /**
     * Send packet to an incompatible port (opposite of normal routing)
     */
    private boolean sendToIncompatiblePort(Packet packet) {
        System.out.println("DDoS: sendToIncompatiblePort called for packet " + packet.getId() + " (type: " + packet.getType() + ")");
        
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager == null) {
            System.out.println("DDoS: PacketManager is null, cannot send packet");
            return false;
        }
        
        System.out.println("DDoS: Available output ports: " + outputPorts.size());
        for (int i = 0; i < outputPorts.size(); i++) {
            Port port = outputPorts.get(i);
            System.out.println("DDoS: Port " + i + " - Type: " + port.getType() + ", Has connection: " + (port.getConnection() != null) + 
                              ", Connection empty: " + (port.getConnection() != null ? port.getConnection().isEmpty() : "N/A"));
        }
        
        // For protected packets, use the underlying type for compatibility checking
        Packet.PacketType packetTypeForCompatibility = packet.getType();
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            packetTypeForCompatibility = protectedPacket.getUnderlyingType();
            System.out.println("DDoS: Using underlying type " + packetTypeForCompatibility + " for protected packet compatibility");
        }
        
        System.out.println("DDoS: Looking for incompatible ports...");
        // Find an incompatible port (different from packet type)
        for (Port outputPort : outputPorts) {
            System.out.println("DDoS: Checking port - Type: " + outputPort.getType() + 
                              ", Incompatible: " + (outputPort.getType() != packetTypeForCompatibility) + 
                              ", Has connection: " + (outputPort.getConnection() != null) + 
                              ", Connection empty: " + (outputPort.getConnection() != null ? outputPort.getConnection().isEmpty() : "N/A"));
            
            if (outputPort.getType() != packetTypeForCompatibility && outputPort.getConnection() != null && outputPort.getConnection().isEmpty()) {
                System.out.println("DDoS: Found incompatible port! Sending packet " + packet.getId() + 
                                  " from " + packet.getType() + " to " + outputPort.getType() + " port");
                
                // Add debug properties to track DDoS packets
                packet.setProperty("originalType", packet.getType().toString());
                packet.setProperty("exitPortType", outputPort.getType().toString());
                packet.setProperty("fromIncompatiblePort", true);
                
                // Simply add packet to connection like a normal system
                Connection connection = outputPort.getConnection();
                connection.addPacket(packet);
                
                System.out.println("DDoS: Successfully sent packet " + packet.getId() + " to incompatible port");
                return true;
            }
        }
        
        // Fallback: use first available port
        for (Port outputPort : outputPorts) {
            if (outputPort.getConnection() != null && outputPort.getConnection().isEmpty()) {
                System.out.println("DDoS: No incompatible ports available, using fallback port: " + outputPort.getType());
                
                // Add debug properties for fallback routing too
                packet.setProperty("originalType", packet.getType().toString());
                packet.setProperty("exitPortType", outputPort.getType().toString());
                packet.setProperty("fromIncompatiblePort", false);
                
                Connection connection = outputPort.getConnection();
                connection.addPacket(packet);
                return true;
            }
        }
        
        System.out.println("DDoS: No available output ports found");
        return false;
    }
    
    @Override
    public boolean isStartSystem() {
        return false;
    }
    
    @Override
    public boolean isEndSystem() {
        return false;
    }
    
    @Override
    public boolean isFixedPosition() {
        return false;
    }
    
    @Override
    public boolean shouldShowCapacityLabel() {
        return true;
    }
    
    @Override
    public boolean shouldShowInnerBox() {
        return true;
    }
    
    @Override
    public boolean shouldShowPlayButton() {
        return false;
    }
    
    @Override
    public String getSystemStyleClass() {
        return "ddos-system";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return "DDoS";
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 0;
    }
    
    @Override
    public boolean isDraggable() {
        return false;
    }
    
    @Override
    public double getPreferredWidth(double defaultWidth) {
        return defaultWidth;
    }
    
    @Override
    public double getPreferredHeight(double defaultHeight) {
        return defaultHeight;
    }
    
    @Override
    public boolean shouldPositionPacketsInInnerBox() {
        return true;
    }
    
    @Override
    public boolean arePortsCorrectlyConnected() {
        return areAllPortsCorrectlyConnected();
    }
    
    @Override
    public boolean canProcessDeliveredPackets() {
        return true;
    }
    
    @Override
    public boolean needsPacketTransferTimeline() {
        return true;
    }
    
    @Override
    public boolean canGeneratePackets() {
        return true;
    }
    
    @Override
    public boolean canTransferStoredPackets() {
        return true;
    }
    
    @Override
    public String getPortArrangementStrategy() {
        return "INPUT_LEFT_OUTPUT_RIGHT";
    }
} 
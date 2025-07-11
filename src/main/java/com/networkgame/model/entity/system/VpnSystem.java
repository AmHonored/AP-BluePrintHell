package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.packettype.messenger.ProtectedPacket;
import com.networkgame.model.state.GameState;
import com.networkgame.model.manager.PacketManager;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 * VpnSystem - A system that provides VPN functionality to encrypt and protect packet transmissions
 * 
 * Key Features:
 * - Converts incoming packets to protected packets
 * - Fails when high-speed packets (≥100) enter the system
 * - When failed, reverts all protected packets it created back to original types
 * - Uses cyan blue indicator lamp to show VPN status
 */
public class VpnSystem extends BaseSystem {
    
    // Constants for VPN operation
    private static final double HIGH_SPEED_THRESHOLD = 100.0; // VPN fails at this speed or higher
    private static final String VPN_CONVERTED_PROPERTY = "vpnConvertedBy"; // Property to track which VPN converted a packet
    
    // VPN state
    private boolean isFailed = false;
    private final Set<Integer> convertedPacketIds = new HashSet<>(); // Track packets we converted
    
    public VpnSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
        this.label = "VPN System";
    }
    
    @Override
    protected void initializeSystem() {
        // Initialize VPN system specific components
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // Update VPN-specific logic
        updateFailureStatus();
    }
    
    /**
     * Update VPN failure status and handle reversion if needed
     */
    private void updateFailureStatus() {
        if (isFailed) {
            // If we're failed, check if we should try to recover
            // For now, VPN stays failed until manually reset or repaired
            revertConvertedPackets();
        }
    }
    
    @Override
    public void handleOutputPortAvailable(Port port) {
        if (isFailed) {
            System.out.println("VPN: System is FAILED - cannot process packets");
            return;
        }
        
        // Handle output port availability for VPN system - try to send stored packets
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.transferPacket();
        }
    }
    
    @Override
    public void receivePacket(Packet packet) {
        System.out.println("=== VPN SYSTEM: Packet " + packet.getId() + " (" + packet.getType() + ") ENTERING ===");
        System.out.println("VPN: Packet speed: " + packet.getSpeed() + ", VPN status: " + (isFailed ? "FAILED" : "ACTIVE"));
        
        // Skip if this is a reference system
        if (isReference()) {
            System.out.println("VPN: Skipping reference system");
            return;
        }
        
        // Clear the connection reference
        if (packet.getCurrentConnection() != null) {
            System.out.println("VPN: Clearing connection reference");
            packet.setCurrentConnection(null);
        }
        
        // Check if packet speed causes VPN failure
        if (packet.getSpeed() >= HIGH_SPEED_THRESHOLD) {
            System.out.println("VPN: HIGH SPEED PACKET DETECTED! Speed " + packet.getSpeed() + " >= " + HIGH_SPEED_THRESHOLD);
            triggerVpnFailure();
            System.out.println("VPN: High-speed packet passes through failed system");
            super.receivePacket(packet);
            return;
        }
        
        // If VPN is already failed, packets pass through without conversion
        if (isFailed) {
            System.out.println("VPN: System FAILED - packet passes through without protection");
            super.receivePacket(packet);
            return;
        }
        
        // Check if this is already a protected packet - if so, just pass it through
        if (packet instanceof ProtectedPacket) {
            System.out.println("VPN: Protected packet detected - passing through normally");
            super.receivePacket(packet);
            return;
        }
        
        // Debug: Show packet type detection
        System.out.println("🔍 VPN DEBUG: Packet type = " + packet.getType());
        System.out.println("🔍 VPN DEBUG: Is PENTAGON? " + (packet.getType() == Packet.PacketType.PENTAGON));
        System.out.println("🔍 VPN DEBUG: Packet class = " + packet.getClass().getSimpleName());
        
        // Convert regular messenger packets to protected packets
        if (packet.getType() == Packet.PacketType.SQUARE || 
            packet.getType() == Packet.PacketType.TRIANGLE || 
            packet.getType() == Packet.PacketType.HEXAGON) {
            
            System.out.println("VPN: Converting " + packet.getType() + " packet to protected packet");
            convertToProtectedPacket(packet);
        } 
        // Convert Pentagon packets to Circle packets (secret enhancement)
        else if (packet.getType() == Packet.PacketType.PENTAGON) {
            System.out.println("🔥 VPN: PENTAGON DETECTED! Converting to Circle packet (secret enhancement)");
            convertPentagonToCirclePacket(packet);
        } else {
            System.out.println("⚠️ VPN: No conversion for " + packet.getType() + " - processing normally");
            // For other packet types, process normally
            super.receivePacket(packet);
        }
        
        System.out.println("=== VPN SYSTEM: Packet processing COMPLETE ===");
    }
    
    /**
     * Trigger VPN system failure due to high-speed packet
     */
    private void triggerVpnFailure() {
        if (!isFailed) {
            isFailed = true;
            System.out.println("🚨 VPN SYSTEM FAILURE! Caused by high-speed packet");
            
            // Revert all packets this VPN has converted
            revertConvertedPackets();
            
            // Update indicator lamp to show failure
            updateIndicatorLamp();
        }
    }
    
    /**
     * Revert all protected packets that were converted by this VPN system
     */
    private void revertConvertedPackets() {
        if (gameState == null || convertedPacketIds.isEmpty()) {
            return;
        }
        
        System.out.println("VPN: Reverting " + convertedPacketIds.size() + " protected packets back to original types");
        
        // Find and revert all protected packets created by this VPN
        Iterator<Packet> packetIterator = gameState.getActivePackets().iterator();
        while (packetIterator.hasNext()) {
            Packet packet = packetIterator.next();
            
            if (packet instanceof ProtectedPacket && 
                packet.hasProperty(VPN_CONVERTED_PROPERTY) &&
                packet.getProperty(VPN_CONVERTED_PROPERTY, "").equals(getSystemId())) {
                
                ProtectedPacket protectedPacket = (ProtectedPacket) packet;
                System.out.println("VPN: Reverting protected packet " + packet.getId() + 
                                 " back to " + protectedPacket.getUnderlyingType() + 
                                 " (was disguised as " + protectedPacket.getDisguiseMovementType() + ")");
                
                // Create new packet of original type at the same position
                Packet revertedPacket = createPacketOfType(protectedPacket.getUnderlyingType(), packet.getPosition());
                
                if (revertedPacket != null) {
                    // Copy relevant properties
                    revertedPacket.setProperty("revertedFromProtected", true);
                    revertedPacket.setProperty("originalProtectedId", packet.getId());
                    revertedPacket.setProperty("originalDisguiseType", protectedPacket.getDisguiseMovementType().toString());
                    
                    // Copy connection state if the packet was moving through a connection
                    Connection currentConnection = packet.getCurrentConnection();
                    if (currentConnection != null) {
                        System.out.println("VPN: Reverted packet was moving through a connection - transferring state");
                        
                        // Copy connection and movement properties
                        revertedPacket.setCurrentConnection(currentConnection);
                        if (packet.hasProperty("progress")) {
                            revertedPacket.setProperty("progress", packet.getProperty("progress"));
                        }
                        
                        // Copy velocity and position
                        double[] unitVector = packet.getUnitVector();
                        revertedPacket.setUnitVector(unitVector[0], unitVector[1]);
                        
                        // Adjust speed based on compatibility with current port
                        adjustRevertedPacketSpeed(revertedPacket, currentConnection, protectedPacket.getDisguiseMovementType());
                        
                        // Remove the protected packet from the connection and add the reverted one
                        currentConnection.removePacket(packet);
                        currentConnection.addPacket(revertedPacket);
                    }
                    
                    // Remove the protected packet and add the reverted one
                    packetIterator.remove();
                    gameState.addActivePacket(revertedPacket);
                    
                    System.out.println("VPN: Successfully reverted packet " + packet.getId() + 
                                     " to " + revertedPacket.getType() + " packet " + revertedPacket.getId());
                }
            }
        }
        
        // Clear the tracking set
        convertedPacketIds.clear();
    }
    
    /**
     * Adjust the speed of a reverted packet based on its compatibility with the current connection
     */
    private void adjustRevertedPacketSpeed(Packet revertedPacket, Connection connection, Packet.PacketType originalDisguiseType) {
        Port targetPort = connection.getTargetPort();
        Packet.PacketType revertedType = revertedPacket.getType();
        Packet.PacketType portType = targetPort.getType();
        
        // Check if the reverted packet type is compatible with the target port
        boolean wasCompatibleAsDisguise = (originalDisguiseType == portType);
        boolean nowCompatibleAsOriginal = (revertedType == portType);
        
        System.out.println("VPN: Speed adjustment for reverted packet " + revertedPacket.getId() + ":");
        System.out.println("  - Original disguise: " + originalDisguiseType + " (was compatible: " + wasCompatibleAsDisguise + ")");
        System.out.println("  - Reverted to: " + revertedType + " (now compatible: " + nowCompatibleAsOriginal + ")");
        System.out.println("  - Target port: " + portType);
        
        // Apply speed based on the reverted packet type and port compatibility
        double newSpeed = calculateSpeedForPacketType(revertedType, nowCompatibleAsOriginal);
        revertedPacket.setSpeed(newSpeed);
        
        // If compatibility changed, log the impact
        if (wasCompatibleAsDisguise != nowCompatibleAsOriginal) {
            if (nowCompatibleAsOriginal) {
                System.out.println("VPN: Packet became COMPATIBLE after reversion - speed reduced to " + newSpeed);
            } else {
                System.out.println("VPN: Packet became INCOMPATIBLE after reversion - speed increased to " + newSpeed);
            }
        }
    }
    
    /**
     * Calculate the appropriate speed for a packet type based on port compatibility
     */
    private double calculateSpeedForPacketType(Packet.PacketType packetType, boolean compatible) {
        switch (packetType) {
            case SQUARE:
                return compatible ? 50.0 : 100.0; // Slower when compatible, faster when incompatible
            case TRIANGLE:
                return compatible ? 80.0 : 120.0; // Triangle is generally faster
            case HEXAGON:
                return compatible ? 100.0 : 70.0; // Hexagon is slower when incompatible
            default:
                return 60.0; // Default speed
        }
    }
    
    /**
     * Create a packet of the specified type at the given position
     */
    private Packet createPacketOfType(Packet.PacketType type, Point2D position) {
        switch (type) {
            case SQUARE:
                return new com.networkgame.model.entity.packettype.messenger.SquarePacket(position);
            case TRIANGLE:
                return new com.networkgame.model.entity.packettype.messenger.TrianglePacket(position);
            case HEXAGON:
                return new com.networkgame.model.entity.packettype.messenger.HexagonPacket(position);
            default:
                return null;
        }
    }
    
    /**
     * Converts a regular messenger packet to a protected packet.
     * The new protected packet will have the original packet as its underlying type.
     */
    private void convertToProtectedPacket(Packet originalPacket) {
        // Create a new protected packet at the same position with the original packet type as underlying
        ProtectedPacket protectedPacket = new ProtectedPacket(originalPacket.getPosition(), originalPacket.getType());
        
        // Copy relevant properties from the original packet
        protectedPacket.setProperty("convertedFrom", originalPacket.getType().toString());
        protectedPacket.setProperty("originalPacketId", originalPacket.getId());
        protectedPacket.setProperty(VPN_CONVERTED_PROPERTY, getSystemId()); // Track which VPN converted this
        
        // Track this packet for potential reversion
        convertedPacketIds.add(protectedPacket.getId());
        
        // Remove the original packet from the game
        if (gameState != null) {
            gameState.getActivePackets().remove(originalPacket);
        }
        
        // Add the new protected packet to the game
        if (gameState != null && !gameState.getActivePackets().contains(protectedPacket)) {
            gameState.addActivePacket(protectedPacket);
        }
        
        System.out.println("VPN: Created protected packet " + protectedPacket.getId() + 
                         " from " + originalPacket.getType() + " packet " + originalPacket.getId() +
                         " (underlying type: " + protectedPacket.getUnderlyingType() + 
                         ", disguise movement: " + protectedPacket.getDisguiseMovementType() + ")");
        
        // Immediately route the protected packet to the appropriate output port based on its disguise type
        routeProtectedPacketToOutput(protectedPacket);
    }
    
    /**
     * Converts a Pentagon packet to a Circle packet (secret VPN enhancement).
     * The new Circle packet will have enhanced preservation behavior.
     */
    private void convertPentagonToCirclePacket(Packet originalPentagonPacket) {
        // Create a new Circle packet at the same position
        com.networkgame.model.entity.packettype.secret.CirclePacket circlePacket = 
            new com.networkgame.model.entity.packettype.secret.CirclePacket(originalPentagonPacket.getPosition());
        
        // Copy relevant properties from the original Pentagon packet
        circlePacket.setProperty("convertedFrom", "Pentagon");
        circlePacket.setProperty("originalPacketId", originalPentagonPacket.getId());
        circlePacket.setProperty("vpnEnhanced", true);
        circlePacket.setProperty("preservationMode", true);
        circlePacket.setProperty("gameState", gameState); // CRITICAL: Set gameState for analyzing other packets
        circlePacket.setProperty(VPN_CONVERTED_PROPERTY, getSystemId()); // Track which VPN converted this
        
        // Track this packet for potential reversion (if VPN fails)
        convertedPacketIds.add(circlePacket.getId());
        
        // Remove the original Pentagon packet from the game
        if (gameState != null) {
            gameState.getActivePackets().remove(originalPentagonPacket);
        }
        
        // Add the new Circle packet to the game
        if (gameState != null && !gameState.getActivePackets().contains(circlePacket)) {
            gameState.addActivePacket(circlePacket);
        }
        
        System.out.println("VPN: Created Circle packet " + circlePacket.getId() + 
                         " from Pentagon packet " + originalPentagonPacket.getId() +
                         " (enhanced preservation behavior enabled)");
        
        // Initialize the adaptive preservation distance based on current network traffic
        circlePacket.initializeTargetPreservationDistance();
        System.out.println("VPN: Initialized adaptive preservation distance for Circle packet " + circlePacket.getId());
        
        // Route the Circle packet to an available Square output port (Circle packets use Square ports)
        routeCirclePacketToOutput(circlePacket);
    }
    
    /**
     * Route the Circle packet to an available Square output port (Circle packets are compatible with Square ports)
     */
    private void routeCirclePacketToOutput(com.networkgame.model.entity.packettype.secret.CirclePacket circlePacket) {
        // Find an available Square output port (Circle packets use Square ports)
        Port targetPort = null;
        for (Port outputPort : outputPorts) {
            if (outputPort.getType() == Packet.PacketType.SQUARE && 
                outputPort.isConnected() && 
                outputPort.getConnection().isEmpty()) {
                targetPort = outputPort;
                break;
            }
        }
        
        if (targetPort != null) {
            System.out.println("VPN: Routing Circle packet " + circlePacket.getId() + 
                             " to Square output port (Circle packets use Square ports)");
            
            // Set packet position at the output port
            circlePacket.setPosition(targetPort.getPosition());
            
            // Send packet directly to the connection
            targetPort.getConnection().addPacket(circlePacket);
        } else {
            System.out.println("VPN: No available Square output port for Circle packet, storing temporarily");
            
            // Fallback: store the packet if no appropriate port is available
            PacketManager packetManager = managerRegistry.get(PacketManager.class);
            if (packetManager != null) {
                packetManager.receivePacket(circlePacket);
            }
        }
    }
    
    /**
     * Route the protected packet to the appropriate output port based on its disguise movement type
     */
    private void routeProtectedPacketToOutput(ProtectedPacket protectedPacket) {
        // Find the output port that matches the disguise movement type
        Port targetPort = null;
        for (Port outputPort : outputPorts) {
            if (outputPort.getType() == protectedPacket.getDisguiseMovementType() && 
                outputPort.isConnected() && 
                outputPort.getConnection().isEmpty()) {
                targetPort = outputPort;
                break;
            }
        }
        
        if (targetPort != null) {
            System.out.println("VPN: Routing protected packet " + protectedPacket.getId() + 
                             " (disguise: " + protectedPacket.getDisguiseMovementType() + 
                             ") to " + targetPort.getType() + " output port");
            
            // Set packet position at the output port
            protectedPacket.setPosition(targetPort.getPosition());
            
            // Send packet directly to the connection
            targetPort.getConnection().addPacket(protectedPacket);
        } else {
            System.out.println("VPN: No available output port for disguise type " + 
                             protectedPacket.getDisguiseMovementType() + 
                             ", storing packet temporarily");
            
            // Fallback: store the packet if no appropriate port is available
            PacketManager packetManager = managerRegistry.get(PacketManager.class);
            if (packetManager != null) {
                packetManager.receivePacket(protectedPacket);
            }
        }
    }
    
    /**
     * Check if the VPN system has failed
     */
    public boolean isFailed() {
        return isFailed;
    }
    
    /**
     * Reset the VPN system (for manual repair or level restart)
     */
    public void resetVpnSystem() {
        isFailed = false;
        convertedPacketIds.clear();
        updateIndicatorLamp();
        System.out.println("VPN: System RESET - VPN is now operational");
    }
    
    /**
     * Get a unique identifier for this VPN system
     */
    private String getSystemId() {
        return getLabel() + "@" + hashCode();
    }
    
    /**
     * Override indicator lamp behavior to show VPN status with cyan blue
     */
    @Override
    public boolean isIndicatorOn() {
        // VPN indicator shows cyan when operational, red when failed
        return !isFailed && super.isIndicatorOn();
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
        return "vpn-system";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return "VPN";
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 0;
    }
    
    @Override
    public boolean isDraggable() {
        return false; // VPN systems are undraggable for security
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
        return true; // VPN systems can generate protected packets
    }
    
    @Override
    public boolean canTransferStoredPackets() {
        return !isFailed; // Cannot transfer when failed
    }
    
    @Override
    public String getPortArrangementStrategy() {
        return "INPUT_LEFT_OUTPUT_RIGHT";
    }
} 
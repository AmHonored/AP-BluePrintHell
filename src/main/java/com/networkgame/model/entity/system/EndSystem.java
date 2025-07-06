package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.state.GameState;
import com.networkgame.model.manager.PacketManager;

/**
 * Represents an end system that receives packets from the network and processes them.
 * End systems are the destination for packets in the network.
 */
public class EndSystem extends BaseSystem {
    
    public EndSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
    }
    
    @Override
    protected void initializeSystem() {
        // End systems don't need packet transfer timeline since they only receive packets
        // No additional initialization needed for end systems
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // Special update for end systems: check if we need to process delivered packets
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.processDeliveredPacket();
        }
    }
    
    @Override
    public boolean isStartSystem() {
        return false;
    }
    
    @Override
    public boolean isEndSystem() {
        return true;
    }
    
    @Override
    public boolean isFixedPosition() {
        return true; // End systems are typically fixed in position
    }
    
    /**
     * End systems don't send packets to other systems
     */
    @Override
    public void startSendingPackets(double interval) {
        // End systems don't send packets
    }
    
    /**
     * End systems don't need to stop sending packets since they don't send
     */
    @Override
    public void stopSendingPackets() {
        // End systems don't send packets
    }
    
    /**
     * Override the active status update for end systems
     */
    @Override
    protected void updateActiveStatus() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        
        if (packetManager != null) {
            // End system is active if it has packets to process
            isActive = !packetManager.getPackets().isEmpty();
        }
    }
    
    /**
     * Process stored packets after network changes - end systems process delivered packets
     */
    @Override
    public void processStoredPacketsAfterNetworkChange() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            // Process any delivered packets
            packetManager.processDeliveredPacket();
        }
    }
    
    // System-specific rendering behavior
    @Override
    public boolean shouldShowCapacityLabel() {
        return false; // End systems don't show capacity
    }
    
    @Override
    public boolean shouldShowInnerBox() {
        return false; // End systems don't have inner boxes
    }
    
    @Override
    public boolean shouldShowPlayButton() {
        return false; // End systems don't have play buttons
    }
    
    @Override
    public String getSystemStyleClass() {
        return "system-end";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return "END";
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 15.0; // Offset for "END" text
    }
    
    @Override
    public boolean isDraggable() {
        return false; // End systems are usually fixed position
    }
    
    // System-specific visual management behavior
    @Override
    public double getPreferredWidth(double defaultWidth) {
        return defaultWidth; // End systems use default width
    }
    
    @Override
    public double getPreferredHeight(double defaultHeight) {
        return defaultHeight; // End systems use default height
    }
    
    @Override
    public boolean shouldPositionPacketsInInnerBox() {
        return false; // End systems don't position packets in inner box
    }
    
    @Override
    public boolean arePortsCorrectlyConnected() {
        // End system only needs input ports connected
        return !getInputPorts().isEmpty() && areAllPortsCorrectlyConnected();
    }
    
    // System-specific manager behavior
    @Override
    public boolean canProcessDeliveredPackets() {
        return true; // End systems process delivered packets
    }
    
    @Override
    public boolean needsPacketTransferTimeline() {
        return false; // End systems don't need transfer timeline
    }
    
    @Override
    public boolean canGeneratePackets() {
        return false; // End systems don't generate packets
    }
    
    @Override
    public boolean canTransferStoredPackets() {
        return false; // End systems don't transfer stored packets
    }
    
    @Override
    public String getPortArrangementStrategy() {
        return "input-only"; // End systems only have input ports
    }
} 
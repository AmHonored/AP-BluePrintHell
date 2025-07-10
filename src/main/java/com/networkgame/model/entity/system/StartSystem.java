package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.state.GameState;
import com.networkgame.model.manager.TimelineManager;
import com.networkgame.model.manager.PacketManager;

/**
 * Represents a start system that generates packets and sends them to the network.
 * Start systems are the source of packets in the network.
 */
public class StartSystem extends BaseSystem {
    
    public StartSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
    }
    
    @Override
    protected void initializeSystem() {
        // Start systems don't need packet transfer timeline since they only generate packets
        // No additional initialization needed for start systems
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // No specific updates needed for start systems beyond base functionality
        // Packet generation is handled by TimelineManager
    }
    
    @Override
    public boolean isStartSystem() {
        return true;
    }
    
    @Override
    public boolean isEndSystem() {
        return false;
    }
    
    @Override
    public boolean isFixedPosition() {
        return false; // Start systems can be moved
    }
    
    /**
     * Start systems don't receive packets from connections
     */
    @Override
    public void receivePacket(com.networkgame.model.entity.Packet packet) {
        // Start systems don't receive packets from the network
        // They only generate packets
    }
    
    /**
     * Override the active status update for start systems
     */
    @Override
    protected void updateActiveStatus() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        
        if (packetManager != null && timelineManager != null) {
            // Start system is active if it's generating packets
            if (timelineManager.getPacketGenerationTimeline() != null && 
                timelineManager.getPacketGenerationTimeline().getStatus() == javafx.animation.Animation.Status.RUNNING) {
                isActive = true;
            } else {
                isActive = false;
            }
        }
    }
    
    /**
     * Process stored packets after network changes - start systems don't store packets
     */
    @Override
    public void processStoredPacketsAfterNetworkChange() {
        // Start systems don't store packets, so no action needed
    }
    
    // System-specific rendering behavior
    @Override
    public boolean shouldShowCapacityLabel() {
        return false; // Start systems don't show capacity
    }
    
    @Override
    public boolean shouldShowInnerBox() {
        return false; // Start systems don't have inner boxes
    }
    
    @Override
    public boolean shouldShowPlayButton() {
        return true; // Start systems have play buttons
    }
    
    @Override
    public String getSystemStyleClass() {
        return "system-start";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return "START";
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 25.0; // Offset for "START" text
    }
    
    @Override
    public boolean isDraggable() {
        return false; // Start systems are not draggable in test levels
    }
    
    // System-specific visual management behavior
    @Override
    public double getPreferredWidth(double defaultWidth) {
        return defaultWidth; // Start systems use default width
    }
    
    @Override
    public double getPreferredHeight(double defaultHeight) {
        return defaultHeight; // Start systems use default height
    }
    
    @Override
    public boolean shouldPositionPacketsInInnerBox() {
        return false; // Start systems don't position packets in inner box
    }
    
    @Override
    public boolean arePortsCorrectlyConnected() {
        // Start system only needs output ports connected
        return !getOutputPorts().isEmpty() && areAllPortsCorrectlyConnected();
    }
    
    // System-specific manager behavior
    @Override
    public boolean canProcessDeliveredPackets() {
        return false; // Start systems don't process delivered packets
    }
    
    @Override
    public boolean needsPacketTransferTimeline() {
        return false; // Start systems don't need transfer timeline
    }
    
    @Override
    public boolean canGeneratePackets() {
        return true; // Start systems generate packets
    }
    
    @Override
    public boolean canTransferStoredPackets() {
        return false; // Start systems don't transfer stored packets
    }
    
    @Override
    public String getPortArrangementStrategy() {
        return "output-only"; // Start systems only have output ports
    }
} 
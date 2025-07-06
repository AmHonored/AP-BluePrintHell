package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.state.GameState;
import com.networkgame.model.manager.PacketManager;
import com.networkgame.model.manager.TimelineManager;

/**
 * Represents an intermediate system that receives packets, stores them, and forwards them.
 * Intermediate systems include normal systems and storage systems.
 */
public class IntermediateSystem extends BaseSystem {
    
    public IntermediateSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
    }
    
    @Override
    protected void initializeSystem() {
        // Intermediate systems need packet transfer timeline to forward packets
        // But don't start it immediately - wait until first update cycle
        // This avoids circular dependency issues during initialization
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // Start the packet transfer timeline if not already running
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null && timelineManager.getPacketTransferTimeline() == null) {
            timelineManager.startPacketTransferTimeline();
        }
        
        // No additional specific updates needed for intermediate systems
        // Packet transfer is handled by TimelineManager
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
        return false; // Intermediate systems can be moved
    }
    
    /**
     * Override the active status update for intermediate systems
     */
    @Override
    protected void updateActiveStatus() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        
        if (packetManager != null && timelineManager != null) {
            // Intermediate system is active if it has packets to process or is transferring packets
            if (!packetManager.getPackets().isEmpty() || 
                (timelineManager.getPacketTransferTimeline() != null && 
                 timelineManager.getPacketTransferTimeline().getStatus() == javafx.animation.Animation.Status.RUNNING)) {
                isActive = true;
            } else {
                isActive = false;
            }
        }
    }
    
    /**
     * Process stored packets after network changes - intermediate systems forward packets
     */
    @Override
    public void processStoredPacketsAfterNetworkChange() {
        // Timeline-based approach handles this, but immediately check for packet transfers
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null && !packetManager.getPackets().isEmpty()) {
            packetManager.transferPacket();
        }
        
        // Mark system as active to ensure it gets updated in the next game cycle
        isActive = true;
    }
    
    // System-specific rendering behavior
    @Override
    public boolean shouldShowCapacityLabel() {
        return true; // Intermediate systems show capacity labels
    }
    
    @Override
    public boolean shouldShowInnerBox() {
        return true; // Intermediate systems have inner boxes for packet storage
    }
    
    @Override
    public boolean shouldShowPlayButton() {
        return false; // Intermediate systems don't have play buttons
    }
    
    @Override
    public String getSystemStyleClass() {
        return isReference() ? "system-reference" : "system-normal";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return null; // Intermediate systems don't have display labels like "START"/"END"
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 0.0; // Not used since no display label
    }
    
    @Override
    public boolean isDraggable() {
        return true; // Intermediate systems can be moved
    }
    
    // System-specific visual management behavior
    @Override
    public double getPreferredWidth(double defaultWidth) {
        return 80.0; // Intermediate systems use consistent width
    }
    
    @Override
    public double getPreferredHeight(double defaultHeight) {
        return 100.0; // Intermediate systems use consistent height
    }
    
    @Override
    public boolean shouldPositionPacketsInInnerBox() {
        return true; // Intermediate systems position packets in inner box
    }
    
    @Override
    public boolean arePortsCorrectlyConnected() {
        // Intermediate system needs both input and output ports connected
        return !getInputPorts().isEmpty() && 
               !getOutputPorts().isEmpty() && 
               areAllPortsCorrectlyConnected();
    }
    
    // System-specific manager behavior
    @Override
    public boolean canProcessDeliveredPackets() {
        return false; // Intermediate systems don't process delivered packets
    }
    
    @Override
    public boolean needsPacketTransferTimeline() {
        return true; // Intermediate systems need transfer timeline
    }
    
    @Override
    public boolean canGeneratePackets() {
        return false; // Intermediate systems don't generate packets
    }
    
    @Override
    public boolean canTransferStoredPackets() {
        return true; // Intermediate systems transfer stored packets
    }
    
    @Override
    public String getPortArrangementStrategy() {
        return "bidirectional"; // Intermediate systems have both input and output ports
    }
} 
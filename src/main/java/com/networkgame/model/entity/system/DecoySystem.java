package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.Port;
import com.networkgame.model.state.GameState;

/**
 * DecoySystem - A system that acts as a decoy to divert and confuse attacking packets
 */
public class DecoySystem extends BaseSystem {
    
    public DecoySystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
        this.label = "Decoy System";
    }
    
    @Override
    protected void initializeSystem() {
        // TODO: Initialize decoy system specific components
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // TODO: Implement decoy system specific update logic
    }
    
    @Override
    public void handleOutputPortAvailable(Port port) {
        // TODO: Handle output port availability for decoy system
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
        return "decoy-system";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return "Decoy";
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 0;
    }
    
    @Override
    public boolean isDraggable() {
        return !isReference;
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
        return false;
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
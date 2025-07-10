package com.networkgame.model.entity.system; 

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.state.GameState;
import com.networkgame.model.state.GameStateProvider;
import com.networkgame.model.manager.ManagerRegistry;
import com.networkgame.model.manager.PacketManager;
import com.networkgame.model.manager.VisualManager;
import com.networkgame.model.manager.TimelineManager;
import com.networkgame.model.manager.PortManager;
import com.networkgame.model.entity.system.BaseSystem;
import com.networkgame.model.entity.system.SystemFactory;

/**
 * Represents a network system in the game
 * This class now delegates functionality to specialized system classes
 * Implements interfaces to break circular dependencies
 */
public class NetworkSystem implements NetworkSystemProvider, NetworkSystemCallbacks {
    // The specialized system instance that this class delegates to
    private BaseSystem actualSystem;
    
    // Legacy fields for backward compatibility
    private boolean isStartSystem;
    private boolean isEndSystem;
    private boolean isSpySystem;
    private boolean isDdosSystem;
    
    public NetworkSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        this.isStartSystem = false;
        this.isEndSystem = false;
        this.isSpySystem = false;
        this.isDdosSystem = false;
        
        // Create the appropriate system type (default to intermediate)
        this.actualSystem = SystemFactory.createIntermediateSystem(position, width, height, isReference, gameState);
        
        // Initialize the managers with this NetworkSystem instance as the provider
        this.actualSystem.initializeManagers(this);
    }
    
    /**
     * Recreate the system with the appropriate type based on the current flags
     */
    private void recreateSystem() {
        if (actualSystem == null) return;
        
        // Store current state
        Point2D position = actualSystem.getPosition();
        double width = actualSystem.getWidth();
        double height = actualSystem.getHeight();
        boolean isReference = actualSystem.isReference();
        GameState gameState = actualSystem.getGameState();
        String label = actualSystem.getLabel();
        List<Port> inputPorts = new ArrayList<>(actualSystem.getInputPorts());
        List<Port> outputPorts = new ArrayList<>(actualSystem.getOutputPorts());
        
        // Create new system with appropriate type
        if (isSpySystem) {
            this.actualSystem = SystemFactory.createSpySystem(position, width, height, isReference, gameState);
        } else if (isDdosSystem) {
            this.actualSystem = SystemFactory.createDdosSystem(position, width, height, isReference, gameState);
        } else {
            this.actualSystem = SystemFactory.createSystem(position, width, height, isReference, gameState, isStartSystem, isEndSystem);
        }
        
        // Initialize the managers for the new system
        this.actualSystem.initializeManagers(this);
        
        // Restore state
        actualSystem.setLabel(label);
        for (Port port : inputPorts) {
            actualSystem.addInputPort(port);
        }
        for (Port port : outputPorts) {
            actualSystem.addOutputPort(port);
        }
    }
    
    /**
     * Check if all ports are correctly connected
     */
    public boolean areAllPortsCorrectlyConnected() {
        return actualSystem.areAllPortsCorrectlyConnected();
    }
    
    /**
     * Move the system by the specified delta
     */
    public void moveBy(double dx, double dy) {
        actualSystem.moveBy(dx, dy);
    }
    
    /**
     * Add an input port to the system
     */
    public void addInputPort(Port port) {
        actualSystem.addInputPort(port);
    }
    
    /**
     * Add an output port to the system
     */
    public void addOutputPort(Port port) {
        actualSystem.addOutputPort(port);
    }
    
    /**
     * Update the system state
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        actualSystem.update(deltaTime);
    }
    
    /**
     * Process all stored packets when an output port becomes available
     */
    public void handleOutputPortAvailable(Port port) {
        actualSystem.handleOutputPortAvailable(port);
    }

    /**
     * Process stored packets after network changes
     */
    public void processStoredPacketsAfterNetworkChange() {
        actualSystem.processStoredPacketsAfterNetworkChange();
    }
    
    /**
     * Receive a packet into this system
     */
    public void receivePacket(Packet packet) {
        actualSystem.receivePacket(packet);
    }
    
    /**
     * Start sending packets from this system
     * @param interval The interval between packet generation in seconds
     */
    public void startSendingPackets(double interval) {
        actualSystem.startSendingPackets(interval);
    }
    
    /**
     * Stop sending packets from this system
     */
    public void stopSendingPackets() {
        actualSystem.stopSendingPackets();
    }
    
    /**
     * Update packet generation speed based on game speed multiplier
     */
    public void updatePacketGenerationSpeed(double speedMultiplier) {
        actualSystem.updatePacketGenerationSpeed(speedMultiplier);
    }
    
    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        actualSystem.cleanup();
    }
    
    // Getters and setters
    public Rectangle getShape() {
        return actualSystem.getShape();
    }
    
    public Circle getIndicatorLamp() {
        return actualSystem.getIndicatorLamp();
    }
    
    public Rectangle getInnerBox() {
        return actualSystem.getInnerBox();
    }
    
    public Point2D getPosition() {
        return actualSystem.getPosition();
    }
    
    public double getWidth() {
        return actualSystem.getWidth();
    }
    
    public double getHeight() {
        return actualSystem.getHeight();
    }
    
    public List<Port> getInputPorts() {
        return actualSystem.getInputPorts();
    }
    
    public List<Port> getOutputPorts() {
        return actualSystem.getOutputPorts();
    }
    
    /**
     * Get combined list of all input and output ports
     */
    public List<Port> getAllPorts() {
        return actualSystem.getAllPorts();
    }
    
    public boolean isReference() {
        return actualSystem.isReference();
    }
    
    public boolean isActive() {
        return actualSystem.isActive();
    }
    
    public void setActive(boolean active) {
        actualSystem.setActive(active);
    }
    
    public boolean isStartSystem() {
        return actualSystem.isStartSystem();
    }
    
    public void setStartSystem(boolean isStartSystem) {
        this.isStartSystem = isStartSystem;
        // Recreate the system with the new type
        recreateSystem();
    }
    
    public boolean isEndSystem() {
        return actualSystem.isEndSystem();
    }
    
    public void setEndSystem(boolean isEndSystem) {
        this.isEndSystem = isEndSystem;
        // Recreate the system with the new type
        recreateSystem();
    }
    
    public boolean isSpySystem() {
        return this.isSpySystem;
    }
    
    public void setSpySystem(boolean isSpySystem) {
        this.isSpySystem = isSpySystem;
        // Recreate the system with the new type
        recreateSystem();
    }
    
    public boolean isDdosSystem() {
        return this.isDdosSystem;
    }
    
    public void setDdosSystem(boolean isDdosSystem) {
        this.isDdosSystem = isDdosSystem;
        // Recreate the system with the new type
        recreateSystem();
    }
    
    public List<Packet> getStoredPackets() {
        return actualSystem.getStoredPackets();
    }
    
    public boolean hasEmptyOutputPort() {
        return actualSystem.hasEmptyOutputPort();
    }
    
    public boolean isFull() {
        return actualSystem.isFull();
    }
    
    public GameState getGameState() {
        return actualSystem.getGameState();
    }
    
    public String getLabel() {
        return actualSystem.getLabel();
    }
    
    public void setLabel(String label) {
        actualSystem.setLabel(label);
    }
    
    public boolean isIndicatorOn() {
        return actualSystem.isIndicatorOn();
    }
    
    public void setPlayButton(Group playButton) {
        actualSystem.setPlayButton(playButton);
    }
    
    public Group getPlayButton() {
        return actualSystem.getPlayButton();
    }
    
    /**
     * Update the indicator lamp based on connection status
     */
    public void updateIndicatorLamp() {
        actualSystem.updateIndicatorLamp();
    }
    
    /**
     * Update the capacity visual representation
     */
    public void updateCapacityVisual() {
        actualSystem.updateCapacityVisual();
    }
    
    /**
     * Update the visibility of stored packets
     */
    public void updateStoredPacketsVisibility() {
        actualSystem.updateStoredPacketsVisibility();
    }

    /**
     * Process a delivered packet from this system if it's an end system
     */
    public Packet processDeliveredPacket() {
        return actualSystem.processDeliveredPacket();
    }
    
    /**
     * Get the current capacity used by the system
     */
    public int getCurrentCapacityUsed() {
        return actualSystem.getCurrentCapacityUsed();
    }
    
    // Manager getters
    public PacketManager getPacketManager() {
        return actualSystem.getPacketManager();
    }
    
    public VisualManager getVisualManager() {
        return actualSystem.getVisualManager();
    }
    
    public TimelineManager getTimelineManager() {
        return actualSystem.getTimelineManager();
    }
    
    public PortManager getPortManager() {
        return actualSystem.getPortManager();
    }

    /**
     * Return whether this system is at a fixed position (for end systems)
     */
    public boolean isFixedPosition() {
        return actualSystem.isFixedPosition();
    }

    // NetworkSystemProvider interface implementation
    @Override
    public GameStateProvider getGameStateProvider() {
        return actualSystem.getGameStateProvider();
    }

    // NetworkSystemCallbacks interface implementation
    @Override
    public void addStoredPacket(Packet packet) {
        actualSystem.addStoredPacket(packet);
    }
    
    @Override
    public void removeStoredPacket(Packet packet) {
        actualSystem.removeStoredPacket(packet);
    }
    
    @Override
    public void clearStoredPackets() {
        actualSystem.clearStoredPackets();
    }
    
    @Override
    public void setStorageCapacity(int capacity) {
        actualSystem.setStorageCapacity(capacity);
    }
    
    @Override
    public void setShape(Rectangle shape) {
        actualSystem.setShape(shape);
    }
    
    @Override
    public void setIndicatorLamp(Circle lamp) {
        actualSystem.setIndicatorLamp(lamp);
    }
    
    @Override
    public void setInnerBox(Rectangle innerBox) {
        actualSystem.setInnerBox(innerBox);
    }
    
    @Override
    public void updatePortPositions() {
        actualSystem.updatePortPositions();
    }
    
    @Override
    public void startPacketTransferTimeline() {
        actualSystem.startPacketTransferTimeline();
    }
    
    @Override
    public void stopPacketTransferTimeline() {
        actualSystem.stopPacketTransferTimeline();
    }
    
    // System-specific rendering behavior delegation
    public boolean shouldShowCapacityLabel() {
        return actualSystem.shouldShowCapacityLabel();
    }
    
    public boolean shouldShowInnerBox() {
        return actualSystem.shouldShowInnerBox();
    }
    
    public boolean shouldShowPlayButton() {
        return actualSystem.shouldShowPlayButton();
    }
    
    public String getSystemStyleClass() {
        return actualSystem.getSystemStyleClass();
    }
    
    public String getSystemDisplayLabel() {
        return actualSystem.getSystemDisplayLabel();
    }
    
    public double getSystemLabelOffset() {
        return actualSystem.getSystemLabelOffset();
    }
    
    public boolean isDraggable() {
        return actualSystem.isDraggable();
    }
    
    public boolean shouldTrackCapacity() {
        return actualSystem.shouldTrackCapacity();
    }
    
    // Visual management behavior delegation
    public double getPreferredWidth(double defaultWidth) {
        return actualSystem.getPreferredWidth(defaultWidth);
    }
    
    public double getPreferredHeight(double defaultHeight) {
        return actualSystem.getPreferredHeight(defaultHeight);
    }
    
    public boolean shouldPositionPacketsInInnerBox() {
        return actualSystem.shouldPositionPacketsInInnerBox();
    }
    
    public boolean arePortsCorrectlyConnected() {
        return actualSystem.arePortsCorrectlyConnected();
    }
    
    // Manager behavior delegation
    public boolean canProcessDeliveredPackets() {
        return actualSystem.canProcessDeliveredPackets();
    }
    
    public boolean needsPacketTransferTimeline() {
        return actualSystem.needsPacketTransferTimeline();
    }
    
    public boolean canGeneratePackets() {
        return actualSystem.canGeneratePackets();
    }
    
    public boolean canTransferStoredPackets() {
        return actualSystem.canTransferStoredPackets();
    }
    
    public String getPortArrangementStrategy() {
        return actualSystem.getPortArrangementStrategy();
    }
} 

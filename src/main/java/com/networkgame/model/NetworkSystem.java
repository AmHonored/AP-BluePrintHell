package com.networkgame.model;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Circle;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a network system in the game
 * This class now delegates functionality to specialized manager classes
 */
public class NetworkSystem {
    // Core properties
    private Point2D position;
    private double width;
    private double height;
    private List<Port> inputPorts;
    private List<Port> outputPorts;
    private boolean isReference;
    private boolean isActive;
    private boolean isStartSystem;
    private boolean isEndSystem;
    private String label = "System";
    private GameState gameState;
    
    // Manager classes
    private PacketManager packetManager;
    private VisualManager visualManager;
    private TimelineManager timelineManager;
    private PortManager portManager;
    
    // Field to keep track of the play button for start systems
    private Group playButton;
    
    public NetworkSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        this.position = position;
        this.isReference = isReference;
        this.isActive = false;
        this.gameState = gameState;
        this.isStartSystem = false;
        this.isEndSystem = false;
        
        // Store the original dimensions
        this.width = width;
        this.height = height;
        
        // Initialize lists
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        List<Packet> packets = new ArrayList<>();
        
        // Default storage capacity
        int storageCapacity = "Storage System".equals(this.label) ? 10 : 5;
        
        // Initialize manager classes
        this.visualManager = new VisualManager(this, position, width, height);
        this.packetManager = new PacketManager(this, packets, storageCapacity, gameState);
        this.timelineManager = new TimelineManager(this);
        this.portManager = new PortManager(this);
        
        // Start the packet transfer timeline if this is not a start or end system
        if (!isStartSystem && !isEndSystem) {
            timelineManager.startPacketTransferTimeline();
        }
    }
    
    /**
     * Check if all ports are correctly connected
     */
    public boolean areAllPortsCorrectlyConnected() {
        return portManager.areAllPortsCorrectlyConnected();
    }
    
    /**
     * Move the system by the specified delta
     */
    public void moveBy(double dx, double dy) {
        // Update position
        position = new Point2D(position.getX() + dx, position.getY() + dy);
        
        // Update visual elements
        visualManager.moveBy(dx, dy);
        
        // Update ports positions
        portManager.updatePortPositions();
    }
    
    /**
     * Add an input port to the system
     */
    public void addInputPort(Port port) {
        inputPorts.add(port);
        portManager.updatePortPositions(); // Reposition all ports when adding a new one
    }
    
    /**
     * Add an output port to the system
     */
    public void addOutputPort(Port port) {
        outputPorts.add(port);
        portManager.updatePortPositions(); // Reposition all ports when adding a new one
    }
    
    /**
     * Update the system state
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        // Skip processing if this is a reference system (template)
        if (isReference) {
            return;
        }
        
        // Update indicator lamp based on active state
        visualManager.updateIndicatorLamp();
        
        // Special update for end systems: check if we need to process delivered packets
        if (isEndSystem) {
            packetManager.processDeliveredPacket();
        }
        
        // Always check for output port availability to ensure packet flow
        for (Port port : outputPorts) {
            Connection connection = port.getConnection();
            if (connection != null && connection.isEmpty() && !packetManager.getPackets().isEmpty()) {
                handleOutputPortAvailable(port);
            }
        }
        
        // Mark system as inactive if it has no more work to do
        if (packetManager.getPackets().isEmpty() && 
            (!isStartSystem || (timelineManager.getPacketGenerationTimeline() == null || 
                              timelineManager.getPacketGenerationTimeline().getStatus() != javafx.animation.Animation.Status.RUNNING))) {
            isActive = false;
        }
    }
    
    /**
     * Process all stored packets when an output port becomes available
     */
    public void handleOutputPortAvailable(Port port) {
        // Timeline-based approach handles this, but implement for compatibility
        if (timelineManager.getPacketTransferTimeline() != null && 
            timelineManager.getPacketTransferTimeline().getStatus() == javafx.animation.Animation.Status.RUNNING) {
            // The timeline will handle packet transfers
            return;
        }
        
        // If timeline isn't running for some reason, manually trigger a transfer
        packetManager.transferPacket();
    }

    /**
     * Process stored packets after network changes
     */
    public void processStoredPacketsAfterNetworkChange() {
        // Timeline-based approach handles this, but immediately check for packet transfers
        if (!isStartSystem && !isEndSystem && !packetManager.getPackets().isEmpty()) {
            packetManager.transferPacket();
        }
        
        // Mark system as active to ensure it gets updated in the next game cycle
        isActive = true;
    }
    
    /**
     * Receive a packet into this system
     */
    public void receivePacket(Packet packet) {
        packetManager.receivePacket(packet);
    }
    
    /**
     * Start sending packets from this system
     * @param interval The interval between packet generation in seconds
     */
    public void startSendingPackets(double interval) {
        timelineManager.startSendingPackets(interval);
    }
    
    /**
     * Stop sending packets from this system
     */
    public void stopSendingPackets() {
        timelineManager.stopSendingPackets();
    }
    
    /**
     * Update packet generation speed based on game speed multiplier
     */
    public void updatePacketGenerationSpeed(double speedMultiplier) {
        timelineManager.updatePacketGenerationSpeed(speedMultiplier);
    }
    
    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        timelineManager.cleanup();
        packetManager.clearPackets();
        System.out.println("NetworkSystem resources cleaned up: " + this);
    }
    
    // Getters and setters
    public Rectangle getShape() {
        return visualManager.getShape();
    }
    
    public Circle getIndicatorLamp() {
        return visualManager.getIndicatorLamp();
    }
    
    public Rectangle getInnerBox() {
        return visualManager.getInnerBox();
    }
    
    public Point2D getPosition() {
        return position;
    }
    
    public double getWidth() {
        return width;
    }
    
    public double getHeight() {
        return height;
    }
    
    public List<Port> getInputPorts() {
        return inputPorts;
    }
    
    public List<Port> getOutputPorts() {
        return outputPorts;
    }
    
    public boolean isReference() {
        return isReference;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public boolean isStartSystem() {
        return isStartSystem;
    }
    
    public void setStartSystem(boolean isStartSystem) {
        this.isStartSystem = isStartSystem;
    }
    
    public boolean isEndSystem() {
        return isEndSystem;
    }
    
    public void setEndSystem(boolean isEndSystem) {
        this.isEndSystem = isEndSystem;
    }
    
    public List<Packet> getStoredPackets() {
        return packetManager.getPackets();
    }
    
    public boolean hasEmptyOutputPort() {
        return outputPorts.stream()
                .anyMatch(port -> port.isConnected() && port.getConnection().isEmpty());
    }
    
    public boolean isFull() {
        return packetManager.isFull();
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public boolean isIndicatorOn() {
        return visualManager.isIndicatorOn();
    }
    
    public void setPlayButton(Group playButton) {
        this.playButton = playButton;
    }
    
    public Group getPlayButton() {
        return playButton;
    }
    
    /**
     * Update the indicator lamp based on connection status
     * @deprecated Use visualManager.updateIndicatorLamp() directly
     */
    public void updateIndicatorLamp() {
        visualManager.updateIndicatorLamp();
    }
    
    /**
     * Update the capacity visual representation
     * @deprecated Use visualManager.updateCapacityVisual() directly
     */
    public void updateCapacityVisual() {
        visualManager.updateCapacityVisual();
    }
    
    /**
     * Update the visibility of stored packets
     * @deprecated Use visualManager.updateStoredPacketsVisibility() directly
     */
    public void updateStoredPacketsVisibility() {
        visualManager.updateStoredPacketsVisibility();
    }
    
    /**
     * Process a delivered packet (for end systems)
     * @return The packet that was processed, or null if no packet was processed
     * @deprecated Use packetManager.processDeliveredPacket() directly
     */
    public Packet processDeliveredPacket() {
        return packetManager.processDeliveredPacket();
    }
    
    /**
     * Get the current capacity used by stored packets
     * @return The current capacity used
     * @deprecated Use packetManager.getCurrentCapacityUsed() directly
     */
    public int getCurrentCapacityUsed() {
        return packetManager.getCurrentCapacityUsed();
    }
    
    // Manager getters
    public PacketManager getPacketManager() {
        return packetManager;
    }
    
    public VisualManager getVisualManager() {
        return visualManager;
    }
    
    public TimelineManager getTimelineManager() {
        return timelineManager;
    }
    
    public PortManager getPortManager() {
        return portManager;
    }
    
    /**
     * Determines if this system should be fixed in position (not draggable)
     * @return true if the system should be fixed, false otherwise
     */
    public boolean isFixedPosition() {
        // All systems should be fixed
        return true;
    }
} 
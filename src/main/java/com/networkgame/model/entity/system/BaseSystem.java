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
import com.networkgame.model.entity.system.NetworkSystemProvider;
import com.networkgame.model.entity.system.NetworkSystemCallbacks;

/**
 * Base class for all network system types.
 * Contains common functionality shared by StartSystem, EndSystem, and IntermediateSystem.
 */
public abstract class BaseSystem implements NetworkSystemProvider, NetworkSystemCallbacks {
    // Core properties
    protected Point2D position;
    protected double width;
    protected double height;
    protected List<Port> inputPorts;
    protected List<Port> outputPorts;
    protected boolean isReference;
    protected boolean isActive;
    protected String label = "System";
    protected GameState gameState;
    
    // Manager registry to break cyclic dependencies
    protected final ManagerRegistry managerRegistry;
    
    // Field to keep track of the play button for start systems
    protected Group playButton;
    
    protected BaseSystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        this.position = position;
        this.isReference = isReference;
        this.isActive = false;
        this.gameState = gameState;
        
        // Store the original dimensions
        this.width = width;
        this.height = height;
        
        // Initialize lists
        this.inputPorts = new ArrayList<>();
        this.outputPorts = new ArrayList<>();
        
        // Initialize manager registry
        this.managerRegistry = new ManagerRegistry();
        
        // Note: initializeSystem() is now called after managers are initialized
    }
    
    /**
     * Initialize the managers - called from NetworkSystem wrapper
     */
    public void initializeManagers(com.networkgame.model.entity.system.NetworkSystem networkSystem) {
        // Default storage capacity
        int storageCapacity = "Storage System".equals(this.label) ? 10 : 5;
        List<Packet> packets = new ArrayList<>();
        
        // Initialize manager classes and register them
        VisualManager visualManager = new VisualManager(networkSystem, position, width, height);
        managerRegistry.register(VisualManager.class, visualManager);
        
        PacketManager packetManager = new PacketManager(networkSystem, packets, storageCapacity, gameState);
        managerRegistry.register(PacketManager.class, packetManager);
        
        TimelineManager timelineManager = new TimelineManager(networkSystem);
        managerRegistry.register(TimelineManager.class, timelineManager);
        
        PortManager portManager = new PortManager(networkSystem);
        managerRegistry.register(PortManager.class, portManager);
        
        // Now that managers are initialized, allow subclasses to perform additional initialization
        initializeSystem();
    }
    
    /**
     * Template method for subclasses to perform additional initialization
     */
    protected abstract void initializeSystem();
    
    /**
     * Check if all ports are correctly connected
     */
    public boolean areAllPortsCorrectlyConnected() {
        PortManager portManager = managerRegistry.get(PortManager.class);
        return portManager != null ? portManager.areAllPortsCorrectlyConnected() : false;
    }
    
    /**
     * Move the system by the specified delta
     */
    public void moveBy(double dx, double dy) {
        // Update position
        position = new Point2D(position.getX() + dx, position.getY() + dy);
        
        // Update visual elements
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        if (visualManager != null) {
            visualManager.moveBy(dx, dy);
        }
        
        // Update ports positions
        PortManager portManager = managerRegistry.get(PortManager.class);
        if (portManager != null) {
            portManager.updatePortPositions();
        }
    }
    
    /**
     * Add an input port to the system
     */
    public void addInputPort(Port port) {
        inputPorts.add(port);
        PortManager portManager = managerRegistry.get(PortManager.class);
        if (portManager != null) {
            portManager.updatePortPositions(); // Reposition all ports when adding a new one
        }
    }
    
    /**
     * Add an output port to the system
     */
    public void addOutputPort(Port port) {
        outputPorts.add(port);
        PortManager portManager = managerRegistry.get(PortManager.class);
        if (portManager != null) {
            portManager.updatePortPositions(); // Reposition all ports when adding a new one
        }
    }
    
    /**
     * Update the system state - template method that can be overridden
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        // Skip processing if this is a reference system (template)
        if (isReference) {
            return;
        }
        
        // Update indicator lamp based on active state
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        if (visualManager != null) {
            visualManager.updateIndicatorLamp();
        }
        
        // Allow subclasses to perform specific updates
        updateSystemSpecific(deltaTime);
        
        // Always check for output port availability to ensure packet flow
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            for (Port port : outputPorts) {
                Connection connection = port.getConnection();
                if (connection != null && connection.isEmpty() && !packetManager.getPackets().isEmpty()) {
                    handleOutputPortAvailable(port);
                }
            }
        }
        
        // Mark system as inactive if it has no more work to do
        updateActiveStatus();
    }
    
    /**
     * Template method for system-specific updates
     */
    protected abstract void updateSystemSpecific(double deltaTime);
    
    /**
     * Update the active status of the system
     */
    protected void updateActiveStatus() {
        PacketManager packetManagerForCheck = managerRegistry.get(PacketManager.class);
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (packetManagerForCheck != null && timelineManager != null) {
            if (packetManagerForCheck.getPackets().isEmpty() && 
                (!isStartSystem() || (timelineManager.getPacketGenerationTimeline() == null || 
                                  timelineManager.getPacketGenerationTimeline().getStatus() != javafx.animation.Animation.Status.RUNNING))) {
                isActive = false;
            }
        }
    }
    
    /**
     * Process all stored packets when an output port becomes available
     */
    public void handleOutputPortAvailable(Port port) {
        // Timeline-based approach handles this, but implement for compatibility
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null && timelineManager.getPacketTransferTimeline() != null && 
            timelineManager.getPacketTransferTimeline().getStatus() == javafx.animation.Animation.Status.RUNNING) {
            // The timeline will handle packet transfers
            return;
        }
        
        // If timeline isn't running for some reason, manually trigger a transfer
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.transferPacket();
        }
    }

    /**
     * Process stored packets after network changes
     */
    public void processStoredPacketsAfterNetworkChange() {
        // Timeline-based approach handles this, but immediately check for packet transfers
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (!isStartSystem() && !isEndSystem() && packetManager != null && !packetManager.getPackets().isEmpty()) {
            packetManager.transferPacket();
        }
        
        // Mark system as active to ensure it gets updated in the next game cycle
        isActive = true;
    }
    
    /**
     * Receive a packet into this system
     */
    public void receivePacket(Packet packet) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.receivePacket(packet);
        }
    }
    
    /**
     * Start sending packets from this system
     * @param interval The interval between packet generation in seconds
     */
    public void startSendingPackets(double interval) {
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null) {
            timelineManager.startSendingPackets(interval);
        }
    }
    
    /**
     * Stop sending packets from this system
     */
    public void stopSendingPackets() {
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null) {
            timelineManager.stopSendingPackets();
        }
    }
    
    /**
     * Update packet generation speed based on game speed multiplier
     */
    public void updatePacketGenerationSpeed(double speedMultiplier) {
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null) {
            timelineManager.updatePacketGenerationSpeed(speedMultiplier);
        }
    }
    
    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        
        if (timelineManager != null) {
            timelineManager.cleanup();
        }
        if (packetManager != null) {
            packetManager.clearPackets();
        }
    }
    
    // Getters and setters
    public Rectangle getShape() {
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        return visualManager != null ? visualManager.getShape() : null;
    }
    
    public Circle getIndicatorLamp() {
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        return visualManager != null ? visualManager.getIndicatorLamp() : null;
    }
    
    public Rectangle getInnerBox() {
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        return visualManager != null ? visualManager.getInnerBox() : null;
    }
    
    @Override
    public Point2D getPosition() {
        return position;
    }
    
    @Override
    public double getWidth() {
        return width;
    }
    
    @Override
    public double getHeight() {
        return height;
    }
    
    @Override
    public List<Port> getInputPorts() {
        return inputPorts;
    }
    
    @Override
    public List<Port> getOutputPorts() {
        return outputPorts;
    }
    
    /**
     * Get combined list of all input and output ports
     */
    @Override
    public List<Port> getAllPorts() {
        List<Port> allPorts = new ArrayList<>();
        allPorts.addAll(inputPorts);
        allPorts.addAll(outputPorts);
        return allPorts;
    }
    
    @Override
    public boolean isReference() {
        return isReference;
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    @Override
    public List<Packet> getStoredPackets() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.getPackets() : new ArrayList<>();
    }
    
    @Override
    public boolean hasEmptyOutputPort() {
        return outputPorts.stream().anyMatch(port -> port.getConnection() != null && port.getConnection().isEmpty());
    }
    
    @Override
    public boolean isFull() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.isFull() : false;
    }
    
    public GameState getGameState() {
        return gameState;
    }
    
    @Override
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    @Override
    public boolean isIndicatorOn() {
        return isActive || areAllPortsCorrectlyConnected();
    }
    
    public void setPlayButton(Group playButton) {
        this.playButton = playButton;
    }
    
    @Override
    public Group getPlayButton() {
        return playButton;
    }
    
    // Abstract methods that subclasses must implement
    @Override
    public abstract boolean isStartSystem();
    
    @Override
    public abstract boolean isEndSystem();
    
    @Override
    public abstract boolean isFixedPosition();
    
    public void updateIndicatorLamp() {
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        if (visualManager != null) {
            visualManager.updateIndicatorLamp();
        }
    }
    
    public void updateCapacityVisual() {
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        if (visualManager != null) {
            visualManager.updateCapacityVisual();
        }
    }
    
    public void updateStoredPacketsVisibility() {
        VisualManager visualManager = managerRegistry.get(VisualManager.class);
        if (visualManager != null) {
            visualManager.updateStoredPacketsVisibility();
        }
    }

    /**
     * Process a delivered packet from this system if it's an end system
     */
    public Packet processDeliveredPacket() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.processDeliveredPacket() : null;
    }
    
    /**
     * Get the current capacity used by the system
     */
    @Override
    public int getCurrentCapacityUsed() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.getCurrentCapacityUsed() : 0;
    }
    
    // Manager getters
    public PacketManager getPacketManager() {
        return managerRegistry.get(PacketManager.class);
    }
    
    public VisualManager getVisualManager() {
        return managerRegistry.get(VisualManager.class);
    }
    
    public TimelineManager getTimelineManager() {
        return managerRegistry.get(TimelineManager.class);
    }
    
    public PortManager getPortManager() {
        return managerRegistry.get(PortManager.class);
    }

    // NetworkSystemProvider interface implementation
    @Override
    public GameStateProvider getGameStateProvider() {
        return gameState;
    }

    // NetworkSystemCallbacks interface implementation
    @Override
    public void addStoredPacket(Packet packet) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.getPackets().add(packet);
        }
    }
    
    @Override
    public void removeStoredPacket(Packet packet) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.getPackets().remove(packet);
        }
    }
    
    @Override
    public void clearStoredPackets() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.clearPackets();
        }
    }
    
    @Override
    public void setStorageCapacity(int capacity) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.setStorageCapacity(capacity);
        }
    }
    
    @Override
    public void setShape(Rectangle shape) {
        // TODO: Implement setShape in VisualManager if needed
    }
    
    @Override
    public void setIndicatorLamp(Circle lamp) {
        // TODO: Implement setIndicatorLamp in VisualManager if needed
    }
    
    @Override
    public void setInnerBox(Rectangle innerBox) {
        // TODO: Implement setInnerBox in VisualManager if needed
    }
    
    @Override
    public void updatePortPositions() {
        PortManager portManager = managerRegistry.get(PortManager.class);
        if (portManager != null) {
            portManager.updatePortPositions();
        }
    }
    
    @Override
    public void startPacketTransferTimeline() {
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null) {
            timelineManager.startPacketTransferTimeline();
        }
    }
    
    @Override
    public void stopPacketTransferTimeline() {
        TimelineManager timelineManager = managerRegistry.get(TimelineManager.class);
        if (timelineManager != null) {
            timelineManager.stopSendingPackets();
        }
    }

    // System-specific rendering behavior methods
    /**
     * Determine if this system type should show a capacity label
     */
    public abstract boolean shouldShowCapacityLabel();
    
    /**
     * Determine if this system type should show an inner box
     */
    public abstract boolean shouldShowInnerBox();
    
    /**
     * Determine if this system type should show a play button
     */
    public abstract boolean shouldShowPlayButton();
    
    /**
     * Get the CSS style class for this system type
     */
    public abstract String getSystemStyleClass();
    
    /**
     * Get the display label text for this system type (e.g., "START", "END")
     */
    public abstract String getSystemDisplayLabel();
    
    /**
     * Get the horizontal offset for the system display label
     */
    public abstract double getSystemLabelOffset();
    
    /**
     * Determine if this system should be draggable
     */
    public abstract boolean isDraggable();
    
    /**
     * Determine if this system should have capacity tracking
     */
    public boolean shouldTrackCapacity() {
        return shouldShowCapacityLabel();
    }
    
    // System-specific visual management behavior methods
    /**
     * Get the preferred width for this system type
     */
    public abstract double getPreferredWidth(double defaultWidth);
    
    /**
     * Get the preferred height for this system type
     */
    public abstract double getPreferredHeight(double defaultHeight);
    
    /**
     * Determine if packets should be positioned in inner box for this system type
     */
    public abstract boolean shouldPositionPacketsInInnerBox();
    
    /**
     * Check if all ports are correctly connected for this system type
     */
    public abstract boolean arePortsCorrectlyConnected();
    
    // System-specific manager behavior methods
    /**
     * Determine if this system can process delivered packets
     */
    public abstract boolean canProcessDeliveredPackets();
    
    /**
     * Determine if this system needs packet transfer timeline
     */
    public abstract boolean needsPacketTransferTimeline();
    
    /**
     * Determine if this system can generate packets
     */
    public abstract boolean canGeneratePackets();
    
    /**
     * Determine if this system can transfer stored packets
     */
    public abstract boolean canTransferStoredPackets();
    
    /**
     * Get the arrangement strategy for ports of this system type
     */
    public abstract String getPortArrangementStrategy();
} 
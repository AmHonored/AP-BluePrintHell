package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.packettype.messenger.ProtectedPacket;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * SpySystem - A system that can intercept and monitor packets passing through the network
 * 
 * Functionality:
 * - Packets entering any spy system can exit from any spy system in the network (randomly)
 * - Confidential packets are completely destroyed when entering spy systems
 * - Protected packets are unaffected by spy systems
 */
public class SpySystem extends BaseSystem {
    
    private static final Random random = new Random();
    
    public SpySystem(Point2D position, double width, double height, boolean isReference, GameState gameState) {
        super(position, width, height, isReference, gameState);
        this.label = "Spy System";
    }
    
    @Override
    public void receivePacket(Packet packet) {
        if (packet == null || isReference()) {
            return;
        }
        
        // Mark the system as active
        setActive(true);
        
        // Check if this is a protected packet and reveal it
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            if (!protectedPacket.isRevealed()) {
                System.out.println("SpySystem: REVEALING protected packet " + packet.getId() + 
                                 " - true type: " + protectedPacket.getUnderlyingType());
                protectedPacket.revealTrueType();
            }
            // After revelation, process the packet normally (no special treatment)
        }
        
        // Check if this is a confidential packet (spy packet)
        if (isConfidentialPacket(packet)) {
            // TODO: Implement spy packet detection logic
            // For now, we'll use a placeholder method
            destroyConfidentialPacket(packet);
            return;
        }
        
        // For all packets (including revealed protected packets), handle spy system routing
        handleSpySystemRouting(packet);
    }
    
    /**
     * Check if a packet is confidential (spy packet)
     * TODO: Implement proper detection logic based on packet properties or type
     */
    private boolean isConfidentialPacket(Packet packet) {
        // TODO: Implement proper confidential packet detection
        // This could be based on packet properties, packet type, or other markers
        // For now, return false as spy packets are not implemented yet
        return false;
    }
    
    /**
     * Check if a packet is protected
     * TODO: Implement proper protection detection logic
     */
    private boolean isProtectedPacket(Packet packet) {
        // TODO: Implement proper protected packet detection
        // This could be based on packet properties or encryption status
        return packet.hasProperty("protected") && (Boolean) packet.getProperty("protected", false);
    }
    
    /**
     * Destroy a confidential packet completely (counts as packet loss)
     */
    private void destroyConfidentialPacket(Packet packet) {
        // Remove packet from the game state (counts as packet loss)
        if (gameState != null) {
            gameState.getActivePackets().remove(packet);
            
            // Increment packet loss counter
            if (gameState.getPacketManager() != null) {
                gameState.getPacketManager().incrementLostPackets();
            }
            
            // Remove packet shape from scene
            if (packet.getShape() != null && packet.getShape().getParent() != null) {
                javafx.application.Platform.runLater(() -> {
                    if (packet.getShape().getParent() instanceof javafx.scene.layout.Pane) {
                        ((javafx.scene.layout.Pane) packet.getShape().getParent()).getChildren().remove(packet.getShape());
                    }
                });
            }
            
            System.out.println("SpySystem: Confidential packet " + packet.getId() + " destroyed");
        }
    }
    
    /**
     * Handle protected packets - they pass through normally
     */
    private void handleProtectedPacket(Packet packet) {
        // Protected packets are unaffected by spy systems
        // Route them through normal packet processing
        super.receivePacket(packet);
    }
    
    /**
     * Handle spy system routing for regular packets
     * Packets can exit from any spy system in the network randomly
     */
    private void handleSpySystemRouting(Packet packet) {
        // Find all spy systems in the network
        List<SpySystem> allSpySystems = getAllSpySystemsInNetwork();
        
        if (allSpySystems.isEmpty()) {
            // No spy systems found, fallback to normal processing
            super.receivePacket(packet);
            System.out.println("SpySystem: No spy systems found, packet " + packet.getId() + " processed normally");
            return;
        }
        
        System.out.println("SpySystem: Found " + allSpySystems.size() + " spy systems in network");
        
        // Spy system behavior: Add some delay or special processing
        // This makes it different from intermediate systems
        if (allSpySystems.size() == 1) {
            // Single spy system - add monitoring delay but still process
            System.out.println("SpySystem: Single spy system - monitoring packet " + packet.getId() + " before routing");
            
            // Add a small delay to simulate monitoring (this is the key difference from intermediate systems)
            try {
                Thread.sleep(50); // 50ms monitoring delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Randomly select a spy system to exit from (or use current one if only one exists)
        SpySystem selectedSpySystem = allSpySystems.get(random.nextInt(allSpySystems.size()));
        
        if (selectedSpySystem == this) {
            System.out.println("SpySystem: Packet " + packet.getId() + " monitored and routed through SAME spy system (spy behavior)");
        } else {
            System.out.println("SpySystem: Packet " + packet.getId() + " intercepted and routed through DIFFERENT spy system " + selectedSpySystem.getId());
        }
        
        // Try to route packet through the selected spy system
        List<Port> availableOutputPorts = selectedSpySystem.getAvailableOutputPorts();
        
        if (availableOutputPorts.isEmpty()) {
            // Selected spy system has no available output ports, try another one
            for (SpySystem altSpySystem : allSpySystems) {
                if (altSpySystem != selectedSpySystem) {
                    List<Port> altOutputPorts = altSpySystem.getAvailableOutputPorts();
                    if (!altOutputPorts.isEmpty()) {
                        selectedSpySystem = altSpySystem;
                        availableOutputPorts = altOutputPorts;
                        System.out.println("SpySystem: Switched to alternative spy system " + selectedSpySystem.getId());
                        break;
                    }
                }
            }
        }
        
        if (availableOutputPorts.isEmpty()) {
            // No spy system has available output ports, store packet in this system
            super.receivePacket(packet);
            System.out.println("SpySystem: No spy systems have available outputs, packet " + packet.getId() + " stored");
            return;
        }
        
        // Route packet through the selected spy system's output
        Port outputPort = availableOutputPorts.get(0);
        
        if (outputPort.getConnection() != null) {
            outputPort.getConnection().addPacket(packet);
            packet.setCurrentConnection(outputPort.getConnection());
            packet.setInsideSystem(false);
            System.out.println("SpySystem: Packet " + packet.getId() + " successfully routed through spy system " + selectedSpySystem.getId());
        } else {
            // Fallback to normal processing
            super.receivePacket(packet);
            System.out.println("SpySystem: Connection issue, packet " + packet.getId() + " processed normally (fallback)");
        }
    }
    
    /**
     * Get all available output ports that have connections
     */
    private List<Port> getAvailableOutputPorts() {
        List<Port> availablePorts = new ArrayList<>();
        
        for (Port outputPort : getOutputPorts()) {
            if (outputPort.getConnection() != null) {
                availablePorts.add(outputPort);
            }
        }
        
        return availablePorts;
    }
    
    /**
     * Get all spy systems in the current network
     */
    private List<SpySystem> getAllSpySystemsInNetwork() {
        List<SpySystem> spySystems = new ArrayList<>();
        
        if (gameState != null && gameState.getLevelManager() != null) {
            // Get the current level number and then the level object
            int currentLevelNumber = gameState.getCurrentLevel();
            var currentLevel = gameState.getLevelManager().getLevel(currentLevelNumber);
            
            if (currentLevel != null && currentLevel.getSystems() != null) {
                for (NetworkSystem networkSystem : currentLevel.getSystems()) {
                    // Check if this NetworkSystem is configured as a spy system
                    if (networkSystem.isSpySystem()) {
                        // Get the actual BaseSystem implementation
                        var actualSystem = getActualSystemFromNetworkSystem(networkSystem);
                        if (actualSystem instanceof SpySystem) {
                            spySystems.add((SpySystem) actualSystem);
                        }
                    }
                }
            }
        }
        
        return spySystems;
    }
    
    /**
     * Helper method to get the actual BaseSystem from a NetworkSystem
     * This uses reflection to access the actualSystem field
     */
    private BaseSystem getActualSystemFromNetworkSystem(Object networkSystem) {
        try {
            java.lang.reflect.Field field = networkSystem.getClass().getDeclaredField("actualSystem");
            field.setAccessible(true);
            return (BaseSystem) field.get(networkSystem);
        } catch (Exception e) {
            System.err.println("SpySystem: Could not access actualSystem field: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a unique identifier for this spy system
     */
    private String getId() {
        return "SpySystem_" + this.hashCode();
    }
    
    @Override
    protected void initializeSystem() {
        // Initialize spy system specific components
        System.out.println("SpySystem: Initializing spy system at position " + getPosition());
    }
    
    @Override
    protected void updateSystemSpecific(double deltaTime) {
        // Update spy system specific logic
        // Could include monitoring network traffic, updating spy capabilities, etc.
    }
    
    @Override
    public void handleOutputPortAvailable(Port port) {
        // Handle output port availability for spy system
        // This could be used to send stored packets when output becomes available
        if (port != null && port.getConnection() != null && !isFull()) {
            // Try to send any stored packets
            if (getPacketManager() != null && !getStoredPackets().isEmpty()) {
                getPacketManager().transferPacket();
            }
        }
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
        return false; // Spy systems don't show capacity
    }
    
    @Override
    public boolean shouldShowInnerBox() {
        return false; // Spy systems don't have inner boxes
    }
    
    @Override
    public boolean shouldShowPlayButton() {
        return false; // Spy systems don't have play buttons
    }
    
    @Override
    public String getSystemStyleClass() {
        return "spy-system";
    }
    
    @Override
    public String getSystemDisplayLabel() {
        return "SPY";
    }
    
    @Override
    public double getSystemLabelOffset() {
        return 25.0; // Offset for "SPY" text
    }
    
    @Override
    public boolean isDraggable() {
        return false; // Spy systems are not draggable
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
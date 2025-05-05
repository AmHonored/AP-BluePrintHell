package com.networkgame.model;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;

/**
 * Handles all packet operations including storage, routing, and transfer
 */
public class PacketManager {
    private NetworkSystem parentSystem;
    private GameState gameState;
    
    // Specialized packet handling components
    private PacketStorage packetStorage;
    private PacketAnimator packetAnimator;
    private PacketStatistics packetStatistics;
    private PacketLifecycle packetLifecycle;

    public PacketManager(NetworkSystem parentSystem, List<Packet> packets, int storageCapacity, GameState gameState) {
        this.parentSystem = parentSystem;
        this.gameState = gameState;
        
        // Initialize specialized packet handlers
        this.packetStorage = new PacketStorage(parentSystem, packets, storageCapacity, gameState);
        this.packetAnimator = new PacketAnimator(parentSystem, gameState);
        this.packetStatistics = new PacketStatistics(parentSystem, gameState);
        this.packetLifecycle = new PacketLifecycle(parentSystem, gameState);
    }

    public void reset() {
        packetStorage.reset();
        packetStatistics.reset();
        packetLifecycle.reset();
    }

    /**
     * Get the current capacity usage of the system
     */
    public int getCurrentCapacityUsed() {
        return packetStorage.getCurrentCapacityUsed();
    }
    
    /**
     * Check if adding this packet would exceed system capacity
     */
    public boolean wouldExceedCapacity(Packet packet) {
        return packetStorage.wouldExceedCapacity(packet);
    }

    /**
     * Try to send a packet through any available output port
     */
    public boolean tryToSendPacket(Packet packet) {
        // First, try a type-matching port that's empty
        for (Port port : parentSystem.getOutputPorts()) {
            Connection connection = port.getConnection();
            if (port.getType() == packet.getType() && 
                connection != null && 
                connection.isEmpty()) {
                
                packetAnimator.sendPacketToConnection(packet, port, connection);
                System.out.println("Sent packet through matching output port");
                return true;
            }
        }
        
        // If no matching port, try any available port
        for (Port port : parentSystem.getOutputPorts()) {
            Connection connection = port.getConnection();
            if (connection != null && connection.isEmpty()) {
                packetAnimator.sendPacketToConnection(packet, port, connection);
                System.out.println("Sent packet through non-matching output port");
                return true;
            }
        }
        
        System.out.println("No available output ports found for packet");
        return false;
    }
    
    /**
     * Find an empty output port for the given packet, prioritizing shape-matching ports
     */
    public Port findEmptyOutputPortForPacket(Packet packet) {
        // First priority: find a matching empty port
        Optional<Port> matchingPort = parentSystem.getOutputPorts().stream()
                .filter(port -> port.isConnected() && port.getConnection().isEmpty())
                .filter(port -> port.getType() == packet.getType())
                .findFirst();
        
        if (matchingPort.isPresent()) {
            return matchingPort.get();
        }
        
        // Second priority: For special cases like Level 1, we may want specific fallback behaviors
        if (gameState != null && gameState.getCurrentLevel() == 1) {
            if (packet.getType() == Packet.PacketType.TRIANGLE) {
                // Find a square port as an alternative for triangle packets
                Optional<Port> squarePort = parentSystem.getOutputPorts().stream()
                        .filter(port -> port.isConnected() && port.getConnection().isEmpty())
                        .filter(port -> port.getType() == Packet.PacketType.SQUARE)
                        .findFirst();
                        
                if (squarePort.isPresent()) {
                    return squarePort.get();
                }
            }
        }
        
        // Third priority: use any available port
        Optional<Port> anyEmptyPort = parentSystem.getOutputPorts().stream()
                .filter(port -> port.isConnected() && port.getConnection().isEmpty())
                .findFirst();
                
        if (anyEmptyPort.isPresent()) {
            return anyEmptyPort.get();
        }
        
        return null;
    }
    
    /**
     * Check if all connected output wires are currently occupied
     */
    public boolean areAllOutputWiresFull() {
        // Count connected output ports
        long connectedPortCount = parentSystem.getOutputPorts().stream()
                .filter(Port::isConnected)
                .count();
                
        // Count output ports with non-empty connections
        long nonEmptyPortCount = parentSystem.getOutputPorts().stream()
                .filter(port -> port.isConnected() && !port.getConnection().isEmpty())
                .count();
                
        // If there are no connected ports, return false (can't be full)
        if (connectedPortCount == 0) {
            return false;
        }
        
        // Debug output to help diagnose issues
        System.out.println("Connected ports: " + connectedPortCount + ", Non-empty ports: " + nonEmptyPortCount);
        
        // Simply check if ANY output port has an empty connection - if so, wires are not full
        boolean anyEmptyWire = parentSystem.getOutputPorts().stream()
                .anyMatch(port -> port.isConnected() && port.getConnection().isEmpty());
        
        return !anyEmptyWire; // Return false if there's at least one empty wire
    }
    
    /**
     * Receive a packet into this system
     */
    public void receivePacket(Packet packet) {
        // Clear the connection reference
        if (packet.getCurrentConnection() != null) {
            packet.setCurrentConnection(null);
        }
        
        // Skip if this is a reference system
        if (parentSystem.isReference()) {
            return;
        }
        
        // Start systems don't receive packets
        if (parentSystem.isStartSystem()) {
            return;
        }
        
        // If this is an end system, handle with special end system logic
        if (parentSystem.isEndSystem()) {
            handlePacketArrival(packet, parentSystem.getPortManager().findClosestInputPort(packet));
            return;
        }
        
        // Mark the system as active
        parentSystem.setActive(true);
        
        // For level 1, determine if this is a packet that needs to be visibly stored
        boolean isLevel1StorageSystem = gameState.getCurrentLevel() == 1 && "Storage System".equals(parentSystem.getLabel());
        boolean isSquarePacket = packet.getType() == Packet.PacketType.SQUARE;
        
        // For intermediate systems, try to send packet directly if possible
        // But in Level 1 storage system, we want to demonstrate packet storage,
        // so we will store the packet instead of trying to send it directly
        boolean packetSent = false;
        
        if (!isLevel1StorageSystem || !isSquarePacket) {
            // Try type-based routing first - match packet type to port type
            if (packet instanceof SquarePacket) {
                // First try to find matching port type (Square)
                for (Port outputPort : parentSystem.getOutputPorts()) {
                    if (outputPort.getType() == Packet.PacketType.SQUARE && 
                        outputPort.getConnection() != null && 
                        outputPort.getConnection().isEmpty()) {
                        
                        // Send packet directly through this port
                        packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                        packetSent = true;
                        break;
                    }
                }
                
                // If no matching port found, try any available output port
                if (!packetSent) {
                    for (Port outputPort : parentSystem.getOutputPorts()) {
                        if (outputPort.getConnection() != null && 
                            outputPort.getConnection().isEmpty()) {
                            
                            // Send packet directly through this port
                            packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                            packetSent = true;
                            break;
                        }
                    }
                }
            } 
            else if (packet instanceof TrianglePacket) {
                // First try to find matching port type (Triangle)
                for (Port outputPort : parentSystem.getOutputPorts()) {
                    if (outputPort.getType() == Packet.PacketType.TRIANGLE && 
                        outputPort.getConnection() != null && 
                        outputPort.getConnection().isEmpty()) {
                        
                        // Send packet directly through this port
                        packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                        packetSent = true;
                        break;
                    }
                }
                
                // If no matching port found, try any available output port
                if (!packetSent) {
                    for (Port outputPort : parentSystem.getOutputPorts()) {
                        if (outputPort.getConnection() != null && 
                            outputPort.getConnection().isEmpty()) {
                            
                            // Send packet directly through this port
                            packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                            packetSent = true;
                            break;
                        }
                    }
                }
            }
        } else {
            // This is the Level 1 storage system with a square packet
            // Force the packet to be stored for demonstration purposes
            packetSent = false;
        }
        
        // Only store the packet if we couldn't send it immediately
        if (!packetSent) {
            // Store the packet for later processing
            packetStorage.storePacket(packet);
        }
    }
    
    /**
     * Handle a packet arriving at an input port
     */
    public void handlePacketArrival(Packet packet, Port port) {
        // Only process if it's actually an input port
        if (!port.isInputPort()) {
            System.out.println("ERROR: Attempt to handle packet arrival at non-input port");
            return;
        }
        
        // Set packet's position to the input port
        packet.setPosition(port.getPosition());
        
        // Always mark the packet as no longer inside a connection
        if (packet.getCurrentConnection() != null) {
            packet.setCurrentConnection(null);
        }
        
        // If this is an end system, handle it specially
        if (parentSystem.isEndSystem()) {
            System.out.println("**** PACKET REACHED END SYSTEM ****");
            
            // Mark packet as having reached the end system
            packet.setReachedEndSystem(true);
            
            if (!packet.hasProperty("counted")) {
                // Mark packet as counted
                packet.setProperty("counted", true);
                
                // Update game state
                if (gameState != null) {
                    // Increment delivered packets counter
                    packetStatistics.incrementPacketsDelivered();
                    packetStatistics.incrementVisualPacketsDelivered();
                    
                    // Add coins for this packet
                    gameState.addCoins(packet.getCoinValue());
                    
                    // Play delivery sound
                    AudioManager.getInstance().playSoundEffect(
                        AudioManager.SoundType.CONNECTION_SUCCESS);
                    
                    // Update HUD immediately
                    if (gameState.getGameScene() != null) {
                        Platform.runLater(() -> {
                            gameState.getGameScene().updatePacketsCollectedLabel(gameState.getPacketsDelivered());
                        });
                    }
                }
            }
            
            // Consistent handling for all levels
            if (gameState != null) {
                // 1. Immediately remove the visual representation
                if (packet.getShape() != null) {
                    Platform.runLater(() -> {
                        try {
                            // Remove from parent if it has one
                            if (packet.getShape().getParent() != null) {
                                if (packet.getShape().getParent() instanceof javafx.scene.Group) {
                                    ((javafx.scene.Group) packet.getShape().getParent()).getChildren().remove(packet.getShape());
                                } else if (packet.getShape().getParent() instanceof javafx.scene.layout.Pane) {
                                    ((javafx.scene.layout.Pane) packet.getShape().getParent()).getChildren().remove(packet.getShape());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("Error removing packet shape: " + e.getMessage());
                        }
                    });
                }
                
                // 2. Disconnect from any connection and mark connection as available
                if (packet.getCurrentConnection() != null) {
                    packet.getCurrentConnection().removePacket(packet);
                    packet.getCurrentConnection().setAvailable(true);
                    packet.setCurrentConnection(null);
                }
                
                // 3. Remove from active packets list
                gameState.getActivePackets().remove(packet);
                
                // 4. Force immediate game state update
                if (gameState.getGameScene() != null) {
                    Platform.runLater(() -> {
                        gameState.getGameScene().render();
                    });
                }
                
                System.out.println("Packet completely removed from end system");
            }
            
            return;
        }
        
        // For intermediate systems, check if we can immediately send the packet
        // without storing it first if output wires are available
        boolean packetSent = false;
        
        // Try type-based routing first - match packet type to port type
        if (packet instanceof SquarePacket) {
            // First try to find matching port type (Square)
            for (Port outputPort : parentSystem.getOutputPorts()) {
                if (outputPort.getType() == Packet.PacketType.SQUARE && 
                    outputPort.getConnection() != null && 
                    outputPort.getConnection().isEmpty()) {
                    
                    // Send packet directly through this port
                    packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                    packetSent = true;
                    break;
                }
            }
            
            // If no matching port found, try any available output port
            if (!packetSent) {
                for (Port outputPort : parentSystem.getOutputPorts()) {
                    if (outputPort.getConnection() != null && 
                        outputPort.getConnection().isEmpty()) {
                        
                        // Send packet directly through this port
                        packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                        packetSent = true;
                        break;
                    }
                }
            }
        } 
        else if (packet instanceof TrianglePacket) {
            // First try to find matching port type (Triangle)
            for (Port outputPort : parentSystem.getOutputPorts()) {
                if (outputPort.getType() == Packet.PacketType.TRIANGLE && 
                    outputPort.getConnection() != null && 
                    outputPort.getConnection().isEmpty()) {
                    
                    // Send packet directly through this port
                    packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                    packetSent = true;
                    break;
                }
            }
            
            // If no matching port found, try any available output port
            if (!packetSent) {
                for (Port outputPort : parentSystem.getOutputPorts()) {
                    if (outputPort.getConnection() != null && 
                        outputPort.getConnection().isEmpty()) {
                        
                        // Send packet directly through this port
                        packetAnimator.sendPacketToConnection(packet, outputPort, outputPort.getConnection());
                        packetSent = true;
                        break;
                    }
                }
            }
        }
        
        // Only store the packet if we couldn't send it immediately
        if (!packetSent) {
            // Store the packet for later processing
            packetStorage.storePacket(packet);
        }
    }
    
    /**
     * Transfer the first packet in the queue (FIFO) if possible
     */
    public void transferPacket() {
        // Skip if this is a start or end system, or if there are no packets
        if (parentSystem.isStartSystem() || parentSystem.isEndSystem() || packetStorage.isEmpty()) {
            return;
        }
        
        // Get the first packet in the queue (FIFO)
        Packet packet = packetStorage.getPackets().get(0);
        
        // Try type-based routing first - match packet type to port type
        if (packet instanceof SquarePacket) {
            // First try to find matching port type (Square)
            for (Port port : parentSystem.getOutputPorts()) {
                if (port.getType() == Packet.PacketType.SQUARE && 
                    port.getConnection() != null && 
                    port.getConnection().isEmpty()) {
                    
                    // Send packet through this port
                    packetAnimator.sendPacketToConnection(packet, port, port.getConnection());
                    // Remove from storage
                    packetStorage.getPackets().remove(packet);
                    // Update system's visual indicator
                    parentSystem.getVisualManager().updateCapacityVisual();
                    return;
                }
            }
            
            // If no matching port, try any available output port
            for (Port port : parentSystem.getOutputPorts()) {
                if (port.getConnection() != null && port.getConnection().isEmpty()) {
                    // Send packet through this port
                    packetAnimator.sendPacketToConnection(packet, port, port.getConnection());
                    // Remove from storage
                    packetStorage.getPackets().remove(packet);
                    // Update system's visual indicator
                    parentSystem.getVisualManager().updateCapacityVisual();
                    return;
                }
            }
        } 
        else if (packet instanceof TrianglePacket) {
            // First try to find matching port type (Triangle)
            for (Port port : parentSystem.getOutputPorts()) {
                if (port.getType() == Packet.PacketType.TRIANGLE && 
                    port.getConnection() != null && 
                    port.getConnection().isEmpty()) {
                    
                    // Send packet through this port
                    packetAnimator.sendPacketToConnection(packet, port, port.getConnection());
                    // Remove from storage
                    packetStorage.getPackets().remove(packet);
                    // Update system's visual indicator
                    parentSystem.getVisualManager().updateCapacityVisual();
                    return;
                }
            }
            
            // If no matching port, try any available output port
            for (Port port : parentSystem.getOutputPorts()) {
                if (port.getConnection() != null && port.getConnection().isEmpty()) {
                    // Send packet through this port
                    packetAnimator.sendPacketToConnection(packet, port, port.getConnection());
                    // Remove from storage
                    packetStorage.getPackets().remove(packet);
                    // Update system's visual indicator
                    parentSystem.getVisualManager().updateCapacityVisual();
                    return;
                }
            }
        }
    }

    /**
     * Process a delivered packet
     * @return The packet that was processed, or null if no packet was processed
     */
    public Packet processDeliveredPacket() {
        return packetStorage.processDeliveredPacket();
    }
    
    /**
     * Safely remove a packet from stored packets
     */
    public void removeStoredPacket(Packet packet) {
        packetStorage.removeStoredPacket(packet);
    }
    
    /**
     * Get all stored packets
     */
    public List<Packet> getPackets() {
        return packetStorage.getPackets();
    }
    
    /**
     * Check if the system is at full capacity
     */
    public boolean isFull() {
        return packetStorage.isFull();
    }
    
    /**
     * Check if there are any packets stored
     */
    public boolean isEmpty() {
        return packetStorage.isEmpty();
    }
    
    /**
     * Clear all stored packets
     */
    public void clearPackets() {
        packetStorage.clearPackets();
    }
    
    /**
     * Set the storage capacity
     */
    public void setStorageCapacity(int capacity) {
        packetStorage.setStorageCapacity(capacity);
    }
    
    /**
     * Get the storage capacity
     */
    public int getStorageCapacity() {
        return packetStorage.getStorageCapacity();
    }

    /**
     * Add an active packet to the game
     */
    public void addActivePacket(Packet packet) {
        packetLifecycle.addActivePacket(packet);
    }

    /**
     * Increment the counter for packets that reached an end system
     */
    public void incrementPacketsDelivered() {
        packetStatistics.incrementPacketsDelivered();
    }
    
    /**
     * Increment visual packet counter when a packet is visually seen reaching the end system
     */
    public void incrementVisualPacketsDelivered() {
        packetStatistics.incrementVisualPacketsDelivered();
    }
    
    /**
     * Increment the lost packets counter
     */
    public void incrementLostPackets() {
        packetStatistics.incrementLostPackets();
    }

    /**
     * Get number of packets delivered to end systems
     */
    public int getPacketsDelivered() {
        return packetStatistics.getPacketsDelivered();
    }

    /**
     * Reset delivered packets counter
     */
    public void resetPacketsDelivered() {
        packetStatistics.resetPacketsDelivered();
    }
    
    /**
     * Increment the total packets counter
     */
    public void incrementTotalPackets() {
        packetStatistics.incrementTotalPackets();
    }

    public int getTotalPacketsSent() {
        return packetStatistics.getTotalPacketsSent();
    }

    public int getPacketsLost() {
        return packetStatistics.getPacketsLost();
    }
    
    public List<Packet> getActivePackets() {
        return packetLifecycle.getActivePackets();
    }

    /**
     * Starts the animation of a packet along a connection using JavaFX Timeline
     */
    public void animatePacketAlongConnection(Packet packet, Connection connection) {
        packetAnimator.animatePacketAlongConnection(packet, connection);
    }
    
    /**
     * Common method to safely remove a packet from all collections and dispose its resources
     */
    public boolean safelyRemovePacket(Packet packet, boolean immediate) {
        return packetLifecycle.safelyRemovePacket(packet, immediate);
    }
    
    /**
     * Simple version for direct removal - use with caution outside animation loop
     */
    public boolean safelyRemovePacket(Packet packet) {
        return packetLifecycle.safelyRemovePacket(packet);
    }

    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        packetLifecycle.cleanup();
    }

    /**
     * Update animation state for all packets
     * @param deltaTime Time elapsed since last frame
     */
    public void updatePacketAnimations(double deltaTime) {
        packetAnimator.updatePacketAnimations(deltaTime, packetLifecycle.getActivePackets(), packetLifecycle);
    }
} 
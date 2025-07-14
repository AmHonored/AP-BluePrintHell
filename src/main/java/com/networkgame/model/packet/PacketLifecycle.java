package com.networkgame.model.packet;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.state.GameState;

/**
 * Handles packet lifecycle operations (creation, removal, cleanup)
 */
public class PacketLifecycle {
    private NetworkSystem parentSystem;
    private GameState gameState;
    private List<Packet> activePackets;

    public PacketLifecycle(NetworkSystem parentSystem, GameState gameState) {
        this.parentSystem = parentSystem;
        this.gameState = gameState;
        this.activePackets = new ArrayList<>();
    }

    public void reset() {
        this.activePackets.clear();
    }

    /**
     * Add an active packet to the game
     * @param packet the packet to add
     */
    public void addActivePacket(Packet packet) {
        // Ensure the packet is not null
        if (packet == null) {
            System.out.println("ERROR: Attempted to add null packet to active packets");
            return;
        }
        
        // Skip if packet is already in the list
        if (activePackets.contains(packet)) {
            System.out.println("Packet already in active packets list, skipping");
            return;
        }
        
        // Add a reference to the gameState in the packet's properties
        packet.setProperty("gameState", gameState);
        
        // Always ensure packets are visible when added to the active list
        ensurePacketVisibility(packet);
        
        // Add to active packet list
        activePackets.add(packet);
        
        // Initialize packet shape if needed
        initializePacketShape(packet);
        
        // Log the packet creation
        logPacketCreation(packet);
    }

    /**
     * Common method to safely remove a packet from all collections and dispose its resources
     * @param packet The packet to remove and dispose
     * @param immediate Whether to remove immediately or mark for later removal
     * @return true if the packet was successfully removed
     */
    public boolean safelyRemovePacket(Packet packet, boolean immediate) {
        if (packet == null) return false;
        
        try {
            // Special protection for reversing hexagon packets
            if (packet instanceof com.networkgame.model.entity.packettype.messenger.HexagonPacket) {
                com.networkgame.model.entity.packettype.messenger.HexagonPacket hexPacket = 
                    (com.networkgame.model.entity.packettype.messenger.HexagonPacket) packet;
                if (hexPacket.isReversing()) {
                    System.out.println("*** PACKET LIFECYCLE: Protecting reversing hexagon packet " + packet.getId() + " from removal ***");
                    return false; // Don't remove reversing hexagon packets
                }
            }
            
            // If not immediate removal, just return
            if (!immediate) {
                return false; // We'll handle removal in the next animation frame
            }
            
            // Check if this packet has reached an end system
            boolean hasReachedEnd = packet.hasReachedEndSystem();
            
            // Remove from active packets
            boolean removed = activePackets.remove(packet);
            
            // Clear its connection reference
            clearConnectionReference(packet);
            
            // Handle the visual removal of the packet if it has a shape
            removePacketShape(packet, hasReachedEnd);
            
            return removed;
        } catch (Exception e) {
            System.err.println("Error removing packet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Simple version for direct removal - use with caution outside animation loop
     */
    public boolean safelyRemovePacket(Packet packet) {
        return safelyRemovePacket(packet, true);
    }

    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        // Clear all packets safely
        List<Packet> packetsToRemove = new ArrayList<>(activePackets);
        for (Packet packet : packetsToRemove) {
            removePacketShapeFromParent(packet.getShape());
        }
        // Clear the collection
        activePackets.clear();
    }
    
    /**
     * Get all active packets
     * @return List of active packets
     */
    public List<Packet> getActivePackets() {
        return activePackets;
    }
    
    // Helper methods to follow SRP and reduce code duplication
    
    private void ensurePacketVisibility(Packet packet) {
        if (packet.getShape() != null) {
            packet.getShape().setVisible(true);
            packet.getShape().setOpacity(1.0);
            System.out.println("Force visibility for new packet " + packet.getId() + " in level " + gameState.getCurrentLevel());
        }
    }
    
    private void initializePacketShape(Packet packet) {
        Shape packetShape = packet.getShape();
        if (packetShape == null) return;
        
        // Add base packet style class
        packetShape.getStyleClass().add("packet");
        
        // Handle shape-specific styling and initialization
        switch (packet.getType()) {
            case SQUARE:
                initializeSquarePacket(packet, packetShape);
                break;
            case TRIANGLE:
                initializeTrianglePacket(packet, packetShape);
                break;
        }
    }
    
    private void initializeSquarePacket(Packet packet, Shape packetShape) {
        packetShape.getStyleClass().add("square-packet");
        
        // Make sure Rectangle packets have proper size
        if (packetShape instanceof Rectangle) {
            Rectangle rect = (Rectangle) packetShape;
            if (rect.getWidth() <= 0 || rect.getHeight() <= 0) {
                rect.setWidth(10);
                rect.setHeight(10);
            }
        }
    }
    
    private void initializeTrianglePacket(Packet packet, Shape packetShape) {
        packetShape.getStyleClass().add("triangle-packet");
        
        // Make sure Triangle packets have proper points
        if (packetShape instanceof Polygon) {
            Polygon poly = (Polygon) packetShape;
            if (poly.getPoints().isEmpty()) {
                double size = 10.0;
                Point2D pos = packet.getPosition();
                poly.getPoints().setAll(
                    pos.getX(), pos.getY() - size,              // Top
                    pos.getX() - size * 0.866, pos.getY() + size * 0.5,  // Bottom left
                    pos.getX() + size * 0.866, pos.getY() + size * 0.5   // Bottom right
                );
            }
        }
    }
    
    private void logPacketCreation(Packet packet) {
        System.out.println("Added active packet: " + packet.getId() + 
                          " at position " + packet.getPosition() + 
                          " connection: " + (packet.getCurrentConnection() != null ? "yes" : "no"));
    }
    
    private void clearConnectionReference(Packet packet) {
        if (packet.getCurrentConnection() != null) {
            packet.getCurrentConnection().removePacket(packet);
            packet.setCurrentConnection(null);
        }
    }
    
    private void removePacketShape(Packet packet, boolean hasReachedEnd) {
        Shape packetShape = packet.getShape();
        if (packetShape == null) return;
        
        removePacketShapeFromParent(packetShape);
        
        if (hasReachedEnd) {
            System.out.println("Packet " + packet.getId() + " reached end system and has been removed");
        }
    }
    
    private void removePacketShapeFromParent(Shape packetShape) {
        if (packetShape == null || packetShape.getParent() == null) return;
        
        Platform.runLater(() -> {
            try {
                if (packetShape.getParent() instanceof Group) {
                    ((Group) packetShape.getParent()).getChildren().remove(packetShape);
                } else if (packetShape.getParent() instanceof Pane) {
                    ((Pane) packetShape.getParent()).getChildren().remove(packetShape);
                }
            } catch (Exception e) {
                System.err.println("Error removing packet shape: " + e.getMessage());
            }
        });
    }
} 

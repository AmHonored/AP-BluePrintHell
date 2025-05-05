package com.networkgame.model;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all packet movement and animation concerns
 */
public class PacketAnimator {
    private static final double BASE_SPEED = 100.0;
    private static final double TRIANGLE_SPEED_MULTIPLIER = 1.5;
    private static final double LEVEL_ONE_SPEED_MODIFIER = 0.5;
    
    private NetworkSystem parentSystem;
    private GameState gameState;

    public PacketAnimator(NetworkSystem parentSystem, GameState gameState) {
        this.parentSystem = parentSystem;
        this.gameState = gameState;
    }

    /**
     * Send a packet directly to a connection without storing it
     */
    public void sendPacketToConnection(Packet packet, Port outputPort, Connection connection) {
        // Set packet at output port position
        packet.setPosition(outputPort.getPosition());
        
        // Clear the "inside system" flag since it's now leaving
        packet.setInsideSystem(false);
        
        // Reset the packet's visual appearance
        resetPacketAppearance(packet);
        
        // Adjust packet speed based on its type
        adjustPacketSpeed(packet, outputPort);
        
        // Set velocity direction
        Point2D sourcePos = outputPort.getPosition();
        Point2D targetPos = connection.getTargetPort().getPosition();
        packet.alignVelocityToWire(sourcePos.getX(), sourcePos.getY(), 
                        targetPos.getX(), targetPos.getY());
        
        // Add packet to connection and ensure it's fully transferred
        connection.addPacket(packet);
    }

    /**
     * Reset the packet's visual appearance to standard class-defined size
     */
    private void resetPacketAppearance(Packet packet) {
        Shape shape = packet.getShape();
        
        if (packet instanceof SquarePacket && shape instanceof Rectangle) {
            Rectangle rect = (Rectangle) shape;
            rect.setWidth(10.0);
            rect.setHeight(10.0);
            rect.setEffect(null);
        } else if (packet instanceof TrianglePacket && shape instanceof Polygon) {
            Polygon triangle = (Polygon) shape;
            double size = 14.0;
            double height = Math.sqrt(3) * size / 2;
            
            triangle.getPoints().setAll(
                packet.getPosition().getX(), packet.getPosition().getY() - height * 2/3,
                packet.getPosition().getX() - size/2, packet.getPosition().getY() + height/3,
                packet.getPosition().getX() + size/2, packet.getPosition().getY() + height/3
            );
            triangle.setEffect(null);
        }
    }
    
    /**
     * Adjust packet speed based on its type and the port
     */
    private void adjustPacketSpeed(Packet packet, Port outputPort) {
        if (packet instanceof SquarePacket) {
            ((SquarePacket) packet).adjustSpeedForPort(outputPort);
        } else if (packet instanceof TrianglePacket) {
            ((TrianglePacket) packet).adjustSpeedForPort(outputPort);
        }
    }

    /**
     * Starts the animation of a packet along a connection using JavaFX Timeline
     */
    public void animatePacketAlongConnection(Packet packet, Connection connection) {
        // Calculate path vectors
        Point2D startPos = connection.getSourcePort().getPosition();
        Point2D endPos = connection.getTargetPort().getPosition();
        Point2D pathVector = calculatePathVector(startPos, endPos);
        
        // Calculate and set speed
        double speed = calculatePacketSpeed(packet);
        packet.setSpeed(speed);
        
        // Set movement parameters
        packet.setUnitVector(pathVector.getX(), pathVector.getY());
        packet.setVelocity(pathVector.multiply(speed));
        packet.setPosition(startPos);
        packet.setCurrentConnection(connection);
    }
    
    /**
     * Calculate the normalized direction vector between two points
     */
    private Point2D calculatePathVector(Point2D startPos, Point2D endPos) {
        double dx = endPos.getX() - startPos.getX();
        double dy = endPos.getY() - startPos.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return new Point2D(dx / distance, dy / distance);
    }
    
    /**
     * Calculate speed based on packet type and current game level
     */
    private double calculatePacketSpeed(Packet packet) {
        double speed = BASE_SPEED;
        
        // Apply speed modifiers based on packet type
        if (packet instanceof TrianglePacket) {
            speed *= TRIANGLE_SPEED_MULTIPLIER;
        }
        
        // For Level 1, slow down packets for better visibility
        if (gameState.getCurrentLevel() == 1) {
            speed *= LEVEL_ONE_SPEED_MODIFIER;
        }
        
        return speed;
    }

    /**
     * Update animation state for all packets
     */
    public void updatePacketAnimations(double deltaTime, List<Packet> activePackets, com.networkgame.model.PacketLifecycle packetLifecycle) {
        long currentTime = System.currentTimeMillis();
        List<Packet> packetsToRemove = new ArrayList<>();
        List<Packet> packetsCopy = new ArrayList<>(activePackets);
        
        for (Packet packet : packetsCopy) {
            if (shouldSkipPacket(packet, packetsToRemove)) {
                continue;
            }
            
            if (packet.hasReachedEndSystem()) {
                packetsToRemove.add(packet);
                System.out.println("[DEBUG] Packet removed at END system. HUD should update. Packet: " + packet);
                continue;
            }
            
            if (hasActiveAnimation(packet)) {
                updatePacketPosition(packet, currentTime);
            }
        }
        
        removeFinishedPackets(packetsToRemove, packetLifecycle);
    }
    
    /**
     * Check if packet should be skipped in the update loop
     */
    private boolean shouldSkipPacket(Packet packet, List<Packet> packetsToRemove) {
        return packetsToRemove.contains(packet);
    }
    
    /**
     * Check if packet has an active animation
     */
    private boolean hasActiveAnimation(Packet packet) {
        return packet.hasProperty("animationActive") && 
               (boolean)packet.getProperty("animationActive", false);
    }
    
    /**
     * Update packet position based on animation properties
     */
    private void updatePacketPosition(Packet packet, long currentTime) {
        // Calculate current position based on elapsed time
        long startTime = (long)packet.getProperty("animationStartTime", currentTime);
        double elapsedSeconds = (currentTime - startTime) / 1000.0;
        
        // Get animation properties
        double speed = updateSpeed(packet, elapsedSeconds);
        double distance = (double)packet.getProperty("animationDistance", 0.0);
        double unitX = (double)packet.getProperty("animationUnitX", 0.0);
        double unitY = (double)packet.getProperty("animationUnitY", 0.0);
        double startX = (double)packet.getProperty("animationStartX", 0.0);
        double startY = (double)packet.getProperty("animationStartY", 0.0);
        
        // Calculate traveled distance
        double traveled = speed * elapsedSeconds;
        
        if (traveled >= distance) {
            handlePacketArrival(packet);
        } else {
            // Update position along path
            double newX = startX + unitX * traveled;
            double newY = startY + unitY * traveled;
            packet.setPosition(new Point2D(newX, newY));
        }
    }
    
    /**
     * Update and return speed value with acceleration if applicable
     */
    private double updateSpeed(Packet packet, double elapsedSeconds) {
        double speed = (double)packet.getProperty("animationSpeed", 100.0);
        
        if (packet.hasProperty("acceleration")) {
            double acceleration = (double)packet.getProperty("acceleration", 0.0);
            speed += acceleration * elapsedSeconds;
            packet.setProperty("animationSpeed", speed);
        }
        
        return speed;
    }
    
    /**
     * Handle packet arrival at destination
     */
    private void handlePacketArrival(Packet packet) {
        Connection connection = getPacketConnection(packet);
        if (connection == null) return;
        
        // Position at exact endpoint
        Point2D endPos = connection.getTargetPort().getPosition();
        packet.setPosition(endPos);
        
        // Get target system and handle packet delivery
        NetworkSystem targetSystem = connection.getTargetPort().getSystem();
        
        // Remove packet from connection and mark animation as complete
        connection.removePacket(packet);
        packet.setProperty("animationActive", false);
        
        if (targetSystem.isEndSystem()) {
            handleEndSystemArrival(packet);
        } else {
            handleIntermediateSystemArrival(packet, targetSystem);
        }
    }
    
    /**
     * Get the connection associated with a packet
     */
    private Connection getPacketConnection(Packet packet) {
        if (packet.hasProperty("connection")) {
            return (Connection)packet.getProperty("connection", null);
        }
        return null;
    }
    
    /**
     * Handle packet arrival at an end system
     */
    private void handleEndSystemArrival(Packet packet) {
        System.out.println("[INPUT PORT] Packet reached END system input port - processing immediately");
        
        // Mark packet state
        packet.setReachedEndSystem(true);
        packet.setProperty("isVisiblePacket", true);
        
        // Process delivered packet
        if (!packet.hasProperty("counted")) {
            processDeliveredPacket(packet);
        }
        
        // Clean up visual elements
        cleanupPacketVisuals(packet);
    }
    
    /**
     * Process scoring and feedback for delivered packets
     */
    private void processDeliveredPacket(Packet packet) {
        packet.setProperty("counted", true);
        gameState.getPacketManager().incrementVisualPacketsDelivered();
        gameState.getPacketManager().incrementPacketsDelivered();
        
        // Force level completion check
        if (gameState.isLevelCompleted()) {
            System.out.println("Level completion triggered from input port!");
        }
        
        // Add coins and play sound
        gameState.addCoins(packet.getCoinValue());
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.CONNECTION_SUCCESS);
        
        System.out.println("[FIXED] Packet delivered and processed at END system INPUT PORT! Total delivered: " + 
                          gameState.getPacketManager().getPacketsDelivered());
    }
    
    /**
     * Clean up visual elements of a packet
     */
    private void cleanupPacketVisuals(Packet packet) {
        Shape packetShape = packet.getShape();
        if (packetShape != null) {
            packetShape.setEffect(null);
            packetShape.setVisible(false);
            
            if (packetShape.getParent() != null) {
                try {
                    javafx.application.Platform.runLater(() -> {
                        if (packetShape.getParent() != null) {
                            packetShape.getParent().getChildrenUnmodifiable().remove(packetShape);
                        }
                    });
                } catch (Exception e) {
                    // Ignore removal errors
                }
            }
        }
    }
    
    /**
     * Handle packet arrival at an intermediate system
     */
    private void handleIntermediateSystemArrival(Packet packet, NetworkSystem targetSystem) {
        // Log positioning information for debugging
        if (!packet.hasProperty("manuallyPositionedInInnerBox") &&
            !targetSystem.isStartSystem() && !targetSystem.isEndSystem()) {
            System.out.println("Packet not yet positioned - flagging for inner box positioning");
        } else if (packet.hasProperty("manuallyPositionedInInnerBox")) {
            System.out.println("Packet was already positioned in inner box by Connection class");
        }
        
        // Deliver the packet to the system
        targetSystem.receivePacket(packet);
        
        // For intermediate systems, packets will remain visible but inside the system
        if (!targetSystem.isStartSystem()) {
            packet.setInsideSystem(true);
        }
    }
    
    /**
     * Remove packets that have completed their animation
     */
    private void removeFinishedPackets(List<Packet> packetsToRemove, com.networkgame.model.PacketLifecycle packetLifecycle) {
        if (!packetsToRemove.isEmpty()) {
            for (Packet packet : packetsToRemove) {
                packetLifecycle.safelyRemovePacket(packet, true);
            }
        }
    }
} 
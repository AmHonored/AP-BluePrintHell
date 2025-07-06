package com.networkgame.model.packet;

import javafx.animation.TranslateTransition;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.effect.Glow;
import javafx.scene.paint.Color;
import javafx.animation.Timeline;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.packettype.messenger.SquarePacket;
import com.networkgame.model.entity.packettype.messenger.TrianglePacket;
import com.networkgame.model.state.GameState;
import com.networkgame.service.audio.AudioManager;
import com.networkgame.model.packet.PacketLifecycle;

/**
 * Handles all packet movement and animation concerns
 */
public class PacketAnimator {
    private static final double BASE_SPEED = 100.0;
    private static final double TRIANGLE_SPEED_MULTIPLIER = 1.5;
    private static final double LEVEL_ONE_SPEED_MODIFIER = 0.5;
    
    private final NetworkSystem parentSystem;
    private final GameState gameState;
    private final List<Packet> activePackets = new ArrayList<>();
    private final PacketLifecycle packetLifecycle;

    public PacketAnimator(NetworkSystem parentSystem, GameState gameState) {
        this.parentSystem = parentSystem;
        this.gameState = gameState;
        this.packetLifecycle = new PacketLifecycle(parentSystem, gameState);
    }

    /**
     * Send a packet directly to a connection without storing it
     */
    public void sendPacketToConnection(Packet packet, Port outputPort, Connection connection) {
        if (packet == null || outputPort == null || connection == null) {
            return;
        }

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
    public void updateAnimationState() {
        long currentTime = System.currentTimeMillis();
        List<Packet> packetsToRemove = new ArrayList<>();
        List<Packet> packetsCopy = new ArrayList<>(activePackets);
        
        for (Packet packet : packetsCopy) {
            if (packet == null) {
                continue;
            }

            if (shouldSkipPacket(packet, packetsToRemove)) {
                continue;
            }
            
            if (packet.hasReachedEndSystem()) {
                packetsToRemove.add(packet);
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
        if (packet == null) {
            return;
        }

        Connection connection = getPacketConnection(packet);
        if (connection == null) {
            return;
        }
        
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
        System.out.println("[PacketAnimator] handleEndSystemArrival called for packet: " + packet.getId());
        
        // Mark packet state
        packet.setReachedEndSystem(true);
        packet.setProperty("isVisiblePacket", true);

        // Get the target system
        NetworkSystem targetSystem = null;
        if (packet.hasProperty("connection")) {
            Connection conn = (Connection)packet.getProperty("connection");
            if (conn != null && conn.getTargetPort() != null) {
                targetSystem = conn.getTargetPort().getSystem();
            }
        }

        // Process the packet for coins and scoring BEFORE cleanup
        if (!packet.hasProperty("counted") && targetSystem != null) {
            System.out.println("[PacketAnimator] Processing packet " + packet.getId() + " for end system delivery");
            targetSystem.receivePacket(packet);
        }

        // Keep packet visible briefly to show delivery, then clean up
        Platform.runLater(() -> {
            // Process delivery immediately
                    if (gameState != null && gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().render();
        }
            
            // Clean up after a short delay
            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
            delay.setOnFinished(e -> {
                // Clean up visual elements and resources
                cleanupPacketVisuals(packet);
                
                // Remove from active packets
                if (gameState != null) {
                    boolean removed = gameState.getActivePackets().remove(packet);
                    System.out.println("[PacketAnimator] Removed packet " + packet.getId() + " from activePackets: " + removed);
                    
                    // Remove packet shape - will be handled by UI layer
                    if (packet.getShape() != null && gameState.getUIUpdateListener() != null) {
                        System.out.println("[PacketAnimator] Packet shape removal will be handled by UI layer for packet " + packet.getId());
                    }
                    
                    // Update game scene
                    if (gameState.getUIUpdateListener() != null) {
                        gameState.getUIUpdateListener().render();
                    }
                }
            });
            delay.play();
        });
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
        }
        
        // Add coins and play sound
        gameState.addCoins(packet.getCoinValue());
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.CONNECTION_SUCCESS);
    }
    
    /**
     * Clean up visual elements of a packet
     */
    private void cleanupPacketVisuals(Packet packet) {
        // Stop any active animations
        if (packet.hasProperty("timeline")) {
            Timeline timeline = (Timeline)packet.getProperty("timeline");
            if (timeline != null) {
                timeline.stop();
            }
        }
        if (packet.hasProperty("timeline2")) {
            Timeline timeline2 = (Timeline)packet.getProperty("timeline2");
            if (timeline2 != null) {
                timeline2.stop();
            }
        }
        
        // Clear any visual effects
        if (packet.getShape() != null) {
            packet.getShape().setEffect(null);
        }
    }
    
    /**
     * Handle packet arrival at an intermediate system
     */
    private void handleIntermediateSystemArrival(Packet packet, NetworkSystem targetSystem) {
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
    private void removeFinishedPackets(List<Packet> packetsToRemove, PacketLifecycle packetLifecycle) {
        if (!packetsToRemove.isEmpty()) {
            for (Packet packet : packetsToRemove) {
                packetLifecycle.safelyRemovePacket(packet, true);
            }
        }
    }
} 

package com.networkgame.view;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.effect.Glow;
import javafx.geometry.Point2D;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.state.GameState;
import com.networkgame.service.audio.AudioManager;

/**
 * Handles packet visualization and animations
 */
public class PacketVisualizer {
    private GameState gameState;
    private Pane gamePane;
    private long lastWaveSoundTime = 0;
    private List<Timeline> activeTimelines = new ArrayList<>();
    
    // Animation constants
    private static final double WAVE_INITIAL_RADIUS = 5.0;
    private static final double INNER_WAVE_INITIAL_RADIUS = 3.0;
    private static final double WAVE_MID_RADIUS = 25.0;
    private static final double INNER_WAVE_MID_RADIUS = 15.0;
    private static final double WAVE_FINAL_RADIUS = 70.0;
    private static final double INNER_WAVE_FINAL_RADIUS = 40.0;
    private static final Duration WAVE_MID_DURATION = Duration.seconds(0.2);
    private static final Duration WAVE_FINAL_DURATION = Duration.seconds(0.6);
    
    public PacketVisualizer(GameState gameState, Pane gamePane) {
        this.gameState = gameState;
        this.gamePane = gamePane;
    }
    
    /**
     * Add and position packet shapes in the game pane
     */
    public void updatePackets() {
        // Skip removal during level transitions to prevent visual artifacts
        if (gameState != null && gameState.getActivePackets() != null && !gameState.getActivePackets().isEmpty()) {
            removeOldPacketShapes();
        }
        arrangeZOrder();
        addActivePackets();
    }
    
    private void removeOldPacketShapes() {
        if (gameState == null || gameState.getActivePackets() == null) {
            return;
        }
        
        // Get all currently active packet shapes
        Set<Shape> activeShapes = new HashSet<>();
        for (Packet packet : gameState.getActivePackets()) {
            if (packet != null && packet.getShape() != null) {
                activeShapes.add(packet.getShape());
            }
        }
        
        // Count current packet shapes
        long packetShapesBefore = gamePane.getChildren().stream()
            .filter(node -> node instanceof Shape && node.getStyleClass().contains("packet"))
            .count();
        
        // CRITICAL SAFEGUARD: If we have active packets but no active shapes, 
        // or if active packets list is empty but we have shapes, don't remove anything
        // This prevents removal during level transitions
        if ((gameState.getActivePackets().size() > 0 && activeShapes.isEmpty()) ||
            (gameState.getActivePackets().isEmpty() && packetShapesBefore > 0)) {
            System.out.println("PacketVisualizer: Skipping shape removal during level transition. " +
                              "Active packets: " + gameState.getActivePackets().size() + 
                              ", Active shapes: " + activeShapes.size() + 
                              ", Total shapes: " + packetShapesBefore);
            return;
        }
        
        // Only remove packet shapes that are no longer associated with active packets
        gamePane.getChildren().removeIf(node -> 
            node instanceof Shape && 
            node.getStyleClass().contains("packet") && 
            !activeShapes.contains(node)
        );
        
        // Count shapes after removal for debugging
        long packetShapesAfter = gamePane.getChildren().stream()
            .filter(node -> node instanceof Shape && node.getStyleClass().contains("packet"))
            .count();
        
        // Log only if shapes were removed (avoid spam)
        if (packetShapesBefore > packetShapesAfter) {
            System.out.println("PacketVisualizer: Removed " + (packetShapesBefore - packetShapesAfter) + 
                              " old packet shapes. Active packets: " + gameState.getActivePackets().size() + 
                              ", Remaining shapes: " + packetShapesAfter);
        }
    }
    
    private void arrangeZOrder() {
        // Add connections to the back (behind packets)
        for (Connection connection : gameState.getConnections()) {
            connection.getConnectionShape().toBack();
        }
        
        // Ensure proper z-order of systems and inner boxes
        for (NetworkSystem system : gameState.getSystems()) {
            system.getShape().toBack();
            
            // Ensure inner boxes are in front of system shapes but behind packets
            if (system.shouldShowInnerBox()) {
                Rectangle innerBox = system.getInnerBox();
                if (innerBox != null) {
                    innerBox.toFront();
                    system.getShape().toBack();
                }
            }
        }
    }
    
    private void addActivePackets() {
        // Use a set to track shapes we've already added to prevent duplicates
        Set<Shape> addedShapes = new HashSet<>();
        
        // Failsafe: Process and remove any packets that have reached the end system
        for (Iterator<Packet> it = gameState.getActivePackets().iterator(); it.hasNext(); ) {
            Packet packet = it.next();
            if (packet == null) continue;
            if (packet.hasReachedEndSystem()) {
                System.out.println("PacketVisualizer: Removing packet " + packet.getId() + " - reached end system");
                it.remove();
                continue;
            }
            
            Shape packetShape = packet.getShape();
            if (packetShape != null && !addedShapes.contains(packetShape)) {
                // Debug logging for DDoS packets
                if (packet.hasProperty("originalType")) {
                    System.out.println("PacketVisualizer: Processing DDoS packet " + packet.getId() + " for display");
                    System.out.println("PacketVisualizer:   - Position: " + packet.getPosition());
                    System.out.println("PacketVisualizer:   - Has connection: " + (packet.getCurrentConnection() != null));
                    System.out.println("PacketVisualizer:   - Shape visible: " + packetShape.isVisible());
                }
                
                // Ensure packet is properly initialized
                if (packetShape.getStyleClass().isEmpty()) {
                    applyPacketStyles(packet, packetShape);
                }
                
                // Ensure packet visibility and positioning
                ensurePacketVisibility(packet, packetShape);
                positionPacket(packet);
                
                // Add to scene if not already there
                if (!gamePane.getChildren().contains(packetShape)) {
                    gamePane.getChildren().add(packetShape);
                    if (packet.hasProperty("originalType")) {
                        System.out.println("PacketVisualizer: Added DDoS packet " + packet.getId() + " to scene");
                    }
                }
                addedShapes.add(packetShape);
            }
        }
    }
    
    private void ensurePacketVisibility(Packet packet, Shape packetShape) {
        // Always ensure packet is visible regardless of level
        packetShape.setVisible(true);
        packetShape.setOpacity(1.0);
    }
    
    private void applyPacketStyles(Packet packet, Shape packetShape) {
        // Add style classes
        packetShape.getStyleClass().add("packet");
            
        // Apply packet-type specific style
        if (packet.getType() == Packet.PacketType.SQUARE) {
            packetShape.getStyleClass().add("square-packet");
        } else if (packet.getType() == Packet.PacketType.TRIANGLE) {
            packetShape.getStyleClass().add("triangle-packet");
        }
    }
    
    private void positionPacket(Packet packet) {
        // Position packets inside systems correctly
        if (packet.isInsideSystem()) {
            positionPacketInSystem(packet, gameState.getSystems());
        } else if (packet.getCurrentConnection() != null) {
            // For packets on wires, update position based on connection
            updatePacketOnConnection(packet);
        }
    }

    /**
     * Position a packet inside a system
     */
    private void positionPacketInSystem(Packet packet, List<NetworkSystem> systems) {
        // Find which system contains this packet
        for (NetworkSystem system : systems) {
            if (system.getStoredPackets().contains(packet)) {
                Point2D centerPosition = getPacketCenterInSystem(packet, system);
                
                // Position the packet
                packet.setPosition(centerPosition);
                
                // Update shape position directly based on packet type
                updatePacketShapePosition(packet, centerPosition);
                
                break;
            }
        }
    }
    
    private Point2D getPacketCenterInSystem(Packet packet, NetworkSystem system) {
        // Use system center as default
        double centerX = system.getPosition().getX() + system.getWidth()/2;
        double centerY = system.getPosition().getY() + system.getHeight()/2;
        
        // For systems with inner boxes, use the inner box for positioning
        if (system.shouldShowInnerBox()) {
            Rectangle innerBox = system.getInnerBox();
            if (innerBox != null) {
                // Get index of packet in system's storage
                int index = system.getStoredPackets().indexOf(packet);
                int totalPackets = system.getStoredPackets().size();
                
                // Calculate grid layout
                int cols = Math.max(1, (int)Math.ceil(Math.sqrt(totalPackets)));
                int rows = (totalPackets + cols - 1) / cols;
                int row = index / cols;
                int col = index % cols;
                
                // Calculate cell size
                double padding = 5.0;
                double cellWidth = (innerBox.getWidth() - 2 * padding) / Math.max(1, cols);
                double cellHeight = (innerBox.getHeight() - 2 * padding) / Math.max(1, rows);
                
                // Calculate center position
                centerX = innerBox.getX() + padding + col * cellWidth + cellWidth/2;
                centerY = innerBox.getY() + padding + row * cellHeight + cellHeight/2;
            }
        }
        
        return new Point2D(centerX, centerY);
    }
    
    private void updatePacketShapePosition(Packet packet, Point2D centerPosition) {
        Shape packetShape = packet.getShape();
        double centerX = centerPosition.getX();
        double centerY = centerPosition.getY();
        
        if (packetShape instanceof Rectangle) {
            Rectangle rect = (Rectangle)packetShape;
            rect.setX(centerX - rect.getWidth()/2);
            rect.setY(centerY - rect.getHeight()/2);
        } else if (packetShape instanceof Polygon) {
            Polygon poly = (Polygon)packetShape;
            double size = 10.0; // Standard size
            double height = Math.sqrt(3) * size / 2;
            poly.getPoints().setAll(
                centerX, centerY - height * 2/3,  // Top point
                centerX - size/2, centerY + height/3,  // Bottom left
                centerX + size/2, centerY + height/3   // Bottom right
            );
        }
    }

    /**
     * Update a packet position along a connection
     */
    private void updatePacketOnConnection(Packet packet) {
        Connection connection = packet.getCurrentConnection();
        if (connection == null) return;
        
        Point2D sourcePos = connection.getSourcePort().getPosition();
        Point2D targetPos = connection.getTargetPort().getPosition();
        
        // Calculate position along connection based on progress
        double progress = packet.hasProperty("progress") ? 
                          packet.getProperty("progress", 0.0) : 0.0;
        
        // Calculate interpolated position
        double x = sourcePos.getX() + (targetPos.getX() - sourcePos.getX()) * progress;
        double y = sourcePos.getY() + (targetPos.getY() - sourcePos.getY()) * progress;
        
        // Update packet position
        packet.setPosition(new Point2D(x, y));
    }
    
    /**
     * Create a visual effect for impact waves
     */
    public void createImpactWaveEffect(Point2D position) {
        if (gameState.isAtar()) {
            return;
        }
        
        createWaveEffect(position, Color.DODGERBLUE, Color.WHITE, 
                         Color.color(0.4, 0.7, 1.0, 0.3), Color.color(1.0, 1.0, 1.0, 0.4),
                         3, 2, false);
        
        // Play a collision sound
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
    }
    
    /**
     * Create a wave animation at a specific point
     */
    public void createWaveAnimation(double x, double y, Color color) {
        createWaveEffect(new Point2D(x, y), color, color.brighter(), 
                         null, null, 2, 1.5, true);
        
        // Play a collision sound with throttling
        if (System.currentTimeMillis() - lastWaveSoundTime > 100) {
            AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
            lastWaveSoundTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Reusable wave effect creator with customizable parameters
     */
    private void createWaveEffect(Point2D position, Color strokeColor, Color innerStrokeColor, 
                                  Color fillColor, Color innerFillColor, 
                                  double strokeWidth, double innerStrokeWidth,
                                  boolean trackTimeline) {
        // Create the main wave circle
        Circle wave = new Circle(position.getX(), position.getY(), WAVE_INITIAL_RADIUS);
        wave.setStroke(strokeColor);
        wave.setStrokeWidth(strokeWidth);
        wave.setFill(fillColor);
        
        // Add inner wave for better visual effect
        Circle innerWave = new Circle(position.getX(), position.getY(), INNER_WAVE_INITIAL_RADIUS);
        innerWave.setStroke(innerStrokeColor);
        innerWave.setStrokeWidth(innerStrokeWidth);
        innerWave.setFill(innerFillColor);
        
        // Use z-index to ensure wave appears below packets
        wave.setViewOrder(100);
        innerWave.setViewOrder(99);
        
        // Add effects to game pane
        gamePane.getChildren().addAll(wave, innerWave);
        
        // Create animation
        Timeline waveAnimation = createWaveTimeline(wave, innerWave);
        
        // Set cleanup action
        waveAnimation.setOnFinished(e -> {
            if (gamePane.getChildren().contains(wave)) {
                gamePane.getChildren().removeAll(wave, innerWave);
            }
            if (trackTimeline) {
                activeTimelines.remove(waveAnimation);
            }
        });
        
        // Track the timeline if needed
        if (trackTimeline) {
            activeTimelines.add(waveAnimation);
        }
        
        // Start the animation
        waveAnimation.play();
    }
    
    private Timeline createWaveTimeline(Circle wave, Circle innerWave) {
        return new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(wave.radiusProperty(), WAVE_INITIAL_RADIUS),
                new KeyValue(wave.opacityProperty(), 0.9),
                new KeyValue(innerWave.radiusProperty(), INNER_WAVE_INITIAL_RADIUS),
                new KeyValue(innerWave.opacityProperty(), 1.0)
            ),
            new KeyFrame(
                WAVE_MID_DURATION,
                new KeyValue(wave.radiusProperty(), WAVE_MID_RADIUS, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(innerWave.radiusProperty(), INNER_WAVE_MID_RADIUS, javafx.animation.Interpolator.EASE_OUT)
            ),
            new KeyFrame(
                WAVE_FINAL_DURATION,
                new KeyValue(wave.radiusProperty(), WAVE_FINAL_RADIUS, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(wave.opacityProperty(), 0, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(innerWave.radiusProperty(), INNER_WAVE_FINAL_RADIUS, javafx.animation.Interpolator.EASE_OUT),
                new KeyValue(innerWave.opacityProperty(), 0, javafx.animation.Interpolator.EASE_OUT)
            )
        );
    }
    
    /**
     * Process collisions between packets and create impact effects
     */
    public void processCollisions() {
        if (gameState.isAiryaman()) {
            return;
        }
        
        List<Packet> exposedPackets = getExposedPackets();
        processPacketCollisions(exposedPackets);
    }
    
    private List<Packet> getExposedPackets() {
        return gameState.getActivePackets().stream()
            .filter(p -> p != null && !p.isInsideSystem() && !p.hasReachedEndSystem())
            .collect(Collectors.toList());
    }
    
    private void processPacketCollisions(List<Packet> exposedPackets) {
        for (int i = 0; i < exposedPackets.size(); i++) {
            Packet p1 = exposedPackets.get(i);
            
            if (p1.hasReachedEndSystem()) {
                continue;
            }
            
            for (int j = i + 1; j < exposedPackets.size(); j++) {
                Packet p2 = exposedPackets.get(j);
                
                if (p2.hasReachedEndSystem()) {
                    continue;
                }
                
                checkAndHandleCollision(p1, p2);
            }
        }
    }
    
    private void checkAndHandleCollision(Packet p1, Packet p2) {
        if (p1.getShape() == null || p2.getShape() == null) {
            return;
        }
        
        Shape intersect = Shape.intersect(p1.getShape(), p2.getShape());
        if (intersect.getBoundsInLocal().isEmpty()) {
            return; // No collision
        }
        
        Point2D collisionPoint = getCollisionPoint(intersect);
        handlePacketCollisionEffects(p1, p2, collisionPoint);
    }
    
    private Point2D getCollisionPoint(Shape intersect) {
        javafx.geometry.Bounds bounds = intersect.getBoundsInLocal();
        double x = bounds.getMinX() + bounds.getWidth() / 2;
        double y = bounds.getMinY() + bounds.getHeight() / 2;
        return new Point2D(x, y);
    }
    
    private void handlePacketCollisionEffects(Packet p1, Packet p2, Point2D collisionPoint) {
        if (gameState.isAtar()) {
            applyGlowEffect(p1);
            applyGlowEffect(p2);
        } else {
            createImpactWaveEffect(collisionPoint);
        }
        
        applyFlashEffect(p1.getShape());
        applyFlashEffect(p2.getShape());
    }
    
    private void applyGlowEffect(Packet packet) {
        if (packet.getShape() != null) {
            Glow glow = new Glow(0.3);
            packet.getShape().setEffect(glow);
        }
    }
    
    private void applyFlashEffect(Shape shape) {
        if (shape != null) {
            FadeTransition flashEffect = new FadeTransition(Duration.seconds(0.15), shape);
            flashEffect.setFromValue(1.0);
            flashEffect.setToValue(0.3);
            flashEffect.setCycleCount(4);
            flashEffect.setAutoReverse(true);
            flashEffect.play();
        }
    }
    
    /**
     * Process any packets that have been displaced too far from their wire path
     */
    public void processPacketLoss() {
        List<Packet> lostPackets = findLostPackets();
        applyFadeOutEffectsToLostPackets(lostPackets);
    }
    
    private List<Packet> findLostPackets() {
        List<Packet> lostPackets = new ArrayList<>();
        
        for (Packet packet : new ArrayList<>(gameState.getActivePackets())) {
            if (packet.isInsideSystem()) {
                continue;
            }
            
            if (isPacketLost(packet)) {
                lostPackets.add(packet);
            }
        }
        
        return lostPackets;
    }
    
    private boolean isPacketLost(Packet packet) {
        if (packet.hasReachedEndSystem()) {
            return false;
        }
        
        // Check if packet is on any connection
        for (Connection conn : gameState.getConnections()) {
            if (conn.getPackets().contains(packet)) {
                return false; // Packet is on a connection
            }
        }
        
        return true; // Packet is lost
    }
    
    private void applyFadeOutEffectsToLostPackets(List<Packet> lostPackets) {
        if (lostPackets.isEmpty()) {
            return;
        }
        
        for (Packet packet : lostPackets) {
            if (packet.getShape() != null) {
                FadeTransition fade = new FadeTransition(Duration.millis(500), packet.getShape());
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                fade.setOnFinished(event -> removePacket(packet));
                fade.play();
            } else {
                removePacket(packet);
            }
        }
    }
    
    private void removePacket(Packet packet) {
        gameState.getActivePackets().remove(packet);
        if (packet.getShape() != null) {
            gamePane.getChildren().remove(packet.getShape());
        }
    }
    
    /**
     * Render a packet along a connection wire, handling curved paths
     */
    public void renderPacketAlongConnection(Packet packet) {
        if (packet == null || packet.getCurrentConnection() == null) {
            return;
        }
        
        // Debug logging for DDoS packets
        if (packet.hasProperty("originalType")) {
            System.out.println("PacketVisualizer: Rendering DDoS packet " + packet.getId() + " along connection");
        }
        
        // Calculate visual offset for packet from the wire
        double offsetX = 0;
        double offsetY = 0;
        
        // Add small random variation to position based on packet noise
        if (packet.getNoiseLevel() > 0) {
            double noiseOffset = packet.getNoiseLevel() * 2.0; // Increased multiplier for more visible effect
            offsetX = (Math.random() - 0.5) * noiseOffset;
            offsetY = (Math.random() - 0.5) * noiseOffset;
        }
        
        // Apply visual offset to packet
        Point2D currentPos = packet.getPosition();
        packet.getShape().setTranslateX(currentPos.getX() + offsetX);
        packet.getShape().setTranslateY(currentPos.getY() + offsetY);
        
        // Ensure packet is visible
        packet.getShape().setVisible(true);
        packet.getShape().toFront();
    }
    
    /**
     * Create a visual effect for time jump/travel
     */
    public void createTimeJumpEffect() {
        // Create a full-screen flash effect
        Rectangle flash = new Rectangle(0, 0, gamePane.getWidth(), gamePane.getHeight());
        flash.setFill(Color.rgb(0, 170, 255, 0.5));
        flash.setOpacity(0.7);
        
        // Add glow effect
        Glow glow = new Glow(0.8);
        flash.setEffect(glow);
        
        // Add to game pane (on top)
        gamePane.getChildren().add(flash);
        
        // Animate the flash - fade in and out
        FadeTransition fadeOut = new FadeTransition(Duration.millis(400), flash);
        fadeOut.setFromValue(0.7);
        fadeOut.setToValue(0.0);
        fadeOut.setCycleCount(1);
        fadeOut.setOnFinished(e -> gamePane.getChildren().remove(flash));
        fadeOut.play();
        
        // Play a time travel sound effect
        try {
            AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.BUTTON_CLICK);
        } catch (Exception e) {
            // Sound playback is optional
        }
    }
    
    /**
     * Make sure all packets are properly visible
     */
    public void ensurePacketsVisible() {
        if (gameState == null || gameState.getActivePackets() == null) {
            return;
        }
        
        // During level transitions, be more aggressive about ensuring visibility
        for (Packet packet : gameState.getActivePackets()) {
            if (packet != null && packet.getShape() != null) {
                ensurePacketVisibilityAndStyling(packet);
                
                // Extra safeguard: if the packet shape is not in the scene, force re-add it
                Shape shape = packet.getShape();
                if (shape.getParent() == null || !gamePane.getChildren().contains(shape)) {
                    // Remove it first if it exists to prevent duplicates
                    gamePane.getChildren().remove(shape);
                    // Then add it back
                    gamePane.getChildren().add(shape);
                    shape.toFront();
                    System.out.println("PacketVisualizer: Force re-added packet shape for packet " + packet.getId());
                }
            }
        }
    }
    
    private void ensurePacketVisibilityAndStyling(Packet packet) {
        Shape packetShape = packet.getShape();
        
        // Force packets to be visible with full opacity
        packetShape.setVisible(true);
        packetShape.setOpacity(1.0);
        
        // Make sure the packet has proper styling
        applyPacketStyles(packet, packetShape);
        
        // Ensure the packet is in the scene graph
        if (packetShape.getParent() == null && gamePane != null) {
            gamePane.getChildren().add(packetShape);
            // Bring packet to front for visibility
            packetShape.toFront();
        }
        
        // Extra check: if the packet is supposed to be visible but has been removed
        // from the scene graph, re-add it
        if (packetShape.getParent() == null && gamePane != null && gamePane.getChildren().contains(packetShape) == false) {
            gamePane.getChildren().add(packetShape);
            packetShape.toFront();
        }
    }
    
    /**
     * Clean up any resources used by the packet visualizer
     */
    public void cleanup() {
        // Stop all running timelines
        for (Timeline timeline : activeTimelines) {
            timeline.stop();
        }
        activeTimelines.clear();
    }
} 
package com.networkgame.controller;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;
import com.networkgame.service.audio.AudioManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized collision controller with reduced redundancy and improved performance.
 */
public class CollisionController {
    // Physics constants
    private static final double EXPLOSION_RADIUS = 200.0;
    private static final double BLEND_FACTOR = 0.25;
    private static final double COLLISION_DEVIATION = 0.1;
    private static final double EPSILON = 0.001;
    private static final double WIRE_THRESHOLD_MULTIPLIER = 1.5;
    private static final double CORE_EXPLOSION_RADIUS_FACTOR = 0.3;
    private static final int MAX_COLLISION_HISTORY = 1000;
    private static final int FRAME_RATE_MS = 16; // 60fps
    
    private final GameState gameState;
    private Timeline collisionTimeline;
    private volatile boolean isPaused = false;
    
    // Collision tracking to prevent duplicate handling
    private final Set<String> currentCollisions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // Reusable list to avoid repeated allocations
    private final List<Packet> packetsCache = new ArrayList<>();
    
    public CollisionController(GameState gameState) {
        this.gameState = gameState;
    }
    
    public void startCollisionDetection() {
        stopExistingTimeline();
        System.out.println("DEBUG: Starting collision detection system");
        
        collisionTimeline = new Timeline();
        collisionTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(FRAME_RATE_MS), event -> {
            if (!isPaused) {
                checkForCollision();
                checkPacketsOffWire();
            }
        }));
        collisionTimeline.setCycleCount(Animation.INDEFINITE);
        collisionTimeline.play();
    }
    
    private void stopExistingTimeline() {
        if (collisionTimeline != null) {
            collisionTimeline.stop();
        }
    }
    
    private void checkForCollision() {
        if (gameState.isAiryaman()) return;
        
        // Maintain collision history and update packet cache
        if (currentCollisions.size() > MAX_COLLISION_HISTORY) {
            currentCollisions.clear();
        }
        packetsCache.clear();
        packetsCache.addAll(gameState.getActivePackets());
        
        int packetCount = packetsCache.size();
        if (packetCount <= 1) return;
        
        // Check all packet pairs for collision
        for (int i = 0; i < packetCount; i++) {
            Packet p1 = packetsCache.get(i);
            if (isPacketProtected(p1)) continue;
            
            Point2D pos1 = p1.getPosition();
            for (int j = i + 1; j < packetCount; j++) {
                Packet p2 = packetsCache.get(j);
                if (isPacketProtected(p2)) continue;
                
                Point2D pos2 = p2.getPosition();
                String pairKey = createPairKey(p1, p2);
                
                if (arePacketsCloseForCollision(pos1, pos2, p1.getSize(), p2.getSize()) &&
                    checkDetailedCollision(p1, p2, pairKey)) {
                    handleCollision(p1, p2, getCollisionPoint(p1.getShape(), p2.getShape()));
                }
            }
        }
    }
    
    private boolean isPacketProtected(Packet packet) {
        return packet.isInsideSystem() || packet.hasReachedEndSystem();
    }
    
    private String createPairKey(Packet p1, Packet p2) {
        String id1 = String.valueOf(p1.getId());
        String id2 = String.valueOf(p2.getId());
        return (id1.compareTo(id2) < 0) ? id1 + "-" + id2 : id2 + "-" + id1;
    }
    
    private boolean arePacketsCloseForCollision(Point2D pos1, Point2D pos2, double size1, double size2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double combinedSize = (size1 + size2) / 2.0;
        return dx * dx + dy * dy <= combinedSize * combinedSize;
    }
    
    private boolean checkDetailedCollision(Packet p1, Packet p2, String pairKey) {
        Shape s1 = p1.getShape();
        Shape s2 = p2.getShape();
        if (s1 == null || s2 == null) return false;
        
        boolean isColliding = !Shape.intersect(s1, s2).getBoundsInLocal().isEmpty();
        
        if (isColliding && !currentCollisions.contains(pairKey)) {
            currentCollisions.add(pairKey);
            return true;
        } else if (!isColliding) {
            currentCollisions.remove(pairKey);
        }
        return false;
    }
    
    private Point2D getCollisionPoint(Shape s1, Shape s2) {
        Bounds bounds = Shape.intersect(s1, s2).getBoundsInLocal();
        return new Point2D(bounds.getMinX() + bounds.getWidth() / 2, 
                          bounds.getMinY() + bounds.getHeight() / 2);
    }
    
    private void handleCollision(Packet p1, Packet p2, Point2D collisionPoint) {
        System.out.println("DEBUG: Collision detected between " + p1.getType() + " and " + p2.getType() + " packets");
        System.out.println("DEBUG: At position " + collisionPoint);
        
        // Apply damage to both packets
        p1.reduceHealth(1);
        p2.reduceHealth(1);
        
        // Apply deviation to both packets
        applyDeviationToPackets(p1, p2);
        
        System.out.println("DEBUG: Packet health after collision - " + 
                p1.getType() + ": " + p1.getHealth() + "/" + p1.getSize() + ", " +
                p2.getType() + ": " + p2.getHealth() + "/" + p2.getSize());
        
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
        
        // Handle destruction and explosion
        boolean p1Destroyed = checkAndDestroyPacket(p1);
        boolean p2Destroyed = checkAndDestroyPacket(p2);
        
        if (!gameState.isAtar() && (p1Destroyed || p2Destroyed)) {
            createExplosion(collisionPoint, p1, p2);
        }
    }
    
    private void applyDeviationToPackets(Packet p1, Packet p2) {
        for (Packet p : new Packet[]{p1, p2}) {
            double[] unit = p.getUnitVector();
            double newX = unit[0] + (Math.random() - 0.5) * COLLISION_DEVIATION;
            double newY = unit[1] + (Math.random() - 0.5) * COLLISION_DEVIATION;
            normalizeAndSetVector(p, newX, newY);
        }
    }
    
    private void normalizeAndSetVector(Packet packet, double x, double y) {
        double magnitude = Math.sqrt(x * x + y * y);
        if (magnitude > EPSILON) {
            packet.setUnitVector(x / magnitude, y / magnitude);
        }
    }
    
    private boolean checkAndDestroyPacket(Packet packet) {
        if (packet.isDestroyed()) {
            System.out.println("DEBUG: " + packet.getType() + " packet destroyed by collision");
            killPacket(packet);
            return true;
        }
        return false;
    }
    
    private void createExplosion(Point2D center, Packet excludeP1, Packet excludeP2) {
        System.out.println("DEBUG: Creating explosion at " + center);
        
        for (Packet packet : packetsCache) {
            if (packet == excludeP1 || packet == excludeP2 || isPacketProtected(packet)) {
                continue;
            }
            
            Point2D packetPos = packet.getPosition();
            double dx = packetPos.getX() - center.getX();
            double dy = packetPos.getY() - center.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            if (distance <= EXPLOSION_RADIUS && distance > EPSILON) {
                applyExplosionEffect(packet, dx, dy, distance);
            }
        }
        
        gameState.emitImpactEvent(center);
    }
    
    private void applyExplosionEffect(Packet packet, double dx, double dy, double distance) {
        // Apply deflection
        double deflectX = dx / distance;
        double deflectY = dy / distance;
        
        double[] unitVector = packet.getUnitVector();
        double newX = (1 - BLEND_FACTOR) * unitVector[0] + BLEND_FACTOR * deflectX;
        double newY = (1 - BLEND_FACTOR) * unitVector[1] + BLEND_FACTOR * deflectY;
        
        normalizeAndSetVector(packet, newX, newY);
        
        // Apply core damage if close enough
        if (distance < EXPLOSION_RADIUS * CORE_EXPLOSION_RADIUS_FACTOR) {
            packet.reduceHealth(1);
            if (packet.isDestroyed()) {
                killPacket(packet);
            }
        }
    }
    
    private void checkPacketsOffWire() {
        for (Packet packet : packetsCache) {
            if (isPacketProtected(packet) || packet.getCurrentConnection() == null) {
                continue;
            }
            
            if (isPacketOffWire(packet)) {
                handlePacketOffWire(packet);
            }
        }
    }
    
    private boolean isPacketOffWire(Packet packet) {
        Point2D wireStart = packet.getCurrentConnection().getSourcePort().getPosition();
        Point2D wireEnd = packet.getCurrentConnection().getTargetPort().getPosition();
        Point2D packetPos = packet.getPosition();
        double threshold = packet.getSize() * WIRE_THRESHOLD_MULTIPLIER;
        
        double dx = wireEnd.getX() - wireStart.getX();
        double dy = wireEnd.getY() - wireStart.getY();
        double wireLength = Math.sqrt(dx * dx + dy * dy);
        
        if (wireLength < EPSILON) {
            // Handle zero-length wire
            double pdx = packetPos.getX() - wireStart.getX();
            double pdy = packetPos.getY() - wireStart.getY();
            return pdx * pdx + pdy * pdy > threshold * threshold;
        }
        
        // Handle normal wire
        double wireX = dx / wireLength;
        double wireY = dy / wireLength;
        double packetX = packetPos.getX() - wireStart.getX();
        double packetY = packetPos.getY() - wireStart.getY();
        double t = Math.max(0, Math.min(wireLength, packetX * wireX + packetY * wireY));
        
        double closestX = wireStart.getX() + wireX * t;
        double closestY = wireStart.getY() + wireY * t;
        double cdx = packetPos.getX() - closestX;
        double cdy = packetPos.getY() - closestY;
        
        return cdx * cdx + cdy * cdy > threshold * threshold;
    }
    
    private void handlePacketOffWire(Packet packet) {
        if (!gameState.isAtar()) {
            gameState.emitImpactEvent(packet.getPosition());
        }
        killPacket(packet);
    }
    
    private void killPacket(Packet packet) {
        System.out.println("DEBUG: Killing packet at " + packet.getPosition() + 
                " (type: " + packet.getType() + ", connection: " + 
                (packet.getCurrentConnection() != null ? "yes" : "no") + ")");
        
        gameState.incrementLostPackets();
        gameState.safelyRemovePacket(packet);
        
        System.out.println("DEBUG: Updated packet loss percentage: " + 
                String.format("%.1f%%", gameState.getPacketLossPercentage()));
    }
    
    public void pause() {
        isPaused = true;
        System.out.println("DEBUG: Collision detection paused");
    }
    
    public void resume() {
        isPaused = false;
        System.out.println("DEBUG: Collision detection resumed");
    }
    
    public void stop() {
        if (collisionTimeline != null) {
            collisionTimeline.stop();
            collisionTimeline = null;
        }
        isPaused = true;
        System.out.println("DEBUG: Collision detection stopped");
    }
    
    public void cleanup() {
        stop();
        int collisionsCleared = currentCollisions.size();
        int packetsCleared = packetsCache.size();
        currentCollisions.clear();
        packetsCache.clear();
        System.out.println("DEBUG: Collision controller cleaned up - cleared " + 
                collisionsCleared + " collisions and " + packetsCleared + " cached packets");
    }
} 


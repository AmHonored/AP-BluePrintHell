package com.networkgame.controller;

import com.networkgame.model.Packet;
import com.networkgame.model.GameState;
import com.networkgame.model.AudioManager;
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
 * Controller that handles all collision-related operations.
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
        KeyFrame kf = new KeyFrame(Duration.millis(FRAME_RATE_MS), event -> {
            if (!isPaused) {
                checkForCollison();
                checkPacketsOffWire();
            }
        });
        
        collisionTimeline.getKeyFrames().add(kf);
        collisionTimeline.setCycleCount(Animation.INDEFINITE);
        collisionTimeline.play();
    }
    
    private void stopExistingTimeline() {
        if (collisionTimeline != null) {
            collisionTimeline.stop();
        }
    }
    
    private void checkForCollison() {
        if (gameState.isAiryaman()) return;
        
        maintainCollisionHistory();
        updatePacketCache();
        
        int packetCount = packetsCache.size();
        if (packetCount <= 1) return;
        
        checkPacketPairsForCollision(packetCount);
    }
    
    private void maintainCollisionHistory() {
        if (currentCollisions.size() > MAX_COLLISION_HISTORY) {
            currentCollisions.clear();
        }
    }
    
    private void updatePacketCache() {
        packetsCache.clear();
        packetsCache.addAll(gameState.getActivePackets());
    }
    
    private void checkPacketPairsForCollision(int packetCount) {
        for (int i = 0; i < packetCount; i++) {
            Packet p1 = packetsCache.get(i);
            if (isPacketProtected(p1)) continue;
            
            for (int j = i + 1; j < packetCount; j++) {
                Packet p2 = packetsCache.get(j);
                if (isPacketProtected(p2)) continue;
                
                String pairKey = createPairKey(p1, p2);
                
                if (arePacketsCloseEnoughForCollision(p1, p2)) {
                    checkDetailedCollision(p1, p2, pairKey);
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
    
    private boolean arePacketsCloseEnoughForCollision(Packet p1, Packet p2) {
        Point2D pos1 = p1.getPosition();
        Point2D pos2 = p2.getPosition();
        
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double distanceSquared = dx * dx + dy * dy;
        
        double combinedSize = (p1.getSize() + p2.getSize()) / 2.0;
        double thresholdSquared = combinedSize * combinedSize;
        
        return distanceSquared <= thresholdSquared;
    }
    
    private void checkDetailedCollision(Packet p1, Packet p2, String pairKey) {
        if (p1.getShape() == null || p2.getShape() == null) {
            return;
        }
        
        Shape intersect = Shape.intersect(p1.getShape(), p2.getShape());
        boolean isColliding = !intersect.getBoundsInLocal().isEmpty();
        
        if (isColliding && !currentCollisions.contains(pairKey)) {
            handleNewCollision(p1, p2, pairKey, getCollisionPoint(intersect));
        } else if (!isColliding) {
            currentCollisions.remove(pairKey);
        }
    }
    
    private Point2D getCollisionPoint(Shape intersect) {
        Bounds bounds = intersect.getBoundsInLocal();
        double x = bounds.getMinX() + bounds.getWidth() / 2;
        double y = bounds.getMinY() + bounds.getHeight() / 2;
        return new Point2D(x, y);
    }
    
    private void handleNewCollision(Packet p1, Packet p2, String pairKey, Point2D collisionPoint) {
        currentCollisions.add(pairKey);
        
        logCollision(p1, p2, collisionPoint);
        
        handleCollision(p1, p2);
        addSmallDeviation(p1, p2);
        
        if (!gameState.isAtar()) {
            System.out.println("DEBUG: Creating explosion at " + collisionPoint);
            createExplosion(collisionPoint, p1, p2);
        }
    }
    
    private void logCollision(Packet p1, Packet p2, Point2D collisionPoint) {
        System.out.println("DEBUG: Collision detected between " + p1.getType() + " and " + p2.getType() + " packets");
        System.out.println("DEBUG: At position " + collisionPoint);
    }
    
    private void addSmallDeviation(Packet p1, Packet p2) {
        for (Packet p : new Packet[]{p1, p2}) {
            double[] unit = p.getUnitVector();
            double newX = unit[0] + (Math.random() - 0.5) * COLLISION_DEVIATION;
            double newY = unit[1] + (Math.random() - 0.5) * COLLISION_DEVIATION;
            double magnitude = Math.sqrt(newX * newX + newY * newY);
            
            if (magnitude > EPSILON) {
                p.setUnitVector(newX / magnitude, newY / magnitude);
            }
        }
    }
    
    private void handleCollision(Packet p1, Packet p2) {
        p1.reduceHealth(1);
        p2.reduceHealth(1);
        
        logPacketHealthStatus(p1, p2);
        playCollisionSound();
        
        checkAndHandlePacketDestruction(p1);
        checkAndHandlePacketDestruction(p2);
    }
    
    private void logPacketHealthStatus(Packet p1, Packet p2) {
        System.out.println("DEBUG: Packet health after collision - " + 
                          p1.getType() + ": " + p1.getHealth() + "/" + p1.getSize() + ", " +
                          p2.getType() + ": " + p2.getHealth() + "/" + p2.getSize());
    }
    
    private void checkAndHandlePacketDestruction(Packet packet) {
        if (packet.isDestroyed()) {
            System.out.println("DEBUG: " + packet.getType() + " packet destroyed by collision");
            killPacket(packet);
        }
    }
    
    private void createExplosion(Point2D explosionCenter, Packet excludeP1, Packet excludeP2) {
        updatePacketCache();
        
        for (Packet packet : packetsCache) {
            if (packet == excludeP1 || packet == excludeP2 || isPacketProtected(packet)) {
                continue;
            }
            
            Point2D packetPos = packet.getPosition();
            double distanceToExplosion = packetPos.distance(explosionCenter);
            
            if (distanceToExplosion <= EXPLOSION_RADIUS) {
                applyExplosionEffectToPacket(packet, packetPos, explosionCenter, distanceToExplosion);
            }
        }
        
        gameState.emitImpactEvent(explosionCenter);
    }
    
    private void applyExplosionEffectToPacket(Packet packet, Point2D packetPos, Point2D explosionCenter, double distanceToExplosion) {
        double dx = packetPos.getX() - explosionCenter.getX();
        double dy = packetPos.getY() - explosionCenter.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > EPSILON) {
            applyDeflectionToPacket(packet, dx, dy, distance);
            
            if (distanceToExplosion < EXPLOSION_RADIUS * CORE_EXPLOSION_RADIUS_FACTOR) {
                damagePacketInExplosionCore(packet);
            }
        }
    }
    
    private void applyDeflectionToPacket(Packet packet, double dx, double dy, double distance) {
        double deflectX = dx / distance;
        double deflectY = dy / distance;
        
        double[] unitVector = packet.getUnitVector();
        double newUnitX = (1 - BLEND_FACTOR) * unitVector[0] + BLEND_FACTOR * deflectX;
        double newUnitY = (1 - BLEND_FACTOR) * unitVector[1] + BLEND_FACTOR * deflectY;
        
        double magnitude = Math.sqrt(newUnitX * newUnitX + newUnitY * newUnitY);
        if (magnitude > EPSILON) {
            packet.setUnitVector(newUnitX / magnitude, newUnitY / magnitude);
        }
    }
    
    private void damagePacketInExplosionCore(Packet packet) {
        packet.reduceHealth(1);
        if (packet.isDestroyed()) {
            killPacket(packet);
        }
    }
    
    private void checkPacketsOffWire() {
        updatePacketCache();
        
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
        
        // Handle zero-length wires (avoid division by zero)
        Point2D wireVector = wireEnd.subtract(wireStart);
        double wireLength = wireVector.magnitude();
        
        if (wireLength < EPSILON) {
            return isPacketOffZeroLengthWire(packetPos, wireStart, threshold);
        }
        
        return isPacketOffNormalWire(packetPos, wireStart, wireVector, wireLength, threshold);
    }
    
    private boolean isPacketOffZeroLengthWire(Point2D packetPos, Point2D wireStart, double threshold) {
        double dx = packetPos.getX() - wireStart.getX();
        double dy = packetPos.getY() - wireStart.getY();
        return dx*dx + dy*dy > threshold*threshold;
    }
    
    private boolean isPacketOffNormalWire(Point2D packetPos, Point2D wireStart, Point2D wireVector, double wireLength, double threshold) {
        Point2D wireDirection = wireVector.normalize();
        double dotProduct = packetPos.subtract(wireStart).dotProduct(wireDirection);
        double t = Math.max(0, Math.min(wireLength, dotProduct));
        Point2D closestPoint = wireStart.add(wireDirection.multiply(t));
        
        double dx = packetPos.getX() - closestPoint.getX();
        double dy = packetPos.getY() - closestPoint.getY();
        return dx*dx + dy*dy > threshold*threshold;
    }
    
    private void handlePacketOffWire(Packet packet) {
        if (!gameState.isAtar()) {
            gameState.emitImpactEvent(packet.getPosition());
        }
        killPacket(packet);
    }
    
    private void playCollisionSound() {
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
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
                          collisionsCleared + " collisions and " + 
                          packetsCleared + " cached packets");
    }
} 
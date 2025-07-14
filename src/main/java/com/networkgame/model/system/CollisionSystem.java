package com.networkgame.model.system; 

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;

/**
 * Handles packet collisions and related effects
 */
public class CollisionSystem {
    private final GameState gameState;
    private final Set<String> currentCollisions = new HashSet<>();
    
    // Feature flags
    private boolean airyaman = false; // When true, disables collisions
    private boolean atar = false;     // When true, disables explosion effects
    
    // Collision disabling
    private boolean collisionDisabled = false;
    private long collisionDisabledUntil = 0;
    
    public CollisionSystem(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Check for collisions between active packets
     * DISABLED: This system conflicts with CollisionController. Use CollisionController instead.
     */
    public void checkCollisions() {
        // DISABLED: This collision system conflicts with CollisionController
        // The CollisionController handles all collision detection and response
        return;
    }
    
    private void processPacketCollisions(List<Packet> activePackets) {
        int numPackets = activePackets.size();
        
        for (int i = 0; i < numPackets; i++) {
            Packet p1 = activePackets.get(i);
            if (p1.hasReachedEndSystem() || p1.isInsideSystem()) continue;
            
            for (int j = i + 1; j < numPackets; j++) {
                Packet p2 = activePackets.get(j);
                if (p2.hasReachedEndSystem() || p2.isInsideSystem()) continue;
                
                String key = createCollisionKey(p1.getId(), p2.getId());
                
                if (checkAndHandleCollision(p1, p2, key, activePackets)) {
                    i--; // Adjust index since p1 was removed
                    break;
                }
            }
        }
    }
    
    private String createCollisionKey(long id1, long id2) {
        return (id1 < id2) ? id1 + "-" + id2 : id2 + "-" + id1;
    }
    
    private boolean checkAndHandleCollision(Packet p1, Packet p2, String key, List<Packet> packets) {
        if (p1.getShape() == null || p2.getShape() == null) return false;
        
        Shape intersect = Shape.intersect(p1.getShape(), p2.getShape());
        boolean isColliding = !intersect.getBoundsInLocal().isEmpty();
        
        if (isColliding && !currentCollisions.contains(key)) {
            currentCollisions.add(key);
            
            // Get collision location for effects
            javafx.geometry.Bounds bounds = intersect.getBoundsInLocal();
            Point2D collisionPoint = new Point2D(
                bounds.getMinX() + bounds.getWidth() / 2, 
                bounds.getMinY() + bounds.getHeight() / 2
            );
            
            // Reduce health for both packets
            p1.reduceHealth(1);
            p2.reduceHealth(1);
            
            // Handle packet removal if needed
            boolean p1Removed = p1.getHealth() <= 0 && removePacket(p1, packets);
            if (p2.getHealth() <= 0) removePacket(p2, packets);
            
            // Create impact effect if enabled
            if (!gameState.getImpactEffectSystem().isImpactEffectDisabled() && !atar) {
                createExplosion(collisionPoint);
            }
            
            return p1Removed;
        } else if (!isColliding) {
            currentCollisions.remove(key);
        }
        
        return false;
    }
    
    private boolean removePacket(Packet packet, List<Packet> packets) {
        gameState.getPacketManager().incrementLostPackets();
        return packets.remove(packet);
    }
    
    private void createExplosion(Point2D center) {
        double radius = 200.0;
        List<Packet> packets = gameState.getPacketManager().getActivePackets();
        
        // Apply explosion effects to nearby packets
        for (Packet packet : new ArrayList<>(packets)) {
            if (packet.hasReachedEndSystem() || packet.isInsideSystem()) continue;
            
            double distance = packet.getPosition().distance(center);
            if (distance > radius) continue;
            
            // Apply explosion force
            double strength = 1.0 - (distance / radius);
            Point2D deflectDir = packet.getPosition().subtract(center).normalize();
            Point2D currentVel = packet.getVelocity();
            
            // Blend velocity (25% influence from explosion)
            Point2D newVel = currentVel.multiply(0.75).add(deflectDir.multiply(strength * 20.0));
            
            // Normalize and maintain speed
            if (newVel.magnitude() > 0) {
                newVel = newVel.normalize().multiply(packet.getSpeed());
            }
            
            packet.setVelocity(newVel);
            
            // Damage packets close to explosion center
            if (distance < radius * 0.3) {
                packet.reduceHealth(1);
                if (packet.getHealth() <= 0) removePacket(packet, packets);
            }
        }
        
        // Create visual impact wave
        gameState.getImpactEffectSystem().applyImpactWave(center, 150.0);
    }
    
    public boolean isCollisionDisabled() {
        if (collisionDisabled && System.currentTimeMillis() > collisionDisabledUntil) {
            collisionDisabled = false;
        }
        return collisionDisabled;
    }
    
    public void disableCollision(int seconds) {
        if (seconds <= 0) return;
        collisionDisabled = true;
        collisionDisabledUntil = System.currentTimeMillis() + (seconds * 1000);
        System.out.println("Collisions disabled for " + seconds + " seconds");
    }
    
    public void reset() {
        currentCollisions.clear();
        collisionDisabled = false;
    }
    
    // Feature flag getters and setters
    public void setAiryaman(boolean airyaman) { this.airyaman = airyaman; }
    public boolean isAiryaman() { return airyaman; }
    public void setAtar(boolean atar) { this.atar = atar; }
    public boolean isAtar() { return atar; }
} 

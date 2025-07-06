package com.networkgame.model.system; 

import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.packettype.messenger.TrianglePacket;
import com.networkgame.service.audio.AudioManager;


/**
 * Manages impact wave effects and their visualization
 */
public class ImpactEffectSystem {
    private GameState gameState;
    
    // Impact effect disabling
    private boolean impactEffectDisabled = false;
    private long impactEffectDisabledUntil = 0;
    
    // Impact event tracking
    private List<Point2D> impactEventPoints = new ArrayList<>();
    private List<Long> impactEventTimes = new ArrayList<>();
    private static final long IMPACT_EVENT_DURATION = 1000; // Events last 1 second
    
    // Physics constants
    private static final double TRIANGLE_IMPACT_MULTIPLIER = 60.0;
    private static final double SQUARE_IMPACT_MULTIPLIER = 90.0;
    private static final double TRIANGLE_IMPACT_FACTOR = 0.3;
    private static final double TRIANGLE_NOISE_FACTOR = 0.2;
    private static final double SQUARE_NOISE_FACTOR = 0.4;
    private static final double DIVERSION_MULTIPLIER = 30.0;
    private static final double TRIANGLE_DIVERSION_FACTOR = 0.3;
    private static final double SQUARE_DIVERSION_FACTOR = 0.6;
    private static final double TRIANGLE_THRESHOLD_MULTIPLIER = 1.2;
    private static final double SQUARE_THRESHOLD_MULTIPLIER = 0.9;
    private static final double RADIUS_NOISE_THRESHOLD = 0.3;
    
    public ImpactEffectSystem(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Apply an impact wave effect to packets near a collision point
     * @param center The center point of the impact
     * @param radius The radius of impact
     */
    public void applyImpactWave(Point2D center, double radius) {
        if (isImpactEffectDisabled()) return;
        
        emitImpactEvent(center);
        
        List<Packet> packetsToProcess = new ArrayList<>(gameState.getPacketManager().getActivePackets());
        
        for (Packet packet : packetsToProcess) {
            if (packet.isInsideSystem() || packet.hasReachedEndSystem()) continue;
            
            double distance = center.distance(packet.getPosition());
            if (distance > radius) continue;
            
            double impactStrength = 1.0 - (distance / radius);
            Point2D impactDirection = packet.getPosition().subtract(center).normalize();
            
            applyImpactToPacket(packet, impactDirection, impactStrength, distance, radius);
            
            Connection connection = findPacketConnection(packet);
            if (connection != null) {
                handlePacketConnection(packet, connection, impactStrength);
            }
        }
    }
    
    private void applyImpactToPacket(Packet packet, Point2D impactDirection, double impactStrength, double distance, double radius) {
        boolean isTriangle = packet instanceof TrianglePacket;
        
        double multiplier = isTriangle ? TRIANGLE_IMPACT_MULTIPLIER : SQUARE_IMPACT_MULTIPLIER;
        Point2D impactForce = impactDirection.multiply(impactStrength * multiplier);
        Point2D currentVelocity = packet.getVelocity();
        Point2D newVelocity;
        
        if (isTriangle) {
            double impactFactor = TRIANGLE_IMPACT_FACTOR * impactStrength;
            newVelocity = currentVelocity.multiply(1 - impactFactor)
                          .add(impactForce.multiply(impactFactor));
            
            if (distance < radius * RADIUS_NOISE_THRESHOLD) {
                packet.addNoise(impactStrength * TRIANGLE_NOISE_FACTOR);
            }
        } else {
            newVelocity = currentVelocity.add(impactForce);
            packet.addNoise(impactStrength * SQUARE_NOISE_FACTOR);
        }
        
        packet.setVelocity(newVelocity);
    }
    
    private void handlePacketConnection(Packet packet, Connection connection, double impactStrength) {
        Point2D closestPoint = projectPointToConnection(packet.getPosition(), connection);
        double deviationDistance = packet.getPosition().distance(closestPoint);
        Point2D diversionDirection = packet.getPosition().subtract(closestPoint).normalize();
        
        boolean isTriangle = packet instanceof TrianglePacket;
        double diversionFactor = isTriangle ? TRIANGLE_DIVERSION_FACTOR : SQUARE_DIVERSION_FACTOR;
        
        Point2D diversionVel = diversionDirection.multiply(impactStrength * DIVERSION_MULTIPLIER * diversionFactor);
        packet.setVelocity(packet.getVelocity().add(diversionVel));
        
        double thresholdMultiplier = isTriangle ? TRIANGLE_THRESHOLD_MULTIPLIER : SQUARE_THRESHOLD_MULTIPLIER;
        double threshold = packet.getSize() * thresholdMultiplier;
        
        if (deviationDistance > threshold) {
            handlePacketLoss(packet);
        }
    }
    
    private void handlePacketLoss(Packet packet) {
        gameState.getPacketManager().incrementLostPackets();
        gameState.getPacketManager().getActivePackets().remove(packet);
        
        packet.setProperty("lostPosition", packet.getPosition());
        packet.setProperty("lostReason", "deviation");
        
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
        
        if (gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().render();
        }
    }
    
    private Connection findPacketConnection(Packet packet) {
        for (Connection connection : gameState.getConnectionManager().getConnections()) {
            if (connection.getPackets().contains(packet)) {
                return connection;
            }
        }
        return null;
    }
    
    private Point2D projectPointToConnection(Point2D point, Connection connection) {
        Point2D start = connection.getSourcePort().getPosition();
        Point2D end = connection.getTargetPort().getPosition();
        Point2D line = end.subtract(start);
        Point2D pointToStart = point.subtract(start);
        
        double lineLength = line.magnitude();
        double t = pointToStart.dotProduct(line) / (lineLength * lineLength);
        t = Math.max(0, Math.min(1, t)); // Clamp t between 0 and 1
        
        return start.add(line.multiply(t));
    }
    
    /**
     * Emit an impact wave event at the specified position
     * The view layer will use this to create visual effects
     */
    public void emitImpactEvent(Point2D position) {
        impactEventPoints.add(position);
        impactEventTimes.add(System.currentTimeMillis());
    }
    
    /**
     * Get current impact event positions
     * The view layer uses this to render impact waves
     */
    public List<Point2D> getImpactEventPositions() {
        cleanupExpiredImpactEvents();
        return impactEventPoints;
    }
    
    /**
     * Clean up expired impact events
     */
    public void cleanupExpiredImpactEvents() {
        long currentTime = System.currentTimeMillis();
        
        Iterator<Long> timeIterator = impactEventTimes.iterator();
        Iterator<Point2D> pointIterator = impactEventPoints.iterator();
        
        while (timeIterator.hasNext()) {
            if (currentTime - timeIterator.next() > IMPACT_EVENT_DURATION) {
                timeIterator.remove();
                pointIterator.next();
                pointIterator.remove();
            } else {
                pointIterator.next();
            }
        }
    }
    
    /**
     * Checks if impact effect is currently disabled
     * @return true if impact effect is disabled
     */
    public boolean isImpactEffectDisabled() {
        if (impactEffectDisabled && System.currentTimeMillis() > impactEffectDisabledUntil) {
            impactEffectDisabled = false;
        }
        return impactEffectDisabled;
    }
    
    /**
     * Disable impact effect for the specified number of seconds
     * @param seconds Number of seconds to disable impact effect
     */
    public void disableImpactEffect(int seconds) {
        if (seconds <= 0) return;
        
        impactEffectDisabled = true;
        impactEffectDisabledUntil = System.currentTimeMillis() + (seconds * 1000);
        
        System.out.println("Impact effect disabled for " + seconds + " seconds");
    }
    
    /**
     * Reset impact effect system
     */
    public void reset() {
        impactEventPoints.clear();
        impactEventTimes.clear();
        impactEffectDisabled = false;
    }
} 

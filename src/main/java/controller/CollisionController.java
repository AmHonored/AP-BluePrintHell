package controller;

import model.logic.system.CollisionSystem;
import model.levels.Level;
import model.entity.packets.Packet;
import java.util.HashSet;
import java.util.Set;
import manager.game.ImpactManager;
import controller.PacketController;
import javafx.geometry.Point2D;

public class CollisionController {
    private final Level level;
    private final CollisionSystem collisionSystem;
    private final Set<String> activeCollisionPairs = new HashSet<>();
    private final PacketController packetController;

    public CollisionController(Level level, PacketController packetController) {
        this.level = level;
        this.packetController = packetController;
        this.collisionSystem = new CollisionSystem(packetController); // Pass PacketController to CollisionSystem
    }

    public void runCollisionCheck() {
        if (level.isPaused()) return;
        if (level.isCollisionsDisabled()) return; // Skip if collisions are disabled
        
        java.util.List<Packet> packets = new java.util.ArrayList<>(level.getPackets());
        for (int i = 0; i < packets.size(); i++) {
            Packet p1 = packets.get(i);
            if (p1.isInSystem()) continue;
            for (int j = i + 1; j < packets.size(); j++) {
                Packet p2 = packets.get(j);
                if (p2.isInSystem()) continue;
                String pairKey = p1.getId() + ":" + p2.getId();
                if (collisionSystem.detectCollision(p1, p2)) {
                    if (!activeCollisionPairs.contains(pairKey)) {
                        // Calculate collision point (midpoint)
                        Point2D collisionPoint = new Point2D(
                            (p1.getPosition().getX() + p2.getPosition().getX()) / 2.0,
                            (p1.getPosition().getY() + p2.getPosition().getY()) / 2.0
                        );
                        
                        // Log the collision detection
                        System.out.println("COLLISION DETECTED: " + p1.getId() + " (" + p1.getType() + ") collided with " + 
                                          p2.getId() + " (" + p2.getType() + ") at (" + collisionPoint.getX() + ", " + collisionPoint.getY() + ")");
                        
                        // Handle collision response (damage and effects)
                        handleCollisionResponse(p1, p2, collisionPoint);
                        
                        activeCollisionPairs.add(pairKey);
                        
                        // Log collision detection for debugging
                        System.out.println("🔥 COLLISION DETECTED: " + p1.getId() + " (" + p1.getType() + ") vs " + 
                                          p2.getId() + " (" + p2.getType() + ") at (" + 
                                          String.format("%.1f", collisionPoint.getX()) + ", " + 
                                          String.format("%.1f", collisionPoint.getY()) + ")");
                        
                        // Play packet damage sound
                        service.AudioManager.playPacketDamage();
                    }
                } else {
                    activeCollisionPairs.remove(pairKey);
                }
            }
        }
    }
    
    /**
     * Handle collision response including health reduction and impact waves
     */
    private void handleCollisionResponse(Packet p1, Packet p2, Point2D collisionPoint) {
        // Reduce health for both packets
        p1.takeDamage(1);
        p2.takeDamage(1);
        
        // Check if packets should be destroyed
        if (!p1.isAlive()) {
            System.out.println("COLLISION: Packet " + p1.getId() + " destroyed due to collision");
            packetController.killPacket(p1);
        }
        
        if (!p2.isAlive()) {
            System.out.println("COLLISION: Packet " + p2.getId() + " destroyed due to collision");
            packetController.killPacket(p2);
        }
        
        // Handle impact wave if not disabled
        if (!level.isImpactDisabled()) {
            ImpactManager.handleImpactWave(collisionPoint, level.getPackets(), packetController);
        } else {
            System.out.println("IMPACT WAVES DISABLED: Skipping impact wave generation");
        }
    }
}
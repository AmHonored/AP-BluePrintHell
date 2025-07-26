package model.logic.Collision;

import model.levels.Level;
import model.entity.packets.Packet;
import model.entity.packets.HexagonPacket;
import javafx.geometry.Point2D;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

public class CollisionDetector {
    private final Level level;
    private final Set<String> activeCollisionPairs = new HashSet<>();
    
    public CollisionDetector(Level level) {
        this.level = level;
    }
    
    /**
     * Run collision detection for all packets in the level
     */
    public void runCollisionCheck() {
        if (level.isPaused()) return;
        if (level.isCollisionsDisabled()) return;
        
        List<Packet> packets = new java.util.ArrayList<>(level.getPackets());
        for (int i = 0; i < packets.size(); i++) {
            Packet p1 = packets.get(i);
            if (p1.isInSystem()) continue;
            for (int j = i + 1; j < packets.size(); j++) {
                Packet p2 = packets.get(j);
                if (p2.isInSystem()) continue;
                String pairKey = p1.getId() + ":" + p2.getId();
                if (detectCollision(p1, p2)) {
                    if (!activeCollisionPairs.contains(pairKey)) {
                        // Calculate collision point (midpoint)
                        Point2D collisionPoint = new Point2D(
                            (p1.getPosition().getX() + p2.getPosition().getX()) / 2.0,
                            (p1.getPosition().getY() + p2.getPosition().getY()) / 2.0
                        );
                        
                        // Log the collision detection
                        System.out.println("COLLISION DETECTED: " + p1.getId() + " (" + p1.getType() + ") collided with " + 
                                          p2.getId() + " (" + p2.getType() + ") at (" + collisionPoint.getX() + ", " + collisionPoint.getY() + ")");
                        
                        activeCollisionPairs.add(pairKey);
                        
                        // Log collision detection for debugging
                        System.out.println("🔥 COLLISION DETECTED: " + p1.getId() + " (" + p1.getType() + ") vs " + 
                                          p2.getId() + " (" + p2.getType() + ") at (" + 
                                          String.format("%.1f", collisionPoint.getX()) + ", " + 
                                          String.format("%.1f", collisionPoint.getY()) + ")");
                        
                        // Debug output for hexagon packets
                        if (p1.getType() == model.entity.packets.PacketType.HEXAGON || p2.getType() == model.entity.packets.PacketType.HEXAGON) {
                            System.out.println("🔶 HEXAGON COLLISION: " + p1.getId() + " (" + p1.getType() + ") vs " + p2.getId() + " (" + p2.getType() + ")");
                        }
                        
                        // Play packet damage sound
                        service.AudioManager.playPacketDamage();
                        
                        // Notify collision occurred (CollisionController will handle hexagon direction changes)
                        onCollisionDetected(p1, p2, collisionPoint);
                    }
                } else {
                    activeCollisionPairs.remove(pairKey);
                }
            }
        }
    }
    
    /**
     * Basic collision detection using packet intersection
     */
    private boolean detectCollision(Packet p1, Packet p2) {
        return p1.intersects(p2);
    }
    
    /**
     * Callback method for when collision is detected
     * This will be overridden by the controller to handle response
     */
    protected void onCollisionDetected(Packet p1, Packet p2, Point2D collisionPoint) {
        // Default implementation - override in controller
    }
}

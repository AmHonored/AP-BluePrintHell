package model.logic.Collision;

import model.levels.Level;
import model.entity.packets.Packet;
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
    
    public void runCollisionCheck() {
        if (level.isPaused()) return;
        if (level.isCollisionsDisabled()) return;
        if (level.isGameOver()) return;
        
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
                        Point2D collisionPoint = new Point2D(
                            (p1.getPosition().getX() + p2.getPosition().getX()) / 2.0,
                            (p1.getPosition().getY() + p2.getPosition().getY()) / 2.0
                        );

                        activeCollisionPairs.add(pairKey);

                        service.AudioManager.playPacketDamage();
                        onCollisionDetected(p1, p2, collisionPoint);
                    }
                } else {
                    activeCollisionPairs.remove(pairKey);
                }
            }
        }
    }
    
    private boolean detectCollision(Packet p1, Packet p2) {
        return p1.intersects(p2);
    }
    
    protected void onCollisionDetected(Packet p1, Packet p2, Point2D collisionPoint) {
    }
}

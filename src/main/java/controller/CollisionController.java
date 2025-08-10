package controller;

import model.logic.Collision.CollisionDetector;
import model.levels.Level;
import model.entity.packets.Packet;
import manager.game.CollisionManager;
import manager.game.ImpactManager;
import javafx.geometry.Point2D;
import model.entity.packets.HexagonPacket;

public class CollisionController {
    private final Level level;
    private final CollisionDetector collisionDetector;
    private final PacketController packetController;

    public CollisionController(Level level, PacketController packetController) {
        this.level = level;
        this.packetController = packetController;
        this.collisionDetector = new CollisionDetector(level) {
            @Override
            protected void onCollisionDetected(Packet p1, Packet p2, Point2D collisionPoint) {
                handleCollisionResponse(p1, p2, collisionPoint);
            }
        };
    }

    public void runCollisionCheck() {
        collisionDetector.runCollisionCheck();
    }
    
    private void handleCollisionResponse(Packet p1, Packet p2, Point2D collisionPoint) {

        if (p1 instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) p1;
            if (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD) {
                hexPacket.changeDirection();
            }
        }
        if (p2 instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) p2;
            if (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD) {
                hexPacket.changeDirection();
            }
        }
        
        p1.takeDamage(1);
        p2.takeDamage(1);
        
        if (!p1.isAlive()) {
            packetController.killPacket(p1);
        }
        
        if (!p2.isAlive()) {
            packetController.killPacket(p2);
        }
        
        CollisionManager.applyCollisionDeflection(p1, p2);
        CollisionManager.cleanupOffWire(level.getPackets(), packetController);

        if (!level.isImpactDisabled()) {
            ImpactManager.handleImpactWave(collisionPoint, level.getPackets(), packetController);
        }
    }
}
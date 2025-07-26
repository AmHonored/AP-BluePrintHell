package controller;

import model.logic.Collision.CollisionDetector;
import model.logic.system.CollisionSystem;
import model.levels.Level;
import model.entity.packets.Packet;
import manager.game.ImpactManager;
import controller.PacketController;
import javafx.geometry.Point2D;
import model.entity.packets.HexagonPacket;

public class CollisionController {
    private final Level level;
    private final CollisionSystem collisionSystem;
    private final CollisionDetector collisionDetector;
    private final PacketController packetController;

    public CollisionController(Level level, PacketController packetController) {
        this.level = level;
        this.packetController = packetController;
        this.collisionSystem = new CollisionSystem(packetController);
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
    
    /**
     * Handle collision response including health reduction and impact waves
     */
    private void handleCollisionResponse(Packet p1, Packet p2, Point2D collisionPoint) {
        // Debug output for hexagon packets
        if (p1.getType() == model.entity.packets.PacketType.HEXAGON || p2.getType() == model.entity.packets.PacketType.HEXAGON) {
            System.out.println("🔶 HEXAGON COLLISION RESPONSE: Processing collision between " + p1.getId() + " and " + p2.getId());
        }
        
        // Handle hexagon packet direction changes - ONLY ONCE
        if (p1 instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) p1;
            if (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD) {
                hexPacket.changeDirection();
                System.out.println("🔄 HEXAGON COLLISION: " + p1.getId() + " changed to RETURNING due to collision");
            }
        }
        if (p2 instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) p2;
            if (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD) {
                hexPacket.changeDirection();
                System.out.println("🔄 HEXAGON COLLISION: " + p2.getId() + " changed to RETURNING due to collision");
            }
        }
        
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
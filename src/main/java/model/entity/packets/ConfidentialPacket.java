package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public abstract class ConfidentialPacket extends Packet {
    private static final int CONFIDENTIAL_HEALTH = 4;
    private static final double BASE_SPEED = 60.0;
    private static final double PENTAGON_SIZE = 16.0;

    public ConfidentialPacket(String id, PacketType type, Point2D position, Point2D direction, int coinValue) {
        super(id, type, coinValue, position, direction, CONFIDENTIAL_HEALTH);
    }
    
    // Constructor for packets with custom health
    protected ConfidentialPacket(String id, PacketType type, Point2D position, Point2D direction, int coinValue, int health) {
        super(id, type, coinValue, position, direction, health);
    }

    @Override
    public double getSpeed() {
        // Both types have constant base speed - specific behavior handled by managers
        return BASE_SPEED;
    }

    @Override
    public Shape getCollisionShape() {
        // Create pentagon collision shape
        Polygon pentagon = new Polygon();
        double half = PENTAGON_SIZE / 2.0;
        double x = getPosition().getX();
        double y = getPosition().getY();
        
        // Pentagon vertices (5 points around a circle)
        // Top point, then clockwise
        pentagon.getPoints().addAll(new Double[]{
            x, y - half,                                    // Top
            x + half * 0.951, y - half * 0.309,           // Top-right
            x + half * 0.588, y + half * 0.809,           // Bottom-right
            x - half * 0.588, y + half * 0.809,           // Bottom-left
            x - half * 0.951, y - half * 0.309            // Top-left
        });
        
        return pentagon;
    }

    /**
     * Type 1 confidential packet - red pentagon
     */
    public static class Type1 extends ConfidentialPacket {
        public Type1(String id, Point2D position, Point2D direction) {
            super(id, PacketType.CONFIDENTIAL_TYPE1, position, direction, 3);
        }
    }

    /**
     * Type 2 confidential packet - blue pentagon (created from Type 1 via VPN)
     */
    public static class Type2 extends ConfidentialPacket {
        private static final int TYPE2_HEALTH = 6;
        
        public Type2(String id, Point2D position, Point2D direction) {
            super(id, PacketType.CONFIDENTIAL_TYPE2, position, direction, 4, TYPE2_HEALTH);
        }
        
        /**
         * Create Type 2 from Type 1 (VPN transformation)
         */
        public static Type2 fromType1(Type1 type1Packet) {
            Type2 type2 = new Type2(type1Packet.getId(), type1Packet.getPosition(), type1Packet.getDirection());
            
            // Scale health proportionally from Type 1 (4 max) to Type 2 (6 max)
            double healthRatio = (double) type1Packet.getCurrentHealth() / type1Packet.getHealth();
            int scaledHealth = (int) Math.round(healthRatio * TYPE2_HEALTH);
            type2.setCurrentHealth(scaledHealth);
            
            // Copy movement state if moving
            if (type1Packet.isMoving()) {
                type2.setMoving(true);
                type2.setCurrentWire(type1Packet.getCurrentWire());
                type2.setMovementProgress(type1Packet.getMovementProgress());
                type2.setStartPosition(type1Packet.getStartPosition());
                type2.setTargetPosition(type1Packet.getTargetPosition());
                type2.setCompatibleWithCurrentPort(type1Packet.isCompatibleWithCurrentPort());
            }
            
            // Copy system state
            type2.setInSystem(type1Packet.isInSystem());
            
            return type2;
        }
    }
}

package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

public class SquarePacket extends Packet {
    private static final double BASE_SPEED = 80.0; // Increased from 50.0 for faster square packets

    public SquarePacket(String id, Point2D position, Point2D direction) {
        super(id, PacketType.SQUARE, 2, position, direction, 2);
    }

    @Override
    public double getSpeed() {
        if (isCompatibleWithCurrentPort()) {
            return BASE_SPEED / 2.0; // Reduced penalty from 0.5 to 0.8 for compatible ports
        } else {
            return BASE_SPEED;
        }
    }

    @Override
    public Shape getCollisionShape() {
        // Use original visual size (14x14) for collision detection
        double size = 14.0;
        double half = size / 2.0;
        double x = getPosition().getX();
        double y = getPosition().getY();
        
        // Create rectangle collision shape centered at packet position
        javafx.scene.shape.Rectangle collisionRect = new javafx.scene.shape.Rectangle(
            x - half,
            y - half,
            size,
            size
        );
        return collisionRect;
    }
}

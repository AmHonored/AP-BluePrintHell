package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;

public class TrianglePacket extends Packet {
    private static final double BASE_SPEED = 50.0;
    private static final double MAX_SPEED = 100.0;
    private static final double ACCELERATION = 25.0;
    private double currentSpeed = BASE_SPEED;

    public TrianglePacket(String id, Point2D position, Point2D direction) {
        super(id, PacketType.TRIANGLE, 3, position, direction, 3);
    }

    @Override
    public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
        super.updateMovement(deltaTimeSeconds, compatiblePort);
        
        if (compatiblePort) {
            currentSpeed = BASE_SPEED;
        } else {
            currentSpeed += ACCELERATION * deltaTimeSeconds;
            if (currentSpeed > MAX_SPEED) {
                currentSpeed = MAX_SPEED;
            }
        }
    }

    @Override
    public double getSpeed() {
        return currentSpeed;
    }

    public void resetSpeed() {
        this.currentSpeed = BASE_SPEED;
    }

    @Override
    public Shape getCollisionShape() {
        // Use original visual size (16) for collision detection
        double size = 16.0;
        double half = size / 2.0;
        double x = getPosition().getX();
        double y = getPosition().getY();
        
        // Create triangle collision shape matching visual representation
        // Equilateral triangle centered at packet position
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(new Double[]{
            x, y - size / Math.sqrt(3),                    // Top vertex (peak)
            x - size / 2, y + size / (2 * Math.sqrt(3)),  // Bottom-left
            x + size / 2, y + size / (2 * Math.sqrt(3))   // Bottom-right
        });
        return triangle;
    }
}
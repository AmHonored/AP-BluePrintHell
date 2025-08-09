package model.entity.packets.bits;

import javafx.geometry.Point2D;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;

public class BitCirclePacket extends Packet {
    private static final double SPEED = 70.0;

    public BitCirclePacket(String id, Point2D position, Point2D direction) {
        super(id, PacketType.BIT_CIRCLE, 1, position, direction, 1);
    }

    @Override
    public double getSpeed() {
        return SPEED;
    }

    @Override
    public Shape getCollisionShape() {
        double radius = 6.0; // Increased from 4.0
        return new Circle(getPosition().getX(), getPosition().getY(), radius);
    }
}



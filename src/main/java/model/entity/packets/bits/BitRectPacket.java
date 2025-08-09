package model.entity.packets.bits;

import javafx.geometry.Point2D;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;

public class BitRectPacket extends Packet {
    private static final double SPEED = 70.0;

    public BitRectPacket(String id, Point2D position, Point2D direction) {
        super(id, PacketType.BIT_RECT, 1, position, direction, 1);
    }

    @Override
    public double getSpeed() {
        return SPEED;
    }

    @Override
    public Shape getCollisionShape() {
        double size = 12.0; // Increased from 8.0
        double half = size / 2.0;
        return new Rectangle(getPosition().getX() - half, getPosition().getY() - half, size, size);
    }
}



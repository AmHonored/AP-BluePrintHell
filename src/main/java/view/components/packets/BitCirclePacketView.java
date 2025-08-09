package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import model.entity.packets.bits.BitCirclePacket;

public class BitCirclePacketView extends PacketView {
    private static final double RADIUS = 6.0; // Increased from 4.0

    public BitCirclePacketView(BitCirclePacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        Circle c = new Circle(RADIUS);
        // Random distinct color per bit
        c.setFill(Color.hsb(Math.random() * 360.0, 0.8, 0.9));
        c.setStroke(Color.WHITE);
        c.setStrokeWidth(1.0);
        return c;
    }

    @Override
    public void updatePosition() {
        if (packet != null && packet.getPosition() != null) {
            double centerX = packet.getPosition().getX();
            double centerY = packet.getPosition().getY();
            
            // Apply deflection effects to visual position
            centerX += packet.getDeflectedX();
            centerY += packet.getDeflectedY();
            
            // Center the circle on the wire - offset by radius to center properly
            centerY -= RADIUS;
            
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }
}



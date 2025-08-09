package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import model.entity.packets.bits.BitRectPacket;

public class BitRectPacketView extends PacketView {
    private static final double SIZE = 12.0; // Increased from 8.0

    public BitRectPacketView(BitRectPacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        Rectangle r = new Rectangle(SIZE, SIZE);
        r.setFill(Color.hsb(Math.random() * 360.0, 0.8, 0.9));
        r.setStroke(Color.WHITE);
        r.setStrokeWidth(1.0);
        return r;
    }

    @Override
    public void updatePosition() {
        if (packet != null && packet.getPosition() != null) {
            double centerX = packet.getPosition().getX();
            double centerY = packet.getPosition().getY();
            
            // Apply deflection effects to visual position
            centerX += packet.getDeflectedX();
            centerY += packet.getDeflectedY();
            
            // Center the rectangle on the wire - offset by half size to center properly
            centerY -= SIZE / 2.0;
            centerX -= SIZE / 2.0;
            
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }
}



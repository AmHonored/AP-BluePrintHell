package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import model.entity.packets.MassivePacket;
import model.entity.packets.PacketType;

public class MassivePacketView extends PacketView {
    private static final double RADIUS = 8.0;

    public MassivePacketView(MassivePacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        Circle circle = new Circle(RADIUS);
        // Color by type
        if (packet.getType() == PacketType.MASSIVE_TYPE1) {
            circle.setFill(Color.CORAL);
            circle.setStroke(Color.DARKSALMON);
        } else {
            circle.setFill(Color.GRAY);
            circle.setStroke(Color.DARKGRAY);
        }
        circle.setStrokeWidth(1.5);
        return circle;
    }
}




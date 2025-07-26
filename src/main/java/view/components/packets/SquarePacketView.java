package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Rectangle;
import model.entity.packets.SquarePacket;

public class SquarePacketView extends PacketView {
    public SquarePacketView(SquarePacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        Rectangle rect = new Rectangle(14, 14);
        rect.setX(-7);
        rect.setY(-7);
        rect.setFill(Color.GREEN);
        return rect;
    }
}

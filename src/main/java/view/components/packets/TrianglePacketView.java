package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import model.entity.packets.TrianglePacket;

public class TrianglePacketView extends PacketView {
    public TrianglePacketView(TrianglePacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        // Create an equilateral triangle centered at (0,0)
        double size = 16;
        Polygon triangle = new Polygon(
            0.0, -size / Math.sqrt(3),
            -size / 2, size / (2 * Math.sqrt(3)),
            size / 2, size / (2 * Math.sqrt(3))
        );
        triangle.setFill(Color.YELLOW);
        return triangle;
    }
}

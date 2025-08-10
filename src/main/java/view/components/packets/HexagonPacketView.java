package view.components.packets;

import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import model.entity.packets.HexagonPacket;

public class HexagonPacketView extends PacketView {
    public HexagonPacketView(HexagonPacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        Polygon hexagon = new Polygon();
        double radius = 8.0; 
        
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3; 
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }
        
        return hexagon;
    }
}

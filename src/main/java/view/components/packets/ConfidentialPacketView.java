package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import model.entity.packets.ConfidentialPacket;
import model.entity.packets.PacketType;

public class ConfidentialPacketView extends PacketView {
    private static final double PENTAGON_SIZE = 16.0;
    private static final Color TYPE1_COLOR = Color.RED;        // Type 1: Red pentagon
    private static final Color TYPE2_COLOR = Color.BLUE;       // Type 2: Blue pentagon
    private static final double STROKE_WIDTH = 1.5;

    public ConfidentialPacketView(ConfidentialPacket packet) {
        super(packet);
    }

    @Override
    protected Shape createPacketShape() {
        Polygon pentagon = new Polygon();
        double half = PENTAGON_SIZE / 2.0;
        
        // Pentagon vertices (5 points around a circle)
        // Calculate using regular pentagon geometry
        pentagon.getPoints().addAll(new Double[]{
            0.0, -half,                                     // Top vertex
            half * 0.951, -half * 0.309,                  // Top-right vertex
            half * 0.588, half * 0.809,                   // Bottom-right vertex
            -half * 0.588, half * 0.809,                  // Bottom-left vertex
            -half * 0.951, -half * 0.309                  // Top-left vertex
        });
        
        // Set color based on packet type
        Color fillColor = (packet.getType() == PacketType.CONFIDENTIAL_TYPE1) ? TYPE1_COLOR : TYPE2_COLOR;
        pentagon.setFill(fillColor);
        
        // Set stroke color based on type for better visibility
        Color strokeColor = (packet.getType() == PacketType.CONFIDENTIAL_TYPE1) ? 
            Color.DARKRED : Color.DARKBLUE;
        pentagon.setStroke(strokeColor);
        pentagon.setStrokeWidth(STROKE_WIDTH);
        
        return pentagon;
    }
}
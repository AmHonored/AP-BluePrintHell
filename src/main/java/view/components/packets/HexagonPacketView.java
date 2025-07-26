package view.components.packets;

import javafx.scene.shape.Polygon;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import model.entity.packets.HexagonPacket;
import model.entity.packets.Packet;

public class HexagonPacketView extends PacketView {
    private final Polygon hexagonShape;

    public HexagonPacketView(HexagonPacket packet) {
        super(packet);
        this.hexagonShape = (Polygon) createPacketShape();
        setupHexagonAppearance();
    }

    @Override
    protected Shape createPacketShape() {
        Polygon hexagon = new Polygon();
        double radius = 8.0; // Visual size
        
        // Create a regular hexagon (6 sides, 60° angles)
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3; // 60 degrees each
            double x = radius * Math.cos(angle);
            double y = radius * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }
        
        return hexagon;
    }

    private void setupHexagonAppearance() {
        // Set black color as specified
        hexagonShape.setFill(Color.BLACK);
        hexagonShape.setStroke(Color.DARKGRAY);
        hexagonShape.setStrokeWidth(1.0);
    }

    @Override
    public void updatePosition() {
        super.updatePosition();
        // Additional hexagon-specific positioning if needed
    }

    @Override
    public void updateHealth() {
        super.updateHealth();
        
        // Additional hexagon-specific health visualization
        double healthPercentage = (double) packet.getCurrentHealth() / packet.getHealth();
        
        // Change color based on health (darker when damaged)
        if (healthPercentage <= 0.5) {
            hexagonShape.setFill(Color.DARKRED);
        } else if (healthPercentage <= 0.75) {
            hexagonShape.setFill(Color.DARKGRAY);
        } else {
            hexagonShape.setFill(Color.BLACK);
        }
    }

    public void updateMovementState() {
        // Update visual representation based on movement state
        HexagonPacket hexagonPacket = (HexagonPacket) getPacket();
        
        if (hexagonPacket.getMovementState() == model.logic.packet.PacketState.RETURNING) {
            // Add visual indicator for returning state (e.g., different stroke)
            hexagonShape.setStroke(Color.RED);
            hexagonShape.setStrokeWidth(2.0);
        } else {
            // Normal forward movement
            hexagonShape.setStroke(Color.DARKGRAY);
            hexagonShape.setStrokeWidth(1.0);
        }
    }
}

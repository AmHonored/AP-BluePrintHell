package view.components.packets;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Shape;
import model.entity.packets.Packet;
import model.entity.packets.ConfidentialPacket;

public abstract class PacketView extends StackPane {
    protected final Packet packet;
    protected final Shape packetShape;
    
    public PacketView(Packet packet) {
        this.packet = packet;
        this.packetShape = createPacketShape();
        // Position the packet
        updatePosition();
        // Add shape to container
        this.getChildren().add(packetShape);
    }
    
    protected abstract Shape createPacketShape();
    
    public void updatePosition() {
        if (packet != null && packet.getPosition() != null) {
            // Apply centering logic: Y-position offset by half the visual size to center vertically on wire
            double centerX = packet.getPosition().getX();
            double centerY = packet.getPosition().getY();
            
            // Apply deflection effects to visual position (for impact wave effects)
            double deflectedX = packet.getDeflectedX();
            double deflectedY = packet.getDeflectedY();
            centerX += deflectedX;
            centerY += deflectedY;
            
            // Calculate Y-offset based on packet type for proper centering
            double yOffset = 0;
            if (packet instanceof model.entity.packets.SquarePacket) {
                yOffset = 7.0; // Half of 14 (square packet visual size)
            } else if (packet instanceof model.entity.packets.TrianglePacket) {
                yOffset = 8.0; // Half of 16 (triangle packet visual size)
            } else if (packet instanceof model.entity.packets.HexagonPacket) {
                yOffset = 8.0; // Half of 16 (hexagon packet visual size)
            } else if (packet instanceof ConfidentialPacket) {
                yOffset = 8.0; // Half of 16 (confidential pentagon visual size)
            }
            
            centerY -= yOffset;
            
            // Set the packet's layout position to the centered coordinates
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }
    
    public void updateHealth() {
        // Do nothing: keep packet color and opacity static regardless of health or collisions
    }
    
    public void updateDeflection() {
        // Apply deflection visual effects
        double deflectedX = packet.getDeflectedX();
        double deflectedY = packet.getDeflectedY();
        if (Math.abs(deflectedX) > 0.1 || Math.abs(deflectedY) > 0.1) {
            // Add shake effect for deflection
            packetShape.getStyleClass().add("packet-deflected");
        } else {
            packetShape.getStyleClass().remove("packet-deflected");
        }
    }
    
    public void setPacketVisible(boolean visible) {
        super.setVisible(visible);
    }
    
    public Packet getPacket() {
        return packet;
    }
    
    public Shape getPacketShape() {
        return packetShape;
    }
}

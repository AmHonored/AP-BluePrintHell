package view.components.packets;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Shape;
import model.entity.packets.Packet;

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
            
            // Calculate Y-offset based on packet type for proper centering
            double yOffset = 0;
            if (packet instanceof model.entity.packets.SquarePacket) {
                yOffset = 7.0; // Half of 14 (square packet visual size)
            } else if (packet instanceof model.entity.packets.TrianglePacket) {
                yOffset = 8.0; // Half of 16 (triangle packet visual size)
            }
            
            centerY -= yOffset;
            
            // Set the packet's layout position to the centered coordinates
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }
    
    public void updateHealth() {
        // Update visual representation based on health
        double healthPercentage = (double) packet.getCurrentHealth() / packet.getHealth();
        // Adjust opacity based on health
        packetShape.setOpacity(Math.max(0.3, healthPercentage));
        // Add visual effects for low health
        if (healthPercentage < 0.3) {
            packetShape.getStyleClass().add("packet-critical");
        } else if (healthPercentage < 0.6) {
            packetShape.getStyleClass().add("packet-damaged");
        }
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

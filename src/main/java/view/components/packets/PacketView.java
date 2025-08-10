package view.components.packets;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Shape;
import model.entity.packets.Packet;
import model.entity.packets.ConfidentialPacket;

public abstract class PacketView extends StackPane {
    protected final Packet packet;
    protected final Shape packetShape;
    
    public PacketView(Packet packet) {
        this.packet = packet; // data model
        this.packetShape = createPacketShape();
        updatePosition();
        this.getChildren().add(packetShape);
    }
    
    protected abstract Shape createPacketShape();
    
    public void updatePosition() {
        if (packet != null && packet.getPosition() != null) {
            double centerX = packet.getPosition().getX();
            double centerY = packet.getPosition().getY();
            
            double deflectedX = packet.getDeflectedX();
            double deflectedY = packet.getDeflectedY();
            centerX += deflectedX;
            centerY += deflectedY;
            
            double yOffset = 0;
            if (packet instanceof model.entity.packets.SquarePacket) {
                yOffset = 7.0; 
            } else if (packet instanceof model.entity.packets.TrianglePacket) {
                yOffset = 8.0; 
            } else if (packet instanceof model.entity.packets.HexagonPacket) {
                yOffset = 8.0; 
            } else if (packet instanceof ConfidentialPacket) {
                yOffset = 8.0; 
            } else if (packet instanceof model.entity.packets.MassivePacket) {
                yOffset = 8.0; 
            }
            
            centerY -= yOffset;
            
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }
    
    public void updateHealth() {
    }
    
    public void updateDeflection() {
        double deflectedX = packet.getDeflectedX();
        double deflectedY = packet.getDeflectedY();
        if (Math.abs(deflectedX) > 0.1 || Math.abs(deflectedY) > 0.1) {
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

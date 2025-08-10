package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import model.entity.packets.ProtectedPacket;

public class ProtectedPacketView extends PacketView {
    private static final Color PACKET_COLOR = Color.VIOLET;
    private static final Color STROKE_COLOR = Color.DARKVIOLET;
    private static final double PACKET_SIZE = 16.0;

    public ProtectedPacketView(ProtectedPacket packet) {
        super(packet);
        addHealthIndicator();
    }

    @Override
    protected Shape createPacketShape() {

        Polygon diamond = new Polygon();
        double half = PACKET_SIZE / 2.0;
        
        // Diamond vertices (rotated square)
        diamond.getPoints().addAll(new Double[]{
            0.0, -half,     // Top vertex
            half, 0.0,      // Right vertex
            0.0, half,      // Bottom vertex
            -half, 0.0      // Left vertex
        });
        
        diamond.setFill(PACKET_COLOR);
        diamond.setStroke(STROKE_COLOR);
        diamond.setStrokeWidth(2.0);

        return diamond;
    }

    private void addHealthIndicator() {
        ProtectedPacket protectedPacket = (ProtectedPacket) this.packet;
        if (protectedPacket.getCurrentHealth() < protectedPacket.getHealth()) {
            javafx.scene.shape.Rectangle healthBar = new javafx.scene.shape.Rectangle();
            double healthPercentage = (double) protectedPacket.getCurrentHealth() / protectedPacket.getHealth();
            
            healthBar.setWidth(PACKET_SIZE * healthPercentage);
            healthBar.setHeight(3);
            healthBar.setFill(healthPercentage > 0.5 ? Color.GREEN : Color.RED);
            healthBar.setY(-PACKET_SIZE / 2 - 6); 
            healthBar.setX(-PACKET_SIZE / 2);
            healthBar.getStyleClass().add("health-indicator");
            
            getChildren().add(healthBar);
        }
    }

    @Override
    public void updatePosition() {
        ProtectedPacket protectedPacket = (ProtectedPacket) this.packet;
        if (protectedPacket != null && protectedPacket.getPosition() != null) {

            double centerX = protectedPacket.getPosition().getX();
            double centerY = protectedPacket.getPosition().getY();
            
            double deflectedX = protectedPacket.getDeflectedX();
            double deflectedY = protectedPacket.getDeflectedY();
            centerX += deflectedX;
            centerY += deflectedY;
            
            double yOffset = 8.0;
            centerY -= yOffset;
            
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }

    public ProtectedPacket getProtectedPacket() {
        return (ProtectedPacket) this.packet;
    }
} 
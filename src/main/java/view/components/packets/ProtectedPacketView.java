package view.components.packets;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import model.entity.packets.ProtectedPacket;

public class ProtectedPacketView extends PacketView {
    private static final Color PACKET_COLOR = Color.VIOLET;
    private static final Color STROKE_COLOR = Color.DARKVIOLET;
    private static final double PACKET_SIZE = 16.0;

    private ProtectedPacket protectedPacket;

    public ProtectedPacketView(ProtectedPacket packet) {
        super(packet);
        this.protectedPacket = packet;
        addHealthIndicator();
    }

    @Override
    protected Shape createPacketShape() {
        // Create diamond shape (rotated square)
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
        if (protectedPacket.getCurrentHealth() < protectedPacket.getHealth()) {
            // Create health bar for damaged protected packets
            javafx.scene.shape.Rectangle healthBar = new javafx.scene.shape.Rectangle();
            double healthPercentage = (double) protectedPacket.getCurrentHealth() / protectedPacket.getHealth();
            
            healthBar.setWidth(PACKET_SIZE * healthPercentage);
            healthBar.setHeight(3);
            healthBar.setFill(healthPercentage > 0.5 ? Color.GREEN : Color.RED);
            healthBar.setY(-PACKET_SIZE / 2 - 6); // Position above the packet
            healthBar.setX(-PACKET_SIZE / 2);
            healthBar.getStyleClass().add("health-indicator");
            
            getChildren().add(healthBar);
        }
    }

    @Override
    public void updatePosition() {
        if (protectedPacket != null && protectedPacket.getPosition() != null) {
            // Apply centering logic similar to base class but for protected packets
            double centerX = protectedPacket.getPosition().getX();
            double centerY = protectedPacket.getPosition().getY();
            
            // Apply deflection effects
            double deflectedX = protectedPacket.getDeflectedX();
            double deflectedY = protectedPacket.getDeflectedY();
            centerX += deflectedX;
            centerY += deflectedY;
            
            // Y-offset for centering (similar to other packets)
            double yOffset = 8.0; // Half of packet visual size
            centerY -= yOffset;
            
            this.setLayoutX(centerX);
            this.setLayoutY(centerY);
        }
    }

    public ProtectedPacket getProtectedPacket() {
        return protectedPacket;
    }
} 
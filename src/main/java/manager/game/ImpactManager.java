package manager.game;

import model.entity.packets.Packet;
import model.wire.Wire;
import controller.PacketController;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import java.util.List;

public class ImpactManager {
    private static final double EXPLOSION_RADIUS = 150.0; 
    private static final double MAX_DEFLECTION = 12.0; 

    public static void handleImpactWave(Point2D explosionCenter, List<Packet> allPackets, PacketController packetController) {
        // copy for CME
        List<Packet> packetsCopy = new java.util.ArrayList<>(allPackets);
        
        createVisualImpactWave(explosionCenter, packetController);
        
        for (Packet packet : packetsCopy) {
            if (packet.isInSystem()) continue; // Only affect moving packets
            double dist = packet.getPosition().distance(explosionCenter);
            if (dist <= EXPLOSION_RADIUS) {
                Wire currentWire = packet.getCurrentWire();
                
                double force = MAX_DEFLECTION * (1.0 - (dist / EXPLOSION_RADIUS));
                
                double dx = packet.getPosition().getX() - explosionCenter.getX();
                double dy = packet.getPosition().getY() - explosionCenter.getY();
                double len = Math.sqrt(dx*dx + dy*dy);
                if (len == 0) {
                    dx = 1.0; dy = 0.0; len = 1.0;
                }
                dx /= len;
                dy /= len;
                
                double finalDeflectionX, finalDeflectionY;
                if (currentWire != null) {
                    // Get wire direction
                    Point2D wireSource = currentWire.getSource().getPosition();
                    Point2D wireDest = currentWire.getDest().getPosition();
                    double wireDx = wireDest.getX() - wireSource.getX();
                    double wireDy = wireDest.getY() - wireSource.getY();
                    double wireLen = Math.sqrt(wireDx * wireDx + wireDy * wireDy);
                    
                    if (wireLen > 0) {
                        // normalize wire direction
                        wireDx /= wireLen;
                        wireDy /= wireLen;
                        

                        double perpendicularDx = -wireDy; 
                        double perpendicularDy = wireDx;
                        
                        double projection = dx * perpendicularDx + dy * perpendicularDy;
                        finalDeflectionX = perpendicularDx * force * Math.abs(projection);
                        finalDeflectionY = perpendicularDy * force * Math.abs(projection);
                    } else {
                        finalDeflectionX = dx * force;
                        finalDeflectionY = dy * force;
                    }
                } else {
                    finalDeflectionX = dx * force;
                    finalDeflectionY = dy * force;
                }
                
                packet.applyDeflection(finalDeflectionX, finalDeflectionY);
                
                // If deflection is too large or health is 0, kill the packet
                if (packet.isDeflectionTooLarge() || !packet.isAlive()) {
                    packetController.killPacket(packet);
                }
            }
        }
    }
    

    private static void createVisualImpactWave(Point2D center, PacketController packetController) {
        // Get the packet layer from the packet controller
        Pane packetLayer = packetController.getPacketLayer();
        if (packetLayer == null) return;
        
        // Create impact wave circle
        Circle impactWave = new Circle(center.getX(), center.getY(), 5);
        impactWave.setFill(Color.TRANSPARENT);
        impactWave.setStroke(Color.RED);
        impactWave.setStrokeWidth(3);
        impactWave.getStyleClass().add("impact-wave");
        
        // Add to the packet layer
        packetLayer.getChildren().add(impactWave);
        
        // Create animations
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(500), impactWave);
        scaleTransition.setFromX(0.1);
        scaleTransition.setFromY(0.1);
        scaleTransition.setToX(EXPLOSION_RADIUS / 5.0); 
        scaleTransition.setToY(EXPLOSION_RADIUS / 5.0);
        
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), impactWave);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        
        // Run animations in parallel
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);
        parallelTransition.setOnFinished(event -> {
            packetLayer.getChildren().remove(impactWave);
        });
        
        parallelTransition.play();
    }
}

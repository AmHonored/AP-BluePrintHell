package manager.game;

import model.entity.packets.Packet;
import model.levels.Level;
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
    private static final double EXPLOSION_RADIUS = 150.0; // Increased from 100.0 for more dramatic effect
    private static final double MAX_DEFLECTION = 12.0; // Increased from 8.0 for more visible deflections

    public static void handleImpactWave(Point2D explosionCenter, List<Packet> allPackets, PacketController packetController) {
        // Create a copy to avoid ConcurrentModificationException
        List<Packet> packetsCopy = new java.util.ArrayList<>(allPackets);
        
        // Log the collision for debugging
        System.out.println("💥 IMPACT WAVE: Explosion at (" + String.format("%.1f", explosionCenter.getX()) + ", " + String.format("%.1f", explosionCenter.getY()) + ")");
        System.out.println("💥 IMPACT WAVE: Affecting " + packetsCopy.size() + " packets in explosion radius");
        
        // Create visual impact wave effect
        createVisualImpactWave(explosionCenter, packetController);
        
        for (Packet packet : packetsCopy) {
            if (packet.isInSystem()) continue; // Only affect moving packets
            double dist = packet.getPosition().distance(explosionCenter);
            if (dist <= EXPLOSION_RADIUS) {
                // Log packet position and wire info before deflection
                Wire currentWire = packet.getCurrentWire();
                if (currentWire != null) {
                    Point2D wireSource = currentWire.getSource().getPosition();
                    Point2D wireDest = currentWire.getDest().getPosition();
                    double wireProgress = packet.getMovementProgress();
                    Point2D expectedWirePos = new Point2D(
                        wireSource.getX() + wireProgress * (wireDest.getX() - wireSource.getX()),
                        wireSource.getY() + wireProgress * (wireDest.getY() - wireSource.getY())
                    );
                    
                    System.out.println("📍 PACKET POSITION: " + packet.getId() + " at (" + 
                                      String.format("%.1f", packet.getPosition().getX()) + ", " + 
                                      String.format("%.1f", packet.getPosition().getY()) + 
                                      ") - Expected wire position: (" + 
                                      String.format("%.1f", expectedWirePos.getX()) + ", " + 
                                      String.format("%.1f", expectedWirePos.getY()) + 
                                      ") - Progress: " + String.format("%.2f", wireProgress));
                }
                // Linear falloff: effect decreases with distance
                double force = MAX_DEFLECTION * (1.0 - (dist / EXPLOSION_RADIUS));
                
                // Direction from explosion center to packet
                double dx = packet.getPosition().getX() - explosionCenter.getX();
                double dy = packet.getPosition().getY() - explosionCenter.getY();
                double len = Math.sqrt(dx*dx + dy*dy);
                if (len == 0) {
                    // Randomize direction if exactly at center
                    dx = 1.0; dy = 0.0; len = 1.0;
                }
                dx /= len;
                dy /= len;
                
                // Calculate deflection perpendicular to the wire direction to prevent backward movement
                double finalDeflectionX, finalDeflectionY;
                if (currentWire != null) {
                    // Get wire direction
                    Point2D wireSource = currentWire.getSource().getPosition();
                    Point2D wireDest = currentWire.getDest().getPosition();
                    double wireDx = wireDest.getX() - wireSource.getX();
                    double wireDy = wireDest.getY() - wireSource.getY();
                    double wireLen = Math.sqrt(wireDx * wireDx + wireDy * wireDy);
                    
                    if (wireLen > 0) {
                        // Normalize wire direction
                        wireDx /= wireLen;
                        wireDy /= wireLen;
                        
                        // Calculate deflection perpendicular to wire direction
                        // This prevents packets from moving backward along the wire
                        double perpendicularDx = -wireDy; // Perpendicular vector
                        double perpendicularDy = wireDx;
                        
                        // Project the explosion force onto the perpendicular direction
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
                
                // Log the deflection for debugging
                System.out.println("💨 IMPACT WAVE: Packet " + packet.getId() + " (" + packet.getType() + ") deflected by (" + 
                                  String.format("%.1f", finalDeflectionX) + ", " + String.format("%.1f", finalDeflectionY) + ") at distance " + String.format("%.1f", dist));
                
                packet.smoothDeflecting(finalDeflectionX, finalDeflectionY);
                
                // If deflection is too large or health is 0, kill the packet
                if (packet.isDeflectionTooLarge() || !packet.isAlive()) {
                    System.out.println("💥 IMPACT WAVE: Packet " + packet.getId() + " (" + packet.getType() + ") destroyed due to excessive deflection or zero health");
                    packetController.killPacket(packet);
                }
            }
        }
    }
    
    /**
     * Create a visual impact wave effect at the collision point
     */
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
        scaleTransition.setToX(EXPLOSION_RADIUS / 5.0); // Scale to match explosion radius
        scaleTransition.setToY(EXPLOSION_RADIUS / 5.0);
        
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(500), impactWave);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        
        // Run animations in parallel
        ParallelTransition parallelTransition = new ParallelTransition(scaleTransition, fadeTransition);
        parallelTransition.setOnFinished(event -> {
            // Remove the impact wave from the scene
            packetLayer.getChildren().remove(impactWave);
        });
        
        parallelTransition.play();
    }
}

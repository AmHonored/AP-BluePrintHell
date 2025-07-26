package model.logic.system;

import model.entity.packets.Packet;
import model.levels.Level;
import javafx.scene.shape.Shape;
import controller.PacketController;
import view.components.packets.PacketView;

public class CollisionSystem {
    private final PacketController packetController;
    
    public CollisionSystem(PacketController packetController) {
        this.packetController = packetController;
    }
    
    /**
     * Detect collision between two packets using JavaFX Shape.intersect() method
     * This provides more precise collision detection than boundary checking
     */
    public boolean detectCollision(Packet p1, Packet p2) {
        try {
            // Get the packet views from the controller
            PacketView view1 = packetController.getPacketView(p1);
            PacketView view2 = packetController.getPacketView(p2);
            
            if (view1 == null || view2 == null) {
                return false; // Can't detect collision without views
            }
            
            // Get the shapes from the packet views
            Shape shape1 = view1.getPacketShape();
            Shape shape2 = view2.getPacketShape();
            
            if (shape1 == null || shape2 == null) {
                return false; // Fallback to basic collision detection
            }
            
            // Use JavaFX Shape.intersect() for precise collision detection
            Shape intersect = Shape.intersect(shape1, shape2);
            boolean isColliding = !intersect.getBoundsInLocal().isEmpty();
            
            return isColliding;
            
        } catch (Exception e) {
            // Log exception and fallback to basic collision detection
            System.err.println("Error in Shape.intersect() collision detection: " + e.getMessage());
            e.printStackTrace();
            return p1.intersects(p2); // Fallback to existing method
        }
    }
}

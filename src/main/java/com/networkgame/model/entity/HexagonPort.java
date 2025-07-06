package com.networkgame.model.entity;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import com.networkgame.model.entity.system.NetworkSystem;

/**
 * HexagonPort represents a hexagonal port that accepts hexagon packets.
 * It follows the same structure as other port types but with hexagonal shape.
 */
public class HexagonPort extends Port {
    
    private static final double PORT_SIZE = 12.0;
    private static final int HEXAGON_VERTICES = 6;
    
    /**
     * Creates a new HexagonPort at the specified position.
     * @param position The position of the port
     * @param isInput Whether this is an input port (true) or output port (false)
     * @param system The network system this port belongs to
     */
    public HexagonPort(Point2D position, boolean isInput, NetworkSystem system) {
        super(position, isInput, Packet.PacketType.HEXAGON, system);
        
        // Create the hexagonal shape
        Polygon hexagon = new Polygon();
        hexagon.getPoints().addAll(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0); // Placeholder points
        
        // Set colors based on port type
        hexagon.setFill(isInput ? Color.LIGHTSEAGREEN : Color.SEAGREEN);
        hexagon.setStroke(Color.BLACK);
        hexagon.setStrokeWidth(1.5);
        
        this.shape = hexagon;
        
        // Update position if it was provided
        if (position != null) {
            updateHexagonPoints(hexagon, position);
        }
    }
    
    /**
     * Updates the hexagon vertices based on the current position.
     * Creates a regular hexagon with 6 equally spaced vertices.
     * @param hexagon The polygon to update
     * @param center The center position of the hexagon
     */
    private void updateHexagonPoints(Polygon hexagon, Point2D center) {
        // Calculate radius for the hexagon
        double radius = PORT_SIZE / 2.0;
        
        // Clear existing points and generate 6 vertices
        hexagon.getPoints().clear();
        
        for (int i = 0; i < HEXAGON_VERTICES; i++) {
            // Calculate angle for this vertex (starting from 0 degrees)
            double angle = (i * Math.PI) / 3.0; // 60 degrees in radians
            
            // Calculate vertex position
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + radius * Math.sin(angle);
            
            // Add vertex to polygon
            hexagon.getPoints().addAll(x, y);
        }
    }
    
    /**
     * Updates the shape position when the port moves.
     */
    @Override
    protected void updateShapePosition() {
        if (shape != null && shape instanceof Polygon) {
            updateHexagonPoints((Polygon) shape, position);
        }
    }
    
    /**
     * Returns a string representation of the port for debugging.
     * @return A descriptive string of the port
     */
    @Override
    public String toString() {
        return String.format("HexagonPort{position=(%.1f,%.1f), isInput=%b, type=HEXAGON}", 
                            position.getX(), position.getY(), isInput());
    }
} 
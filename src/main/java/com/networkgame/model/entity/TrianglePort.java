package com.networkgame.model.entity; 

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import com.networkgame.model.entity.system.NetworkSystem;

public class TrianglePort extends Port {
    private static final double PORT_SIZE = 10;
    
    public TrianglePort(Point2D position, boolean isInput, NetworkSystem system) {
        super(position, isInput, Packet.PacketType.TRIANGLE, system);
        
        // Create the triangle shape
        Polygon triangle = new Polygon();
        triangle.getPoints().addAll(0.0, 0.0, 0.0, 0.0, 0.0, 0.0); // Placeholder points
        triangle.setFill(isInput ? Color.LIGHTGREEN : Color.GREEN);
        triangle.setStroke(Color.BLACK);
        this.shape = triangle;
        
        // Update position if it was provided
        if (position != null) {
            updateTrianglePoints(triangle, position);
        }
    }
    
    private void updateTrianglePoints(Polygon triangle, Point2D pos) {
        triangle.getPoints().setAll(
            pos.getX(), pos.getY() - PORT_SIZE,     // Top
            pos.getX() - PORT_SIZE * 0.866, pos.getY() + PORT_SIZE * 0.5,  // Bottom left
            pos.getX() + PORT_SIZE * 0.866, pos.getY() + PORT_SIZE * 0.5   // Bottom right
        );
    }
    
    @Override
    protected void updateShapePosition() {
        Polygon triangle = (Polygon) shape;
        updateTrianglePoints(triangle, position);
    }
} 

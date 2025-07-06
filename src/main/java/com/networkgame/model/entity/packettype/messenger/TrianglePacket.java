package com.networkgame.model.entity.packettype.messenger; 

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

public class TrianglePacket extends Packet {
    private static final int DEFAULT_SIZE = 3;
    private static final double DEFAULT_SPEED = 80;
    private static final double COMPATIBLE_PORT_SPEED = 80.0; 
    private static final double INCOMPATIBLE_PORT_SPEED = 120.0; 
    private static final double VISUAL_SIZE = 14.0;
    
    public TrianglePacket(Point2D position) {
        super(position, PacketType.TRIANGLE, DEFAULT_SIZE);
        initializeShape(position);
        this.currentSpeed = DEFAULT_SPEED;
    }
    
    private void initializeShape(Point2D position) {
        Polygon triangle = new Polygon();
        updateTrianglePoints(triangle, position);
        
        triangle.setFill(getBaseColor());
        triangle.setStroke(Color.BLACK);
        triangle.setStrokeWidth(1);
        this.shape = triangle;
    }
    
    private void updateTrianglePoints(Polygon triangle, Point2D pos) {
        double height = Math.sqrt(3) * VISUAL_SIZE / 2;
        
        triangle.getPoints().setAll(
            pos.getX(), pos.getY() - height * 2/3,            // Top point
            pos.getX() - VISUAL_SIZE/2, pos.getY() + height/3, // Bottom left
            pos.getX() + VISUAL_SIZE/2, pos.getY() + height/3  // Bottom right
        );
    }
    
    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
    }
    
    @Override
    protected void updateShapePosition() {
        if (shape != null) {
            updateTrianglePoints((Polygon) shape, position);
        }
    }
    
    @Override
    public boolean isCompatible(Port port) {
        return port.getType() == PacketType.TRIANGLE;
    }
    
    /**
     * Sets the appropriate speed based on port compatibility
     * Triangle packets have constant speed on compatible ports
     * and higher speed on incompatible ones
     */
    public void adjustSpeedForPort(Port port) {
        boolean compatible = isCompatible(port);
        this.currentSpeed = compatible ? COMPATIBLE_PORT_SPEED : INCOMPATIBLE_PORT_SPEED;
        this.setCompatibility(compatible);
        
        // Apply the new speed to velocity, maintaining direction
        if (velocity.magnitude() > 0) {
            velocity = velocity.normalize().multiply(currentSpeed);
        }
    }
    
    @Override
    protected Color getBaseColor() {
        return Color.ORANGE;
    }
} 
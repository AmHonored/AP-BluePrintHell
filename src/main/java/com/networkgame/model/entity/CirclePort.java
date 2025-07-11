package com.networkgame.model.entity;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import com.networkgame.model.entity.system.NetworkSystem;

/**
 * CirclePort - Port type for Circle packets
 * 
 * CirclePorts are compatible with CirclePackets and can handle their
 * special preservation and bidirectional movement behaviors.
 */
public class CirclePort extends Port {
    private static final double PORT_RADIUS = 5;
    
    /**
     * Creates a new CirclePort
     * @param position Position of the port (can be null, will be set later)
     * @param isInput True if this is an input port, false for output port
     * @param system The network system this port belongs to
     */
    public CirclePort(Point2D position, boolean isInput, NetworkSystem system) {
        super(position, isInput, Packet.PacketType.CIRCLE, system);
        
        // Create the circle shape
        Circle circle = new Circle(0, 0, PORT_RADIUS);
        circle.setFill(isInput ? Color.LIGHTCYAN : Color.CYAN);
        circle.setStroke(Color.DARKBLUE);
        circle.setStrokeWidth(1.5);
        this.shape = circle;
        
        // Update position if it was provided
        if (position != null) {
            updateShapePosition();
        }
    }
    
    @Override
    protected void updateShapePosition() {
        Circle circle = (Circle) shape;
        circle.setCenterX(position.getX());
        circle.setCenterY(position.getY());
    }
    
    @Override
    public String toString() {
        return "CirclePort{" +
               "position=" + getPosition() +
               ", isInput=" + isInput() +
               ", type=" + getType() +
               ", connected=" + isConnected() +
               '}';
    }
} 
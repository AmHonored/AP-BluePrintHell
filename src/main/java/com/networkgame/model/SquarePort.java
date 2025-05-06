package com.networkgame.model;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SquarePort extends Port {
    private static final double PORT_SIZE = 10;
    
    public SquarePort(Point2D position, boolean isInput, NetworkSystem system) {
        super(position, isInput, Packet.PacketType.SQUARE, system);
        
        // Create the square shape
        Rectangle square = new Rectangle(0, 0, PORT_SIZE, PORT_SIZE);
        square.setFill(isInput ? Color.LIGHTBLUE : Color.BLUE);
        square.setStroke(Color.BLACK);
        this.shape = square;
        
        // Update position if it was provided
        if (position != null) {
            updateShapePosition();
        }
    }
    
    @Override
    protected void updateShapePosition() {
        Rectangle square = (Rectangle) shape;
        square.setX(position.getX() - PORT_SIZE / 2);
        square.setY(position.getY() - PORT_SIZE / 2);
    }
} 
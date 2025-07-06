package com.networkgame.model.entity.packettype.messenger; 

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class SquarePacket extends Packet {
    private static final int DEFAULT_SIZE = 2;
    private static final double VISUAL_SIZE = 10.0;
    private static final double DEFAULT_SPEED = 100;
    private static final double COMPATIBLE_SPEED_FACTOR = 0.5;
    private static final double INCOMPATIBLE_SPEED_FACTOR = 1.0;
    
    public SquarePacket(Point2D position) {
        super(position, PacketType.SQUARE, DEFAULT_SIZE);
        initializeShape(position);
        this.currentSpeed = DEFAULT_SPEED;
    }
    
    private void initializeShape(Point2D position) {
        Rectangle rectangle = new Rectangle(
            position.getX() - VISUAL_SIZE/2, 
            position.getY() - VISUAL_SIZE/2, 
            VISUAL_SIZE, 
            VISUAL_SIZE
        );
        rectangle.setFill(getBaseColor());
        rectangle.setStroke(Color.BLACK);
        rectangle.setStrokeWidth(1);
        this.shape = rectangle;
    }
    
    @Override
    protected Color getBaseColor() {
        return Color.CORNFLOWERBLUE;
    }
    
    @Override
    protected void updateShapePosition() {
        Rectangle rectangle = (Rectangle) shape;
        rectangle.setX(position.getX() - VISUAL_SIZE/2);
        rectangle.setY(position.getY() - VISUAL_SIZE/2);
        rectangle.setWidth(VISUAL_SIZE);
        rectangle.setHeight(VISUAL_SIZE);
    }
    
    @Override
    public boolean isCompatible(Port port) {
        return port.getType() == PacketType.SQUARE;
    }
    
    /**
     * Sets the appropriate speed based on whether the port is compatible
     * Square packets move at half speed through compatible ports and full speed through incompatible ports
     */
    public void adjustSpeedForPort(Port port) {
        boolean compatible = isCompatible(port);
        currentSpeed = DEFAULT_SPEED * (compatible ? COMPATIBLE_SPEED_FACTOR : INCOMPATIBLE_SPEED_FACTOR);
        setCompatibility(compatible);
    }
} 
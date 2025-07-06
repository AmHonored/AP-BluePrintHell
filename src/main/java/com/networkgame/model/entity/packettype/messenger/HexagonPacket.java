package com.networkgame.model.entity.packettype.messenger;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * HexagonPacket represents a double hexagonal packet in the messenger category.
 * It has a shape of two attached hexagons but is compatible with single hexagon ports.
 * It moves slower than other packets but has higher value and durability.
 */
public class HexagonPacket extends Packet {
    
    // Constants for hexagon packet configuration
    private static final int DEFAULT_SIZE = 1;
    private static final int DEFAULT_COIN_VALUE = 1;
    private static final double DEFAULT_SPEED = 70.0;
    private static final double COMPATIBLE_PORT_SPEED = 100.0;  // Faster on compatible ports
    private static final double INCOMPATIBLE_PORT_SPEED = 70.0;  // Slower on incompatible ports
    private static final double VISUAL_SIZE = 14.0;  // Slightly smaller to accommodate two hexagons
    private static final int HEXAGON_VERTICES = 6;
    
    /**
     * Creates a new HexagonPacket at the specified position.
     * @param position The initial position of the packet
     */
    public HexagonPacket(Point2D position) {
        super(position, PacketType.HEXAGON, DEFAULT_SIZE);
        initializeShape(position);
        this.currentSpeed = DEFAULT_SPEED;
    }
    
    /**
     * Initializes the double hexagonal shape with proper styling.
     * Creates two hexagons attached together horizontally.
     * @param position The center position of the double hexagon
     */
    private void initializeShape(Point2D position) {
        Polygon doubleHexagon = new Polygon();
        updateDoubleHexagonPoints(doubleHexagon, position);
        
        // Apply visual styling
        doubleHexagon.setFill(getBaseColor());
        doubleHexagon.setStroke(Color.BLACK);
        doubleHexagon.setStrokeWidth(1.5);
        
        this.shape = doubleHexagon;
    }
    
    /**
     * Updates the double hexagon vertices based on the current position.
     * Creates two regular hexagons attached together horizontally.
     * @param polygon The polygon to update
     * @param center The center position of the double hexagon
     */
    private void updateDoubleHexagonPoints(Polygon polygon, Point2D center) {
        // Calculate radius for each hexagon
        double radius = VISUAL_SIZE / 2.0;
        
        // Calculate the distance between hexagon centers
        // The distance should be slightly less than 2*radius to create attachment
        double hexagonSpacing = radius * 1.5; // Distance between centers
        
        // Clear existing points
        polygon.getPoints().clear();
        
        // Create the left hexagon
        Point2D leftCenter = new Point2D(center.getX() - hexagonSpacing / 2, center.getY());
        addHexagonPoints(polygon, leftCenter, radius);
        
        // Create the right hexagon
        Point2D rightCenter = new Point2D(center.getX() + hexagonSpacing / 2, center.getY());
        addHexagonPoints(polygon, rightCenter, radius);
    }
    
    /**
     * Adds the vertices of a single hexagon to the polygon.
     * @param polygon The polygon to add points to
     * @param center The center of the hexagon
     * @param radius The radius of the hexagon
     */
    private void addHexagonPoints(Polygon polygon, Point2D center, double radius) {
        // Generate 6 vertices of the hexagon
        for (int i = 0; i < HEXAGON_VERTICES; i++) {
            // Calculate angle for this vertex (starting from 0 degrees)
            double angle = (i * Math.PI) / 3.0; // 60 degrees in radians
            
            // Calculate vertex position
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + radius * Math.sin(angle);
            
            // Add vertex to polygon
            polygon.getPoints().addAll(x, y);
        }
    }
    
    /**
     * Updates the packet's shape position when the packet moves.
     */
    @Override
    protected void updateShapePosition() {
        if (shape != null && shape instanceof Polygon) {
            updateDoubleHexagonPoints((Polygon) shape, position);
        }
    }
    
    /**
     * Returns the base color for the hexagon packet.
     * @return A distinctive color for hexagon packets
     */
    @Override
    protected Color getBaseColor() {
        return Color.MEDIUMSEAGREEN;
    }
    
    /**
     * Checks if this packet is compatible with the given port.
     * Double hexagon packets are compatible with single hexagon ports.
     * @param port The port to check compatibility with
     * @return true if the port accepts hexagon packets
     */
    @Override
    public boolean isCompatible(Port port) {
        return port.getType() == PacketType.HEXAGON;
    }
    
    /**
     * Adjusts the packet's speed based on port compatibility.
     * Hexagon packets maintain consistent speed on compatible ports
     * and move faster on incompatible ports.
     * @param port The port to adjust speed for
     */
    public void adjustSpeedForPort(Port port) {
        boolean compatible = isCompatible(port);
        this.currentSpeed = compatible ? COMPATIBLE_PORT_SPEED : INCOMPATIBLE_PORT_SPEED;
        this.setCompatibility(compatible);
        
        // Apply the new speed to velocity while maintaining direction
        if (velocity.magnitude() > 0) {
            velocity = velocity.normalize().multiply(currentSpeed);
        }
    }
    
    /**
     * Updates the packet state each frame.
     * @param deltaTime The time elapsed since the last update
     */
    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);
        
        // Additional hexagon-specific update logic can be added here
        // For example, rotation or special effects
    }
    
    /**
     * Returns the coin value of this packet type.
     * @return The coin value for hexagon packets
     */
    @Override
    public int getCoinValue() {
        return DEFAULT_COIN_VALUE;
    }
    
    /**
     * Returns a string representation of the packet for debugging.
     * @return A descriptive string of the packet
     */
    @Override
    public String toString() {
        return String.format("HexagonPacket{id=%d, position=(%.1f,%.1f), speed=%.1f, health=%d/%d}", 
                            getId(), position.getX(), position.getY(), currentSpeed, getHealth(), getSize());
    }
} 
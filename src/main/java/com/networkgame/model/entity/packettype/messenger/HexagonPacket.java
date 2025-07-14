package com.networkgame.model.entity.packettype.messenger;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

/**
 * HexagonPacket represents a double hexagonal packet in the messenger category.
 * It has a shape of two attached hexagons but is compatible with single hexagon ports.
 * It moves with acceleration behavior - constant acceleration on compatible ports,
 * negative acceleration on incompatible ports.
 */
public class HexagonPacket extends Packet {
    
    // Constants for hexagon packet configuration
    private static final int DEFAULT_SIZE = 2;
    private static final int DEFAULT_COIN_VALUE = 1;
    private static final double DEFAULT_SPEED = 70.0;
    private static final double VISUAL_SIZE = 14.0;  // Slightly smaller to accommodate two hexagons
    private static final int HEXAGON_VERTICES = 6;
    
    // Acceleration constants
    private static final double COMPATIBLE_ACCELERATION = 25.0;    // Positive acceleration on compatible ports
    private static final double INCOMPATIBLE_ACCELERATION = -15.0; // Negative acceleration on incompatible ports
    private static final double MAX_SPEED = 150.0;                 // Maximum speed limit
    private static final double MIN_SPEED = 30.0;                  // Minimum speed limit
    
    // Acceleration state
    private double currentAcceleration = 0.0;
    private boolean isAccelerating = false;
    
    // Direction reversal state
    private boolean isReversing = false;
    private Point2D originalDestination = null;
    private Point2D sourceSystemPosition = null;
    private int retryCount = 0;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final double COLLISION_DETECTION_RADIUS = 25.0;
    private boolean retryScheduled = false; // Flag to prevent multiple retry attempts
    
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
     * Adjusts the packet's acceleration based on port compatibility.
     * Hexagon packets have constant positive acceleration on compatible ports
     * and negative acceleration on incompatible ports.
     * @param port The port to adjust acceleration for
     */
    public void adjustSpeedForPort(Port port) {
        boolean compatible = isCompatible(port);
        this.currentAcceleration = compatible ? COMPATIBLE_ACCELERATION : INCOMPATIBLE_ACCELERATION;
        this.isAccelerating = true;
        this.setCompatibility(compatible);
        
        System.out.println("HexagonPacket " + getId() + ": Acceleration set to " + currentAcceleration + 
                          " (compatible=" + compatible + ")");
    }
    
    /**
     * Updates the packet state each frame with acceleration behavior.
     * @param deltaTime The time elapsed since the last update
     */
    @Override
    public void update(double deltaTime) {
        // Apply acceleration if active (including during reversal)
        if (isAccelerating && !isInsideSystem() && !hasReachedEndSystem()) {
            applyAcceleration(deltaTime);
        }
        
        // Check for collisions with other packets if not reversing
        if (!isReversing && !isInsideSystem() && !hasReachedEndSystem()) {
            checkForPacketCollisions();
        }
        
        // Check if packet has returned to source while reversing
        if (isReversing && hasReturnedToSource()) {
            System.out.println("*** HEXAGON UPDATE: Packet " + getId() + " detected return to source, calling handleReturnToSource ***");
            handleReturnToSource();
        }
        
        // Call parent update for standard movement
        super.update(deltaTime);
    }
    
    /**
     * Applies acceleration to change the packet's speed over time.
     * @param deltaTime The time elapsed since the last update
     */
    private void applyAcceleration(double deltaTime) {
        // Calculate new speed based on acceleration
        double newSpeed = currentSpeed + (currentAcceleration * deltaTime);
        
        // Clamp speed to limits
        newSpeed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, newSpeed));
        
        // Update current speed
        if (Math.abs(newSpeed - currentSpeed) > 0.1) {
            System.out.println("HexagonPacket " + getId() + ": Speed changing from " + 
                              String.format("%.1f", currentSpeed) + " to " + String.format("%.1f", newSpeed));
        }
        
        currentSpeed = newSpeed;
        
        // Update velocity with new speed while maintaining direction
        if (velocity.magnitude() > 0) {
            velocity = velocity.normalize().multiply(currentSpeed);
        }
    }
    
    /**
     * Stops acceleration (called when packet enters system or reaches destination).
     */
    public void stopAcceleration() {
        isAccelerating = false;
        currentAcceleration = 0.0;
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

    /**
     * Checks for collisions with other packets while moving on wires.
     */
    private void checkForPacketCollisions() {
        if (getCurrentConnection() == null) return;
        
        // Get other packets on the same connection
        for (Packet otherPacket : getCurrentConnection().getPackets()) {
            if (otherPacket != this && !otherPacket.isInsideSystem() && !otherPacket.hasReachedEndSystem()) {
                double distance = this.getPosition().distance(otherPacket.getPosition());
                
                if (distance <= COLLISION_DETECTION_RADIUS) {
                    System.out.println("HexagonPacket " + getId() + ": Collision detected with " + 
                                     otherPacket.getType() + " packet " + otherPacket.getId());
                    handlePacketCollision(otherPacket);
                    break; // Only handle one collision at a time
                }
            }
        }
    }
    
    /**
     * Handles collision with another packet by reversing direction.
     * @param otherPacket The packet this HexagonPacket collided with
     */
    public void handlePacketCollision(Packet otherPacket) {
        if (isReversing) {
            System.out.println("*** HEXAGON COLLISION IGNORED: Packet " + getId() + " already reversing, ignoring collision with " + otherPacket.getId() + " ***");
            return; // Already reversing, ignore further collisions
        }
        
        System.out.println("*** HEXAGON COLLISION START: Packet " + getId() + " collision with " + otherPacket.getType() + " " + otherPacket.getId() + " ***");
        System.out.println("*** HEXAGON COLLISION POSITION: Current=" + getPosition() + " ***");
        
        // Reset retry state for new collision
        retryScheduled = false;
        
        // Store original destination for retry
        if (getCurrentConnection() != null) {
            originalDestination = getCurrentConnection().getTargetPort().getPosition();
            sourceSystemPosition = getCurrentConnection().getSourcePort().getPosition();
            System.out.println("*** HEXAGON COLLISION ROUTE: Source=" + sourceSystemPosition + ", Target=" + originalDestination + " ***");
        } else {
            System.out.println("*** HEXAGON COLLISION WARNING: No current connection for reversal! ***");
        }
        
        // Start reversing
        isReversing = true;
        retryCount++;
        
        // Set speed to default - let the packet router handle the direction
        currentSpeed = DEFAULT_SPEED;
        
        // Apply acceleration logic for return movement based on port compatibility
        if (getCurrentConnection() != null) {
            Port sourcePort = getCurrentConnection().getSourcePort();
            boolean compatible = isCompatible(sourcePort);
            this.currentAcceleration = compatible ? COMPATIBLE_ACCELERATION : INCOMPATIBLE_ACCELERATION;
            this.isAccelerating = true;
            
            System.out.println("*** HEXAGON COLLISION ACCELERATION: Return acceleration=" + currentAcceleration + 
                              " (compatible=" + compatible + ") ***");
        }
        
        // Note: Unit vector and velocity will be handled by PacketRouter based on isReversing flag
        
        System.out.println("*** HEXAGON COLLISION COMPLETE: Direction reversed, returning to source (attempt " + retryCount + ") ***");
        System.out.println("*** HEXAGON COLLISION VECTOR: New unit vector=[" + getUnitVector()[0] + ", " + getUnitVector()[1] + "] ***");
    }
    
    /**
     * Checks if the packet has returned to its source system.
     * @return true if the packet is close to the source system
     */
    private boolean hasReturnedToSource() {
        if (sourceSystemPosition == null) {
            System.out.println("*** HEXAGON RETURN CHECK: Packet " + getId() + " - sourceSystemPosition is null ***");
            return false;
        }
        
        double distanceToSource = getPosition().distance(sourceSystemPosition);
        boolean hasReturned = distanceToSource <= 30.0; // Close enough to source system
        
        System.out.println("*** HEXAGON RETURN CHECK: Packet " + getId() + " - distance to source: " + 
                          String.format("%.1f", distanceToSource) + " (threshold: 30.0) ***");
        
        // Also check if we're no longer on a wire (PacketRouter removed us)
        if (getCurrentConnection() == null && isReversing) {
            System.out.println("*** HEXAGON RETURN DETECTED: Packet " + getId() + " - no longer on wire while reversing ***");
            hasReturned = true;
        }
        
        if (hasReturned) {
            System.out.println("*** HEXAGON RETURN SUCCESS: Packet " + getId() + " has returned to source ***");
        }
        
        return hasReturned;
    }
    
    /**
     * Handles the retry mechanism when the packet returns to source.
     */
    private void handleReturnToSource() {
        System.out.println("*** HEXAGON RETURN HANDLER: Packet " + getId() + " - retryCount: " + retryCount + "/" + MAX_RETRY_ATTEMPTS + " ***");
        
        if (retryCount >= MAX_RETRY_ATTEMPTS) {
            System.out.println("*** HEXAGON RETRY LIMIT: Packet " + getId() + " max attempts reached, giving up ***");
            return;
        }
        
        // Prevent multiple retry attempts from being scheduled
        if (retryScheduled) {
            System.out.println("*** HEXAGON RETRY SKIP: Packet " + getId() + " - retry already scheduled ***");
            return;
        }
        
        // CRITICAL: Mark packet as retrying to prevent removal from game state
        setProperty("isRetrying", true);
        setProperty("retryStartTime", System.currentTimeMillis());
        retryScheduled = true;
        
        System.out.println("*** HEXAGON RETRY PREP: Packet " + getId() + " preparing for retry " + (retryCount + 1) + " ***");
        
        // Wait a moment before retrying
        PauseTransition pause = new PauseTransition(Duration.millis(500));
        pause.setOnFinished(event -> {
            System.out.println("*** HEXAGON RETRY TRIGGER: Packet " + getId() + " executing retry after pause ***");
            retryScheduled = false; // Clear flag before retry
            attemptRetry();
        });
        pause.play();
    }
    
    /**
     * Attempts to retry reaching the destination.
     */
    private void attemptRetry() {
        System.out.println("*** HEXAGON RETRY START: Packet " + getId() + " attempting retry ***");
        
        if (originalDestination == null || sourceSystemPosition == null) {
            System.out.println("*** HEXAGON RETRY FAILED: Packet " + getId() + " - missing destination or source position ***");
            return;
        }
        
        System.out.println("*** HEXAGON RETRY SETUP: Packet " + getId() + " resetting direction towards destination ***");
        
        // Reset direction towards destination
        Point2D direction = originalDestination.subtract(sourceSystemPosition).normalize();
        setUnitVector(direction.getX(), direction.getY());
        
        // Reset state
        isReversing = false;
        currentSpeed = DEFAULT_SPEED;
        
        // Clear retry flag
        setProperty("isRetrying", false);
        
        // Apply acceleration logic for forward movement based on port compatibility
        if (getCurrentConnection() != null) {
            Port targetPort = getCurrentConnection().getTargetPort();
            boolean compatible = isCompatible(targetPort);
            this.currentAcceleration = compatible ? COMPATIBLE_ACCELERATION : INCOMPATIBLE_ACCELERATION;
            this.isAccelerating = true;
            
            System.out.println("*** HEXAGON RETRY ACCELERATION: Packet " + getId() + " - acceleration set to " + currentAcceleration + 
                              " (compatible=" + compatible + ") ***");
        } else {
            System.out.println("*** HEXAGON RETRY WARNING: Packet " + getId() + " - no current connection for acceleration setup ***");
            // If no connection, try to get the packet back onto the wire
            attemptReconnectToWire();
        }
        
        // Reset position to source
        setPosition(sourceSystemPosition);
        
        // Reset progress property
        setProperty("progress", 0.0);
        
        // Mark packet as ready for retry
        setProperty("readyForRetry", true);
        
        System.out.println("*** HEXAGON RETRY COMPLETE: Packet " + getId() + " retry setup complete ***");
        System.out.println("*** HEXAGON RETRY DETAILS: Position=" + getPosition() + ", Target=" + originalDestination + " ***");
    }
    
    /**
     * Attempts to reconnect the packet to the wire for retry
     */
    private void attemptReconnectToWire() {
        System.out.println("*** HEXAGON RECONNECT: Packet " + getId() + " attempting to reconnect to wire ***");
        
        // Try to find an available connection from the source system
        if (sourceSystemPosition != null) {
            // Get the game state to access connections
            Object gameStateObj = getProperty("gameState");
            if (gameStateObj instanceof com.networkgame.model.state.GameState) {
                com.networkgame.model.state.GameState gameState = (com.networkgame.model.state.GameState) gameStateObj;
                
                // Find the connection that goes from source to target
                for (com.networkgame.model.entity.Connection connection : gameState.getConnections()) {
                    if (connection.getSourcePort().getPosition().equals(sourceSystemPosition) && 
                        connection.getTargetPort().getPosition().equals(originalDestination)) {
                        
                        System.out.println("*** HEXAGON RECONNECT: Found connection for packet " + getId() + " ***");
                        
                        // Add the packet back to the connection
                        connection.addPacket(this);
                        setCurrentConnection(connection);
                        setProperty("progress", 0.0);
                        
                        System.out.println("*** HEXAGON RECONNECT SUCCESS: Packet " + getId() + " reconnected to wire ***");
                        return;
                    }
                }
                
                System.out.println("*** HEXAGON RECONNECT FAILED: No suitable connection found for packet " + getId() + " ***");
            }
        }
    }
    
    /**
     * Checks if this packet is currently reversing direction.
     * @return true if the packet is reversing
     */
    public boolean isReversing() {
        return isReversing;
    }
    
    /**
     * Gets the current retry count.
     * @return the number of retry attempts made
     */
    public int getRetryCount() {
        return retryCount;
    }
} 
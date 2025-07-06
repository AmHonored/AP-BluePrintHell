package com.networkgame.model.entity; 

import javafx.geometry.Point2D;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

import java.util.List;
import java.util.ArrayList;
import com.networkgame.model.state.GameState;
import com.networkgame.model.ConnectionVisualizer;
import com.networkgame.model.packet.PacketRouter;
import com.networkgame.model.entity.Packet;
import com.networkgame.service.WireCollisionService;

/**
 * Represents a connection between two ports in the network
 */
public class Connection {
    // Core properties
    private final Port sourcePort;
    private final Port targetPort;
    private double length;
    private double maxWireLength;
    private double remainingWireLength;
    private GameState gameState;
    
    // Delegation objects
    private final ConnectionVisualizer visualizer;
    private final PacketRouter packetRouter;
    
    /** Creates a new connection between two ports */
    public Connection(Port sourcePort, Port targetPort) {
        this.sourcePort = sourcePort;
        this.targetPort = targetPort;
        this.packetRouter = new PacketRouter(this);
        this.visualizer = new ConnectionVisualizer(this, false);
        
        updateLength();
        this.maxWireLength = this.length * 1.5;
        this.remainingWireLength = this.maxWireLength - this.length;
        
        sourcePort.setConnection(this);
        targetPort.setConnection(this);
    }
    
    /** Sets the game state for this connection */
    public void setGameState(GameState gameState) { 
        this.gameState = gameState; 
    }
    
    /** Checks if the connection can be updated to new positions */
    public boolean canUpdatePosition(Point2D newSourcePos, Point2D newTargetPos) {
        double newLength = newSourcePos.distance(newTargetPos);
        return newLength <= length || remainingWireLength >= (newLength - length);
    }
    
    /** Updates the connection's position and remaining wire length */
    public void updatePosition() {
        double oldLength = length;
        updateLength();
        remainingWireLength -= (length - oldLength);
        visualizer.updatePosition();
        visualizer.updateConnectionColor();
    }
    
    /** Updates the connection's length based on port positions */
    private void updateLength() {
        this.length = sourcePort.getPosition().distance(targetPort.getPosition());
    }
    
    /**
     * Calculate the actual wire length including bend points.
     * This method calculates the total path length from source to target through all bend points.
     * @return The total wire length including all segments
     */
    public double calculateActualWireLength() {
        if (gameState == null || gameState.getWireCollisionManager() == null) {
            // Fallback to direct distance if no wire collision manager
            return sourcePort.getPosition().distance(targetPort.getPosition());
        }
        
        // Get bend points from the wire collision manager
        // Note: This assumes getBendPoints returns a compatible format or needs adaptation
        List<WireCollisionService.BendPoint> bendPoints = getBendPointsFromManager();
        
        // Use the service to calculate length
        return WireCollisionService.calculateActualWireLength(
            sourcePort.getPosition(), 
            targetPort.getPosition(), 
            bendPoints
        );
    }
    
    /**
     * Helper method to get bend points from the wire collision manager.
     * This method handles the conversion from manager-specific bend points to service bend points.
     */
    private List<WireCollisionService.BendPoint> getBendPointsFromManager() {
        if (gameState == null || gameState.getWireCollisionManager() == null) {
            return new ArrayList<>();
        }
        
        // For now, return empty list as we need to refactor WireCollisionManager
        // TODO: Update WireCollisionManager to provide bend points in a way that doesn't create cycles
        return new ArrayList<>();
    }
    
    /**
     * Get the wire length. This now returns the actual wire length including bend points.
     * @return The actual wire length
     */
    public double getLength() {
        return calculateActualWireLength();
    }
    
    // Packet routing operations
    
    /** Adds a packet to this connection */
    public void addPacket(Packet packet) {
        packetRouter.addPacket(packet);
        visualizer.applyGlowEffect();
    }
    
    public void update(double deltaTime) { packetRouter.update(deltaTime); }
    public void removePacket(Packet packet) { packetRouter.removePacket(packet); }

    /** Disconnects this connection from its ports */
    public void disconnect() {
        if (sourcePort != null) sourcePort.setConnection(null);
        if (targetPort != null) targetPort.setConnection(null);
    }
    
    // Visual operations - delegated to ConnectionVisualizer
    public void clearAllStyling() { visualizer.clearAllStyling(); }
    public void applyGlowEffect() { visualizer.applyGlowEffect(); }
    public void clearGlowEffect() { visualizer.clearGlowEffect(); }
    
    // Position information getters
    public Point2D getSourcePosition() { return sourcePort.getPosition(); }
    public Point2D getTargetPosition() { return targetPort.getPosition(); }
    public Point2D getDirection() { return getTargetPosition().subtract(getSourcePosition()).normalize(); }
    
    /** Checks if the given point is on this connection */
    public boolean containsPoint(Point2D point) {
        Point2D start = getSourcePosition();
        Point2D end = getTargetPosition();
        
        double distanceToLine = packetRouter.distanceToLine(point, start, end);
        double distanceStartToEnd = start.distance(end);
        double distanceStartToPoint = start.distance(point);
        double distanceEndToPoint = end.distance(point);
        
        return distanceToLine < 5 && distanceStartToPoint + distanceEndToPoint <= distanceStartToEnd + 0.1;
    }
    
    // Connection state getters and setters
    public boolean isEmpty() { return packetRouter.isEmpty(); }
    public boolean isAvailable() { return packetRouter.isAvailable(); }
    
    /** Sets the availability status of the connection */
    public void setAvailable(boolean available) {
        packetRouter.setAvailable(available);
        if (available) visualizer.clearGlowEffect(); else visualizer.applyGlowEffect();
    }
    
    /** Sets the connection as available and notifies interested parties */
    public void setAvailableAndNotify() {
        setAvailable(true);
        packetRouter.notifyConnectionAvailable();
    }
    
    /** Sets the connection's busy status */
    public void setBusy(boolean busy) {
        if (busy) visualizer.applyGlowEffect(); else visualizer.clearGlowEffect();
    }
    
    // Getters (consolidated for brevity)
    public Port getSourcePort() { return sourcePort; }
    public Port getTargetPort() { return targetPort; }
    public Shape getConnectionShape() { return visualizer.getConnectionShape(); }
    public Line getLine() { return visualizer.getLine(); }
    public double getRemainingWireLength() { return remainingWireLength; }
    public double getMaxWireLength() { return maxWireLength; }
    public boolean isOutOfWire() { return remainingWireLength <= 0; }
    public List<Packet> getPackets() { return packetRouter.getActivePackets(); }
    public boolean isBusy() { return packetRouter.isBusy(); }
} 

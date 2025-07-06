package com.networkgame.view;

import com.networkgame.model.*;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Shape;

import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.Port;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.packettype.messenger.SquarePacket;
import com.networkgame.model.entity.packettype.messenger.TrianglePacket;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.system.NetworkSystem;

import java.util.*;

/**
 * Manages connections between ports in the network game
 */
public class ConnectionManager {
    private GameState gameState;
    private Pane gamePane;
    private java.util.Map<Port, java.util.Set<Connection>> portConnectionMap = new java.util.HashMap<>();
    private long lastWireUpdateTime = 0;
    private final long WIRE_UPDATE_THROTTLE = 100; // milliseconds between updates during drag

    public ConnectionManager(GameState gameState, Pane gamePane) {
        this.gameState = gameState;
        this.gamePane = gamePane;
    }
    
    /**
     * Initialize the port-connection map
     */
    public void initializePortConnectionMap() {
        portConnectionMap.clear();
        
        // Initialize an empty connection set for each port
        for (NetworkSystem system : gameState.getSystems()) {
            for (Port port : system.getInputPorts()) {
                portConnectionMap.put(port, new HashSet<>());
            }
            for (Port port : system.getOutputPorts()) {
                portConnectionMap.put(port, new HashSet<>());
            }
        }
    }

    /**
     * Connect two ports together
     */
    public void connectPorts(Port sourcePort, Port targetPort) {
        Connection connection = gameState.createConnection(sourcePort, targetPort);
        
        if (connection != null) {
            // Add the connection line to the game pane
            Shape connectionShape = connection.getConnectionShape();
            connectionShape.getStyleClass().add("connection");
            
            // Set connection color to green
            if (connectionShape instanceof Line) {
                ((Line) connectionShape).setStroke(Color.rgb(50, 205, 50));
                ((Line) connectionShape).setStrokeWidth(3.0);
            }
            
            // Add the connection to UI elements with Z-order positioning
            // Put it below the ports but above the systems
            gamePane.getChildren().add(connectionShape);
            
            // Reposition connection shape to ensure it's behind ports
            connectionShape.toBack();
            
            // Bring the system shapes back to the front, but keeping them behind the ports
            for (NetworkSystem system : gameState.getSystems()) {
                system.getShape().toFront();
                system.getIndicatorLamp().toFront();
            }
            
            // Bring all ports to the front
            for (NetworkSystem system : gameState.getSystems()) {
                for (Port port : system.getInputPorts()) {
                    port.getShape().toFront();
                }
                for (Port port : system.getOutputPorts()) {
                    port.getShape().toFront();
                }
            }
            
            // Update the indicator lamps after connection changes
            sourcePort.getSystem().updateIndicatorLamp();
            targetPort.getSystem().updateIndicatorLamp();
            
            // Track this connection for both ports
            portConnectionMap.get(sourcePort).add(connection);
            portConnectionMap.get(targetPort).add(connection);
        } else {
            // Display error message or handle connection failure
            System.out.println("Failed to connect ports");
        }
    }
    
    /**
     * Disconnect a port from all its connections
     */
    public void disconnectPort(Port port) {
        if (port.isConnected()) {
            Connection connection = port.getConnection();
            
            // Store related system to update indicator lamp after disconnect
            NetworkSystem sourceSystem = connection.getSourcePort().getSystem();
            NetworkSystem targetSystem = connection.getTargetPort().getSystem();
            
            // Get the associated ports
            Port sourcePort = connection.getSourcePort();
            Port targetPort = connection.getTargetPort();
            
            // Remove connection references from both ports
            gameState.removeConnection(connection);
            
            // Remove the connection line from the game pane
            gamePane.getChildren().remove(connection.getConnectionShape());
            
            // Remove entries from port-connection map
            portConnectionMap.get(sourcePort).remove(connection);
            portConnectionMap.get(targetPort).remove(connection);
            
            // Update indicator lamps after disconnect
            sourceSystem.updateIndicatorLamp();
            targetSystem.updateIndicatorLamp();
        }
    }
    
    /**
     * Remove all connections involving a port
     */
    public void removePortConnections(Port port) {
        // First, find and remove any connections involving this port
        List<Connection> connectionsToRemove = new ArrayList<>();
        
        for (Connection connection : gameState.getConnections()) {
            if (connection.getSourcePort() == port || connection.getTargetPort() == port) {
                connectionsToRemove.add(connection);
            }
        }
        
        // Store related systems to update indicator lamps
        Set<NetworkSystem> systemsToUpdate = new HashSet<>();
        
        // Process the removals outside the loop
        for (Connection connection : connectionsToRemove) {
            // Store systems for lamp updates
            systemsToUpdate.add(connection.getSourcePort().getSystem());
            systemsToUpdate.add(connection.getTargetPort().getSystem());
            
            // Remove the connection line from the game pane
            gamePane.getChildren().remove(connection.getConnectionShape());
            
            // Remove the connection from the game state
            gameState.removeConnection(connection);
            
            // Optionally, update portConnectionMap
            Port sourcePort = connection.getSourcePort();
            Port targetPort = connection.getTargetPort();
            if (portConnectionMap.containsKey(sourcePort)) {
                portConnectionMap.get(sourcePort).remove(connection);
            }
            if (portConnectionMap.containsKey(targetPort)) {
                portConnectionMap.get(targetPort).remove(connection);
            }
        }
        
        // Update all affected indicator lamps
        for (NetworkSystem system : systemsToUpdate) {
            system.updateIndicatorLamp();
        }
    }
    
    /**
     * Find and track which ports are connected by each connection
     */
    public void findAndTrackConnectionPorts(Connection connection) {
        Line line = connection.getLine();
        double startX = line.getStartX();
        double startY = line.getStartY();
        double endX = line.getEndX();
        double endY = line.getEndY();
        
        Port startPort = null;
        Port endPort = null;
        double minStartDist = Double.MAX_VALUE;
        double minEndDist = Double.MAX_VALUE;
        
        // Find the closest ports to each end of the line
        for (NetworkSystem system : gameState.getSystems()) {
            for (Port port : system.getInputPorts()) {
                double[] center = getPortCenter(port);
                double startDist = distance(center[0], center[1], startX, startY);
                double endDist = distance(center[0], center[1], endX, endY);
                
                if (startDist < minStartDist) {
                    minStartDist = startDist;
                    startPort = port;
                }
                
                if (endDist < minEndDist) {
                    minEndDist = endDist;
                    endPort = port;
                }
            }
            
            for (Port port : system.getOutputPorts()) {
                double[] center = getPortCenter(port);
                double startDist = distance(center[0], center[1], startX, startY);
                double endDist = distance(center[0], center[1], endX, endY);
                
                if (startDist < minStartDist) {
                    minStartDist = startDist;
                    startPort = port;
                }
                
                if (endDist < minEndDist) {
                    minEndDist = endDist;
                    endPort = port;
                }
            }
        }
        
        // Only track if we found both ports
        if (startPort != null && endPort != null) {
            // Add this connection to both ports' connection sets
            portConnectionMap.get(startPort).add(connection);
            portConnectionMap.get(endPort).add(connection);
            
            System.out.println("Tracked connection between ports at: " + 
                "(" + startX + "," + startY + ") and (" + endX + "," + endY + ")");
        }
    }
    
    /**
     * Update all connections attached to a specific port
     */
    public void updateConnectionsForPort(Port port) {
        Shape portShape = port.getShape();
        double[] portCenter = getPortCenter(port);
        
        // Check all connections to see if they connect to this port
        for (Connection connection : gameState.getConnections()) {
            Line line = connection.getLine();
            
            // Calculate distance from port to both line endpoints
            double startDist = distance(portCenter[0], portCenter[1], line.getStartX(), line.getStartY());
            double endDist = distance(portCenter[0], portCenter[1], line.getEndX(), line.getEndY());
            
            // Update the endpoint that's closest to this port
            // Using a somewhat generous threshold to ensure all relevant connections are updated
            if (startDist < 100 && startDist < endDist) {
                line.setStartX(portCenter[0]);
                line.setStartY(portCenter[1]);
                System.out.println("Updated connection start point to: " + portCenter[0] + "," + portCenter[1]);
            } else if (endDist < 100) {
                line.setEndX(portCenter[0]);
                line.setEndY(portCenter[1]);
                System.out.println("Updated connection end point to: " + portCenter[0] + "," + portCenter[1]);
            }
        }
    }
    
    /**
     * Update the wire lengths and display on the UI
     */
    public void updateWireLengths() {
        // Recalculate total wire length used
        double totalWireUsed = 0;
        boolean anyConnectionOutOfWire = false;
        
        for (Connection connection : gameState.getConnections()) {
            // Get the direct distance between ports
            double length = connection.getLength();
            totalWireUsed += length;
            
            // Check if any connection is out of wire
            if (connection.isOutOfWire()) {
                anyConnectionOutOfWire = true;
            }
        }
        
        // Update the game state with new total
        gameState.setTotalWireUsed(totalWireUsed);
        
        System.out.println("Updated wire lengths. Total used: " + totalWireUsed + ", Remaining: " + gameState.getRemainingWireLength());
    }
    
    /**
     * Update the color of all connection lines based on their state
     */
    public void updateConnectionColors() {
        for (Connection connection : gameState.getConnections()) {
            Shape connectionShape = connection.getConnectionShape();
            if (connectionShape instanceof Line) {
                ((Line) connectionShape).setStroke(Color.rgb(50, 205, 50));
                ((Line) connectionShape).setStrokeWidth(3.0);
            }
        }
    }
    
    /**
     * Helper method to check if a port is part of a connection
     */
    public boolean isPortInConnection(Connection connection, Port port) {
        return connection.getSourcePort() == port || connection.getTargetPort() == port;
    }
    
    /**
     * Get the center coordinates of a port
     * @param port The port to get center coordinates for
     * @return Array with [x, y] center coordinates
     */
    public double[] getPortCenter(Port port) {
        Shape portShape = port.getShape();
        double centerX = 0;
        double centerY = 0;
        
        if (portShape instanceof javafx.scene.shape.Rectangle) {
            javafx.scene.shape.Rectangle rect = (javafx.scene.shape.Rectangle) portShape;
            centerX = rect.getX() + rect.getWidth() / 2;
            centerY = rect.getY() + rect.getHeight() / 2;
        } else if (portShape instanceof javafx.scene.shape.Circle) {
            javafx.scene.shape.Circle circle = (javafx.scene.shape.Circle) portShape;
            centerX = circle.getCenterX();
            centerY = circle.getCenterY();
        } else if (portShape instanceof javafx.scene.shape.Polygon) {
            javafx.scene.shape.Polygon polygon = (javafx.scene.shape.Polygon) portShape;
            
            // Calculate center of polygon by averaging all points
            double sumX = 0, sumY = 0;
            int pointCount = polygon.getPoints().size() / 2;
            
            for (int i = 0; i < pointCount; i++) {
                sumX += polygon.getPoints().get(i * 2);
                sumY += polygon.getPoints().get(i * 2 + 1);
            }
            
            centerX = sumX / pointCount;
            centerY = sumY / pointCount;
        }
        
        return new double[]{centerX, centerY};
    }
    
    /**
     * Calculate distance between two points
     */
    public double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    /**
     * Find if there's a port at the given position
     */
    public Port findPortAtPosition(double sceneX, double sceneY) {
        // Convert scene coordinates to local game pane coordinates
        javafx.geometry.Point2D localPoint = gamePane.sceneToLocal(sceneX, sceneY);
        
        // Increased detection radius for easier targeting
        double detectionRadius = 15.0; // Increased from implicit small size to 15 pixels
        
        // First look for input ports since users are typically trying to connect TO them
        Port bestMatch = null;
        double closestDistance = Double.MAX_VALUE;
        
        // Check all systems
        for (NetworkSystem system : gameState.getSystems()) {
            // Prioritize checking input ports first
            for (Port port : system.getInputPorts()) {
                // Skip already connected input ports
                if (port.isConnected()) continue;
                
                // Get port center position
                double[] portCenter = getPortCenter(port);
                double distance = distance(portCenter[0], portCenter[1], localPoint.getX(), localPoint.getY());
                
                // Use a proximity check instead of strict bounds containment
                if (distance <= detectionRadius && distance < closestDistance) {
                    bestMatch = port;
                    closestDistance = distance;
                }
            }
            
            // Then check output ports (lower priority when creating connections)
            for (Port port : system.getOutputPorts()) {
                // Only check unconnected output ports
                if (port.isConnected()) continue;
                
                // Get port center position
                double[] portCenter = getPortCenter(port);
                double distance = distance(portCenter[0], portCenter[1], localPoint.getX(), localPoint.getY());
                
                // Use a slightly smaller detection radius for output ports to prioritize inputs
                if (distance <= detectionRadius*0.8 && distance < closestDistance) {
                    bestMatch = port;
                    closestDistance = distance;
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Update templine color based on validity and length
     */
    public void updateTempLineColor(Line tempConnectionLine, Port dragSourcePort, double mouseX, double mouseY) {
        if (tempConnectionLine == null || dragSourcePort == null) return;
        
        // Convert scene coordinates to local coordinates
        javafx.geometry.Point2D localPoint = gamePane.sceneToLocal(mouseX, mouseY);
        
        // Calculate wire length using local coordinates
        double startX = tempConnectionLine.getStartX();
        double startY = tempConnectionLine.getStartY();
        double length = distance(startX, startY, localPoint.getX(), localPoint.getY());
        
        // Check if we're over a valid input port (not from same system and not already connected)
        Port targetPort = findPortAtPosition(mouseX, mouseY);
        boolean validTarget = (targetPort != null && 
                              targetPort.isInput() && 
                              !targetPort.isConnected() &&
                              targetPort.getSystem() != dragSourcePort.getSystem() &&
                              targetPort.getType() == dragSourcePort.getType());
        
        // Calculate maximum allowed length
        double remainingWireLength = gameState.getRemainingWireLength();
        
        // Color logic:
        // - Green if over valid input port of correct type that is not already connected
        // - Red if length exceeds remaining wire
        // - Blue otherwise (default)
        if (validTarget) {
            // Use brighter green to make it more noticeable
            tempConnectionLine.setStroke(Color.rgb(0, 255, 0));
            tempConnectionLine.setStrokeWidth(4.0); // Make line thicker when valid
            
            // Add a glow effect to make it even more visible
            javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow(0.8);
            tempConnectionLine.setEffect(glow);
        } else if (length > remainingWireLength) {
            // Use brighter red for invalid connections
            tempConnectionLine.setStroke(Color.rgb(255, 0, 0));
            tempConnectionLine.setStrokeWidth(3.0);
            tempConnectionLine.setEffect(null);
        } else {
            // Default blue for neutral state
            tempConnectionLine.setStroke(Color.rgb(0, 150, 255));
            tempConnectionLine.setStrokeWidth(3.0);
            tempConnectionLine.setEffect(null);
        }
    }
    
    /**
     * Get the set of connections for a port
     */
    public Set<Connection> getConnectionsForPort(Port port) {
        return portConnectionMap.getOrDefault(port, new HashSet<>());
    }
    
    /**
     * Get the port connection map
     */
    public Map<Port, Set<Connection>> getPortConnectionMap() {
        return portConnectionMap;
    }
} 
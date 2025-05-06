package com.networkgame.model;

import javafx.geometry.Point2D;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles port configuration, positioning and compatibility
 */
public class PortManager {
    private NetworkSystem parentSystem;
    
    public PortManager(NetworkSystem parentSystem) {
        this.parentSystem = parentSystem;
    }
    
    /**
     * Updates positions of all ports based on the system's position
     */
    public void updatePortPositions() {
        Point2D position = parentSystem.getPosition();
        double width = parentSystem.getWidth();
        double height = parentSystem.getHeight();
        
        // Position input ports on left side of rectangle
        positionPorts(
            parentSystem.getInputPorts(), 
            height, 
            port -> new Point2D(position.getX(), port), 
            position.getY()
        );
        
        // Position output ports on right side of rectangle
        positionPorts(
            parentSystem.getOutputPorts(), 
            height, 
            port -> new Point2D(position.getX() + width, port), 
            position.getY()
        );
    }
    
    /**
     * Position a list of ports with proper spacing and type-based adjustments
     */
    private void positionPorts(List<Port> ports, double height, Function<Double, Point2D> positionCalculator, double baseY) {
        double spacing = height / (ports.size() + 1);
        
        for (int i = 0; i < ports.size(); i++) {
            Port port = ports.get(i);
            double portY = baseY + (i + 1) * spacing;
            
            // Adjust position based on port type
            portY = adjustPortYPositionByType(port, portY, baseY, height, ports.size());
            
            // Set the port position
            port.setPosition(positionCalculator.apply(portY));
            
            // Update any connection attached to this port
            updatePortConnection(port);
        }
    }
    
    /**
     * Adjust port Y position based on its type
     */
    private double adjustPortYPositionByType(Port port, double portY, double baseY, double height, int portCount) {
        if (port.getType() == Packet.PacketType.SQUARE) {
            // Position square ports slightly higher if possible
            return Math.max(baseY + 10, portY);
        } else if (port.getType() == Packet.PacketType.TRIANGLE && portCount > 1) {
            // Position triangle ports slightly lower if there are multiple ports
            return Math.min(baseY + height - 10, portY);
        }
        
        return portY;
    }
    
    /**
     * Update connection position if port is connected
     */
    private void updatePortConnection(Port port) {
        if (port.isConnected()) {
            port.getConnection().updatePosition();
        }
    }
    
    /**
     * Add an input port to the system
     */
    public void addInputPort(Port port) {
        parentSystem.getInputPorts().add(port);
        updatePortPositions(); // Reposition all ports when adding a new one
    }
    
    /**
     * Add an output port to the system
     */
    public void addOutputPort(Port port) {
        parentSystem.getOutputPorts().add(port);
        updatePortPositions(); // Reposition all ports when adding a new one
    }
    
    /**
     * Find the closest input port to a packet
     */
    public Port findClosestInputPort(Packet packet) {
        Port closest = null;
        double minDistance = Double.MAX_VALUE;
        
        for (Port port : parentSystem.getInputPorts()) {
            double distance = port.getPosition().distance(packet.getPosition());
            if (distance < minDistance) {
                minDistance = distance;
                closest = port;
            }
        }
        
        return closest;
    }
    
    /**
     * Check if all ports are correctly connected with compatible types
     */
    public boolean areAllPortsCorrectlyConnected() {
        if (parentSystem.isStartSystem()) {
            return validateStartSystemPorts();
        } else if (parentSystem.isEndSystem()) {
            return validateEndSystemPorts();
        } else {
            return validateRegularSystemPorts();
        }
    }
    
    /**
     * Validate port connections for a start system
     */
    private boolean validateStartSystemPorts() {
        List<Port> outputPorts = parentSystem.getOutputPorts();
        
        // For start system: all output ports must be connected
        if (outputPorts.isEmpty() || !outputPorts.stream().allMatch(Port::isConnected)) {
            return false;
        }
        
        // Check if all output connections have compatible port types
        return outputPorts.stream()
            .map(Port::getConnection)
            .filter(connection -> connection != null)
            .allMatch(this::isConnectionTypeCompatible);
    }
    
    /**
     * Validate port connections for an end system
     */
    private boolean validateEndSystemPorts() {
        List<Port> inputPorts = parentSystem.getInputPorts();
        
        // For end system: all input ports must be connected
        if (inputPorts.isEmpty() || !inputPorts.stream().allMatch(Port::isConnected)) {
            return false;
        }
        
        // Check if all input connections have compatible port types
        return inputPorts.stream()
            .map(Port::getConnection)
            .filter(connection -> connection != null)
            .allMatch(this::isConnectionTypeCompatible);
    }
    
    /**
     * Validate port connections for a regular system
     */
    private boolean validateRegularSystemPorts() {
        List<Port> inputPorts = parentSystem.getInputPorts();
        List<Port> outputPorts = parentSystem.getOutputPorts();
        
        // Check if all ports are connected
        boolean allPortsConnected = inputPorts.stream().allMatch(Port::isConnected) && 
                                   outputPorts.stream().allMatch(Port::isConnected);
        
        if (!allPortsConnected) {
            return false;
        }
        
        // Check if all connections have compatible port types
        return areAllConnectionsCompatible(inputPorts) && 
               areAllConnectionsCompatible(outputPorts);
    }
    
    /**
     * Check if all connections for the provided ports are compatible
     */
    private boolean areAllConnectionsCompatible(List<Port> ports) {
        return ports.stream()
            .map(Port::getConnection)
            .filter(connection -> connection != null)
            .allMatch(this::isConnectionTypeCompatible);
    }
    
    /**
     * Check if a connection has compatible port types
     */
    private boolean isConnectionTypeCompatible(Connection connection) {
        if (connection.getSourcePort() == null || connection.getTargetPort() == null) {
            return false;
        }
        
        return connection.getSourcePort().getType() == connection.getTargetPort().getType();
    }
    
    /**
     * Find available output ports that are connected
     */
    public List<Port> findAvailableOutputPorts() {
        return parentSystem.getOutputPorts().stream()
            .filter(Port::isConnected)
            .collect(Collectors.toList());
    }
} 
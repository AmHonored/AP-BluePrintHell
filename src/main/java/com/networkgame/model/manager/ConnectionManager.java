package com.networkgame.model.manager; 

import java.util.ArrayList;
import java.util.List;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;
import com.networkgame.model.state.GameStateProvider;
import com.networkgame.model.state.GameStateCallbacks;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.system.NetworkSystem;
/**
 * Manages connections between network systems.
 */
public class ConnectionManager {
    private GameState gameState;
    private List<Connection> connections;
    private double remainingWireLength = 1000;
    private double totalWireUsed = 0;

    public ConnectionManager(GameState gameState) {
        this.gameState = gameState;
        this.connections = new ArrayList<>();
    }

    /**
     * Create a connection between two ports
     * @param sourcePort Source port
     * @param targetPort Target port
     * @return The created connection, or null if invalid
     */
    public Connection createConnection(Port sourcePort, Port targetPort) {
        // Check if connection is valid
        if (sourcePort.isInput() || !targetPort.isInput()) {
            return null;
        }
        
        // Check if ports are already connected
        if (sourcePort.isConnected() || targetPort.isConnected()) {
            return null;
        }
        
        // Check if source and target are from the same system
        if (sourcePort.getSystem() == targetPort.getSystem()) {
            return null;
        }
        
        // Calculate wire length needed
        double wireLength = sourcePort.getPosition().distance(targetPort.getPosition());
        
        // Check if we have enough wire
        if (wireLength > remainingWireLength) {
            return null;
        }
        
        // Create connection
        Connection connection = new Connection(sourcePort, targetPort);
        connections.add(connection);
        
        // Set the gameState reference in the connection
        connection.setGameState(gameState);
        
        // Deduct wire length
        remainingWireLength -= connection.getLength();
        totalWireUsed += connection.getLength();
        
        // Trigger stored packet processing in both connected systems
        NetworkSystem sourceSystem = sourcePort.getSystem();
        NetworkSystem targetSystem = targetPort.getSystem();
        
        // Process stored packets in source system due to new connection
        if (sourceSystem != null) {
            sourceSystem.processStoredPacketsAfterNetworkChange();
        }
        
        // For completeness, also trigger target system
        if (targetSystem != null) {
            targetSystem.processStoredPacketsAfterNetworkChange();
        }
        
        // Check for wire collisions with systems after creating connection
        if (gameState.getWireCollisionManager() != null) {
            gameState.getWireCollisionManager().checkWireCollision(connection);
        }
        
        return connection;
    }
    
    /**
     * Remove a connection
     * @param connection The connection to remove
     */
    public void removeConnection(Connection connection) {
        if (connections.contains(connection)) {
            // Store systems for update
            NetworkSystem sourceSystem = connection.getSourcePort().getSystem();
            NetworkSystem targetSystem = connection.getTargetPort().getSystem();
            
            // Add back wire length
            remainingWireLength += connection.getLength();
            
            // Remove packets on this connection
            if (gameState.getPacketManager() != null) {
                List<Packet> activePackets = gameState.getPacketManager().getActivePackets();
                activePackets.removeAll(connection.getPackets());
            }
            
            // Disconnect ports
            connection.disconnect();
            
            // Remove connection
            connections.remove(connection);
            
            // Update indicator lamps after connection is removed
            sourceSystem.updateIndicatorLamp();
            targetSystem.updateIndicatorLamp();
            
            // Trigger stored packet processing due to network topology change
            if (sourceSystem != null) {
                sourceSystem.processStoredPacketsAfterNetworkChange();
            }
        }
    }
    
    /**
     * Reset all connection styling to fix any visual glitches with wire glow effects
     */
    public void resetAllConnectionStyling() {
        for (Connection connection : connections) {
            // Only deactivate styling if there are no packets on the connection
            if (connection.isEmpty()) {
                connection.clearAllStyling();
            }
        }
    }
    
    /**
     * Reset the connection manager
     */
    public void reset() {
        this.connections.clear();
        this.remainingWireLength = 1000;
        this.totalWireUsed = 0;
    }
    
    /**
     * Update the remaining wire length after recalculating
     */
    public void updateRemainingWireLength() {
        // Recalculate total wire length used
        double total = 0;
        for (Connection connection : connections) {
            total += connection.getLength();
        }
        setTotalWireUsed(total);
    }
    
    /**
     * Get the list of connections
     * @return List of connections
     */
    public List<Connection> getConnections() {
        return connections;
    }
    
    /**
     * Get the remaining wire length
     * @return Remaining wire length
     */
    public double getRemainingWireLength() {
        return remainingWireLength;
    }
    
    /**
     * Get the total wire used
     * @return Total wire used
     */
    public double getTotalWireUsed() {
        return totalWireUsed;
    }
    
    /**
     * Set the total wire used
     * @param totalWireUsed Total wire used
     */
    public void setTotalWireUsed(double totalWireUsed) {
        this.totalWireUsed = totalWireUsed;
        this.remainingWireLength = Math.max(0, 1000 - totalWireUsed);
    }
    
    /**
     * Set the wire length
     * @param wireLength Wire length
     */
    public void setWireLength(double wireLength) {
        this.remainingWireLength = wireLength;
        this.totalWireUsed = 0;
    }
} 

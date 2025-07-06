package com.networkgame.model.level;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.HexagonPort;
import com.networkgame.model.entity.SquarePort;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.entity.Port;
import com.networkgame.model.state.GameState;

/**
 * HexagonTestLevel - A test level specifically designed to test hexagon packets
 * and verify that the coin HUD updates correctly when they reach the end system.
 */
public class HexagonTestLevel {
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create the hexagon test level
        LevelManager.Level level = new LevelManager.Level(3, "Hexagon Test Level");
        level.setAtarEnabled(true); // Disable impact wave effects for cleaner testing
        
        // Create a simple source-to-destination setup
        NetworkSystem sourceSystem = createSystem(gameState, 150, 100, true, "Hexagon Source", true, false);
        NetworkSystem destinationSystem = createSystem(gameState, 550, 100, true, "Hexagon Destination", false, true);
        
        // Configure source system with hexagon output ports
        setupSourceSystem(sourceSystem);
        
        // Configure destination system with hexagon input ports
        setupDestinationSystem(destinationSystem);
        
        // Add systems to level
        level.addSystem(sourceSystem);
        level.addSystem(destinationSystem);
        
        // Configure level parameters for testing
        level.setWireLength(1000);
        level.setPacketSpawnInterval(2.0); // Spawn packets every 2 seconds for easy observation
        level.setLevelDuration(60); // 1 minute test duration
        
        return level;
    }
    
    private static NetworkSystem createSystem(GameState gameState, int x, int y, boolean active, 
                                             String label, boolean isStart, boolean isEnd) {
        // Create as a real system (not reference), the 'active' parameter should control initial activity
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(active);
        
        if (isStart) {
            system.setStartSystem(true);
        }
        
        if (isEnd) {
            system.setEndSystem(true);
        }
        
        return system;
    }
    
    private static void setupSourceSystem(NetworkSystem system) {
        // Create mixed output ports for testing compatibility effects
        addOutputPort(system, PortType.HEXAGON, 2);  // Compatible ports
        addOutputPort(system, PortType.SQUARE, 1);   // Incompatible port for speed testing
    }
    
    private static void setupDestinationSystem(NetworkSystem system) {
        // Create mixed input ports to receive the packets  
        addInputPort(system, PortType.HEXAGON, 2);   // Compatible ports
        addInputPort(system, PortType.SQUARE, 1);    // Incompatible port for speed testing
    }
    
    private static void addInputPort(NetworkSystem system, PortType portType, int count) {
        for (int i = 0; i < count; i++) {
            Port port = createPort(portType, true, system);
            system.addInputPort(port);
        }
    }
    
    private static void addOutputPort(NetworkSystem system, PortType portType, int count) {
        for (int i = 0; i < count; i++) {
            Port port = createPort(portType, false, system);
            system.addOutputPort(port);
        }
    }
    
    private static Port createPort(PortType portType, boolean isInput, NetworkSystem system) {
        switch (portType) {
            case HEXAGON:
                return new HexagonPort(null, isInput, system);
            case SQUARE:
                return new SquarePort(null, isInput, system);
            default:
                throw new IllegalArgumentException("Unsupported port type: " + portType);
        }
    }
    
    private enum PortType {
        HEXAGON, SQUARE
    }
} 
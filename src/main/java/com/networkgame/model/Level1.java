package com.networkgame.model;

import javafx.geometry.Point2D;

public class Level1 {
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create level 1 - Network Storage Demonstration
        LevelManager.Level level = new LevelManager.Level(1, "Network Storage Challenge");
        level.setAtarEnabled(true); // Disables impact wave effects
        
        // Create the three main systems
        NetworkSystem sourceSystem = createSystem(gameState, 100, 200, true, "Source", true, false);
        NetworkSystem storageSystem = createSystem(gameState, 350, 200, false, "Storage System", false, false);
        NetworkSystem destinationSystem = createSystem(gameState, 600, 200, true, "Destination", false, true);
        
        // Configure port structure for source system (2 square, 1 triangle output)
        setupSourceSystem(sourceSystem);
        
        // Configure port structure for storage system (bottleneck)
        setupStorageSystem(storageSystem);
        
        // Configure port structure for destination system
        setupDestinationSystem(destinationSystem);
        
        // Add systems to level
        level.addSystem(sourceSystem);
        level.addSystem(storageSystem);
        level.addSystem(destinationSystem);
        
        // Configure level parameters
        level.setWireLength(1500);
        level.setPacketSpawnInterval(1.0);
        
        return level;
    }
    
    private static NetworkSystem createSystem(GameState gameState, int x, int y, boolean active, 
                                             String label, boolean isStart, boolean isEnd) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, active, gameState);
        system.setLabel(label);
        
        if (isStart) {
            system.setStartSystem(true);
        }
        
        if (isEnd) {
            system.setEndSystem(true);
        }
        
        return system;
    }
    
    private static void setupSourceSystem(NetworkSystem system) {
        // Create square and triangle output ports
        addOutputPort(system, PortType.SQUARE, 2);
        addOutputPort(system, PortType.TRIANGLE, 1);
    }
    
    private static void setupStorageSystem(NetworkSystem system) {
        // Add input ports (2 square, 1 triangle)
        addInputPort(system, PortType.SQUARE, 2);
        addInputPort(system, PortType.TRIANGLE, 1);
        
        // Add output ports (1 square, 1 triangle)
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.TRIANGLE, 1);
    }
    
    private static void setupDestinationSystem(NetworkSystem system) {
        // Add input ports (1 square, 1 triangle)
        addInputPort(system, PortType.SQUARE, 1);
        addInputPort(system, PortType.TRIANGLE, 1);
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
            case SQUARE:
                return new SquarePort(null, isInput, system);
            case TRIANGLE:
                return new TrianglePort(null, isInput, system);
            default:
                throw new IllegalArgumentException("Unsupported port type: " + portType);
        }
    }
    
    private enum PortType {
        SQUARE, TRIANGLE
    }
} 
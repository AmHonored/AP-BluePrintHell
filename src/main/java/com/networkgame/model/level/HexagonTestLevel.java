package com.networkgame.model.level;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.SquarePort;
import com.networkgame.model.entity.TrianglePort;
import com.networkgame.model.entity.HexagonPort;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;

/**
 * DDoS System Test Level
 * Layout: START -> DDoS -> END
 * 
 * All systems have 3 port types (Square, Triangle, Hexagon)
 * Start system generates all 3 packet types
 * DDoS system sends packets to incompatible ports (malicious behavior)
 * End system receives all packet types
 */
public class HexagonTestLevel {
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create DDoS system test level
        LevelManager.Level level = new LevelManager.Level(3, "DDoS System Test");
        level.setAtarEnabled(false);
        
        // Simple linear layout: Start -> DDoS -> End
        NetworkSystem startSystem = createStartSystem(gameState, 150, 200, "START");
        NetworkSystem ddosSystem = createDdosSystem(gameState, 400, 200, "DDoS");
        NetworkSystem endSystem = createEndSystem(gameState, 650, 200, "END");
        
        // Setup all systems with 3 port types each
        setupStartSystem(startSystem);     // 3 output ports (Square, Triangle, Hexagon)
        setupDdosSystem(ddosSystem);       // 3 input, 3 output ports
        setupEndSystem(endSystem);         // 3 input ports (Square, Triangle, Hexagon)
        
        // Add all systems to level
        level.addSystem(startSystem);
        level.addSystem(ddosSystem);
        level.addSystem(endSystem);
        
        // Configure level parameters
        level.setWireLength(2000);
        level.setPacketSpawnInterval(2.0);      // 2 seconds between packets
        level.setLevelDuration(120);            // 2 minutes
        
        System.out.println("DDoS System Test Level Created:");
        System.out.println("  Layout: START(3 ports) → DDoS(3 in, 3 out) → END(3 ports)");
        System.out.println("  Packet Types: Square, Triangle, Hexagon");
        System.out.println("  Test: DDoS system sends packets to incompatible ports!");
        System.out.println("  Connect all matching ports: Square→Square, Triangle→Triangle, Hexagon→Hexagon");
        
        return level;
    }
    
    private static NetworkSystem createStartSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setStartSystem(true);
        return system;
    }
    
    private static NetworkSystem createDdosSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(false);
        system.setDdosSystem(true);  // Set as DDoS system
        return system;
    }
    
    private static NetworkSystem createEndSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(false);
        system.setEndSystem(true);
        return system;
    }
    
    private static void setupStartSystem(NetworkSystem system) {
        // 3 output ports - one for each packet type
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.TRIANGLE, 1);
        addOutputPort(system, PortType.HEXAGON, 1);
    }
    
    private static void setupDdosSystem(NetworkSystem system) {
        // 3 input ports (one for each packet type)
        addInputPort(system, PortType.SQUARE, 1);
        addInputPort(system, PortType.TRIANGLE, 1);
        addInputPort(system, PortType.HEXAGON, 1);
        
        // 3 output ports (one for each packet type)
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.TRIANGLE, 1);
        addOutputPort(system, PortType.HEXAGON, 1);
    }
    
    private static void setupEndSystem(NetworkSystem system) {
        // 3 input ports (one for each packet type)
        addInputPort(system, PortType.SQUARE, 1);
        addInputPort(system, PortType.TRIANGLE, 1);
        addInputPort(system, PortType.HEXAGON, 1);
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
            case HEXAGON:
                return new HexagonPort(null, isInput, system);
            default:
                throw new IllegalArgumentException("Unsupported port type: " + portType);
        }
    }
    
    private enum PortType {
        SQUARE, TRIANGLE, HEXAGON
    }
} 
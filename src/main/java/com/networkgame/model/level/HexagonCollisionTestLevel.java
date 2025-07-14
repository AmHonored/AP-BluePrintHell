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
 * Hexagon Collision Test Level - Tests collision detection and backward movement
 * 
 * Layout: START -> INTERMEDIATE -> END
 * 
 * START system generates both Hexagon and Square packets
 * INTERMEDIATE system forwards packets but causes potential bottleneck
 * END system receives all packet types
 * 
 * Test: Hexagon packets should collide with other packets and move backward
 * to the START system, then retry reaching the END system
 */
public class HexagonCollisionTestLevel {
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create Hexagon collision test level
        LevelManager.Level level = new LevelManager.Level(7, "Hexagon Collision Test");
        level.setAtarEnabled(false); // Enable collision effects
        
        // Simple linear layout: Start -> Intermediate -> End
        NetworkSystem startSystem = createStartSystem(gameState, 120, 200, "START");
        NetworkSystem intermediateSystem = createIntermediateSystem(gameState, 400, 200, "INTERMEDIATE");
        NetworkSystem endSystem = createEndSystem(gameState, 680, 200, "END");
        
        // Setup systems with appropriate ports
        setupStartSystem(startSystem);         // Generates Hexagon and Square packets
        setupIntermediateSystem(intermediateSystem); // Forwards packets, potential bottleneck
        setupEndSystem(endSystem);             // Receives all packet types
        
        // Add all systems to level
        level.addSystem(startSystem);
        level.addSystem(intermediateSystem);
        level.addSystem(endSystem);
        
        // Configure level parameters for collision testing
        level.setWireLength(1800);                   // Medium wire length for clear observation
        level.setPacketSpawnInterval(1.2);           // Moderate generation rate to create collisions
        level.setLevelDuration(180);                 // 3 minutes for thorough testing
        
        // Log test setup instructions
        System.out.println("Hexagon Collision Test Level Created:");
        System.out.println("  Layout: START(H+S) → INTERMEDIATE(H+S) → END(H+S)");
        System.out.println("  ");
        System.out.println("  REQUIRED CONNECTIONS:");
        System.out.println("  1. START Hexagon → INTERMEDIATE Hexagon");
        System.out.println("  2. START Square → INTERMEDIATE Square");
        System.out.println("  3. INTERMEDIATE Hexagon → END Hexagon");
        System.out.println("  4. INTERMEDIATE Square → END Square");
        System.out.println("  ");
        System.out.println("  PACKET GENERATION:");
        System.out.println("  - START generates Hexagon packets (every 1.5s)");
        System.out.println("  - START generates Square packets (every 0.8s) - collision triggers");
        System.out.println("  - INTERMEDIATE has small storage capacity to create bottleneck");
        System.out.println("  ");
        System.out.println("  EXPECTED BEHAVIOR:");
        System.out.println("  - Hexagon packets should collide with faster Square packets");
        System.out.println("  - Upon collision, Hexagon packet moves backward to START system");
        System.out.println("  - Hexagon packet waits briefly at START, then retries");
        System.out.println("  - Multiple collision attempts possible (max 3 retries)");
        System.out.println("  - Fixed storage bug: packets should flow through INTERMEDIATE, not get stuck");
        
        return level;
    }
    
    private static NetworkSystem createStartSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setStartSystem(true);
        return system;
    }
    
    private static NetworkSystem createIntermediateSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        // Set small storage capacity to potentially create bottleneck
        system.getPacketManager().setStorageCapacity(2);
        return system;
    }
    
    private static NetworkSystem createEndSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setEndSystem(true);
        return system;
    }
    
    // Setup START system - generates Hexagon and Square packets
    private static void setupStartSystem(NetworkSystem system) {
        addOutputPort(system, PortType.HEXAGON, 1);  // Hexagon packets - slower generation
        addOutputPort(system, PortType.SQUARE, 1);   // Square packets - faster generation (collision triggers)
    }
    
    // Setup INTERMEDIATE system - forwards both packet types
    private static void setupIntermediateSystem(NetworkSystem system) {
        // Input ports
        addInputPort(system, PortType.HEXAGON, 1);
        addInputPort(system, PortType.SQUARE, 1);
        
        // Output ports (switched positions)
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.HEXAGON, 1);
    }
    
    // Setup END system - receives both packet types
    private static void setupEndSystem(NetworkSystem system) {
        addInputPort(system, PortType.HEXAGON, 1);
        addInputPort(system, PortType.SQUARE, 1);
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
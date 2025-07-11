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
 * Pentagon Packet Collision Avoidance Test Level
 * 
 * Layout: FAST_GEN -> BOTTLENECK -> END
 *         PENTAGON_GEN -> BOTTLENECK
 * 
 * FAST_GEN generates Square/Triangle packets rapidly to keep BOTTLENECK system busy
 * PENTAGON_GEN generates Pentagon packets that should slow down when approaching busy BOTTLENECK
 * BOTTLENECK system has limited capacity and single output to create congestion
 * END system receives all processed packets
 * 
 * Test: Pentagon packets should intelligently slow down based on BOTTLENECK system's 
 * wire configuration and stored packet count, timing their arrival for when system is clear
 */
public class PentagonTestLevel {
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create Pentagon collision avoidance test level
        LevelManager.Level level = new LevelManager.Level(5, "Pentagon Collision Avoidance Test");
        level.setAtarEnabled(true); // Disable impact waves for cleaner testing
        
        // Layout positions - arranged to clearly show the bottleneck effect
        NetworkSystem fastGenSystem = createStartSystem(gameState, 80, 150, "FAST_GEN");
        NetworkSystem pentagonGenSystem = createStartSystem(gameState, 80, 250, "PENTAGON_GEN");
        NetworkSystem bottleneckSystem = createBottleneckSystem(gameState, 350, 200, "BOTTLENECK");
        NetworkSystem endSystem = createEndSystem(gameState, 600, 200, "END");
        
        // Setup systems with appropriate ports
        setupFastGenSystem(fastGenSystem);           // Generates Square and Triangle packets rapidly
        setupPentagonGenSystem(pentagonGenSystem);   // Generates Pentagon packets slowly
        setupBottleneckSystem(bottleneckSystem);     // Limited capacity, single output (bottleneck)
        setupEndSystem(endSystem);                   // Accepts all packet types
        
        // Add all systems to level
        level.addSystem(fastGenSystem);
        level.addSystem(pentagonGenSystem);
        level.addSystem(bottleneckSystem);
        level.addSystem(endSystem);
        
        // Configure level parameters for optimal testing
        level.setWireLength(1500);                   // Moderate wire length for clear observation
        level.setPacketSpawnInterval(0.1);           // Extremely fast Square/Triangle generation to force storage
        level.setLevelDuration(120);                 // 2 minutes for thorough testing
        
        // Log test setup instructions
        System.out.println("Pentagon Collision Avoidance Test Level Created:");
        System.out.println("  Layout: FAST_GEN(Square+Triangle) → BOTTLENECK(3 in, 2 out) → END");
        System.out.println("          PENTAGON_GEN(Pentagon) → BOTTLENECK");
        System.out.println("  ");
        System.out.println("  REQUIRED CONNECTIONS:");
        System.out.println("  1. FAST_GEN Square → BOTTLENECK Square");
        System.out.println("  2. FAST_GEN Triangle → BOTTLENECK Triangle");
        System.out.println("  3. PENTAGON_GEN Pentagon → BOTTLENECK Pentagon");
        System.out.println("  4. BOTTLENECK Square → END Square");
        System.out.println("  5. BOTTLENECK Pentagon → END Pentagon");
        System.out.println("  (Note: No Triangle output from BOTTLENECK - creates congestion!)");
        System.out.println("  ");
        System.out.println("  EXPECTED BEHAVIOR:");
        System.out.println("  - Square/Triangle packets (every 0.15s) rapidly fill up BOTTLENECK");
        System.out.println("  - Triangle packets get stuck (no output), forcing storage");
        System.out.println("  - Pentagon packets slow down ONLY when BOTTLENECK has stored packets AND busy outputs");
        System.out.println("  - Pentagon packets flow immediately when BOTTLENECK is clear");
        System.out.println("  - Pentagon speed varies: 15.0-45.0 (stored+busy) vs 85.0 (clear)");
        
        return level;
    }
    
    private static NetworkSystem createStartSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setStartSystem(true);
        return system;
    }
    
    private static NetworkSystem createBottleneckSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        // Set reasonable storage capacity to handle triangle packets that can't exit
        system.getPacketManager().setStorageCapacity(3); // Allow some triangle packets to accumulate
        return system;
    }
    
    private static NetworkSystem createEndSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setEndSystem(true);
        return system;
    }
    
    // Setup FAST_GEN system - generates Square and Triangle packets rapidly
    private static void setupFastGenSystem(NetworkSystem system) {
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.TRIANGLE, 1);
    }
    
    // Setup PENTAGON_GEN system - generates Pentagon packets at normal rate  
    // Pentagon packets are compatible with any port, so we'll use a Square port
    private static void setupPentagonGenSystem(NetworkSystem system) {
        addOutputPort(system, PortType.SQUARE, 1);
    }
    
    // Setup BOTTLENECK system - accepts all types but limited outputs create congestion
    private static void setupBottleneckSystem(NetworkSystem system) {
        // Input ports for packet types (Pentagon packets can use Square ports)
        addInputPort(system, PortType.SQUARE, 2);       // Square + Pentagon packets
        addInputPort(system, PortType.TRIANGLE, 1);     // Triangle packets
        
        // LIMITED Output ports - missing Triangle output creates bottleneck!
        addOutputPort(system, PortType.SQUARE, 2);      // Square + Pentagon packets can exit
        // No Triangle output - Triangle packets get stuck, creating congestion
    }
    
    // Setup END system - accepts only types that can exit bottleneck
    private static void setupEndSystem(NetworkSystem system) {
        addInputPort(system, PortType.SQUARE, 2);       // Square + Pentagon packets
        // No Triangle input - matches bottleneck's output limitation
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
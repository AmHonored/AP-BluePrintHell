package com.networkgame.model.level;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.SquarePort;
import com.networkgame.model.entity.TrianglePort;
import com.networkgame.model.entity.HexagonPort;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.system.VpnSystem;
import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;

/**
 * VPN System Test Level
 * Layout: START -> VPN -> INTERMEDIATE -> END
 *         HIGH_SPEED_START -> VPN (to trigger failure)
 * 
 * START system generates normal Triangle packets (speed ~50)
 * HIGH_SPEED_START generates high-speed Triangle packets (speed 120) to trigger VPN failure
 * VPN system has 3 port types (Triangle input, Square/Triangle/Hexagon outputs)
 * INTERMEDIATE system has all 3 port types for input and output
 * END system has all 3 port types for input
 * 
 * Test: Normal Triangle packets become ProtectedPackets with random movement behavior,
 * but when high-speed packet hits VPN, all ProtectedPackets revert to original types
 */
public class VpnTestLevel {
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create VPN system test level
        LevelManager.Level level = new LevelManager.Level(4, "VPN System Test");
        level.setAtarEnabled(true); // Disable impact waves for cleaner testing
        
        // Layout: Start -> VPN -> Intermediate -> End
        //         HighSpeedStart -> VPN (to trigger failure)
        NetworkSystem startSystem = createStartSystem(gameState, 80, 160, "START");
        NetworkSystem highSpeedStartSystem = createHighSpeedStartSystem(gameState, 80, 240, "HIGH_SPEED");
        NetworkSystem vpnSystem = createVpnSystem(gameState, 280, 200, "VPN");
        NetworkSystem intermediateSystem = createIntermediateSystem(gameState, 480, 200, "INTERMEDIATE");
        NetworkSystem endSystem = createEndSystem(gameState, 680, 200, "END");
        
        // Setup all systems with appropriate ports
        setupStartSystem(startSystem);          // 1 Triangle output port only
        setupHighSpeedStartSystem(highSpeedStartSystem); // 1 Triangle output port for high-speed packets
        setupVpnSystem(vpnSystem);              // 2 Triangle inputs, 3 outputs (S,T,H)
        setupIntermediateSystem(intermediateSystem); // 3 inputs, 3 outputs (S,T,H each)
        setupEndSystem(endSystem);              // 3 input ports (S,T,H)
        
        // Add all systems to level
        level.addSystem(startSystem);
        level.addSystem(highSpeedStartSystem);
        level.addSystem(vpnSystem);
        level.addSystem(intermediateSystem);
        level.addSystem(endSystem);
        
        // Configure level parameters
        level.setWireLength(2000);
        level.setPacketSpawnInterval(3.0);      // 3 seconds between packets for easier observation
        level.setLevelDuration(180);            // 3 minutes to observe behavior
        
        System.out.println("VPN System Test Level Created:");
        System.out.println("  Layout: START(Triangle) → VPN(2T in, S+T+H out) → INTERMEDIATE(S+T+H in/out) → END(S+T+H)");
        System.out.println("          HIGH_SPEED(Triangle) → VPN (triggers failure)");
        System.out.println("  Test: Normal Triangle packets become ProtectedPackets with random movement behavior");
        System.out.println("  When high-speed packet (speed 120) hits VPN, all ProtectedPackets revert to original types");
        System.out.println("  Connect: START Triangle → VPN Triangle Input #1");
        System.out.println("  Connect: HIGH_SPEED Triangle → VPN Triangle Input #2");
        System.out.println("  Connect: VPN Square → INTERMEDIATE Square");
        System.out.println("  Connect: VPN Triangle → INTERMEDIATE Triangle");
        System.out.println("  Connect: VPN Hexagon → INTERMEDIATE Hexagon");
        System.out.println("  Connect: INTERMEDIATE outputs → END inputs (matching types)");
        
        return level;
    }
    
    private static NetworkSystem createStartSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setStartSystem(true);
        return system;
    }
    
    private static NetworkSystem createHighSpeedStartSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setStartSystem(true);
        return system;
    }
    
    private static NetworkSystem createVpnSystem(GameState gameState, int x, int y, String label) {
        // Create a NetworkSystem and configure it as a VPN system
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setVpnSystem(true);  // This will recreate it as a VPN system
        return system;
    }
    
    private static NetworkSystem createIntermediateSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        return system;
    }
    
    private static NetworkSystem createEndSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setEndSystem(true);
        return system;
    }
    
    // Setup START system - only generates Triangle packets
    private static void setupStartSystem(NetworkSystem system) {
        addOutputPort(system, PortType.TRIANGLE, 1);
    }
    
    // Setup HIGH_SPEED_START system - generates high-speed Triangle packets
    private static void setupHighSpeedStartSystem(NetworkSystem system) {
        addOutputPort(system, PortType.TRIANGLE, 1);
    }
    
    // Setup VPN system - accepts 2 Triangle inputs, outputs to all 3 types
    private static void setupVpnSystem(NetworkSystem system) {
        addInputPort(system, PortType.TRIANGLE, 2); // 2 triangle inputs (normal + high-speed)
        addOutputPort(system, PortType.TRIANGLE, 1);
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.HEXAGON, 1);
    }
    
    // Setup INTERMEDIATE system - accepts all 3 types, outputs all 3 types
    private static void setupIntermediateSystem(NetworkSystem system) {
        // Input ports
        addInputPort(system, PortType.SQUARE, 1);
        addInputPort(system, PortType.TRIANGLE, 1);
        addInputPort(system, PortType.HEXAGON, 1);
        
        // Output ports
        addOutputPort(system, PortType.SQUARE, 1);
        addOutputPort(system, PortType.TRIANGLE, 1);
        addOutputPort(system, PortType.HEXAGON, 1);
    }
    
    // Setup END system - accepts all 3 packet types
    private static void setupEndSystem(NetworkSystem system) {
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
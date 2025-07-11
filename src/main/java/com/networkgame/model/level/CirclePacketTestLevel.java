package com.networkgame.model.level;

import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.SquarePort;
import com.networkgame.model.entity.TrianglePort;
import com.networkgame.model.entity.packettype.secret.CirclePacket;
import com.networkgame.model.entity.packettype.secret.PentagonPacket;
import com.networkgame.model.entity.packettype.messenger.SquarePacket;
import com.networkgame.model.entity.packettype.messenger.TrianglePacket;
import com.networkgame.model.state.GameState;
import com.networkgame.model.manager.LevelManager;
import javafx.geometry.Point2D;
import javafx.scene.Scene;

import java.util.Arrays;
import java.util.List;

/**
 * CirclePacketTestLevel - A specialized test level for testing Pentagon -> Circle packet transformation:
 * - Direct connections for Triangle and Square packets
 * - Pentagon packets go through VPN system and transform to Circle packets
 * - Tests Pentagon transformation and Circle packet preservation behavior
 * - Secret packets (Pentagon/Circle) use existing ports (Square, Triangle, Hexagon)
 * 
 * Layout: START(S+S+T) -> VPN(S) -> END(S+T)
 *         START(S) -----> END(S) (direct)
 *         START(T) -----> END(T) (direct)
 *         START(S) -> VPN(S) -> END(S) (Pentagon->Circle transformation)
 * 
 * Generation:
 * - Triangle packets: frequent -> direct to END
 * - Square packets: frequent -> direct to END
 * - Pentagon packets: less frequent -> VPN -> CirclePackets -> END (using Square ports)
 */
public class CirclePacketTestLevel {
    
    // Port type enum for this level
    public enum PortType {
        SQUARE(Packet.PacketType.SQUARE),
        TRIANGLE(Packet.PacketType.TRIANGLE),
        PENTAGON(Packet.PacketType.PENTAGON);
        
        private final Packet.PacketType packetType;
        
        PortType(Packet.PacketType packetType) {
            this.packetType = packetType;
        }
        
        public Packet.PacketType getPacketType() {
            return packetType;
        }
    }
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create CirclePacket test level
        LevelManager.Level level = new LevelManager.Level(6, "Pentagon->Circle Transformation Test");
        level.setAtarEnabled(true); // Disable impact waves for cleaner testing
        
        // Create 3 systems: START -> VPN (above) -> END
        NetworkSystem startSystem = createStartSystem(gameState, 50, 200, "START");
        NetworkSystem vpnSystem = createVpnSystem(gameState, 250, 50, "VPN"); // Above to avoid wire collisions
        NetworkSystem endSystem = createEndSystem(gameState, 600, 200, "END");
        
        // Setup system configurations
        setupStartSystem(startSystem);
        setupVpnSystem(vpnSystem);
        setupEndSystem(endSystem);
        
        // Add all systems to level
        level.addSystem(startSystem);
        level.addSystem(vpnSystem);
        level.addSystem(endSystem);
        
        // Configure level parameters for optimal testing
        level.setWireLength(2000);                    // Appropriate wire length for system spacing
        level.setPacketSpawnInterval(0.3);           // Fast generation for testing
        level.setLevelDuration(240);                 // 4 minutes for thorough testing
        
        System.out.println("Pentagon->Circle Transformation Test Level Created:");
        System.out.println("  Layout: START(S+S+T) -> VPN(S) -> END(S+T)");
        System.out.println("  VPN positioned above to avoid wire collisions");
        System.out.println("  Secret packets use existing ports (Square/Triangle/Hexagon)");
        System.out.println("");
        System.out.println("  Port Layout:");
        System.out.println("    START: Square + Square + Triangle");
        System.out.println("    VPN:   Square (input/output)");
        System.out.println("    END:   Square + Triangle");
        System.out.println("");
        System.out.println("  Generation:");
        System.out.println("    - Triangle packets: frequent -> direct to END");
        System.out.println("    - Square packets: frequent -> direct to END");  
        System.out.println("    - Pentagon packets: less frequent -> VPN transformation -> Circle packets");
        System.out.println("");
        System.out.println("  REQUIRED CONNECTIONS:");
        System.out.println("  1. START Square #1 -> END Square (direct)");
        System.out.println("  2. START Square #2 -> VPN Square -> END Square (Pentagon->Circle path)");
        System.out.println("  3. START Triangle -> END Triangle (direct)");
        System.out.println("");
        System.out.println("  EXPECTED BEHAVIOR:");
        System.out.println("  - Triangle and Square packets flow directly to END");
        System.out.println("  - Pentagon packets transform to Circle packets in VPN (using Square ports)");
        System.out.println("  - Circle packets maintain distance from other moving packets");
        System.out.println("  - Circle packets move backward when other packets are too close");
        System.out.println("  - VPN system is undraggable for security");
        
        return level;
    }
    
    private static NetworkSystem createStartSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setStartSystem(true);
        return system;
    }
    
    private static NetworkSystem createVpnSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setVpnSystem(true); // This makes it a VPN system
        return system;
    }
    
    private static NetworkSystem createEndSystem(GameState gameState, int x, int y, String label) {
        NetworkSystem system = new NetworkSystem(new Point2D(x, y), 80, 100, false, gameState);
        system.setLabel(label);
        system.setActive(true);
        system.setEndSystem(true);
        return system;
    }
    
    // Setup START system - generates Square, Square, Triangle packets (Pentagon packets use Square ports)
    private static void setupStartSystem(NetworkSystem system) {
        addOutputPort(system, PortType.SQUARE, 2);    // 2 Square ports (one direct, one for Pentagon->VPN)
        addOutputPort(system, PortType.TRIANGLE, 1);  // Triangle packets -> direct to END
    }
    
    // Setup VPN system - accepts packets via Square port, outputs Circle packets via Square port
    private static void setupVpnSystem(NetworkSystem system) {
        // Input: 1 Square port for Pentagon packets (Pentagon packets use Square ports)
        addInputPort(system, PortType.SQUARE, 1);
        
        // Output: 1 Square port for Circle packets (Circle packets use Square ports)
        addOutputPort(system, PortType.SQUARE, 1);
        
        // Small capacity for VPN processing
        system.getPacketManager().setStorageCapacity(2);
    }
    
    // Setup END system - accepts Square and Triangle packets (Circle packets use Square ports)
    private static void setupEndSystem(NetworkSystem system) {
        addInputPort(system, PortType.SQUARE, 2);     // From START Square (direct) + From VPN (Circle packets)
        addInputPort(system, PortType.TRIANGLE, 1);   // From START Triangle (direct)
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
            case PENTAGON:
                // Pentagon packets use Square ports (they're compatible with any port)
                return new SquarePort(null, isInput, system);
            default:
                throw new IllegalArgumentException("Unsupported port type: " + portType);
        }
    }
} 
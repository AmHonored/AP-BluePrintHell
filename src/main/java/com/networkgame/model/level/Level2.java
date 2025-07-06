package com.networkgame.model.level; 

import javafx.geometry.Point2D;
import java.util.Arrays;
import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.SquarePort;
import com.networkgame.model.entity.TrianglePort;


public class Level2 {
    
    private static final double ROUTER_WIDTH = 80.0;
    private static final double ROUTER_HEIGHT = 100.0;
    
    public static LevelManager.Level createLevel(GameState gameState) {
        // Create level 2 - Complex Network Challenge with Focus on Collisions
        LevelManager.Level level2 = new LevelManager.Level(2, "Network Collision Challenge");
        
        // Disable atar (impact wave prevention) to enable impact waves
        level2.setAtarEnabled(false);
        
        // DEBUGGING: Add log for level creation
        System.out.println("DEBUG: Creating Level 2 with collision focus");
        
        // Create systems
        NetworkSystem[] systems = createNetworkSystems(gameState);
        NetworkSystem startSystem = systems[0];
        NetworkSystem routerTopLeft = systems[1];
        NetworkSystem routerBottomLeft = systems[2];
        NetworkSystem routerTopRight = systems[3];
        NetworkSystem routerBottomRight = systems[4];
        NetworkSystem endSystem = systems[5];
        
        // Log system positions
        logSystemPositions(startSystem, endSystem, routerTopLeft, routerBottomLeft, routerTopRight, routerBottomRight);
        
        // Configure ports for all systems
        configureStartSystemPorts(startSystem);
        configureRouterPorts(routerTopLeft);
        configureRouterPorts(routerBottomLeft);
        configureRouterPorts(routerTopRight);
        configureRouterPorts(routerBottomRight);
        configureEndSystemPorts(endSystem);
        
        // Log port configuration
        System.out.println("DEBUG: Level 2 port configuration complete");
        System.out.println("DEBUG: Start system has " + startSystem.getOutputPorts().size() + " output ports");
        System.out.println("DEBUG: End system has " + endSystem.getInputPorts().size() + " input ports");
        
        // Add all systems to the level
        Arrays.stream(systems).forEach(level2::addSystem);
        
        // Force consistent dimensions for router systems
        standardizeRouterDimensions(level2);
        
        // Configure level parameters
        configureLevelParameters(level2);
        
        // Log final setup
        logLevelSetup(level2);
        
        return level2;
    }
    
    private static NetworkSystem[] createNetworkSystems(GameState gameState) {
        NetworkSystem startSystem = new NetworkSystem(new Point2D(100, 150), ROUTER_WIDTH, ROUTER_HEIGHT, false, gameState);
        NetworkSystem routerTopLeft = new NetworkSystem(new Point2D(250, 80), ROUTER_WIDTH, ROUTER_HEIGHT, false, gameState);
        NetworkSystem routerBottomLeft = new NetworkSystem(new Point2D(250, 220), ROUTER_WIDTH, ROUTER_HEIGHT, false, gameState);
        NetworkSystem routerTopRight = new NetworkSystem(new Point2D(450, 80), ROUTER_WIDTH, ROUTER_HEIGHT, false, gameState);
        NetworkSystem routerBottomRight = new NetworkSystem(new Point2D(450, 220), ROUTER_WIDTH, ROUTER_HEIGHT, false, gameState);
        NetworkSystem endSystem = new NetworkSystem(new Point2D(600, 150), ROUTER_WIDTH, ROUTER_HEIGHT, false, gameState);
        
        // Designate start and end systems
        startSystem.setStartSystem(true);
        endSystem.setEndSystem(true);
        
        // Set clear labels
        startSystem.setLabel("START");
        routerTopLeft.setLabel("ROUTER TL");
        routerBottomLeft.setLabel("ROUTER BL");
        routerTopRight.setLabel("ROUTER TR");
        routerBottomRight.setLabel("ROUTER BR");
        endSystem.setLabel("END");
        
        return new NetworkSystem[] {
            startSystem, routerTopLeft, routerBottomLeft, routerTopRight, routerBottomRight, endSystem
        };
    }
    
    private static void logSystemPositions(NetworkSystem startSystem, NetworkSystem endSystem, 
                                          NetworkSystem routerTopLeft, NetworkSystem routerBottomLeft, 
                                          NetworkSystem routerTopRight, NetworkSystem routerBottomRight) {
        System.out.println("DEBUG: Level 2 system positions:");
        System.out.println("DEBUG: Start: " + startSystem.getPosition());
        System.out.println("DEBUG: End: " + endSystem.getPosition());
        System.out.println("DEBUG: Routers at: " + routerTopLeft.getPosition() + ", " + 
                           routerBottomLeft.getPosition() + ", " + 
                           routerTopRight.getPosition() + ", " + 
                           routerBottomRight.getPosition());
    }
    
    private static void configureStartSystemPorts(NetworkSystem startSystem) {
        // Create and add output ports
        addOutputPort(startSystem, PortType.SQUARE, 2);
        addOutputPort(startSystem, PortType.TRIANGLE, 2);
    }
    
    private static void configureRouterPorts(NetworkSystem router) {
        // Add both input and output ports of both types
        addInputPort(router, PortType.SQUARE, 1);
        addInputPort(router, PortType.TRIANGLE, 1);
        addOutputPort(router, PortType.SQUARE, 1);
        addOutputPort(router, PortType.TRIANGLE, 1);
    }
    
    private static void configureEndSystemPorts(NetworkSystem endSystem) {
        // Create and add input ports
        addInputPort(endSystem, PortType.SQUARE, 2);
        addInputPort(endSystem, PortType.TRIANGLE, 2);
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
        return portType == PortType.SQUARE ? 
               new SquarePort(null, isInput, system) : 
               new TrianglePort(null, isInput, system);
    }
    
    private static void standardizeRouterDimensions(LevelManager.Level level) {
        for (NetworkSystem system : level.getSystems()) {
            if (!system.isStartSystem() && !system.isEndSystem()) {
                system.getVisualManager().forceSystemDimensions(ROUTER_WIDTH, ROUTER_HEIGHT);
                System.out.println("Forcing dimensions for " + system.getLabel() + " to " + 
                                  ROUTER_WIDTH + "x" + ROUTER_HEIGHT);
            }
        }
    }
    
    private static void configureLevelParameters(LevelManager.Level level) {
        // Set wire length to a generous value for level 2
        level.setWireLength(2500); // Increased to allow complex crossings
        
        // Set packet spawn interval - fast enough to create frequent collisions
        level.setPacketSpawnInterval(0.6); // Faster than default to ensure packet density
        
        // Set the packet collision threshold high to make collisions more likely
        level.setCollisionThreshold(2.5); // Higher than default for more dramatic collisions
        
        // Set level duration to 90 seconds
        level.setLevelDuration(90);
    }
    
    private static void logLevelSetup(LevelManager.Level level) {
        System.out.println("DEBUG: Level 2 setup complete");
        System.out.println("DEBUG: Level duration: " + level.getLevelDuration() + " seconds");
        System.out.println("DEBUG: Wire length: " + level.getWireLength());
        System.out.println("DEBUG: Packet interval: " + level.getPacketSpawnInterval());
    }
    
    private enum PortType {
        SQUARE, TRIANGLE
    }
} 

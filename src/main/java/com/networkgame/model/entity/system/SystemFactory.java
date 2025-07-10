package com.networkgame.model.entity.system;

import javafx.geometry.Point2D;
import com.networkgame.model.state.GameState;

/**
 * Factory class for creating different types of network systems.
 * This class provides static methods to create StartSystem, EndSystem, and IntermediateSystem instances.
 */
public class SystemFactory {
    
    /**
     * Create a start system that generates packets
     */
    public static StartSystem createStartSystem(Point2D position, double width, double height, 
                                                boolean isReference, GameState gameState) {
        return new StartSystem(position, width, height, isReference, gameState);
    }
    
    /**
     * Create an end system that receives and processes packets
     */
    public static EndSystem createEndSystem(Point2D position, double width, double height, 
                                           boolean isReference, GameState gameState) {
        return new EndSystem(position, width, height, isReference, gameState);
    }
    
    /**
     * Create an intermediate system that stores and forwards packets
     */
    public static IntermediateSystem createIntermediateSystem(Point2D position, double width, double height, 
                                                              boolean isReference, GameState gameState) {
        return new IntermediateSystem(position, width, height, isReference, gameState);
    }
    
    /**
     * Create a spy system that can intercept and reroute packets
     */
    public static SpySystem createSpySystem(Point2D position, double width, double height, 
                                           boolean isReference, GameState gameState) {
        return new SpySystem(position, width, height, isReference, gameState);
    }
    
    /**
     * Create a DDoS system that maliciously routes packets to incompatible ports
     */
    public static DdosSystem createDdosSystem(Point2D position, double width, double height, 
                                             boolean isReference, GameState gameState) {
        return new DdosSystem(position, width, height, isReference, gameState);
    }
    
    /**
     * Create a VPN system that encrypts packets and provides security
     */
    public static VpnSystem createVpnSystem(Point2D position, double width, double height, 
                                           boolean isReference, GameState gameState) {
        return new VpnSystem(position, width, height, isReference, gameState);
    }
    
    /**
     * Create a system based on its type flags
     */
    public static BaseSystem createSystem(Point2D position, double width, double height, 
                                         boolean isReference, GameState gameState, 
                                         boolean isStartSystem, boolean isEndSystem) {
        if (isStartSystem) {
            return createStartSystem(position, width, height, isReference, gameState);
        } else if (isEndSystem) {
            return createEndSystem(position, width, height, isReference, gameState);
        } else {
            return createIntermediateSystem(position, width, height, isReference, gameState);
        }
    }
    
    /**
     * Create a system based on its label (for backward compatibility)
     */
    public static BaseSystem createSystemFromLabel(Point2D position, double width, double height, 
                                                   boolean isReference, GameState gameState, 
                                                   String label) {
        if (label != null) {
            String lowerLabel = label.toLowerCase();
            if (lowerLabel.contains("start") || lowerLabel.contains("source")) {
                return createStartSystem(position, width, height, isReference, gameState);
            } else if (lowerLabel.contains("end") || lowerLabel.contains("destination")) {
                return createEndSystem(position, width, height, isReference, gameState);
            }
        }
        return createIntermediateSystem(position, width, height, isReference, gameState);
    }
} 
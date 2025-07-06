package com.networkgame.service;

import com.networkgame.controller.GameController;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;

import java.util.ArrayList;

/**
 * Service responsible for cleaning up game resources.
 * Extracted from UIController to follow the Single Responsibility Principle.
 */
public class GameCleanupService {
    
    /**
     * Stops all running game operations.
     * 
     * @param controller The main game controller
     */
    public void stopAllGameOperations(GameController controller) {
        controller.stopGame();
        
        if (controller.getGameState() != null) {
            controller.getGameState().getSystems().stream()
                .filter(NetworkSystem::isStartSystem)
                .forEach(NetworkSystem::stopSendingPackets);
        }
        
        controller.getGameplayController().stopGameLoop();
    }
    
    /**
     * Performs thorough cleanup of game resources before switching scenes
     * 
     * @param gameState The current game state to clean up
     */
    public void cleanupGameResources(GameState gameState) {
        if (gameState == null) return;
        
        cleanupSystems(gameState);
        gameState.stopCollisionSystem();
        cleanupConnections(gameState);
        gameState.cleanup();
    }
    
    /**
     * Cleans up all systems in the game state
     * 
     * @param gameState The game state containing the systems
     */
    private void cleanupSystems(GameState gameState) {
        for (NetworkSystem system : new ArrayList<>(gameState.getSystems())) {
            system.cleanup();
        }
    }
    
    /**
     * Cleans up all connections in the game state
     * 
     * @param gameState The game state containing the connections
     */
    private void cleanupConnections(GameState gameState) {
        for (Connection connection : new ArrayList<>(gameState.getConnections())) {
            connection.getPackets().clear();
            connection.setAvailable(true);
            connection.clearAllStyling();
        }
    }
} 

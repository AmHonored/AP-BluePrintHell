package com.networkgame.controller;

import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.service.audio.AudioManager.SoundType;
import com.networkgame.view.GameScene;
   

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LevelController {
    private GameController mainController;
    private GameState gameState;
    private LevelManager levelManager;
    private boolean levelCompletionHandled = false;
    
    public LevelController(GameController mainController, GameState gameState, LevelManager levelManager) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.levelManager = levelManager;
    }
    
    public void startGame(int levelNumber) {
        mainController.stopGame();
        cleanupGameState();
        
        LevelManager.Level level = levelManager.getLevel(levelNumber);
        if (level == null) return;
        
        // Reset and initialize game state
        gameState.reset();
        gameState.setCurrentLevel(levelNumber);
        level.getSystems().forEach(gameState::addSystem);
        gameState.setWireLength(level.getWireLength());
        
        // Setup game scene and start
        GameScene gameScene = new GameScene(mainController, gameState);
        mainController.setGameScene(gameScene);
        mainController.getAudioManager().switchToGameMusic();
        mainController.getPrimaryStage().setScene(gameScene.getScene());
        
        levelCompletionHandled = false;
        mainController.getGameplayController().startGameLoop();
    }
    
    private void cleanupGameState() {
        if (gameState == null) return;
        
        // Stop all systems and clean up
        new ArrayList<>(gameState.getSystems()).forEach(system -> {
            if (system.isStartSystem()) system.stopSendingPackets();
            system.cleanup();
        });
        
        gameState.stopCollisionSystem();
        
        // Clear connections
        new ArrayList<>(gameState.getConnections()).forEach(connection -> {
            connection.getPackets().clear();
            connection.setAvailable(true);
            connection.clearAllStyling();
        });
        
        gameState.cleanup();
        System.out.println("Game state thoroughly cleaned up before starting new level");
    }
    
    public void initializePacketGeneration() {
        // Get start systems and create timeline
        List<NetworkSystem> startSystems = gameState.getSystems().stream()
            .filter(NetworkSystem::isStartSystem)
            .collect(Collectors.toList());
            
        int currentLevel = gameState.getCurrentLevel();
        LevelManager.Level level = levelManager.getLevel(currentLevel);
        javafx.animation.Timeline timeline = new javafx.animation.Timeline();
        
        // Create staggered start times for each system
        for (int i = 0; i < startSystems.size(); i++) {
            final int index = i;
            timeline.getKeyFrames().add(new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(1.0 + i * 0.5),
                event -> {
                    NetworkSystem system = startSystems.get(index);
                    double interval = level != null ? level.getPacketSpawnInterval() : calculatePacketInterval(system, currentLevel);
                    System.out.println("Starting packet generation for system: " + 
                                      system.getLabel() + " with interval: " + interval + "s");
                    system.startSendingPackets(interval);
                }
            ));
        }
        
        timeline.play();
    }
    
    private double calculatePacketInterval(NetworkSystem system, int currentLevel) {
        if (currentLevel == 1) return 2.0; // Level 1: Slower for tutorial
        if (currentLevel == 2) {
            // Level 2: Faster with variation
            return system.getLabel().equals("START") ? 0.8 : 1.0;
        }
        return 1.5; // Default for other levels
    }
    
    public void levelCompleted() {
        // Stop game and set completion flag
        mainController.stopGame();
        mainController.getGameplayController().stopGameLoop();
        levelCompletionHandled = true;
        
        // Cleanup systems and packets
        gameState.getSystems().forEach(system -> {
            if (system.isStartSystem()) system.stopSendingPackets();
            system.getStoredPackets().clear();
        });
        
        gameState.stopTimer();
        gameState.getActivePackets().clear();
        
        // Clear connections
        gameState.getConnections().forEach(connection -> {
            connection.getPackets().clear();
            connection.setAvailable(true);
        });
        
        // Effects and progress
        mainController.getAudioManager().stopBackgroundMusic();
        mainController.getAudioManager().playSoundEffect(SoundType.LEVEL_COMPLETE);
        
        int currentLevel = gameState.getCurrentLevel();
        levelManager.unlockLevel(currentLevel + 1);
        ((GameScene)mainController.getGameScene()).showLevelComplete(currentLevel);
    }
    
    public void proceedToNextLevel() {
        int nextLevel = gameState.getCurrentLevel() + 1;
        if (nextLevel <= levelManager.getTotalLevels()) {
            startGame(nextLevel);
        } else {
            mainController.returnToMainMenu();
        }
    }
    
    public void restartCurrentLevel() {
        int currentLevel = gameState.getCurrentLevel();
        
        // Stop systems and game
        gameState.getSystems().stream()
            .filter(NetworkSystem::isStartSystem)
            .forEach(NetworkSystem::stopSendingPackets);
            
        mainController.stopGame();
        gameState.getActivePackets().clear();
        
        // Restart the current level
        startGame(currentLevel);
    }
    
    public void handleLevelCompletion() {
        if (!levelCompletionHandled) {
            levelCompletionHandled = true;
            levelCompleted();
            System.out.println("Level completed event triggered!");
        }
    }
    
    public void initialize() {
        gameState.initializeCollisionSystem();
        System.out.println("LevelController initialized at level: " + gameState.getCurrentLevel());
    }
    
    public void cleanup() {
        gameState.getSystems().stream()
            .filter(NetworkSystem::isStartSystem)
            .forEach(NetworkSystem::stopSendingPackets);
    }
    
    public boolean isLevelCompletionHandled() {
        return levelCompletionHandled;
    }
} 

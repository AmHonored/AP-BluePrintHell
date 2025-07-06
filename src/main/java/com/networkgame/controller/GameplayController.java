package com.networkgame.controller;

import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.view.GameScene;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

public class GameplayController {
    // Constants organized by functionality
    private static final class Constants {
        // Game timing
        static final double MAX_DELTA_TIME = 0.1;
        
        // Simulation
        static final double SIMULATION_FRAME_RATE = 16.0; // ms (~60fps)
        static final double SIMULATION_DELTA_TIME = 0.016; // seconds
        static final int SIMULATION_TIMEOUT_BUFFER = 1000; // ms
        static final int SIMULATION_SLEEP_TIME = 10; // ms
        
        // Debugging
        static final int DEBUG_LOG_PROBABILITY = 20; // 1 in X chance
    }
    
    private final GameController mainController;
    private final GameState gameState;
    private final GameLoopManager gameLoopManager;
    private final SimulationEngine simulationEngine;
    private boolean isRunning;
    private double gameSpeed = 1.0;
    
    public GameplayController(GameController mainController, GameState gameState) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.isRunning = false;
        this.gameLoopManager = new GameLoopManager();
        this.simulationEngine = new SimulationEngine();
    }
    
    public void initialize() {
        gameState.initializeCollisionSystem();
    }
    
    public void startGameLoop() {
        gameLoopManager.start();
    }
    
    public void stopGameLoop() {
        gameLoopManager.stop();
    }
    
    public void cleanup() {
        gameState.stopCollisionSystem();
        gameState.cleanup();
        
        stopGameLoop();
        gameLoopManager.dispose();
        
        mainController.getAudioManager().stopBackgroundMusic();
        
        withGameScene(GameScene::cleanup);
    }
    
    public void pauseGame() {
        isRunning = false;
        mainController.getAudioManager().pauseBackgroundMusic();
        
        withGameScene(GameScene::showPauseIndicator);
        
        gameState.pauseCollisionSystem();
    }
    
    public void resumeGame() {
        isRunning = true;
        mainController.getAudioManager().playBackgroundMusic();
        
        withGameScene(GameScene::hidePauseIndicator);
        
        gameState.resumeCollisionSystem();
        
        checkPacketGeneration();
    }
    
    public void stopGame() {
        isRunning = false;
    }
    
    public void setGameSpeed(double speed) {
        gameSpeed = speed;
        updateEntitySpeeds();
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public double getGameSpeed() {
        return gameSpeed;
    }
    
    public void simulateFrames(int durationMs) {
        simulationEngine.simulateFrames(durationMs);
    }
    
    public void resetGameStateToTime(double targetTime) {
        gameState.setElapsedTime(targetTime);
        new ArrayList<>(gameState.getActivePackets()).forEach(gameState::safelyRemovePacket);
    }
    
    private void updateEntities(double deltaTime) {
        double cappedDeltaTime = Math.min(deltaTime, Constants.MAX_DELTA_TIME);
        
        updateGameComponents(cappedDeltaTime);
        updateActivePackets(cappedDeltaTime);
        checkLevelStatus();
    }
    
    private void updateGameComponents(double deltaTime) {
        gameState.update(deltaTime);
        gameState.getConnections().forEach(conn -> conn.update(deltaTime));
        gameState.getSystems().forEach(system -> system.update(deltaTime));
    }
    
    private void updateActivePackets(double deltaTime) {
        gameState.getActivePackets().stream()
            .filter(p -> !p.isInsideSystem() && !p.hasReachedEndSystem())
            .forEach(p -> p.update(deltaTime));
    }
    
    private void checkLevelStatus() {
        if (gameState.isLevelCompleted() && !mainController.getLevelController().isLevelCompletionHandled()) {
            mainController.getLevelController().handleLevelCompletion();
        }
        if (gameState.isLevelFailed()) {
            showTimeUpGameOver();
        }
    }
    
    private void showTimeUpGameOver() {
        ((GameScene)mainController.getGameScene()).showTimeUpGameOver(
            gameState.getPacketsDelivered(), 
            gameState.getLevelRequiredPackets()
        );
    }
    
    private boolean hasStartSystems() {
        return gameState.getSystems().stream().anyMatch(NetworkSystem::isStartSystem);
    }
    
    private void checkPacketGeneration() {
        if (gameState.getActivePackets().isEmpty() && hasStartSystems()) {
            mainController.getLevelController().initializePacketGeneration();
        }
    }
    
    private void updateEntitySpeeds() {
        gameState.getSystems().stream()
            .filter(NetworkSystem::isStartSystem)
            .forEach(system -> system.updatePacketGenerationSpeed(gameSpeed));
            
        gameState.getActivePackets()
            .forEach(packet -> packet.setSpeed(packet.getSpeed() * gameSpeed));
    }
    
    private void withGameScene(Consumer<GameScene> action) {
        Optional.ofNullable(((GameScene)mainController.getGameScene())).ifPresent(action);
    }
    
    /**
     * Manages the game animation loop
     */
    private class GameLoopManager {
        private AnimationTimer gameLoop;
        private long lastUpdateTime;
        
        GameLoopManager() {
            createGameLoop();
        }
        
        void start() {
            lastUpdateTime = 0;
            gameLoop.start();
        }
        
        void stop() {
            if (gameLoop != null) {
                gameLoop.stop();
            }
        }
        
        void dispose() {
            gameLoop = null;
        }
        
        private void createGameLoop() {
            gameLoop = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (lastUpdateTime == 0) {
                        lastUpdateTime = now;
                        return;
                    }
                    
                    double deltaTime = (now - lastUpdateTime) / 1_000_000_000.0;
                    lastUpdateTime = now;
                    
                    if (isRunning && !mainController.isTemporalControlMode()) {
                        updateEntities(deltaTime);
                    }
                    
                    withGameScene(GameScene::render);
                }
            };
        }
    }
    
    /**
     * Handles time-based simulation
     */
    private class SimulationEngine {
        void simulateFrames(int durationMs) {
            final boolean[] simulationComplete = {false};
            
            // Create and configure timeline in one sequence
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(Constants.SIMULATION_FRAME_RATE),
                    event -> gameState.update(Constants.SIMULATION_DELTA_TIME * gameSpeed))
            );
            
            timeline.setCycleCount(durationMs / (int)Constants.SIMULATION_FRAME_RATE);
            timeline.setOnFinished(event -> simulationComplete[0] = true);
            timeline.play();
            
            waitForCompletion(simulationComplete, durationMs);
        }
        
        private void waitForCompletion(boolean[] completionFlag, int durationMs) {
            long startTime = System.currentTimeMillis();
            long timeoutLimit = startTime + durationMs + Constants.SIMULATION_TIMEOUT_BUFFER;
            
            while (!completionFlag[0] && System.currentTimeMillis() < timeoutLimit) {
                try { Thread.sleep(Constants.SIMULATION_SLEEP_TIME); } 
                catch (InterruptedException e) { break; }
            }
        }
    }
} 

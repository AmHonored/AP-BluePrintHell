package com.networkgame.controller;

import com.networkgame.model.state.GameState;
import com.networkgame.view.GameScene;
import java.util.ArrayList;

public class TemporalController {
    private static final double TIME_INCREMENT = 0.02;
    private static final double MAX_PROGRESS = 1.0;
    private static final double MIN_PROGRESS = 0.0;
    private static final double NORMAL_GAME_SPEED = 1.0;
    private static final double TIME_TRAVEL_SPEED = 10.0;
    
    private GameController mainController;
    private GameState gameState;
    private boolean isTemporalControlMode;
    
    public TemporalController(GameController mainController, GameState gameState) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.isTemporalControlMode = false;
    }
    
    public void toggleTemporalControlMode() {
        isTemporalControlMode = !isTemporalControlMode;
        gameState.setTemporalProgressEnabled(isTemporalControlMode);
        
        if (isTemporalControlMode) {
            mainController.pauseGame();
        }
        
        if (((GameScene)mainController.getGameScene()) != null) {
            if (isTemporalControlMode) {
                ((GameScene)mainController.getGameScene()).showTemporalControlIndicator();
            } else {
                ((GameScene)mainController.getGameScene()).hideTemporalControlIndicator();
            }
        }
    }
    
    public void moveTimeForward() {
        adjustTemporalProgress(true);
    }
    
    public void moveTimeBackward() {
        adjustTemporalProgress(false);
    }
    
    private void adjustTemporalProgress(boolean forward) {
        if (!isTemporalControlMode) return;
        
        double progress = gameState.getTemporalProgress();
        double adjustment = forward ? TIME_INCREMENT : -TIME_INCREMENT;
        gameState.setTemporalProgress(Math.max(MIN_PROGRESS, Math.min(MAX_PROGRESS, progress + adjustment)));
        
        if (((GameScene)mainController.getGameScene()) != null) {
            ((GameScene)mainController.getGameScene()).updateTemporalProgress();
        }
    }
    
    /**
     * Travel to a specific point in time in the game
     * @param time Target time to jump to (in seconds)
     */
    public void travelInTime(int time) {
        // Don't proceed if time travel is invalid
        if (gameState.isGameOver() || gameState.isLevelCompleted() || 
            time < 0 || time > gameState.getLevelDuration()) {
            return;
        }
        
        boolean wasRunning = mainController.isRunning();
        if (wasRunning) mainController.pauseGame();
        
        // Temporarily increase game speed for simulation
        mainController.getGameplayController().setGameSpeed(TIME_TRAVEL_SPEED);
        
        // Calculate and simulate time travel
        double currentTime = gameState.getElapsedTime();
        double timeToSimulate = time - currentTime;
        int simulationDuration = (int)Math.abs(timeToSimulate * 100);
        
        if (timeToSimulate > 0) {
            mainController.getGameplayController().simulateFrames(simulationDuration);
        } else if (timeToSimulate < 0) {
            mainController.getGameplayController().resetGameStateToTime(time);
        }
        
        // Reset game speed and update UI
        mainController.getGameplayController().setGameSpeed(NORMAL_GAME_SPEED);
        
        if (((GameScene)mainController.getGameScene()) != null) {
            int levelDuration = gameState.getLevelDuration();
            ((GameScene)mainController.getGameScene()).updateTimeProgress(time, levelDuration);
            ((GameScene)mainController.getGameScene()).createTimeJumpEffect();
            ((GameScene)mainController.getGameScene()).render();
            ((GameScene)mainController.getGameScene()).updatePacketLossLabel(gameState.getPacketLossPercentage());
        }
        
        if (wasRunning) mainController.resumeGame();
        
        System.out.println("Time travel complete - now at time: " + time + "s");
    }
    
    public boolean isTemporalControlMode() {
        return isTemporalControlMode;
    }
} 

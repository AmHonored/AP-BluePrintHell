package com.networkgame.model.state; 

import com.networkgame.model.entity.system.NetworkSystem;
// Removed import of LevelManager to break cyclic dependency

/**
 * Tracks level progress, timing, and completion criteria
 */
public class LevelProgressTracker {
    private GameState gameState;
    
    // Time tracking for level
    private int levelDuration = 60; // in seconds - fixed at 60 seconds for Level 1
    private double elapsedTime = 0; // in seconds
    private boolean isTimeUp = false;
    private boolean timerActive = false; // Flag to track if timer is actively running
    
    // Level state
    private int currentLevel = 1;
    private boolean isLevelCompleted = false;
    private boolean isGameOver = false;
    
    public LevelProgressTracker(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Update the level timer
     * @param deltaTime time passed in seconds
     */
    public void updateLevelTimer(double deltaTime) {
        // Only update timer if it's active
        if (timerActive && !isTimeUp) {
            // Accumulate elapsed time more precisely
            elapsedTime += deltaTime; // Use the actual deltaTime without truncating to integers
            
            // Check if time is up
            if ((int)elapsedTime >= levelDuration) {
                elapsedTime = levelDuration;
                isTimeUp = true;
                
                // When time is up, check if level is completed based on win conditions
                if (currentLevel == 1) {
                    // For level 1, check packet loss percentage
                    if (gameState.getPacketLossPercentage() < 50.0) {
                        isLevelCompleted = true;
                        System.out.println("Level 1 completed! Packet loss: " + gameState.getPacketLossPercentage() + "%");
                    } else {
                        System.out.println("Level 1 failed! Packet loss too high: " + gameState.getPacketLossPercentage() + "%");
                    }
                } else if (currentLevel == 2) {
                    // For level 2, check packet loss percentage when time is up
                    if (gameState.getPacketLossPercentage() < 50.0) {
                        isLevelCompleted = true;
                        System.out.println("Level 2 completed! Packet loss: " + gameState.getPacketLossPercentage() + "%");
                    } else {
                        System.out.println("Level 2 failed! Packet loss too high: " + gameState.getPacketLossPercentage() + "%");
                    }
                } else if (gameState.getPacketManager().getPacketsDelivered() >= getLevelRequiredPackets()) {
                    // For other levels, use the original packet delivery check
                    isLevelCompleted = true;
                }
            }
            
            // Always update time progress display
                    if (gameState.getUIUpdateListener() != null) {
            javafx.application.Platform.runLater(() -> gameState.getUIUpdateListener().updateTimeProgress(elapsedTime, levelDuration));
        }
        }
    }
    
    /**
     * Reset the level timer to zero
     */
    public void resetLevelTimer() {
        this.elapsedTime = 0;
        this.isTimeUp = false;
        // Don't automatically activate the timer - it should only be activated 
        // when the play button is clicked
    }
    
    /**
     * Start the level timer
     */
    public void startTimer() {
        timerActive = true;
    }
    
    /**
     * Stop the level timer
     */
    public void stopTimer() {
        timerActive = false;
    }
    
    /**
     * Check if the level has been completed successfully
     * @return true if level is completed successfully
     */
    public boolean isLevelCompleted() {
        // For level 1, check if time is up and packet loss is below 50%
        if (currentLevel == 1) {
            return isTimeUp && gameState.getPacketLossPercentage() < 50.0;
        }
        
        // For level 2, check if time is up and packet loss is below 50%
        if (currentLevel == 2) {
            return isTimeUp && gameState.getPacketLossPercentage() < 50.0;
        }
        
        // For other levels, check both packet delivery and packet loss
        int requiredPackets = getLevelRequiredPackets();
        double maxPacketLoss = Math.max(50.0 - (currentLevel - 1) * 2, 20.0);
        return gameState.getPacketManager().getPacketsDelivered() >= requiredPackets && 
               gameState.getPacketLossPercentage() < maxPacketLoss;
    }
    
    /**
     * Check if the level has failed
     * @return true if level has failed
     */
    public boolean isLevelFailed() {
        // In level 1, never fail due to packet loss
        if (currentLevel == 1) {
            return false;
        }
    
        // For other levels, fail when packet loss is 50% or higher
        return gameState.getPacketLossPercentage() >= 50.0;
    }
    
    /**
     * Get the required number of packets to deliver for the current level
     */
    public int getLevelRequiredPackets() {
        // Level 1 requires just 6 packets (more achievable)
        if (currentLevel == 1) {
            return 6;
        }
        // Level 2 is time-based, no packet requirement
        if (currentLevel == 2) {
            return 0;
        }
        // Progressive difficulty for higher levels
        return 5 + (currentLevel - 1) * 2;
    }
    
    /**
     * Helper methods to implement the failure condition logic
     */
    public void pauseLevel(int level) {
        // Pause the level
        timerActive = false;
        
        // Stop all start systems from sending packets
        for (NetworkSystem system : gameState.getSystems()) {
            if (system.isStartSystem()) {
                system.stopSendingPackets();
            }
        }
    }
    
    /**
     * Mark the level as completed or failed
     * @param success Whether the level was completed successfully
     */
    public void levelIsOver(boolean success) {
        isGameOver = true;
        
        // Additional logic can be added here based on success or failure
        System.out.println("Level " + currentLevel + " is over. Success: " + success);
    }
    
    /**
     * Set the current level
     * @param level The level number
     */
    public void setCurrentLevel(int level) {
        this.currentLevel = level;
        
        // Reset level completion and time-related flags
        isLevelCompleted = false;
        isTimeUp = false;
        
        // Set default level duration based on level number
        // This removes the dependency on LevelManager
        switch (level) {
            case 1:
                levelDuration = 60; // Level 1 gets 60 seconds
                break;
            case 2:
                levelDuration = 90; // Level 2 gets 90 seconds
                break;
            default:
                levelDuration = 120; // Higher levels get 120 seconds
                break;
        }
        
        // Reset the level timer whenever level changes
        resetLevelTimer();
    }
    
    /**
     * Set the level duration directly (allows GameState/LevelManager to configure duration)
     * @param duration The duration in seconds
     */
    public void setLevelDuration(int duration) {
        this.levelDuration = duration;
    }
    
    /**
     * Add more time to the level timer
     * @param seconds Additional seconds to add
     */
    public void addTime(int seconds) {
        // Add time to the level timer
        this.elapsedTime = Math.max(0, this.elapsedTime - seconds);
        
        // If time was up, make it active again
        if (isTimeUp) {
            isTimeUp = false;
            timerActive = true;
        }
        
        System.out.println("Added " + seconds + " seconds to level timer. Remaining time: " + 
                          (levelDuration - elapsedTime) + " seconds");
    }
    
    /**
     * Reset all level progress tracking
     */
    public void reset() {
        this.elapsedTime = 0;
        this.isTimeUp = false;
        this.timerActive = false;
        this.isLevelCompleted = false;
        this.isGameOver = false;
    }
    
    // Getters and setters
    
    public int getCurrentLevel() {
        return currentLevel;
    }
    
    public double getElapsedTime() {
        return elapsedTime;
    }
    
    public void setElapsedTime(double time) {
        this.elapsedTime = time;
    }
    
    public int getLevelDuration() {
        return levelDuration;
    }
    
    public boolean isTimeUp() {
        return isTimeUp;
    }
    
    public void setLevelCompleted(boolean completed) {
        this.isLevelCompleted = completed;
    }
    
    public boolean isGameOver() {
        return isGameOver;
    }
    
    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }
} 

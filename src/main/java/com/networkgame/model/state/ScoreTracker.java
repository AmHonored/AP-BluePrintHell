package com.networkgame.model.state; 

/**
 * Tracks scores, coins, and game statistics
 */
public class ScoreTracker {
    private final GameState gameState;
    
    // Constants
    private static final long COMBO_TIMEOUT = 2000; // 2 seconds for combo
    private static final int INITIAL_COINS = 100;
    private static final double MAX_PACKET_LOSS_PERCENTAGE = 50.0;
    private static final double LEVEL_ONE_MAX_PACKET_LOSS = 49.0;
    
    // Currency and score
    private int coins = INITIAL_COINS;
    private int score = 0;
    
    // Combo tracking
    private int comboMultiplier = 1;
    private long lastPacketDeliveryTime = 0;
    
    // Packet statistics
    private double packetLossPercentage = 0;
    
    public ScoreTracker(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Add coins to the player's balance
     */
    public void addCoins(int amount) {
        this.coins += amount;
        
        // Update the HUD immediately
        if (gameState != null && gameState.getUIUpdateListener() != null) {
            javafx.application.Platform.runLater(() -> {
                gameState.getUIUpdateListener().updateCoinsLabel(this.coins);
            });
        }
    }
    
    /**
     * Spend coins from the player's balance
     */
    public void spendCoins(int amount) {
        this.coins -= amount;
    }
    
    /**
     * Update the packet loss percentage based on current stats
     */
    public void updatePacketLossPercentage() {
        calculatePacketLoss();
        applyLevelSpecificRules();
        checkGameOverCondition();
        updateUI();
    }
    
    private void calculatePacketLoss() {
        int totalPackets = gameState.getPacketManager().getTotalPacketsSent();
        int packetsLost = gameState.getPacketManager().getPacketsLost();
        
        packetLossPercentage = totalPackets > 0 ? (packetsLost * 100.0) / totalPackets : 0.0;
    }
    
    private void applyLevelSpecificRules() {
        // For level 1, clamp packet loss to never exceed 49%
        if (gameState.getLevelProgressTracker().getCurrentLevel() == 1) {
            packetLossPercentage = Math.min(packetLossPercentage, LEVEL_ONE_MAX_PACKET_LOSS);
        }
    }
    
    private void checkGameOverCondition() {
        // Check for game over condition when packet loss exceeds 50%
        if (packetLossPercentage > MAX_PACKET_LOSS_PERCENTAGE && 
            gameState.getLevelProgressTracker().getCurrentLevel() > 1) {
            gameState.getLevelProgressTracker().setGameOver(true);
            System.out.println("Game over due to packet loss exceeding 50%: " + packetLossPercentage + "%");
        }
    }
    
    private void updateUI() {
        if (gameState.getUIUpdateListener() != null) {
            gameState.getUIUpdateListener().updatePacketLossLabel(packetLossPercentage);
        }
    }
    
    /**
     * Reset the score tracker
     * @param fullReset If true, also resets persistent data like coins
     */
    public void reset(boolean fullReset) {
        this.packetLossPercentage = 0;
        this.comboMultiplier = 1;
        this.lastPacketDeliveryTime = 0;
        
        if (fullReset) {
            this.coins = INITIAL_COINS;
            this.score = 0;
        }
    }
    
    /**
     * Reset the score tracker
     */
    public void reset() {
        reset(false);
    }
    
    /**
     * Reset the score tracker completely, including coins
     */
    public void fullReset() {
        reset(true);
    }
    
    // Getters and setters
    public int getCoins() {
        return coins;
    }
    
    public void setCoins(int coins) {
        this.coins = coins;
        System.out.println("Coins set to: " + coins);
    }
    
    public int getScore() {
        return score;
    }
    
    public int getComboMultiplier() {
        return comboMultiplier;
    }
    
    public double getPacketLossPercentage() {
        return packetLossPercentage;
    }
} 

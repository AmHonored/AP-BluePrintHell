package com.networkgame.model.manager; 

import com.networkgame.model.state.GameState;
import com.networkgame.model.state.GameStateProvider;
import com.networkgame.model.state.GameStateCallbacks;
import com.networkgame.model.entity.Packet;

/**
 * Manages power-ups and temporary abilities in the game
 */
public class PowerUpManager {
    private GameState gameState;
    
    // Anahita feature flag for potential future power-ups
    private boolean anahita = false;
    
    public PowerUpManager(GameState gameState) {
        this.gameState = gameState;
    }
    
    /**
     * Purchase and activate the disable impact effect power-up
     * @param seconds Duration in seconds
     */
    public void purchaseDisableImpactEffect(int seconds) {
        if (seconds <= 0) return;
        
        // Check if player has enough coins
        if (gameState.getScoreTracker().getCoins() < 3) {
            System.out.println("Not enough coins to disable impact effect");
            return;
        }
        
        // Deduct coins
        gameState.getScoreTracker().spendCoins(3);
        
        // Apply effect
        gameState.getImpactEffectSystem().disableImpactEffect(seconds);
    }
    
    /**
     * Purchase and activate the disable collision power-up
     * @param seconds Duration in seconds
     */
    public void purchaseDisableCollision(int seconds) {
        if (seconds <= 0) return;
        
        // Check if player has enough coins
        if (gameState.getScoreTracker().getCoins() < 4) {
            System.out.println("Not enough coins to disable collision");
            return;
        }
        
        // Deduct coins
        gameState.getScoreTracker().spendCoins(4);
        
        // Apply effect
        gameState.getCollisionSystem().disableCollision(seconds);
    }
    
    /**
     * Purchase and use the reset all packet noise power-up
     */
    public void purchaseResetAllPacketNoise() {
        // Check if player has enough coins
        if (gameState.getScoreTracker().getCoins() < 5) {
            System.out.println("Not enough coins to reset packet noise");
            return;
        }
        
        // Deduct coins
        gameState.getScoreTracker().spendCoins(5);
        
        // Reset health for all active packets
        for (Packet packet : gameState.getPacketManager().getActivePackets()) {
            packet.resetHealth();
        }
        
        System.out.println("Reset noise for all " + gameState.getPacketManager().getActivePackets().size() + " packets");
    }
    
    /**
     * Purchase and use the add time power-up
     * @param seconds Additional seconds to add
     */
    public void purchaseAddTime(int seconds) {
        // Check if player has enough coins
        if (gameState.getScoreTracker().getCoins() < 5) {
            System.out.println("Not enough coins to add time");
            return;
        }
        
        // Deduct coins
        gameState.getScoreTracker().spendCoins(5);
        
        // Add time
        gameState.getLevelProgressTracker().addTime(seconds);
    }
    
    /**
     * Get the anahita feature flag
     * @return True if anahita is enabled
     */
    public boolean isAnahita() {
        return anahita;
    }
    
    /**
     * Set the anahita feature flag
     * @param anahita True to enable anahita
     */
    public void setAnahita(boolean anahita) {
        this.anahita = anahita;
    }
} 

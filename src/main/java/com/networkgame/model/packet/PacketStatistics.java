package com.networkgame.model.packet;

import javafx.application.Platform;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.state.GameState;
import com.networkgame.service.audio.AudioManager;


/**
 * Handles tracking of packet statistics (sent, delivered, lost)
 */
public class PacketStatistics {
    private final NetworkSystem parentSystem;
    private final GameState gameState;
    private int totalPacketsSent = 0;
    private int packetsDelivered = 0;
    private int packetsLost = 0;
    private int visualPacketsDelivered = 0;
    private boolean useVisualPacketCountOnly = true;

    public PacketStatistics(NetworkSystem parentSystem, GameState gameState) {
        this.parentSystem = parentSystem;
        this.gameState = gameState;
    }

    public void reset() {
        totalPacketsSent = 0;
        packetsDelivered = 0;
        packetsLost = 0;
        visualPacketsDelivered = 0;
    }

    /**
     * Increment the counter for packets that reached an end system
     */
    public void incrementPacketsDelivered() {
        packetsDelivered++;
        logPacketDelivered();
        updateHudPacketCounter(packetsDelivered);
    }
    
    /**
     * Increment visual packet counter when a packet is visually seen reaching the end system
     */
    public void incrementVisualPacketsDelivered() {
        int requiredPackets = gameState.getLevelRequiredPackets();
        
        // Prevent exceeding required amount
        if (visualPacketsDelivered >= requiredPackets) {
            return;
        }
        
        visualPacketsDelivered++;
        logVisualPacketDelivered(requiredPackets);
        
        // Update the HUD immediately
        updateHudPacketCounterAsync();
        
        // Check for level completion
        checkLevelCompletion();
    }
    
    /**
     * Increment the lost packets counter
     */
    public void incrementLostPackets() {
        packetsLost++;
        gameState.updatePacketLossPercentage();
        
        // Play packet loss sound
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_LOSS);
        
        updateHudPacketLossLabel();
        logPacketLoss();
    }

    /**
     * Get number of packets delivered to end systems
     */
    public int getPacketsDelivered() {
        return useVisualPacketCountOnly ? visualPacketsDelivered : packetsDelivered;
    }

    /**
     * Reset delivered packets counter
     */
    public void resetPacketsDelivered() {
        packetsDelivered = 0;
        visualPacketsDelivered = 0;
    }
    
    /**
     * Increment the total packets counter
     */
    public void incrementTotalPackets() {
        totalPacketsSent++;
    }

    public int getTotalPacketsSent() {
        return totalPacketsSent;
    }

    public int getPacketsLost() {
        return packetsLost;
    }
    
    public int getVisualPacketsDelivered() {
        return visualPacketsDelivered;
    }
    
    public void setUseVisualPacketCountOnly(boolean useVisualPacketCountOnly) {
        this.useVisualPacketCountOnly = useVisualPacketCountOnly;
    }
    
    // ---- Private helper methods ----
    
    private void logPacketDelivered() {
        System.out.println("Packet delivered successfully! Total delivered: " + packetsDelivered);
    }
    
    private void logVisualPacketDelivered(int requiredPackets) {
        System.out.println("Visual packet delivered. Count: " + visualPacketsDelivered + "/" + requiredPackets);
    }
    
    private void logPacketLoss() {
        System.out.println("Packet lost! Total lost: " + packetsLost + 
                          ", Loss %: " + gameState.getPacketLossPercentage());
    }
    
    private void updateHudPacketCounter(int count) {
        if (hasGameScene()) {
            gameState.getUIUpdateListener().updatePacketsCollectedLabel(count);
        }
    }
    
    private void updateHudPacketCounterAsync() {
        if (hasGameScene()) {
            Platform.runLater(() -> {
                int displayCount = useVisualPacketCountOnly ? visualPacketsDelivered : packetsDelivered;
                gameState.getUIUpdateListener().updatePacketsCollectedLabel(displayCount);
            });
        }
    }
    
    private void updateHudPacketLossLabel() {
        if (hasGameScene()) {
            gameState.getUIUpdateListener().updatePacketLossLabel(gameState.getPacketLossPercentage());
        }
    }
    
    private boolean hasGameScene() {
        return gameState.getUIUpdateListener() != null;
    }
    
    private void checkLevelCompletion() {
        if (visualPacketsDelivered == gameState.getLevelRequiredPackets() && 
            gameState.getCurrentLevel() == 1) {
            gameState.setLevelCompleted(true);
            System.out.println("Level " + gameState.getCurrentLevel() + " completion requirements met!");
        }
    }
} 

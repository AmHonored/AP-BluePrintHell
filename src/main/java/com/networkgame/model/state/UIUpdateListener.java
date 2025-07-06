package com.networkgame.model.state;

import com.networkgame.model.entity.system.NetworkSystem;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * Interface for UI update notifications.
 * Breaks the circular dependency between GameState and View classes.
 */
public interface UIUpdateListener {
    // Packet and statistics updates
    void updatePacketLossLabel(double packetLossPercentage);
    void updatePacketsCollectedLabel(int packetsCollected);
    void updateCoinsLabel(int coinsCount);
    void updateTimeProgress(double elapsedTime, int levelDuration);
    void updateCapacityLabels();
    
    // Game state updates
    void render();
    void showCapacityExceededGameOver(NetworkSystem system);
    void showTimeUpGameOver(int packetsDelivered, int requiredPackets);
    void showGameOver(double packetLossPercentage, int successfulPackets);
    void showLevelComplete(int level);
    
    // Visual effects
    void createTimeJumpEffect();
    void createImpactEffect(Point2D position);
    
    // General update notification
    void onGameStateUpdated();
    
    // UI manipulation methods (for WireCollisionManager)
    Pane getGamePane();
    Scene getScene();
} 
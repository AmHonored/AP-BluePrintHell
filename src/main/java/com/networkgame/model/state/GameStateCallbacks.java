package com.networkgame.model.state;

import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.system.NetworkSystem;
import javafx.geometry.Point2D;

/**
 * Interface for callbacks that managers can use to modify game state.
 * This breaks the circular dependency between GameState and managers.
 */
public interface GameStateCallbacks {
    // Packet operations
    void addActivePacket(Packet packet);
    void removeActivePacket(Packet packet);
    void incrementPacketsDelivered();
    void incrementVisualPacketsDelivered();
    void incrementLostPackets();
    void incrementTotalPackets();
    
    // Connection operations
    void addConnection(Connection connection);
    void removeConnection(Connection connection);
    
    // System operations
    void addSystem(NetworkSystem system);
    void removeSystem(NetworkSystem system);
    
    // Resource operations
    void addCoins(int amount);
    void spendCoins(int amount);
    void updateRemainingWireLength();
    void setTotalWireUsed(double totalWireUsed);
    
    // Game state operations
    void setLevelCompleted(boolean completed);
    void setGameOver(boolean gameOver);
    void levelIsOver(boolean success);
    void pauseLevel(int level);
    
    // Effects and events
    void emitImpactEvent(Point2D position);
    void updatePacketLossPercentage();
    
    // Timer operations
    void startTimer();
    void stopTimer();
    void setElapsedTime(double time);
    
    // Temporal operations
    void setTemporalProgress(double progress);
    void setTemporalProgressEnabled(boolean enabled);
    
    // Feature flags
    void setAiryaman(boolean airyaman);
    void setAtar(boolean atar);
    void setAnahita(boolean anahita);
} 
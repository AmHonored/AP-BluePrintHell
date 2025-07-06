package com.networkgame.model.state;

import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.system.NetworkSystem;

import java.util.List;

/**
 * Interface providing read-only access to game state information.
 * Used by managers to avoid direct GameState dependencies.
 */
public interface GameStateProvider {
    // Level information
    int getCurrentLevel();
    int getLevelRequiredPackets();
    boolean isLevelCompleted();
    boolean isLevelFailed();
    
    // Game objects
    List<NetworkSystem> getSystems();
    List<Connection> getConnections();
    List<Packet> getActivePackets();
    
    // Game statistics
    int getPacketsDelivered();
    double getPacketLossPercentage();
    double getElapsedTime();
    int getLevelDuration();
    
    // Resources
    int getCoins();
    double getRemainingWireLength();
    double getTotalWireUsed();
    
    // Game state flags
    boolean isGameOver();
    boolean isTemporalProgressEnabled();
    double getTemporalProgress();
    
    // Feature flags
    boolean isAiryaman();
    boolean isAtar();
    boolean isAnahita();
} 
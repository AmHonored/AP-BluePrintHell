package com.networkgame.model;

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central class that coordinates all game systems and manages overall game state.
 * Delegates specific functionality to specialized manager classes.
 */
public class GameState {
    // Core game components
    private List<NetworkSystem> systems = new ArrayList<>();
    private com.networkgame.view.GameScene gameScene;
    private com.networkgame.controller.CollisionController collisionController;
    
    // Manager classes that handle specific functionalities
    private PacketManager packetManager;
    private ConnectionManager connectionManager;
    private CollisionSystem collisionSystem;
    private ImpactEffectSystem impactEffectSystem;
    private LevelProgressTracker levelProgressTracker;
    private ScoreTracker scoreTracker;
    private PowerUpManager powerUpManager;
    private LevelManager levelManager;
    private HUDManager hudManager;
    
    // Temporal progress fields
    private boolean temporalProgressEnabled = false;
    private double temporalProgress = 0.0;
    
    public GameState() {
        // Initialize ScoreTracker first so other managers can access coins and stats
        this.scoreTracker = new ScoreTracker(this);
        
        // Setup manager classes
        this.connectionManager = new ConnectionManager(this);
        this.packetManager = new PacketManager(null, new ArrayList<>(), 5, this);
        this.collisionSystem = new CollisionSystem(this);
        this.impactEffectSystem = new ImpactEffectSystem(this);
        this.levelProgressTracker = new LevelProgressTracker(this);
        this.powerUpManager = new PowerUpManager(this);
        
        // Get HUD manager instance
        this.hudManager = HUDManager.getInstance();
        
        System.out.println("New GameState created with " + this.scoreTracker.getCoins() + " coins");
    }
    
    // Scene management
    public void setGameScene(com.networkgame.view.GameScene gameScene) {
        this.gameScene = gameScene;
    }
    
    public com.networkgame.view.GameScene getGameScene() {
        return this.gameScene;
    }
    
    /**
     * Main update method called every frame
     */
    public void update(double deltaTime) {
        // Cap deltaTime to prevent large jumps
        double cappedDeltaTime = Math.min(deltaTime, 0.05);
        
        // Clean up expired impact events
        impactEffectSystem.cleanupExpiredImpactEvents();
        
        // Update network systems and process packets in end systems
        updateNetworkSystems(cappedDeltaTime);
        
        // Check packet loss threshold for failure
        if (scoreTracker.getPacketLossPercentage() > 50.0 && levelProgressTracker.getCurrentLevel() > 1) {
            levelProgressTracker.pauseLevel(levelProgressTracker.getCurrentLevel());
            levelProgressTracker.levelIsOver(false);
        }
        
        // Update timers and stats
        levelProgressTracker.updateLevelTimer(cappedDeltaTime);
        scoreTracker.updatePacketLossPercentage();
    }
    
    /**
     * Update all network systems and handle packet delivery
     */
    private void updateNetworkSystems(double deltaTime) {
        for (NetworkSystem system : systems) {
            system.update(deltaTime);
            
            // Only process packets from designated end systems
            if (system.isEndSystem()) {
                processDeliveredPacket(system);
            }
        }
    }
    
    /**
     * Process a delivered packet from an end system
     */
    private void processDeliveredPacket(NetworkSystem system) {
        Packet deliveredPacket = system.processDeliveredPacket();
        if (deliveredPacket == null || 
            (boolean)deliveredPacket.getProperty("counted", false)) {
            return;
        }
        
        // Count this packet as delivered and update game state
        packetManager.incrementPacketsDelivered();
        packetManager.incrementVisualPacketsDelivered();
        deliveredPacket.setReachedEndSystem(true);
        scoreTracker.addCoins(deliveredPacket.getCoinValue());
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.CONNECTION_SUCCESS);
        
        System.out.println("Packet delivered successfully! Total delivered: " + packetManager.getPacketsDelivered());
        
        // Mark packet as counted to prevent double counting
        deliveredPacket.setProperty("counted", true);
        
        // Remove the packet and update UI
        packetManager.getActivePackets().remove(deliveredPacket);
        if (gameScene != null) {
            gameScene.render();
        }
    }
    
    // System and connection management
    public Connection createConnection(Port sourcePort, Port targetPort) {
        return connectionManager.createConnection(sourcePort, targetPort);
    }
    
    public void removeConnection(Connection connection) {
        connectionManager.removeConnection(connection);
    }
    
    public void addSystem(NetworkSystem system) {
        systems.add(system);
    }
    
    public void addActivePacket(Packet packet) {
        packetManager.addActivePacket(packet);
    }
    
    // Collision system management
    public void initializeCollisionSystem() {
        collisionController = new com.networkgame.controller.CollisionController(this);
        collisionController.startCollisionDetection();
    }

    public void pauseCollisionSystem() {
        if (collisionController != null) {
            collisionController.pause();
        }
    }

    public void resumeCollisionSystem() {
        if (collisionController != null) {
            collisionController.resume();
        }
    }

    public void stopCollisionSystem() {
        if (collisionController != null) {
            collisionController.stop();
            collisionController = null;
        }
    }
    
    // Packet management
    public void animatePacketAlongConnection(Packet packet, Connection connection) {
        packetManager.animatePacketAlongConnection(packet, connection);
    }
    
    public boolean safelyRemovePacket(Packet packet, boolean immediate) {
        return packetManager.safelyRemovePacket(packet, immediate);
    }
    
    public boolean safelyRemovePacket(Packet packet) {
        return packetManager.safelyRemovePacket(packet);
    }
    
    // Level management
    public void setLevelManager(LevelManager levelManager) {
        this.levelManager = levelManager;
    }
    
    public LevelManager getLevelManager() {
        return this.levelManager;
    }
    
    public void setCurrentLevel(int level) {
        levelProgressTracker.setCurrentLevel(level);
        
        // Update feature flags from level data
        if (levelManager != null) {
            LevelManager.Level currentLevelData = levelManager.getLevel(level);
            if (currentLevelData != null) {
                collisionSystem.setAtar(currentLevelData.isAtarEnabled());
            }
        }
    }
    
    /**
     * Update capacity displays for all systems
     */
    public void updateCapacityUsed() {
        // Update capacity visuals for all systems
        for (NetworkSystem system : systems) {
            if (!system.isStartSystem() && !system.isEndSystem()) {
                system.setActive(true);
                system.updateCapacityVisual();
                
                System.out.println("System " + system.getLabel() + ": Current capacity: " + 
                                  system.getCurrentCapacityUsed() + "/" + 5);
            }
        }
        
        // Update the UI if gameScene is available
        if (gameScene != null) {
            javafx.application.Platform.runLater(() -> {
                gameScene.updateCapacityLabels();
                gameScene.render();
            });
        }
    }
    
    /**
     * Reset the game state while preserving progress data
     */
    public void reset() {
        // Stop packet generation in all systems
        for (NetworkSystem system : systems) {
            if (system.isStartSystem()) {
                system.stopSendingPackets();
            }
        }
        
        // Store current coins, score, and level before reset
        int currentCoins = this.scoreTracker.getCoins();
        int currentLevelNumber = this.levelProgressTracker.getCurrentLevel();
        
        // Clear all collections
        this.systems.clear();
        
        // Reset all manager classes
        packetManager.reset();
        connectionManager.reset();
        collisionSystem.reset();
        impactEffectSystem.reset();
        levelProgressTracker.reset();
        scoreTracker.reset();
        
        // Restore important values
        levelProgressTracker.setCurrentLevel(currentLevelNumber);
        scoreTracker.setCoins(currentCoins);
        
        // Reset feature flags to level-specific defaults
        if (levelManager != null) {
            LevelManager.Level currentLevelData = levelManager.getLevel(currentLevelNumber);
            if (currentLevelData != null) {
                collisionSystem.setAtar(currentLevelData.isAtarEnabled());
            }
        }
        
        // Force UI update if gameScene is available
        updateUI();
        
        System.out.println("GameState reset with " + scoreTracker.getCoins() + " coins and " + scoreTracker.getScore() + " score");
    }
    
    /**
     * Update the UI components with current game state
     */
    private void updateUI() {
        if (gameScene != null) {
            javafx.application.Platform.runLater(() -> {
                gameScene.updatePacketLossLabel(scoreTracker.getPacketLossPercentage());
                gameScene.updatePacketsCollectedLabel(packetManager.getPacketsDelivered());
                gameScene.updateTimeProgress(levelProgressTracker.getElapsedTime(), levelProgressTracker.getLevelDuration());
                gameScene.updateCapacityLabels();
                gameScene.render();
            });
        }
    }
    
    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        // Clean up packets
        packetManager.cleanup();
        
        // Clean up network systems
        for (NetworkSystem system : new ArrayList<>(systems)) {
            system.cleanup();
        }
        
        // Stop collision system if running
        if (collisionController != null) {
            collisionController.cleanup();
        }
        
        System.out.println("GameState resources cleaned up");
    }
    
    // PowerUp functions
    public void disableImpactEffect(int seconds) {
        powerUpManager.purchaseDisableImpactEffect(seconds);
    }
    
    public void disableCollision(int seconds) {
        powerUpManager.purchaseDisableCollision(seconds);
    }
    
    public void resetAllPacketNoise() {
        powerUpManager.purchaseResetAllPacketNoise();
    }
    
    public void resetAllConnectionStyling() {
        connectionManager.resetAllConnectionStyling();
    }
    
    // Timer controls
    public void startTimer() {
        levelProgressTracker.startTimer();
    }
    
    public void stopTimer() {
        levelProgressTracker.stopTimer();
    }
    
    public void levelIsOver(boolean success) {
        levelProgressTracker.levelIsOver(success);
    }
    
    public void pauseLevel(int level) {
        levelProgressTracker.pauseLevel(level);
    }
    
    // Feature flags
    public boolean isAiryaman() {
        return collisionSystem.isAiryaman();
    }
    
    public void setAiryaman(boolean airyaman) {
        collisionSystem.setAiryaman(airyaman);
    }
    
    public boolean isAtar() {
        return collisionSystem.isAtar();
    }
    
    public void setAtar(boolean atar) {
        collisionSystem.setAtar(atar);
    }
    
    public boolean isAnahita() {
        return powerUpManager.isAnahita();
    }
    
    public void setAnahita(boolean anahita) {
        powerUpManager.setAnahita(anahita);
    }
    
    // Temporal progress
    public boolean isTemporalProgressEnabled() {
        return temporalProgressEnabled;
    }
    
    public void setTemporalProgressEnabled(boolean enabled) {
        this.temporalProgressEnabled = enabled;
    }
    
    public double getTemporalProgress() {
        return temporalProgress;
    }
    
    public void setTemporalProgress(double progress) {
        this.temporalProgress = Math.max(0, Math.min(1, progress));
    }
    
    // Utility methods for impact system
    public void emitImpactEvent(javafx.geometry.Point2D position) {
        impactEffectSystem.emitImpactEvent(position);
    }
    
    public void incrementTotalPackets() {
        packetManager.incrementTotalPackets();
    }
    
    public void incrementLostPackets() {
        packetManager.incrementLostPackets();
    }
    
    public void updatePacketAnimations(double deltaTime) {
        packetManager.updatePacketAnimations(deltaTime);
    }
    
    // Getters and setters
    
    // Resource getters
    public List<NetworkSystem> getSystems() {
        return systems;
    }
    
    public List<Connection> getConnections() {
        return connectionManager.getConnections();
    }
    
    public List<Packet> getActivePackets() {
        return packetManager.getActivePackets();
    }
    
    public List<Point2D> getImpactEventPositions() {
        return impactEffectSystem.getImpactEventPositions();
    }
    
    // Manager getters
    public PacketManager getPacketManager() {
        return packetManager;
    }
    
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    public CollisionSystem getCollisionSystem() {
        return collisionSystem;
    }
    
    public ImpactEffectSystem getImpactEffectSystem() {
        return impactEffectSystem;
    }
    
    public LevelProgressTracker getLevelProgressTracker() {
        return levelProgressTracker;
    }
    
    public ScoreTracker getScoreTracker() {
        return scoreTracker;
    }
    
    public PowerUpManager getPowerUpManager() {
        return powerUpManager;
    }
    
    // Score and currency
    public int getCoins() {
        return scoreTracker.getCoins();
    }
    
    public void addCoins(int amount) {
        scoreTracker.addCoins(amount);
    }
    
    public void spendCoins(int amount) {
        scoreTracker.spendCoins(amount);
    }
    
    public void setCoins(int coins) {
        scoreTracker.setCoins(coins);
    }
    
    // Wire and connection management
    public double getRemainingWireLength() {
        return connectionManager.getRemainingWireLength();
    }
    
    public double getTotalWireUsed() {
        return connectionManager.getTotalWireUsed();
    }
    
    public void setTotalWireUsed(double totalWireUsed) {
        connectionManager.setTotalWireUsed(totalWireUsed);
    }
    
    public void updateRemainingWireLength() {
        connectionManager.updateRemainingWireLength();
    }
    
    public void setWireLength(double wireLength) {
        connectionManager.setWireLength(wireLength);
    }
    
    // Level progression
    public int getCurrentLevel() {
        return levelProgressTracker.getCurrentLevel();
    }
    
    public int getLevelRequiredPackets() {
        return levelProgressTracker.getLevelRequiredPackets();
    }
    
    public boolean isLevelCompleted() {
        return levelProgressTracker.isLevelCompleted();
    }
    
    public void setLevelCompleted(boolean completed) {
        levelProgressTracker.setLevelCompleted(completed);
    }
    
    public boolean isGameOver() {
        return levelProgressTracker.isGameOver();
    }
    
    public void setGameOver(boolean gameOver) {
        levelProgressTracker.setGameOver(gameOver);
    }
    
    public boolean isLevelFailed() {
        return levelProgressTracker.isLevelFailed();
    }
    
    public double getElapsedTime() {
        return levelProgressTracker.getElapsedTime();
    }
    
    public void setElapsedTime(double time) {
        levelProgressTracker.setElapsedTime(time);
    }
    
    public int getLevelDuration() {
        return levelProgressTracker.getLevelDuration();
    }
    
    // Packet statistics
    public int getPacketsDelivered() {
        return packetManager.getPacketsDelivered();
    }
    
    public double getPacketLossPercentage() {
        return scoreTracker.getPacketLossPercentage();
    }
    
    public void updatePacketLossPercentage() {
        scoreTracker.updatePacketLossPercentage();
    }
} 
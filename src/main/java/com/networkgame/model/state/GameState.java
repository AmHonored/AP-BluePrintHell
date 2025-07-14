package com.networkgame.model.state; 

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.manager.ManagerRegistry;
import com.networkgame.model.manager.PacketManager;
import com.networkgame.model.manager.ConnectionManager;
import com.networkgame.model.manager.PowerUpManager;
import com.networkgame.model.manager.LevelManager;
import com.networkgame.model.manager.HUDManager;
import com.networkgame.model.manager.WireCollisionManager;
import com.networkgame.model.system.CollisionSystem;
import com.networkgame.model.system.ImpactEffectSystem;
import com.networkgame.service.audio.AudioManager;

/**
 * Central class that coordinates all game systems and manages overall game state.
 * Delegates specific functionality to specialized manager classes.
 * Now implements interfaces to break circular dependencies.
 */
public class GameState implements GameStateProvider, GameStateCallbacks {
    // Core game components
    private List<NetworkSystem> systems = new ArrayList<>();
    private UIUpdateListener uiUpdateListener; // Replaces direct GameScene reference
    private com.networkgame.controller.CollisionController collisionController;
    
    // Manager registry to break cyclic dependencies
    private final ManagerRegistry managerRegistry;
    
    // Temporal progress fields
    private boolean temporalProgressEnabled = false;
    private double temporalProgress = 0.0;
    
    public GameState() {
        // Initialize manager registry
        this.managerRegistry = new ManagerRegistry();
        
        // Initialize ScoreTracker first so other managers can access coins and stats
        ScoreTracker scoreTracker = new ScoreTracker(this);
        managerRegistry.register(ScoreTracker.class, scoreTracker);
        
        // Setup manager classes using dependency injection
        ConnectionManager connectionManager = new ConnectionManager(this);
        managerRegistry.register(ConnectionManager.class, connectionManager);
        
        PacketManager packetManager = new PacketManager(null, new ArrayList<>(), 5, this);
        managerRegistry.register(PacketManager.class, packetManager);
        
        CollisionSystem collisionSystem = new CollisionSystem(this);
        managerRegistry.register(CollisionSystem.class, collisionSystem);
        
        ImpactEffectSystem impactEffectSystem = new ImpactEffectSystem(this);
        managerRegistry.register(ImpactEffectSystem.class, impactEffectSystem);
        
        LevelProgressTracker levelProgressTracker = new LevelProgressTracker(this);
        managerRegistry.register(LevelProgressTracker.class, levelProgressTracker);
        
        PowerUpManager powerUpManager = new PowerUpManager(this);
        managerRegistry.register(PowerUpManager.class, powerUpManager);
        
        WireCollisionManager wireCollisionManager = new WireCollisionManager(this);
        managerRegistry.register(WireCollisionManager.class, wireCollisionManager);
        
        // Get HUD manager instance
        HUDManager hudManager = HUDManager.getInstance();
        managerRegistry.register(HUDManager.class, hudManager);
    }
    
    // UI listener management
    public void setUIUpdateListener(UIUpdateListener uiUpdateListener) {
        this.uiUpdateListener = uiUpdateListener;
    }
    
    public UIUpdateListener getUIUpdateListener() {
        return this.uiUpdateListener;
    }
    
    /**
     * Main update method called every frame
     */
    public void update(double deltaTime) {
        // Cap deltaTime to prevent large jumps
        double cappedDeltaTime = Math.min(deltaTime, 0.05);
        
        // Clean up expired impact events
        ImpactEffectSystem impactEffectSystem = managerRegistry.get(ImpactEffectSystem.class);
        if (impactEffectSystem != null) {
            impactEffectSystem.cleanupExpiredImpactEvents();
        }
        
        // Update network systems and process packets in end systems
        updateNetworkSystems(cappedDeltaTime);
        
        // Check packet loss threshold for failure
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (scoreTracker != null && levelProgressTracker != null && 
            scoreTracker.getPacketLossPercentage() > 50.0 && levelProgressTracker.getCurrentLevel() > 1) {
            levelProgressTracker.pauseLevel(levelProgressTracker.getCurrentLevel());
            levelProgressTracker.levelIsOver(false);
        }
        
        // Update timers and stats
        if (levelProgressTracker != null) {
            levelProgressTracker.updateLevelTimer(cappedDeltaTime);
        }
        if (scoreTracker != null) {
            scoreTracker.updatePacketLossPercentage();
        }
        
        // Wire collision checking is now handled when connections are created/modified
        // No need to check every frame
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
        
        // Failsafe: Process any packets that reached end systems but haven't been counted
        processUnprocessedPackets();
    }
    
    /**
     * Failsafe to process packets that reached end systems but haven't been counted yet
     */
    private void processUnprocessedPackets() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            for (Packet packet : new ArrayList<>(packetManager.getActivePackets())) {
                if (packet.hasReachedEndSystem() && !packet.hasProperty("counted")) {
                    System.out.println("[GameState] Failsafe: Processing unprocessed packet " + packet.getId());
                    
                    // Find an end system to process this packet
                    for (NetworkSystem system : systems) {
                        if (system.isEndSystem()) {
                            System.out.println("[GameState] Failsafe: Sending packet " + packet.getId() + " to end system for processing");
                            system.receivePacket(packet);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Process a delivered packet from an end system
     */
    private void processDeliveredPacket(NetworkSystem system) {
        Packet deliveredPacket = system.processDeliveredPacket();
        if (deliveredPacket == null) {
            return;
        }
        
        if ((boolean)deliveredPacket.getProperty("counted", false)) {
            System.out.println("[GameState] processDeliveredPacket: Packet " + deliveredPacket.getId() + " already counted, skipping");
            return;
        }
        
        System.out.println("[GameState] Processing delivered packet " + deliveredPacket.getId() + 
                         " (type: " + deliveredPacket.getType() + ")");
        
        // Count this packet as delivered and update game state
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        
        if (packetManager != null) {
            packetManager.incrementPacketsDelivered();
            packetManager.incrementVisualPacketsDelivered();
        }
        
        deliveredPacket.setReachedEndSystem(true);
        
        if (scoreTracker != null) {
            scoreTracker.addCoins(deliveredPacket.getCoinValue());
        }
        
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.CONNECTION_SUCCESS);
        
        // Mark packet as counted to prevent double counting
        deliveredPacket.setProperty("counted", true);
        
        // Remove the packet and update UI
        boolean removed = packetManager != null ? packetManager.getActivePackets().remove(deliveredPacket) : false;
        System.out.println("[GameState] Removed delivered packet from activePackets: " + removed);
        
        if (uiUpdateListener != null) {
            System.out.println("[GameState] Triggering UI render after packet delivery");
            uiUpdateListener.render();
        } else {
            System.out.println("[GameState] No UI update listener available for render after packet delivery");
        }
    }
    
    // System and connection management
    public Connection createConnection(Port sourcePort, Port targetPort) {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        return connectionManager != null ? connectionManager.createConnection(sourcePort, targetPort) : null;
    }
    
    public void removeConnection(Connection connection) {
        WireCollisionManager wireCollisionManager = managerRegistry.get(WireCollisionManager.class);
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        
        if (wireCollisionManager != null) {
            wireCollisionManager.cleanupConnection(connection);
        }
        if (connectionManager != null) {
            connectionManager.removeConnection(connection);
        }
    }
    
    public void addSystem(NetworkSystem system) {
        systems.add(system);
    }
    
    public void addActivePacket(Packet packet) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.addActivePacket(packet);
        }
    }
    
    // Collision system management
    public void initializeCollisionSystem() {
        System.out.println("DEBUG: GameState.initializeCollisionSystem() called");
        collisionController = new com.networkgame.controller.CollisionController(this);
        System.out.println("DEBUG: CollisionController created");
        collisionController.startCollisionDetection();
        System.out.println("DEBUG: CollisionController.startCollisionDetection() called");
    }

    public void pauseCollisionSystem() {
        System.out.println("DEBUG: GameState.pauseCollisionSystem() called");
        if (collisionController != null) {
            collisionController.pause();
        } else {
            System.out.println("DEBUG: collisionController is null in pauseCollisionSystem()");
        }
    }

    public void resumeCollisionSystem() {
        System.out.println("DEBUG: GameState.resumeCollisionSystem() called");
        if (collisionController != null) {
            collisionController.resume();
        } else {
            System.out.println("DEBUG: collisionController is null in resumeCollisionSystem()");
        }
    }



    public void stopCollisionSystem() {
        System.out.println("DEBUG: GameState.stopCollisionSystem() called");
        if (collisionController != null) {
            collisionController.stop();
            collisionController = null;
            System.out.println("DEBUG: CollisionController stopped and set to null");
        } else {
            System.out.println("DEBUG: collisionController is null in stopCollisionSystem()");
        }
    }
    
    // Packet management
    public void animatePacketAlongConnection(Packet packet, Connection connection) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.animatePacketAlongConnection(packet, connection);
        }
    }
    
    public boolean safelyRemovePacket(Packet packet, boolean immediate) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.safelyRemovePacket(packet, immediate) : false;
    }
    
    public boolean safelyRemovePacket(Packet packet) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.safelyRemovePacket(packet) : false;
    }
    
    // Level management
    public void setLevelManager(LevelManager levelManager) {
        managerRegistry.register(LevelManager.class, levelManager);
    }
    
    public LevelManager getLevelManager() {
        return managerRegistry.get(LevelManager.class);
    }
    
    public void setCurrentLevel(int level) {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.setCurrentLevel(level);
        }
        
        // Update feature flags from level data
        LevelManager levelManager = managerRegistry.get(LevelManager.class);
        CollisionSystem collisionSystem = managerRegistry.get(CollisionSystem.class);
        if (levelManager != null && collisionSystem != null) {
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
            }
        }
        
        // Update the UI if uiUpdateListener is available
        if (uiUpdateListener != null) {
            javafx.application.Platform.runLater(() -> {
                uiUpdateListener.updateCapacityLabels();
                uiUpdateListener.render();
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
        
        // Get managers from registry
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        CollisionSystem collisionSystem = managerRegistry.get(CollisionSystem.class);
        ImpactEffectSystem impactEffectSystem = managerRegistry.get(ImpactEffectSystem.class);
        WireCollisionManager wireCollisionManager = managerRegistry.get(WireCollisionManager.class);
        LevelManager levelManager = managerRegistry.get(LevelManager.class);
        
        // Store current coins, score, and level before reset
        int currentCoins = scoreTracker != null ? scoreTracker.getCoins() : 0;
        int currentLevelNumber = levelProgressTracker != null ? levelProgressTracker.getCurrentLevel() : 1;
        
        // Clear all collections
        this.systems.clear();
        
        // Reset all manager classes
        if (packetManager != null) packetManager.reset();
        if (connectionManager != null) connectionManager.reset();
        if (collisionSystem != null) collisionSystem.reset();
        if (impactEffectSystem != null) impactEffectSystem.reset();
        if (levelProgressTracker != null) levelProgressTracker.reset();
        if (scoreTracker != null) scoreTracker.reset();
        
        // Clean up wire collision manager
        if (connectionManager != null && wireCollisionManager != null) {
            for (Connection connection : connectionManager.getConnections()) {
                wireCollisionManager.cleanupConnection(connection);
            }
        }
        
        // Restore important values
        if (levelProgressTracker != null) levelProgressTracker.setCurrentLevel(currentLevelNumber);
        if (scoreTracker != null) scoreTracker.setCoins(currentCoins);
        
        // Reset feature flags to level-specific defaults
        if (levelManager != null && collisionSystem != null) {
            LevelManager.Level currentLevelData = levelManager.getLevel(currentLevelNumber);
            if (currentLevelData != null) {
                collisionSystem.setAtar(currentLevelData.isAtarEnabled());
            }
        }
        
        // Force UI update if gameScene is available
        updateUI();
    }
    
    /**
     * Update the UI components with current game state
     */
    private void updateUI() {
        if (uiUpdateListener != null) {
            ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
            PacketManager packetManager = managerRegistry.get(PacketManager.class);
            LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
            
            javafx.application.Platform.runLater(() -> {
                if (scoreTracker != null) {
                    uiUpdateListener.updatePacketLossLabel(scoreTracker.getPacketLossPercentage());
                }
                if (packetManager != null) {
                    uiUpdateListener.updatePacketsCollectedLabel(packetManager.getPacketsDelivered());
                }
                if (levelProgressTracker != null) {
                    uiUpdateListener.updateTimeProgress(levelProgressTracker.getElapsedTime(), levelProgressTracker.getLevelDuration());
                }
                uiUpdateListener.updateCapacityLabels();
                uiUpdateListener.render();
            });
        }
    }
    
    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        // Clean up packets
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.cleanup();
        }
        
        // Clean up network systems
        for (NetworkSystem system : new ArrayList<>(systems)) {
            system.cleanup();
        }
        
        // Stop collision system if running
        if (collisionController != null) {
            collisionController.cleanup();
        }
    }
    
    // PowerUp functions
    public void disableImpactEffect(int seconds) {
        PowerUpManager powerUpManager = managerRegistry.get(PowerUpManager.class);
        if (powerUpManager != null) {
            powerUpManager.purchaseDisableImpactEffect(seconds);
        }
    }
    
    public void disableCollision(int seconds) {
        PowerUpManager powerUpManager = managerRegistry.get(PowerUpManager.class);
        if (powerUpManager != null) {
            powerUpManager.purchaseDisableCollision(seconds);
        }
    }
    
    public void resetAllPacketNoise() {
        PowerUpManager powerUpManager = managerRegistry.get(PowerUpManager.class);
        if (powerUpManager != null) {
            powerUpManager.purchaseResetAllPacketNoise();
        }
    }
    
    public void resetAllConnectionStyling() {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        if (connectionManager != null) {
            connectionManager.resetAllConnectionStyling();
        }
    }
    
    // Timer controls
    public void startTimer() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.startTimer();
        }
    }
    
    public void stopTimer() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.stopTimer();
        }
    }
    
    public void levelIsOver(boolean success) {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.levelIsOver(success);
        }
    }
    
    public void pauseLevel(int level) {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.pauseLevel(level);
        }
    }
    
    // Feature flags
    public boolean isAiryaman() {
        CollisionSystem collisionSystem = managerRegistry.get(CollisionSystem.class);
        return collisionSystem != null ? collisionSystem.isAiryaman() : false;
    }
    
    public void setAiryaman(boolean airyaman) {
        CollisionSystem collisionSystem = managerRegistry.get(CollisionSystem.class);
        if (collisionSystem != null) {
            collisionSystem.setAiryaman(airyaman);
        }
    }
    
    public boolean isAtar() {
        CollisionSystem collisionSystem = managerRegistry.get(CollisionSystem.class);
        return collisionSystem != null ? collisionSystem.isAtar() : false;
    }
    
    public void setAtar(boolean atar) {
        CollisionSystem collisionSystem = managerRegistry.get(CollisionSystem.class);
        if (collisionSystem != null) {
            collisionSystem.setAtar(atar);
        }
    }
    
    public boolean isAnahita() {
        PowerUpManager powerUpManager = managerRegistry.get(PowerUpManager.class);
        return powerUpManager != null ? powerUpManager.isAnahita() : false;
    }
    
    public void setAnahita(boolean anahita) {
        PowerUpManager powerUpManager = managerRegistry.get(PowerUpManager.class);
        if (powerUpManager != null) {
            powerUpManager.setAnahita(anahita);
        }
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
        this.temporalProgress = progress;
    }
    
    // Impact events
    public void emitImpactEvent(javafx.geometry.Point2D position) {
        ImpactEffectSystem impactEffectSystem = managerRegistry.get(ImpactEffectSystem.class);
        if (impactEffectSystem != null) {
            impactEffectSystem.emitImpactEvent(position);
        }
    }
    
    public void incrementTotalPackets() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.incrementTotalPackets();
        }
    }
    
    public void incrementLostPackets() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.incrementLostPackets();
        }
    }
    
    public void updatePacketAnimations(double deltaTime) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.updatePacketAnimations(deltaTime);
        }
    }
    
    // Getters for systems and data
    public List<NetworkSystem> getSystems() {
        return systems;
    }
    
    public List<Connection> getConnections() {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        return connectionManager != null ? connectionManager.getConnections() : new ArrayList<>();
    }
    
    public List<Packet> getActivePackets() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.getActivePackets() : new ArrayList<>();
    }
    
    public List<Point2D> getImpactEventPositions() {
        ImpactEffectSystem impactEffectSystem = managerRegistry.get(ImpactEffectSystem.class);
        return impactEffectSystem != null ? impactEffectSystem.getImpactEventPositions() : new ArrayList<>();
    }
    
    // Manager getters
    public PacketManager getPacketManager() {
        return managerRegistry.get(PacketManager.class);
    }
    
    public ConnectionManager getConnectionManager() {
        return managerRegistry.get(ConnectionManager.class);
    }
    
    public CollisionSystem getCollisionSystem() {
        return managerRegistry.get(CollisionSystem.class);
    }
    
    public ImpactEffectSystem getImpactEffectSystem() {
        return managerRegistry.get(ImpactEffectSystem.class);
    }
    
    public LevelProgressTracker getLevelProgressTracker() {
        return managerRegistry.get(LevelProgressTracker.class);
    }
    
    public ScoreTracker getScoreTracker() {
        return managerRegistry.get(ScoreTracker.class);
    }
    
    public PowerUpManager getPowerUpManager() {
        return managerRegistry.get(PowerUpManager.class);
    }
    
    // Game state properties
    public int getCoins() {
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        return scoreTracker != null ? scoreTracker.getCoins() : 0;
    }
    
    public void addCoins(int amount) {
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        if (scoreTracker != null) {
            scoreTracker.addCoins(amount);
        }
        
        // Update the UI
        if (uiUpdateListener != null) {
            javafx.application.Platform.runLater(() -> {
                uiUpdateListener.render();
            });
        }
    }
    
    public void spendCoins(int amount) {
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        if (scoreTracker != null) {
            scoreTracker.spendCoins(amount);
        }
    }
    
    public void setCoins(int coins) {
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        if (scoreTracker != null) {
            scoreTracker.setCoins(coins);
        }
    }
    
    // Wire management
    public double getRemainingWireLength() {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        return connectionManager != null ? connectionManager.getRemainingWireLength() : 0.0;
    }
    
    public double getTotalWireUsed() {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        return connectionManager != null ? connectionManager.getTotalWireUsed() : 0.0;
    }
    
    public void setTotalWireUsed(double totalWireUsed) {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        if (connectionManager != null) {
            connectionManager.setTotalWireUsed(totalWireUsed);
        }
    }
    
    public void updateRemainingWireLength() {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        if (connectionManager != null) {
            connectionManager.updateRemainingWireLength();
        }
    }
    
    public void setWireLength(double wireLength) {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        if (connectionManager != null) {
            connectionManager.setWireLength(wireLength);
        }
    }
    
    // Level properties
    public int getCurrentLevel() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.getCurrentLevel() : 1;
    }
    
    public int getLevelRequiredPackets() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.getLevelRequiredPackets() : 0;
    }
    
    public boolean isLevelCompleted() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.isLevelCompleted() : false;
    }
    
    public void setLevelCompleted(boolean completed) {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.setLevelCompleted(completed);
        }
    }
    
    public boolean isGameOver() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.isGameOver() : false;
    }
    
    public void setGameOver(boolean gameOver) {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.setGameOver(gameOver);
        }
    }
    
    public boolean isLevelFailed() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.isLevelFailed() : false;
    }
    
    public double getElapsedTime() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.getElapsedTime() : 0.0;
    }
    
    public void setElapsedTime(double time) {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        if (levelProgressTracker != null) {
            levelProgressTracker.setElapsedTime(time);
        }
    }
    
    public int getLevelDuration() {
        LevelProgressTracker levelProgressTracker = managerRegistry.get(LevelProgressTracker.class);
        return levelProgressTracker != null ? levelProgressTracker.getLevelDuration() : 0;
    }
    
    // Packet statistics
    public int getPacketsDelivered() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        return packetManager != null ? packetManager.getPacketsDelivered() : 0;
    }
    
    public double getPacketLossPercentage() {
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        return scoreTracker != null ? scoreTracker.getPacketLossPercentage() : 0.0;
    }
    
    public void updatePacketLossPercentage() {
        ScoreTracker scoreTracker = managerRegistry.get(ScoreTracker.class);
        if (scoreTracker != null) {
            scoreTracker.updatePacketLossPercentage();
        }
    }
    
    public HUDManager getHudManager() {
        return managerRegistry.get(HUDManager.class);
    }
    
    public WireCollisionManager getWireCollisionManager() {
        return managerRegistry.get(WireCollisionManager.class);
    }
    
    // GameStateCallbacks implementation
    @Override
    public void removeActivePacket(Packet packet) {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.getActivePackets().remove(packet);
        }
    }
    
    @Override
    public void incrementPacketsDelivered() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.incrementPacketsDelivered();
        }
    }
    
    @Override
    public void incrementVisualPacketsDelivered() {
        PacketManager packetManager = managerRegistry.get(PacketManager.class);
        if (packetManager != null) {
            packetManager.incrementVisualPacketsDelivered();
        }
    }
    
    @Override
    public void addConnection(Connection connection) {
        ConnectionManager connectionManager = managerRegistry.get(ConnectionManager.class);
        if (connectionManager != null) {
            connectionManager.getConnections().add(connection);
        }
    }
    
    @Override
    public void removeSystem(NetworkSystem system) {
        systems.remove(system);
    }
} 

package com.networkgame.model.manager; 

import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.Map;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.level.Level1;
import com.networkgame.model.level.Level2;
import com.networkgame.model.level.HexagonTestLevel;

public class LevelManager {
    private Map<Integer, Level> levels;
    private int maxUnlockedLevel;
    private GameState gameState;
    
    public LevelManager(GameState gameState) {
        this.levels = new HashMap<>();
        this.maxUnlockedLevel = 3; // Unlock Level 3 by default for debugging
        this.gameState = gameState;
        
        // Set this LevelManager in the GameState
        if (gameState != null) {
            gameState.setLevelManager(this);
        }
        
        initializeLevels();
    }
    
    private void initializeLevels() {
        // Create level 1 using the Level1 class
        Level level1 = Level1.createLevel(gameState);
        
        // Add level to levels map
        levels.put(1, level1);
        
        // Create level 2 using the Level2 class
        Level level2 = Level2.createLevel(gameState);
        
        // Add level to levels map
        levels.put(2, level2);
        
        // Create level 3 using the HexagonTestLevel class
        Level level3 = HexagonTestLevel.createLevel(gameState);
        
        // Add level to levels map
        levels.put(3, level3);
    }
    
    public Level getLevel(int levelNumber) {
        return levels.get(levelNumber);
    }
    
    public int getMaxUnlockedLevel() {
        return maxUnlockedLevel;
    }
    
    public void unlockLevel(int levelNumber) {
        if (levelNumber > maxUnlockedLevel && levelNumber <= levels.size()) {
            maxUnlockedLevel = levelNumber;
        }
    }
    
    public int getTotalLevels() {
        return levels.size();
    }
    
    public static class Level {
        private int levelNumber;
        private String name;
        private double wireLength;
        private double packetSpawnInterval;
        private java.util.List<NetworkSystem> systems;
        private boolean atarEnabled = false; // Flag to disable impact waves
        private double collisionThreshold = 1.0; // Default collision threshold
        private int levelDuration = 60; // Default level duration in seconds
        
        public Level(int levelNumber, String name) {
            this.levelNumber = levelNumber;
            this.name = name;
            this.systems = new java.util.ArrayList<>();
        }
        
        public int getLevelNumber() {
            return levelNumber;
        }
        
        public String getName() {
            return name;
        }
        
        public double getWireLength() {
            return wireLength;
        }
        
        public void setWireLength(double wireLength) {
            this.wireLength = wireLength;
        }
        
        public double getPacketSpawnInterval() {
            return packetSpawnInterval;
        }
        
        public void setPacketSpawnInterval(double packetSpawnInterval) {
            this.packetSpawnInterval = packetSpawnInterval;
        }
        
        public java.util.List<NetworkSystem> getSystems() {
            return systems;
        }
        
        public void addSystem(NetworkSystem system) {
            systems.add(system);
        }
        
        /**
         * Set the atar flag to disable impact waves
         * @param enabled true to disable impact waves, false to enable them
         */
        public void setAtarEnabled(boolean enabled) {
            this.atarEnabled = enabled;
        }
        
        /**
         * Check if impact waves are disabled for this level
         * @return true if impact waves are disabled
         */
        public boolean isAtarEnabled() {
            return atarEnabled;
        }
        
        /**
         * Set the collision threshold for this level
         * Higher values make collisions more likely
         * @param threshold the collision threshold value
         */
        public void setCollisionThreshold(double threshold) {
            this.collisionThreshold = threshold;
        }
        
        /**
         * Get the collision threshold for this level
         * @return the collision threshold value
         */
        public double getCollisionThreshold() {
            return collisionThreshold;
        }
        
        /**
         * Set the duration of this level in seconds
         * @param duration the level duration in seconds
         */
        public void setLevelDuration(int duration) {
            this.levelDuration = duration;
        }
        
        /**
         * Get the duration of this level in seconds
         * @return the level duration in seconds
         */
        public int getLevelDuration() {
            return levelDuration;
        }
    }
} 

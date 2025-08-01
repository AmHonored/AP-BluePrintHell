package model.levels;

import java.util.ArrayList;
import java.util.List;
import model.entity.systems.System;
import model.entity.packets.Packet;
import model.logic.state.GameState;
import model.logic.state.LevelState;

public class Level {
    private final GameState gameState;
    private final LevelState levelState;
    private final List<System> systems;
    private final List<Packet> packets;

    public Level(int wireLength) {
        this.gameState = new GameState();
        this.levelState = new LevelState(wireLength);
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
    }

    // Delegate to GameState
    public int getCurrentTime() {
        return gameState.getCurrentTime();
    }

    public void setCurrentTime(int currentTime) {
        gameState.setCurrentTime(currentTime);
    }

    public int getCoins() {
        return gameState.getCoins();
    }

    public void addCoins(int amount) {
        gameState.addCoins(amount);
    }

    public boolean isPaused() {
        return gameState.isPaused();
    }

    public void setPaused(boolean paused) {
        gameState.setPaused(paused);
    }

    public boolean isGameOver() {
        return gameState.isGameOver();
    }

    public void setGameOver(boolean gameOver) {
        gameState.setGameOver(gameOver);
    }

    public boolean getGameOverFlag() {
        return gameState.isGameOver();
    }

    public boolean isGameStarted() {
        return gameState.isGameStarted();
    }

    public void setGameStarted(boolean gameStarted) {
        gameState.setGameStarted(gameStarted);
    }

    // Delegate to LevelState
    public int getWireLength() {
        return levelState.getWireLength();
    }

    public int getPacketsGenerated() {
        return levelState.getPacketsGenerated();
    }

    public void incrementPacketsGenerated() {
        levelState.incrementPacketsGenerated();
    }

    public int getPacketLoss() {
        return levelState.getPacketLoss();
    }

    public void incrementPacketLoss() {
        levelState.incrementPacketLoss();
    }

    public int getPacketsCollected() {
        return levelState.getPacketsCollected();
    }

    public void incrementPacketsCollected() {
        levelState.incrementPacketsCollected();
    }

    public double getRemainingWireLength() {
        return levelState.getRemainingWireLength();
    }

    public void subtractWireLength(double length) {
        levelState.subtractWireLength(length);
    }

    public void addWireLength(double length) {
        levelState.addWireLength(length);
    }

    public void setImpactDisabled(boolean disabled) {
        levelState.setImpactDisabled(disabled);
    }

    public boolean isImpactDisabled() {
        return levelState.isImpactDisabled();
    }

    public void setCollisionsDisabled(boolean disabled) {
        levelState.setCollisionsDisabled(disabled);
    }

    public boolean isCollisionsDisabled() {
        return levelState.isCollisionsDisabled();
    }

    // Container methods for systems and packets
    public List<System> getSystems() {
        return systems;
    }

    public void addSystem(System s) {
        systems.add(s);
        
        // Set level reference for spy systems so they can find other spy systems
        if (s instanceof model.entity.systems.SpySystem) {
            ((model.entity.systems.SpySystem) s).setLevel(this);
        }
    }

    public List<Packet> getPackets() {
        return packets;
    }

    public void addPacket(Packet p) {
        packets.add(p);
    }

    public void removePacket(Packet p) {
        packets.remove(p);
    }

    // Getter methods for state objects (for advanced usage)
    public GameState getGameState() {
        return gameState;
    }

    public LevelState getLevelState() {
        return levelState;
    }
} 
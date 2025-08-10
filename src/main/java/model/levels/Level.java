package model.levels;

import java.util.ArrayList;
import java.util.List;
import model.entity.systems.System;
import model.entity.packets.Packet;
import model.logic.state.GameState;
import model.logic.state.LevelState;
import model.logic.Shop.Aergia;
import model.logic.Shop.Eliphas;

public class Level {
    private final GameState gameState;
    private final LevelState levelState;
    private final List<System> systems;
    private final List<Packet> packets;

    private int aergiaScrolls = 0;
    private long aergiaCooldownEnd = 0L;
    private java.util.List<Aergia.AergiaMark> aergiaMarks = new java.util.ArrayList<>();
    
    private int sisyphusScrolls = 0;

    private int eliphasScrolls = 0;
    private java.util.List<Eliphas.EliphasMark> eliphasMarks = new java.util.ArrayList<>();

    public Level(int wireLength) {
        this.gameState = new GameState();
        this.levelState = new LevelState(wireLength);
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
    }

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

    public boolean isGameStarted() {
        return gameState.isGameStarted();
    }

    public void setGameStarted(boolean gameStarted) {
        gameState.setGameStarted(gameStarted);
    }

    public boolean isLevelCompleted() {
        return gameState.isLevelCompleted();
    }

    public void setLevelCompleted(boolean levelCompleted) {
        gameState.setLevelCompleted(levelCompleted);
    }

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

    public List<System> getSystems() {
        return systems;
    }

    public void addSystem(System s) {
        systems.add(s);
        
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

    public GameState getGameState() {
        return gameState;
    }

    public LevelState getLevelState() {
        return levelState;
    }

    // === Aergia marks ===
    public int getAergiaScrolls() { return aergiaScrolls; }
    public void addAergiaScrolls(int delta) { aergiaScrolls = Math.max(0, aergiaScrolls + delta); }
    public boolean isAergiaOnCooldown() { 
        long now = java.lang.System.nanoTime();
        boolean onCooldown = now < aergiaCooldownEnd;
        if (Math.random() < 0.01) {
            java.lang.System.out.println("DEBUG: isAergiaOnCooldown - now: " + now + ", cooldownEnd: " + aergiaCooldownEnd + 
                ", onCooldown: " + onCooldown + ", remaining: " + Math.max(0, (aergiaCooldownEnd - now) / 1_000_000_000.0) + "s");
        }
        return onCooldown;
    }
    public long getAergiaCooldownEnd() { return aergiaCooldownEnd; }
    public void setAergiaCooldownEnd(long nanoTime) { this.aergiaCooldownEnd = nanoTime; }
    public java.util.List<Aergia.AergiaMark> getAergiaMarks() { return aergiaMarks; }
    public void setAergiaMarks(java.util.List<Aergia.AergiaMark> marks) { this.aergiaMarks = marks; }
    
    // === Sisyphus ===
    public int getSisyphusScrolls() { return sisyphusScrolls; }
    public void addSisyphusScrolls(int delta) { sisyphusScrolls = Math.max(0, sisyphusScrolls + delta); }

    // === Eliphas ===
    public int getEliphasScrolls() { return eliphasScrolls; }
    public void addEliphasScrolls(int delta) { eliphasScrolls = Math.max(0, eliphasScrolls + delta); }
    public java.util.List<Eliphas.EliphasMark> getEliphasMarks() { return eliphasMarks; }
    public void setEliphasMarks(java.util.List<Eliphas.EliphasMark> marks) { this.eliphasMarks = marks; }
} 
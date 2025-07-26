package model.levels;

import java.util.ArrayList;
import java.util.List;
import model.entity.systems.System;
import model.entity.packets.Packet;

public class Level {
    private final int wireLength;
    private int currentTime;
    private int packetsGenerated;
    private int packetLoss;
    private int packetsCollected; // Track packets actually collected by end systems
    private int coins;
    private final List<System> systems;
    private final List<Packet> packets;
    private boolean paused = false;
    private boolean gameOver = false;
    private boolean gameStarted = false; // Track if game has been started by play button
    private double remainingWireLength;
    private boolean impactDisabled = false;
    private boolean collisionsDisabled = false;

    public Level(int wireLength) {
        this.wireLength = wireLength;
        this.remainingWireLength = wireLength;
        this.currentTime = 0;
        this.packetsGenerated = 0;
        this.packetLoss = 0;
        this.packetsCollected = 0;
        this.coins = 20; // Give player 20 coins for testing
        this.systems = new ArrayList<>();
        this.packets = new ArrayList<>();
    }

    public int getWireLength() {
        return wireLength;
    }
    public int getCurrentTime() {
        return currentTime;
    }
    public void setCurrentTime(int currentTime) {
        this.currentTime = currentTime;
    }
    public int getPacketsGenerated() {
        return packetsGenerated;
    }
    public void incrementPacketsGenerated() {
        this.packetsGenerated++;
    }
    public int getPacketLoss() {
        return packetLoss;
    }
    public void incrementPacketLoss() {
        this.packetLoss++;
    }
    public int getPacketsCollected() {
        return packetsCollected;
    }
    public void incrementPacketsCollected() {
        this.packetsCollected++;
    }
    public int getCoins() {
        return coins;
    }
    public void addCoins(int amount) {
        this.coins += amount;
    }
    public List<System> getSystems() {
        return systems;
    }
    public void addSystem(System s) {
        systems.add(s);
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

    public boolean isPaused() {
        return paused || gameOver;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isGameOver() {
        if (packetsGenerated == 0) return false;
        return ((double) packetLoss / packetsGenerated) > 0.5;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
        if (gameOver) this.paused = true;
    }

    public boolean getGameOverFlag() {
        return gameOver;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }

    public double getRemainingWireLength() {
        return remainingWireLength;
    }
    public void subtractWireLength(double length) {
        this.remainingWireLength -= length;
        if (this.remainingWireLength < 0) this.remainingWireLength = 0;
    }
    public void addWireLength(double length) {
        this.remainingWireLength += length;
        if (this.remainingWireLength > wireLength) this.remainingWireLength = wireLength;
    }

    public void setImpactDisabled(boolean disabled) { this.impactDisabled = disabled; }
    public boolean isImpactDisabled() { return impactDisabled; }
    public void setCollisionsDisabled(boolean disabled) { this.collisionsDisabled = disabled; }
    public boolean isCollisionsDisabled() { return collisionsDisabled; }
} 
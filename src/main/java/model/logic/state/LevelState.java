package model.logic.state;

public class LevelState {
    private final int wireLength;
    private double remainingWireLength;
    private int packetsGenerated = 0;
    private int packetLoss = 0;
    private int packetsCollected = 0;
    private boolean impactDisabled = false;
    private boolean collisionsDisabled = false;

    public LevelState(int wireLength) {
        this.wireLength = wireLength;
        this.remainingWireLength = wireLength;
    }

    // Wire management
    public int getWireLength() {
        return wireLength;
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

    // Packet statistics
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

    // Game over calculation
    public boolean isGameOver() {
        if (packetsGenerated == 0) return false;
        return ((double) packetLoss / packetsGenerated) > 0.5;
    }

    // Level-specific settings
    public void setImpactDisabled(boolean disabled) {
        this.impactDisabled = disabled;
    }

    public boolean isImpactDisabled() {
        return impactDisabled;
    }

    public void setCollisionsDisabled(boolean disabled) {
        this.collisionsDisabled = disabled;
    }

    public boolean isCollisionsDisabled() {
        return collisionsDisabled;
    }
}

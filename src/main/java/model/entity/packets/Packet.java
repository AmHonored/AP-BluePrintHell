package model.entity.packets;

import javafx.geometry.Point2D;
import model.wire.Wire;
import javafx.scene.shape.Shape;

public abstract class Packet {
    public static final int SIZE = 20; // Standard packet size for centering calculations
    
    private final String id;
    private final PacketType type;
    private final int size;
    private Point2D position;
    private Point2D direction;
    private final int health;
    private int currentHealth;
    private boolean inSystem = false;
    
    private boolean isMoving = false;
    private Point2D startPosition;
    private Point2D targetPosition;
    private Wire currentWire;
    private double movementProgress = 0.0;
    private long movementStartTime = 0;
    private boolean isCompatibleWithCurrentPort = true;

    private double deflectedX = 0.0;
    private double deflectedY = 0.0;
    private int noise = 0;
    private boolean isTrojan = false;
    private boolean isBitFragment = false;

    // Aergia effect state: if active, speed is frozen at aergiaFrozenSpeed until aergiaEffectEndNanos
    private double aergiaFrozenSpeed = -1.0;
    private long aergiaEffectEndNanos = 0L;

    public Packet(String id, PacketType type, int size, Point2D position, Point2D direction, int health) {
        this.id = id;
        this.type = type;
        this.size = size;
        this.position = position;
        this.direction = direction;
        this.health = health;
        this.currentHealth = health;
    }

    public String getId() {
        return id;
    }
    
    public PacketType getType() {
        return type;
    }
    
    public int getSize() {
        return size;
    }
    
    public Point2D getPosition() {
        return position;
    }
    
    public void setPosition(Point2D position) {
        this.position = position;
    }
    
    public Point2D getDirection() {
        return direction;
    }
    
    public void setDirection(Point2D direction) {
        this.direction = direction;
    }
    
    public int getHealth() {
        return health;
    }
    
    public int getCurrentHealth() {
        return currentHealth;
    }
    
    public void setCurrentHealth(int currentHealth) {
        this.currentHealth = Math.max(0, currentHealth);
    }
    
    public void applyNoise() {
        if (currentHealth > 0) {
            currentHealth--;
        }
    }
    
    public boolean isAlive() {
        return currentHealth > 0;
    }
    
    public boolean isInSystem() {
        return inSystem;
    }
    
    public void setInSystem(boolean inSystem) {
        this.inSystem = inSystem;
    }

    public boolean isMoving() {
        return isMoving;
    }

    public void setMoving(boolean moving) {
        this.isMoving = moving;
    }

    public Point2D getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Point2D startPosition) {
        this.startPosition = startPosition;
    }

    public Point2D getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(Point2D targetPosition) {
        this.targetPosition = targetPosition;
    }

    public Wire getCurrentWire() {
        return currentWire;
    }

    public void setCurrentWire(Wire currentWire) {
        this.currentWire = currentWire;
    }

    public double getMovementProgress() {
        return movementProgress;
    }

    public void setMovementProgress(double movementProgress) {
        this.movementProgress = movementProgress;
    }

    public long getMovementStartTime() {
        return movementStartTime;
    }

    public void setMovementStartTime(long movementStartTime) {
        this.movementStartTime = movementStartTime;
    }

    public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
        this.isCompatibleWithCurrentPort = compatiblePort;
    }

    public double getSpeed() {
        return 50.0;
    }

    public boolean isCompatibleWithCurrentPort() {
        return isCompatibleWithCurrentPort;
    }

    public void setCompatibleWithCurrentPort(boolean compatibleWithCurrentPort) {
        this.isCompatibleWithCurrentPort = compatibleWithCurrentPort;
    }

    public double getDeflectedX() {
        return deflectedX;
    }

    public double getDeflectedY() {
        return deflectedY;
    }

    public void applyDeflection(double dx, double dy) {
        this.deflectedX += dx;
        this.deflectedY += dy;
    }

    public void resetDeflection() {
        this.deflectedX = 0.0;
        this.deflectedY = 0.0;
    }

    public boolean isDeflectionTooLarge() {
        return Math.abs(deflectedX) >= 20.0 || Math.abs(deflectedY) >= 20.0; // Increased threshold for Level 2 visibility
    }

    /**
     * Smoothly applies deflection over 100ms (100 steps, 1ms each).
     * This is a stub; implement animation/timing in the manager or subclass as needed.
     */
    public void smoothDeflecting(double totalDx, double totalDy) {
        // To be implemented: animate deflection over time
        applyDeflection(totalDx, totalDy);
    }

    public void setNoise(int noise) { this.noise = noise; }
    
    public int getNoise() { return this.noise; }

    /**
     * Check if this packet is a trojan packet
     * @return true if the packet is a trojan, false otherwise
     */
    public boolean isTrojan() {
        return isTrojan;
    }

    /**
     * Set the trojan status of this packet
     * @param trojan true to make this packet a trojan, false to make it normal
     */
    public void setTrojan(boolean trojan) {
        this.isTrojan = trojan;
    }

    /**
     * Convert this packet to a trojan packet
     */
    public void convertToTrojan() {
        this.isTrojan = true;
    }

    public boolean isBitFragment() {
        return isBitFragment;
    }

    public void setBitFragment(boolean bitFragment) {
        this.isBitFragment = bitFragment;
    }

    // === Aergia helpers ===
    public boolean isAergiaFrozenActive() {
        return aergiaFrozenSpeed >= 0.0 && java.lang.System.nanoTime() < aergiaEffectEndNanos;
    }

    public void setAergiaFreeze(double frozenSpeed, long effectEndNanos) {
        double oldSpeed = this.getSpeed();
        this.aergiaFrozenSpeed = frozenSpeed;
        this.aergiaEffectEndNanos = effectEndNanos;
        double remainingSec = Math.max(0, (effectEndNanos - java.lang.System.nanoTime()) / 1_000_000_000.0);
        java.lang.System.out.println("DEBUG: AERGIA FREEZE APPLIED → packet=" + id + 
            ", originalSpeed=" + String.format("%.2f", oldSpeed) +
            ", frozenSpeed=" + String.format("%.2f", frozenSpeed) + 
            ", remainingSec≈" + String.format("%.1f", remainingSec));
    }

    public void clearAergiaFreezeIfExpired() {
        if (aergiaFrozenSpeed >= 0.0 && java.lang.System.nanoTime() >= aergiaEffectEndNanos) {
            aergiaFrozenSpeed = -1.0;
            aergiaEffectEndNanos = 0L;
            java.lang.System.out.println("DEBUG: AERGIA FREEZE EXPIRED → packet=" + id);
        }
    }

    public double getAergiaFrozenSpeedOrNegative() {
        clearAergiaFreezeIfExpired();
        return aergiaFrozenSpeed;
    }

    /**
     * Take damage and reduce health
     */
    public void takeDamage(int damage) {
        int oldHealth = this.currentHealth;
        this.currentHealth -= damage;
        if (this.currentHealth < 0) {
            this.currentHealth = 0;
        }
        
        // Enhanced health update logging
        if (this.currentHealth <= 0) {
            System.out.println("💀 PACKET DESTROYED: " + this.id + " (" + this.type + ") - Health: " + oldHealth + " → 0");
        } else {
            System.out.println("💔 PACKET DAMAGE: " + this.id + " (" + this.type + ") - Health: " + oldHealth + " → " + this.currentHealth);
        }
    }

    /**
     * Returns the collision shape for this packet.
     * Subclasses must implement this to define their collision geometry.
     */
    public abstract Shape getCollisionShape();

    /**
     * Unified collision detection using JavaFX Shape.intersect.
     * No type-checking needed - works with any packet type.
     */
    public boolean intersects(Packet other) {
        Shape intersection = Shape.intersect(this.getCollisionShape(), other.getCollisionShape());
        return intersection.getBoundsInLocal().getWidth() > 0 && intersection.getBoundsInLocal().getHeight() > 0;
    }
}

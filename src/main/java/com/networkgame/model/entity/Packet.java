package com.networkgame.model.entity; 

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Shape;
import javafx.scene.effect.Glow;
import java.util.HashMap;
import java.util.Map;

public abstract class Packet {
    public enum PacketType {
        SQUARE(2), TRIANGLE(3), HEXAGON(1);
        private final int coinValue;
        PacketType(int coinValue) { this.coinValue = coinValue; }
        public int getCoinValue() { return coinValue; }
    }

    // Constants
    public static final double MAX_SPEED = 150.0, MIN_SPEED = 50.0, COMPATIBLE_SPEED_MULTIPLIER = 0.5;
    public static final double INCOMPATIBLE_SPEED_MULTIPLIER = 1.0, DIVERSION_THRESHOLD = 20.0;
    protected static final double DELTA_TIME_CAP = 0.05;
    
    // Fields
    protected Shape shape;
    protected int size, health;
    protected final int id, coinValue;
    protected double baseSpeed, currentSpeed;
    protected Point2D position, velocity;
    protected final PacketType type;
    protected double[] unitVector = new double[2];
    
    private static int nextId = 0;
    private Connection currentConnection;
    private final Map<String, Object> properties = new HashMap<>();
    private boolean insideSystem = false, reachedEndSystem = false;
    private boolean isCompatibleWithCurrentPort = true, diverting = false;
    private Point2D diversionDirection = new Point2D(0, 0);
    private double diversionDistance = 0.0;
    
    public Packet(Point2D position, PacketType type, int size) {
        this.position = position;
        this.type = type;
        this.size = size;
        this.coinValue = type.getCoinValue();
        this.health = size;
        this.id = nextId++;
        this.velocity = Point2D.ZERO;
        this.baseSpeed = 100.0;
        this.currentSpeed = baseSpeed;
        this.unitVector[0] = 1.0;
        this.unitVector[1] = 0.0;
    }
    
    // Core update method
    public void update(double deltaTime) {
        if (isInsideSystem() || hasReachedEndSystem()) return;
        
        double moveDistance = currentSpeed * Math.min(deltaTime, DELTA_TIME_CAP);
        
        if (currentConnection != null && hasProperty("progress")) {
            updateShapePosition();
            return;
        }
        
        movePacket(moveDistance);
        checkDiversion();
    }
    
    private void movePacket(double moveDistance) {
        position = new Point2D(
            position.getX() + unitVector[0] * moveDistance, 
            position.getY() + unitVector[1] * moveDistance
        );
        updateShapePosition();
    }
    
    private void checkDiversion() {
        if (currentConnection == null || isInsideSystem() || hasReachedEndSystem()) return;
            
        Point2D sourcePos = currentConnection.getSourcePort().getPosition();
        Point2D targetPos = currentConnection.getTargetPort().getPosition();
        
        double t = Math.max(0, Math.min(1, projectPointOntoLine(position, sourcePos, targetPos)));
        Point2D closestPoint = sourcePos.add(targetPos.subtract(sourcePos).multiply(t));
        double distance = position.distance(closestPoint);
        
        setDiverting(distance > DIVERSION_THRESHOLD);
        setDiversionDistance(distance);
        
        if (distance > 0) setDiversionDirection(position.subtract(closestPoint).normalize());
    }
    
    private double projectPointOntoLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        Point2D lineVector = lineEnd.subtract(lineStart);
        Point2D pointVector = point.subtract(lineStart);
        double dotProduct = pointVector.dotProduct(lineVector);
        double lineVectorLengthSquared = lineVector.dotProduct(lineVector);
        return lineVectorLengthSquared < 0.0001 ? 0 : dotProduct / lineVectorLengthSquared;
    }
    
    // Position and movement
    public double[] getUnitVector() { return unitVector; }
    public Point2D getPosition() { return position; }
    public Point2D getVelocity() { return velocity; }
    
    public void setSpeedMultiplier(double multiplier) { currentSpeed = baseSpeed * multiplier; }
    
    public void setUnitVector(double unitX, double unitY) {
        double magnitude = Math.sqrt(unitX * unitX + unitY * unitY);
        if (magnitude > 0.001) {
            this.unitVector[0] = unitX / magnitude;
            this.unitVector[1] = unitY / magnitude;
            this.velocity = new Point2D(this.unitVector[0], this.unitVector[1]).multiply(currentSpeed);
        }
    }
    
    public void setPosition(Point2D position) {
        this.position = position;
        updateShapePosition();
    }
    
    public void setVelocity(Point2D velocity) {
        if (velocity.magnitude() > 0) {
            Point2D normalized = velocity.normalize();
            this.unitVector[0] = normalized.getX();
            this.unitVector[1] = normalized.getY();
            this.velocity = normalized.multiply(this.currentSpeed);
        }
    }
    
    public void adjustPosition(double dx, double dy) {
        position = new Point2D(position.getX() + dx, position.getY() + dy);
        updateShapePosition();
    }
    
    public void alignVelocityToWire(double wireStartX, double wireStartY, double wireEndX, double wireEndY) {
        Point2D wireVector = new Point2D(wireEndX - wireStartX, wireEndY - wireStartY);
        if (wireVector.magnitude() < 0.001) return;
        
        Point2D normalizedWireVector = wireVector.normalize();
        setUnitVector(normalizedWireVector.getX(), normalizedWireVector.getY());
        setVelocity(normalizedWireVector.multiply(currentSpeed));
    }
    
    // Health management
    public int getHealth() { return health; }
    public boolean isDestroyed() { return health <= 0; }
    public double getNoiseLevel() { return size - health; }
    public boolean isOverNoiseLimit() { return isDestroyed(); }
    
    public void reduceHealth(int amount) {
        this.health = Math.max(0, this.health - amount);
        updateShapeAppearance();
    }
    
    public void resetHealth() {
        this.health = this.size;
        updateShapeAppearance();
    }
    
    public void setNoiseLevel(double noiseLevel) {
        this.health = Math.max(0, (int)(size - noiseLevel));
        updateShapeAppearance();
    }
    
    public void addNoise(double amount) { reduceHealth((int)Math.ceil(amount)); }
    
    // Visual representation
    public Shape getShape() { return shape; }
    protected abstract void updateShapePosition();
    protected abstract Color getBaseColor();
    
    public void updateShapeAppearance() {
        if (shape == null) return;
        
        double healthRatio = Math.max(0.0, (double)health / size);
        Color blendedColor = getBaseColor().interpolate(Color.RED, 1.0 - healthRatio);
        shape.setFill(blendedColor);
        
        if (health < size) applyDamageVisualEffect(healthRatio);
    }
    
    private void applyDamageVisualEffect(double healthRatio) {
        double shakeAmount = (1.0 - healthRatio) * 2.0;
        double offsetX = (Math.random() - 0.5) * shakeAmount;
        double offsetY = (Math.random() - 0.5) * shakeAmount;
        
        if (shape != null) {
            shape.setLayoutX(position.getX() - size/2 + offsetX);
            shape.setLayoutY(position.getY() - size/2 + offsetY);
        }
    }
    
    // Connection methods
    public abstract boolean isCompatible(Port port);
    public Connection getCurrentConnection() { return currentConnection; }
    public void setCurrentConnection(Connection connection) { this.currentConnection = connection; }
    public void setCompatibility(boolean isCompatible) { this.isCompatibleWithCurrentPort = isCompatible; }
    
    // System state
    public void setInsideSystem(boolean inside) { this.insideSystem = inside; }
    public boolean isInsideSystem() { return insideSystem; }
    public boolean hasReachedEndSystem() { return reachedEndSystem; }
    
    public void setReachedEndSystem(boolean reached) {
        this.reachedEndSystem = reached;
        
        if (shape != null) {
            shape.setVisible(true);
            shape.setOpacity(1.0);
            if (reached) shape.setEffect(new Glow(0.3));
        }
        
        if (reached) {
            System.out.println("Packet " + getId() + " marked as reached end system");
            
            // Add a property to track that this packet needs coin processing
            if (!hasProperty("counted")) {
                setProperty("needsCoinProcessing", true);
            }
        }
    }
    
    // Diversion management
    public boolean isDiverting() { return diverting; }
    public void setDiverting(boolean diverting) { this.diverting = diverting; }
    public Point2D getDiversionDirection() { return diversionDirection; }
    public void setDiversionDirection(Point2D direction) { this.diversionDirection = direction; }
    public double getDiversionDistance() { return diversionDistance; }
    public void setDiversionDistance(double distance) { this.diversionDistance = distance; }
    
    // Property management
    public boolean hasProperty(String key) { return properties.containsKey(key); }
    public Object getProperty(String key) { return properties.get(key); }
    public void setProperty(String key, Object value) { properties.put(key, value); }
    
    public <T> T getProperty(String key, T defaultValue) {
        if (!properties.containsKey(key)) return defaultValue;
        
        try {
            @SuppressWarnings("unchecked")
            T value = (T) properties.get(key);
            return value;
        } catch (ClassCastException e) {
            System.err.println("Property cast error for key: " + key);
            return defaultValue;
        }
    }
    
    // Basic getters and setters
    public int getId() { return id; }
        public int getCoinValue() {
        return coinValue;
    }
    public int getSize() { return size; }
    public PacketType getType() { return type; }
    public double getSpeed() { return currentSpeed; }
    public void setSpeed(double speed) { this.currentSpeed = speed; }
}

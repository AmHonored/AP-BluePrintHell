package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import java.util.Random;

public class ProtectedPacket extends Packet {
    private static final Random random = new Random();
    private final PacketType originalType;
    private final InheritedMovement inheritedMovement;
    private double currentSpeed = 50.0;

    public enum InheritedMovement {
        SQUARE,    
        TRIANGLE,  
        HEXAGON   
    }

    public ProtectedPacket(String id, Point2D position, Point2D direction, PacketType originalType) {
        super(id, PacketType.PROTECTED, 5, position, direction, getDoubleHealth(originalType));
        this.originalType = originalType;
        this.inheritedMovement = selectRandomMovement();
        initializeSpeed();
    }

    /**
     * Get double the health of the original packet type
     */
    private static int getDoubleHealth(PacketType originalType) {
        switch (originalType) {
            case SQUARE: return 4;    // 2 * 2
            case TRIANGLE: return 6;  // 2 * 3
            case HEXAGON: return 4;   // 2 * 2
            default: return 4;
        }
    }

    private InheritedMovement selectRandomMovement() {
        double rand = random.nextDouble();
        if (rand < 2.0/7.0) {
            return InheritedMovement.SQUARE;
        } else if (rand < 5.0/7.0) { 
            return InheritedMovement.TRIANGLE;
        } else {
            return InheritedMovement.HEXAGON;
        }
    }

    private void initializeSpeed() {
        switch (inheritedMovement) {
            case SQUARE:
                currentSpeed = 80.0; 
                break;
            case TRIANGLE:
                currentSpeed = 50.0;
                break;
            case HEXAGON:
                currentSpeed = 80.0;
                break;
        }
    }

    @Override
    public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
        super.updateMovement(deltaTimeSeconds, compatiblePort);
        switch (inheritedMovement) {
            case SQUARE:
                currentSpeed = finalizeSpeed(
                    compatiblePort ? 80.0 / 2.0 : 80.0,
                    0.0,
                    Double.POSITIVE_INFINITY
                );
                break;
            case TRIANGLE:
                currentSpeed = computeUpdatedSpeed(
                    currentSpeed,
                    deltaTimeSeconds,
                    compatiblePort,
                    50.0,
                    100.0
                );
                break;
            case HEXAGON:
                currentSpeed = computeUpdatedSpeed(
                    currentSpeed,
                    deltaTimeSeconds,
                    compatiblePort,
                    20.0,
                    140.0
                );
                break;
        }
    }

    @Override
    public double getSpeed() {
        return currentSpeed;
    }

    @Override
    protected boolean isAergiaApplicable() {
        return inheritedMovement == InheritedMovement.TRIANGLE || inheritedMovement == InheritedMovement.HEXAGON;
    }

    @Override
    protected double computeBaseSpeed(double current, double dt, boolean compatible) {
        switch (inheritedMovement) {
            case TRIANGLE:
                return compatible ? 50.0 : current + 25.0 * dt;
            case HEXAGON:
                return compatible ? current + 30.0 * dt : current - 25.0 * dt;
            case SQUARE:
            default:
                return current; 
        }
    }

    @Override
    public Shape getCollisionShape() {
        double size = 16.0;
        double half = size / 2.0;
        double x = getPosition().getX();
        double y = getPosition().getY();
        
        Polygon diamond = new Polygon();
        diamond.getPoints().addAll(new Double[]{
            x, y - half,        // Top vertex
            x + half, y,        // Right vertex
            x, y + half,        // Bottom vertex
            x - half, y         // Left vertex
        });
        return diamond;
    }


    public PacketType getOriginalType() {
        return originalType;
    }

    public InheritedMovement getInheritedMovement() {
        return inheritedMovement;
    }

    public Packet convertToOriginalType() {
        int convertedHealth = Math.min(
            Math.max(1, this.getCurrentHealth() / 2), 
            getOriginalMaxHealth(originalType)
        );
        
        switch (originalType) {
            case SQUARE:
                SquarePacket squarePacket = new SquarePacket(getId(), getPosition(), getDirection());
                squarePacket.setCurrentHealth(convertedHealth);
                return squarePacket;
                
            case TRIANGLE:
                TrianglePacket trianglePacket = new TrianglePacket(getId(), getPosition(), getDirection());
                trianglePacket.setCurrentHealth(convertedHealth);
                return trianglePacket;
                
            case HEXAGON:
                HexagonPacket hexagonPacket = new HexagonPacket(getId(), getPosition(), getDirection());
                hexagonPacket.setCurrentHealth(convertedHealth);
                return hexagonPacket;
                
            default:
                SquarePacket fallbackPacket = new SquarePacket(getId(), getPosition(), getDirection());
                fallbackPacket.setCurrentHealth(convertedHealth);
                return fallbackPacket;
        }
    }

    private int getOriginalMaxHealth(PacketType type) {
        switch (type) {
            case SQUARE: return 2;
            case TRIANGLE: return 3;
            case HEXAGON: return 2;
            default: return 2;
        }
    }
} 
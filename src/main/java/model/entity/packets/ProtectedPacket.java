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

    // Movement types that can be inherited
    public enum InheritedMovement {
        SQUARE,    // Constant speed
        TRIANGLE,  // Accelerating movement
        HEXAGON    // Variable speed with state
    }

    public ProtectedPacket(String id, Point2D position, Point2D direction, PacketType originalType) {
        // Protected packet has double health and size 5 for coin bonus
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

    /**
     * Randomly select movement type based on size probability:
     * Square: 2/7, Triangle: 3/7, Hexagon: 2/7
     */
    private InheritedMovement selectRandomMovement() {
        double rand = random.nextDouble();
        if (rand < 2.0/7.0) {
            return InheritedMovement.SQUARE;
        } else if (rand < 5.0/7.0) {  // 2/7 + 3/7 = 5/7
            return InheritedMovement.TRIANGLE;
        } else {
            return InheritedMovement.HEXAGON;
        }
    }

    /**
     * Initialize speed based on inherited movement type
     */
    private void initializeSpeed() {
        switch (inheritedMovement) {
            case SQUARE:
                currentSpeed = 80.0; // Square base speed
                break;
            case TRIANGLE:
                currentSpeed = 50.0; // Triangle base speed
                break;
            case HEXAGON:
                currentSpeed = 80.0; // Hexagon base speed (updated to match HexagonPacket)
                break;
        }
    }

    @Override
    public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
        super.updateMovement(deltaTimeSeconds, compatiblePort);
        
        // Update speed based on inherited movement pattern
        if (isAergiaFrozenActive()) {
            double frozen = getAergiaFrozenSpeedOrNegative();
            if (frozen >= 0.0) currentSpeed = frozen;
            return;
        }
        switch (inheritedMovement) {
            case SQUARE:
                updateSquareMovement(compatiblePort);
                break;
            case TRIANGLE:
                updateTriangleMovement(deltaTimeSeconds, compatiblePort);
                break;
            case HEXAGON:
                updateHexagonMovement(deltaTimeSeconds, compatiblePort);
                break;
        }
    }

    private void updateSquareMovement(boolean compatiblePort) {
        // Square packets have constant speed with compatibility penalty
        if (compatiblePort) {
            currentSpeed = 80.0 / 2.0; // Half speed on compatible ports
        } else {
            currentSpeed = 80.0;
        }
    }

    private void updateTriangleMovement(double deltaTimeSeconds, boolean compatiblePort) {
        // Triangle packets accelerate on incompatible ports
        if (compatiblePort) {
            currentSpeed = 50.0; // Base speed
        } else {
            currentSpeed += 25.0 * deltaTimeSeconds; // Acceleration
            if (currentSpeed > 100.0) {
                currentSpeed = 100.0; // Max speed
            }
        }
    }

    private void updateHexagonMovement(double deltaTimeSeconds, boolean compatiblePort) {
        // Hexagon packets accelerate on compatible, decelerate on incompatible
        if (compatiblePort) {
            currentSpeed += 30.0 * deltaTimeSeconds; // Acceleration
            if (currentSpeed > 140.0) {  // Updated to match HexagonPacket MAX_SPEED
                currentSpeed = 140.0; // Max speed
            }
        } else {
            currentSpeed -= 25.0 * deltaTimeSeconds; // Deceleration
            if (currentSpeed < 20.0) {
                currentSpeed = 20.0; // Min speed
            }
        }
    }

    @Override
    public double getSpeed() {
        return currentSpeed;
    }

    @Override
    public Shape getCollisionShape() {
        // Create violet diamond (rotated square) collision shape
        double size = 16.0;
        double half = size / 2.0;
        double x = getPosition().getX();
        double y = getPosition().getY();
        
        // Create diamond shape (rotated square)
        Polygon diamond = new Polygon();
        diamond.getPoints().addAll(new Double[]{
            x, y - half,        // Top vertex
            x + half, y,        // Right vertex
            x, y + half,        // Bottom vertex
            x - half, y         // Left vertex
        });
        return diamond;
    }

    /**
     * Get the original packet type before protection
     */
    public PacketType getOriginalType() {
        return originalType;
    }

    /**
     * Get the inherited movement type
     */
    public InheritedMovement getInheritedMovement() {
        return inheritedMovement;
    }

    /**
     * Convert back to original packet type
     * Used when VPN system fails or packet enters DDoS/Spy system
     */
    public Packet convertToOriginalType() {
        // Calculate converted health: min(floor(protectedHealth/2), originalMaxHealth)
        int convertedHealth = Math.min(
            Math.max(1, this.getCurrentHealth() / 2), // At least 1 health, divided by 2
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
                // Fallback to square
                SquarePacket fallbackPacket = new SquarePacket(getId(), getPosition(), getDirection());
                fallbackPacket.setCurrentHealth(convertedHealth);
                return fallbackPacket;
        }
    }

    /**
     * Get the maximum health of the original packet type
     */
    private int getOriginalMaxHealth(PacketType type) {
        switch (type) {
            case SQUARE: return 2;
            case TRIANGLE: return 3;
            case HEXAGON: return 2;
            default: return 2;
        }
    }
} 
package model.entity.packets;

import javafx.geometry.Point2D;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Shape;
import model.logic.packet.PacketState;

public class HexagonPacket extends Packet {
    private static final double BASE_SPEED = 50.0; 
    private static final double MAX_SPEED = 120.0;  
    private static final double MIN_SPEED = 20.0;
    private static final double ACCELERATION = 20.0;
    private static final double DECELERATION = 15.0;
    
    private double currentSpeed = BASE_SPEED;
    private PacketState movementState = PacketState.FORWARD;
    private double distanceTraveled = 0.0;
    private double totalPathLength = 0.0;

    public HexagonPacket(String id, Point2D position, Point2D direction) {
        super(id, PacketType.HEXAGON, 2, position, direction, 2);
    }

    @Override
    public void updateMovement(double deltaTimeSeconds, boolean compatiblePort) {
        super.updateMovement(deltaTimeSeconds, compatiblePort);
        
        if (movementState == PacketState.FORWARD) {
            updateForwardMovement(deltaTimeSeconds, compatiblePort);
        } else {
            updateReturningMovement(deltaTimeSeconds);
        }
    }

    private void updateForwardMovement(double deltaTimeSeconds, boolean compatiblePort) {
        currentSpeed = computeUpdatedSpeed(
            currentSpeed,
            deltaTimeSeconds,
            compatiblePort,
            MIN_SPEED,
            MAX_SPEED
        );
        distanceTraveled += currentSpeed * deltaTimeSeconds;
    }

    private void updateReturningMovement(double deltaTimeSeconds) {
        currentSpeed = BASE_SPEED;
        distanceTraveled -= currentSpeed * deltaTimeSeconds;
     
        if (distanceTraveled <= 0) {
            distanceTraveled = 0;
            movementState = PacketState.FORWARD;
            currentSpeed = BASE_SPEED; 
        }
    }

    @Override
    public double getSpeed() {
        return currentSpeed;
    }

    public PacketState getMovementState() {
        return movementState;
    }

    public void setMovementState(PacketState state) {
        this.movementState = state;
    }

    public void changeDirection() {
        if (movementState == PacketState.FORWARD) {
            movementState = PacketState.RETURNING;
        } else {
            movementState = PacketState.FORWARD;
        }
    }

    public void setTotalPathLength(double length) {
        this.totalPathLength = length;
    }

    public double getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setDistanceTraveled(double distance) {
        this.distanceTraveled = distance;
    }

    public boolean hasReachedDestination() {
        boolean reached = distanceTraveled >= totalPathLength;
        return reached;
    }

    @Override
    protected boolean isAergiaApplicable() { return true; }

    @Override
    protected double computeBaseSpeed(double current, double dt, boolean compatible) {
        return compatible ? current + ACCELERATION * dt : current - DECELERATION * dt;
    }

    @Override
    public Shape getCollisionShape() {
        Polygon hexagon = new Polygon();
        double centerX = getPosition().getX();
        double centerY = getPosition().getY();
        double radius = 8.0;
        
        for (int i = 0; i < 6; i++) {
            double angle = i * Math.PI / 3; 
            double x = centerX + radius * Math.cos(angle);
            double y = centerY + radius * Math.sin(angle);
            hexagon.getPoints().addAll(x, y);
        }
        
        return hexagon;
    }
}

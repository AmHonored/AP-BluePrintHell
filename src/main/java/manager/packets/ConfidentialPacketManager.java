package manager.packets;

import model.entity.packets.Packet;
import model.entity.packets.PacketType;
import model.entity.systems.System;
import model.logic.packet.PacketState;
import model.wire.Wire;
import javafx.geometry.Point2D;
import java.util.HashMap;
import java.util.Map;

public class ConfidentialPacketManager {
    private final Packet packet;
    private final Wire wire;
    
    private Map<Packet, Double> previousDistances = new HashMap<>();
    private static final double MIN_DISTANCE = 40.0; 
    
    private static final double REDUCED_SPEED_FACTOR = 0.4; 
    private PacketState movementState = PacketState.FORWARD;

    public ConfidentialPacketManager(Packet packet, Wire wire) {
        this.packet = packet;
        this.wire = wire;
    }

    public void updateMovement(double deltaTimeSeconds) {
        if (packet.getType() == PacketType.CONFIDENTIAL_TYPE1) {
            updateType1Movement(deltaTimeSeconds);
        } else if (packet.getType() == PacketType.CONFIDENTIAL_TYPE2) {
            updateType2Movement(deltaTimeSeconds);
        }
    }

    private void updateType1Movement(double deltaTimeSeconds) {
        double speed = calculateType1Speed();
        
        double wireLength = wire.getLength();
        if (wireLength <= 0) return;
        
        double distanceToMove = speed * deltaTimeSeconds;
        double progressIncrement = distanceToMove / wireLength;
        double newProgress = packet.getMovementProgress() + progressIncrement;
        
        if (newProgress > 1.0) {
            newProgress = 1.0;
        }
        
        packet.setMovementProgress(newProgress);
        Point2D newPosition = wire.getPositionAtProgress(newProgress);
        packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
    }

    private void updateType2Movement(double deltaTimeSeconds) {

        boolean shouldMoveBackward = false;
        double baseSpeed = packet.getSpeed();
        
        for (Packet otherPacket : PacketManager.getMovingPackets()) {
            if (otherPacket == packet || otherPacket.isInSystem()) continue;
            
            double distance = calculateDistance(otherPacket, packet);
            Double previousDistance = previousDistances.get(otherPacket);
            
            if (distance < MIN_DISTANCE) {
                if (previousDistance == null || distance < previousDistance) {
                    shouldMoveBackward = true;
                    break;
                }
            }
            
            previousDistances.put(otherPacket, distance);
        }
        
        double wireLength = wire.getLength();
        if (wireLength <= 0) return;
        
        double speed = shouldMoveBackward ? -baseSpeed * 0.6 : baseSpeed;
        double distanceToMove = speed * deltaTimeSeconds;
        double progressIncrement = distanceToMove / wireLength;
        double newProgress = packet.getMovementProgress() + progressIncrement;
        
        if (newProgress < 0.0) newProgress = 0.0;
        if (newProgress > 1.0) newProgress = 1.0;
        
        packet.setMovementProgress(newProgress);
        Point2D newPosition = wire.getPositionAtProgress(newProgress);
        packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
        
        movementState = shouldMoveBackward ? PacketState.RETURNING : PacketState.FORWARD;
    }

    private double calculateType1Speed() {
        if (wire == null || wire.getDest() == null) {
            return packet.getSpeed();
        }
        
        System destinationSystem = wire.getDest().getSystem();
        if (destinationSystem == null) {
            return packet.getSpeed();
        }
        
        boolean hasStoredPackets = hasPacketsInSystem(destinationSystem);
        
        if (hasStoredPackets) {
            return packet.getSpeed() * REDUCED_SPEED_FACTOR;
        } else {
            return packet.getSpeed();
        }
    }

    private boolean hasPacketsInSystem(System system) {

        if (system instanceof model.entity.systems.IntermediateSystem) {
            model.entity.systems.IntermediateSystem intermediate = 
                (model.entity.systems.IntermediateSystem) system;
            return intermediate.getStorageSize() > 0;
        } else if (system instanceof model.entity.systems.VPNSystem) {
            model.entity.systems.VPNSystem vpn = (model.entity.systems.VPNSystem) system;
            return vpn.getStorageSize() > 0;
        } else if (system instanceof model.entity.systems.DDosSystem) {
            model.entity.systems.DDosSystem ddos = (model.entity.systems.DDosSystem) system;
            return ddos.getStorageSize() > 0;
        } else if (system instanceof model.entity.systems.SpySystem) {
            model.entity.systems.SpySystem spy = (model.entity.systems.SpySystem) system;
            return spy.getStorageSize() > 0;
        } else if (system instanceof model.entity.systems.AntiVirusSystem) {
            model.entity.systems.AntiVirusSystem antivirus = 
                (model.entity.systems.AntiVirusSystem) system;
            return antivirus.getStorageSize() > 0;
        }
        
        return false;
    }

    private double calculateDistance(Packet packet1, Packet packet2) {
        Point2D pos1 = packet1.getPosition();
        Point2D pos2 = packet2.getPosition();
        
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        
        return Math.sqrt(dx * dx + dy * dy);
    }

    public PacketState getMovementState() {
        return movementState;
    }

    public void cleanup() {
        previousDistances.clear();
    }
}

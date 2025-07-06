package com.networkgame.model.packet;

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.state.GameState;

/**
 * Handles packet routing, movement, and tracking on connections
 */
public class PacketRouter {
    private final Connection connection;
    private final List<Packet> activePackets;
    private boolean available = true;

    public PacketRouter(Connection connection) {
        this.connection = connection;
        this.activePackets = new ArrayList<>();
    }

    public void addPacket(Packet packet) {
        if (activePackets.contains(packet)) return;
        
        activePackets.add(packet);
        available = false;
        
        packet.setCurrentConnection(connection);
        
        Point2D startPos = connection.getSourcePort().getPosition();
        Point2D endPos = connection.getTargetPort().getPosition();
        
        configurePacketMovement(packet, startPos, endPos);
        applyPacketStyling(packet);
    }
    
    private void configurePacketMovement(Packet packet, Point2D startPos, Point2D endPos) {
        double dx = endPos.getX() - startPos.getX();
        double dy = endPos.getY() - startPos.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0) {
            dx /= distance;
            dy /= distance;
        } else {
            dx = 1.0;
            dy = 0.0;
        }
        
        packet.setUnitVector(dx, dy);
        
        boolean isCompatible = packet.isCompatible(connection.getTargetPort());
        double speedModifier = isCompatible ? 
            Packet.COMPATIBLE_SPEED_MULTIPLIER : 
            Packet.INCOMPATIBLE_SPEED_MULTIPLIER;
        
        packet.setSpeedMultiplier(speedModifier);
        packet.setPosition(startPos);
        packet.setProperty("progress", 0.0);
    }
    
    private void applyPacketStyling(Packet packet) {
        Shape packetShape = packet.getShape();
        if (packetShape == null) return;
        
        packetShape.getStyleClass().add("packet");
        
        if (packet.getType() == Packet.PacketType.SQUARE) {
            packetShape.getStyleClass().add("square-packet");
        } else if (packet.getType() == Packet.PacketType.TRIANGLE) {
            packetShape.getStyleClass().add("triangle-packet"); 
        }
    }
    
    public void update(double deltaTime) {
        if (activePackets.isEmpty()) return;
        
        Point2D sourcePos = connection.getSourcePort().getPosition();
        Point2D targetPos = connection.getTargetPort().getPosition();
        double totalDistance = sourcePos.distance(targetPos);
        
        List<Packet> packetsToRemove = new ArrayList<>();
        
        for (Packet packet : new ArrayList<>(activePackets)) {
            updatePacketPosition(packet, sourcePos, targetPos, totalDistance, deltaTime, packetsToRemove);
        }
        
        activePackets.removeAll(packetsToRemove);
    }
    
    private void updatePacketPosition(Packet packet, Point2D sourcePos, Point2D targetPos, 
                                      double totalDistance, double deltaTime, List<Packet> packetsToRemove) {
        double progress = packet.hasProperty("progress") ? 
            (double)packet.getProperty("progress", 0.0) : 0.0;
        
        if (!packet.hasProperty("progress")) {
            packet.setProperty("progress", 0.0);
        }
        
        double speed = packet.getSpeed();
        double distanceThisFrame = speed * deltaTime;
        double progressDelta = totalDistance > 0 ? distanceThisFrame / totalDistance : 0;
        
        progress += progressDelta;
        packet.setProperty("progress", progress);
        
        packet.setPosition(sourcePos.add(targetPos.subtract(sourcePos).multiply(progress)));
        
        if (totalDistance > 0) {
            packet.setUnitVector(
                (targetPos.getX() - sourcePos.getX()) / totalDistance,
                (targetPos.getY() - sourcePos.getY()) / totalDistance
            );
        }
        
        if (progress >= 1.0) {
            handlePacketArrival(packet, packetsToRemove);
        }
    }
    
    private void handlePacketArrival(Packet packet, List<Packet> packetsToRemove) {
        Port targetPort = connection.getTargetPort();
        NetworkSystem targetSystem = targetPort.getSystem();
        
        packet.setPosition(targetPort.getPosition());
        packet.setProperty("progress", 1.0);
        packetsToRemove.add(packet);
        packet.setCurrentConnection(null);
        
        targetSystem.receivePacket(packet);
        
        if (targetSystem.isEndSystem()) {
            packet.setReachedEndSystem(true);
        }
        
        setAvailable(true);
        notifyConnectionAvailable();
    }
    
    public void removePacket(Packet packet) {
        activePackets.remove(packet);
        
        if (activePackets.isEmpty()) {
            setAvailable(true);
            notifyConnectionAvailable();
        }
    }
    
    public boolean isEmpty() {
        GameState gameState = connection.getSourcePort().getSystem().getGameState();
        
        if (gameState != null && gameState.getCurrentLevel() == 1) {
            for (Packet p : new ArrayList<>(activePackets)) {
                if (p.hasReachedEndSystem() || p.isInsideSystem()) {
                    activePackets.remove(p);
                } else {
                    return false;
                }
            }
            return true;
        }
        
        for (Packet packet : activePackets) {
            if (!packet.hasReachedEndSystem() && !packet.isInsideSystem()) {
                Double progress = (Double) packet.getProperty("progress");
                if (progress == null || progress < 0.95) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    public boolean isPacketOnWire(Packet packet) {
        Point2D packetPos = packet.getPosition();
        Point2D wireStart = connection.getSourcePosition();
        Point2D wireEnd = connection.getTargetPosition();
        
        double distance = distanceToLine(packetPos, wireStart, wireEnd);
        return distance <= packet.getSize() * 1.5;
    }
    
    public double distanceToLine(Point2D point, Point2D lineStart, Point2D lineEnd) {
        double lineLength = lineStart.distance(lineEnd);
        
        if (lineLength < 0.001) {
            return point.distance(lineStart);
        }
        
        double t = calculateProjectionParameter(point, lineStart, lineEnd);
        Point2D closest = lineStart.add(lineEnd.subtract(lineStart).multiply(t));
        
        return point.distance(closest);
    }
    
    private double calculateProjectionParameter(Point2D point, Point2D lineStart, Point2D lineEnd) {
        Point2D lineVector = lineEnd.subtract(lineStart);
        double lineLength = lineVector.magnitude();
        
        if (lineLength < 0.001) {
            return 0;
        }
        
        double dotProduct = point.subtract(lineStart).dotProduct(lineVector) / (lineLength * lineLength);
        return Math.max(0, Math.min(1, dotProduct));
    }
    
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void notifyConnectionAvailable() {
        Port sourcePort = connection.getSourcePort();
        Port targetPort = connection.getTargetPort();
        
        if (targetPort != null && targetPort.getSystem() != null) {
            targetPort.getSystem().handleOutputPortAvailable(targetPort);
        }
        
        if (sourcePort != null && sourcePort.getSystem() != null) {
            sourcePort.getSystem().handleOutputPortAvailable(sourcePort);
        }
    }
    
    public List<Packet> getActivePackets() {
        return activePackets;
    }
    
    public boolean isBusy() {
        return !activePackets.isEmpty();
    }
} 

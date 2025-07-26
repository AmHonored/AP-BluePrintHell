package manager.packets;

import model.entity.ports.Port;
import model.entity.packets.Packet;
import model.wire.Wire;
import model.entity.systems.System;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import model.levels.Level;
import controller.PacketController;

public class PacketManager {
    private static final List<Packet> movingPackets = new ArrayList<>();
    private static Level level;
    private static PacketController packetController;

    public static void setLevel(Level lvl) {
        level = lvl;
    }

    public static void setPacketController(PacketController controller) {
        packetController = controller;
    }
    
    public static boolean sendPacket(Port sourcePort, Packet packet) {
        Wire wire = sourcePort.getWire();
        
        if (wire == null || !sourcePort.isConnected() || !wire.isAvailable()) {
            return false;
        }
        
        boolean result = startMovement(packet, wire);
        return result;
    }
    
    public static boolean startMovement(Packet packet, Wire wire) {
        if (packet.isMoving()) {
            return false;
        }
        
        // CRITICAL FIX: Show packet if it was hidden (coming out of storage)
        if (packetController != null) {
            packetController.showPacket(packet);
        }
        
        // Set initial position at wire source with deflection for precise centering
        Point2D sourcePos = wire.getSource().getPosition();
        double centerX = sourcePos.getX() + packet.getDeflectedX();
        double centerY = sourcePos.getY() + packet.getDeflectedY();
        packet.setPosition(new Point2D(centerX, centerY));
        
        packet.setStartPosition(wire.getSource().getPosition());
        packet.setTargetPosition(wire.getDest().getPosition());
        packet.setCurrentWire(wire);
        packet.setMovementProgress(0.0);
        packet.setMovementStartTime(java.lang.System.nanoTime());
        packet.setMoving(true);
        packet.setInSystem(false);
        boolean isCompatible = wire.getSource().isCompatible(packet);
        packet.setCompatibleWithCurrentPort(isCompatible);
        if (packet instanceof model.entity.packets.TrianglePacket && !isCompatible) {
            ((model.entity.packets.TrianglePacket) packet).resetSpeed();
        }
        wire.setAvailable(false);
        movingPackets.add(packet);
        if (packetController != null) {
            packetController.addPacket(packet);
        }
        
        return true;
    }
    
    public static void updateMovingPackets(double deltaTimeSeconds) {
        if (level != null && level.isPaused()) return;
        Iterator<Packet> iterator = movingPackets.iterator();
        while (iterator.hasNext()) {
            Packet packet = iterator.next();
            updatePacketMovement(packet, deltaTimeSeconds);
            if (packetController != null) {
                packetController.updatePacket(packet);
            }
            if (packet.getMovementProgress() >= 1.0) {
                completeMovement(packet);
                iterator.remove();
                if (packetController != null) {
                    packetController.updatePacket(packet);
                }
            }
        }
    }
    
    private static void updatePacketMovement(Packet packet, double deltaTimeSeconds) {
        Wire wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }
        double wireLength = wire.getLength();
        if (wireLength <= 0) {
            return;
        }
        packet.updateMovement(deltaTimeSeconds, packet.isCompatibleWithCurrentPort());
        double speed = packet.getSpeed();
        double distanceToMove = speed * deltaTimeSeconds;
        double progressIncrement = distanceToMove / wireLength;
        double newProgress = packet.getMovementProgress() + progressIncrement;
        if (newProgress > 1.0) {
            newProgress = 1.0;
        }
        packet.setMovementProgress(newProgress);
        Point2D newPosition = wire.getPositionAtProgress(newProgress);
        // Use deflection for movement with precise center positioning
        double centerX = newPosition.getX() + packet.getDeflectedX();
        double centerY = newPosition.getY() + packet.getDeflectedY();
        packet.setPosition(new Point2D(centerX, centerY));
    }
    
    private static void completeMovement(Packet packet) {
        Wire wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }
        
        // Set position to destination center + deflection for precise positioning
        Point2D destPos = wire.getDest().getPosition();
        double centerX = destPos.getX() + packet.getDeflectedX();
        double centerY = destPos.getY() + packet.getDeflectedY();
        packet.setPosition(new Point2D(centerX, centerY));
        packet.setInSystem(true);
        packet.setMoving(false);
        packet.setCurrentWire(null);
        packet.setMovementProgress(0.0);
        // Reset deflection after movement completes
        packet.resetDeflection();
        
        wire.setAvailable(true);
        
        model.entity.systems.System destinationSystem = wire.getDest().getSystem();
        
        deliverToDestinationSystem(packet, destinationSystem);
    }
    
    private static void deliverToDestinationSystem(Packet packet, model.entity.systems.System destinationSystem) {
        if (destinationSystem instanceof model.entity.systems.IntermediateSystem) {
            model.entity.systems.IntermediateSystem intermediateSystem = (model.entity.systems.IntermediateSystem) destinationSystem;
            
            manager.systems.IntermediateSystemManager manager = 
                new manager.systems.IntermediateSystemManager(intermediateSystem);
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(intermediateSystem.getPosition());
            
            // Hide the packet visually since it's in internal storage
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.EndSystem) {
            model.entity.systems.EndSystem endSystem = (model.entity.systems.EndSystem) destinationSystem;
            endSystem.claimPacket(packet, level);
            // Don't call deliverPacket here - the iterator in updateMovingPackets will handle removal
            if (packetController != null) {
                packetController.deliverPacket(packet);
            }
        }
    }
    
    public static void removePacket(Packet packet) {
        movingPackets.remove(packet);
        if (packetController != null) {
            packetController.removePacket(packet);
        }
    }
    
    /**
     * Remove packet from view only (for successful delivery).
     * This method should be used when packets successfully reach their destination
     * and should not be counted as packet loss.
     * Note: This method no longer removes from movingPackets as that's handled by the iterator.
     */
    public static void deliverPacket(Packet packet) {
        // Don't remove from movingPackets here - the iterator in updateMovingPackets handles that
        if (packetController != null) {
            packetController.deliverPacket(packet);
        }
    }
    
    public static List<Packet> getMovingPackets() {
        return new ArrayList<>(movingPackets);
    }
    
    public static boolean hasMovingPackets() {
        return !movingPackets.isEmpty();
    }
}

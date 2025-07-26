package manager.packets;

import model.entity.ports.Port;
import model.entity.packets.Packet;
import model.entity.packets.HexagonPacket;
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
        
        // Set initial position to source center for precise positioning
        Point2D sourcePos = wire.getSource().getPosition();
        packet.setPosition(new Point2D(sourcePos.getX(), sourcePos.getY()));
        
        packet.setStartPosition(wire.getSource().getPosition());
        packet.setTargetPosition(wire.getDest().getPosition());
        packet.setCurrentWire(wire);
        packet.setMovementProgress(0.0);
        packet.setMovementStartTime(java.lang.System.nanoTime());
        packet.setMoving(true);
        packet.setInSystem(false);
        boolean isCompatible = wire.getSource().isCompatible(packet);
        packet.setCompatibleWithCurrentPort(isCompatible);
        
        // Special initialization for HexagonPacket
        if (packet instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) packet;
            hexPacket.setTotalPathLength(wire.getLength());
            hexPacket.setDistanceTraveled(0.0);
            java.lang.System.out.println("🚀 HEXAGON PACKET MOVEMENT STARTED: " + packet.getId() + " on wire of length " + String.format("%.1f", wire.getLength()));
        }
        
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
            
            // Check for movement completion
            boolean shouldComplete = false;
            if (packet instanceof HexagonPacket) {
                HexagonPacket hexPacket = (HexagonPacket) packet;
                // Complete when reaching destination in FORWARD state
                shouldComplete = (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD && 
                                packet.getMovementProgress() >= 1.0);
            } else {
                // Standard completion check for other packets
                shouldComplete = packet.getMovementProgress() >= 1.0;
            }
            
            if (shouldComplete) {
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
        
        // Special handling for HexagonPacket using distance-based movement
        if (packet instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) packet;
            double distanceTraveled = hexPacket.getDistanceTraveled();
            double progress = distanceTraveled / wireLength;
            
            // Clamp progress to valid range
            if (progress < 0.0) progress = 0.0;
            if (progress > 1.0) progress = 1.0;
            
            packet.setMovementProgress(progress);
            Point2D newPosition = wire.getPositionAtProgress(progress);
            
            // For precise wire following, don't add deflection during normal movement
            // Deflection should only be a visual effect, not affect the actual path
            packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
            
            // Debug position updates for hexagon packets (reduced frequency)
            if (hexPacket.getMovementState() == model.logic.packet.PacketState.RETURNING && 
                Math.random() < 0.1) { // Only log 10% of the time to reduce spam
                java.lang.System.out.println("⬅️ HEXAGON VISUAL UPDATE: " + packet.getId() + " - Distance: " + String.format("%.1f", distanceTraveled) + 
                                           ", Progress: " + String.format("%.2f", progress) + ", Position: (" + 
                                           String.format("%.1f", newPosition.getX()) + ", " + String.format("%.1f", newPosition.getY()) + ")");
            }
        } else {
            // Standard progress-based movement for other packets
            double speed = packet.getSpeed();
            double distanceToMove = speed * deltaTimeSeconds;
            double progressIncrement = distanceToMove / wireLength;
            double newProgress = packet.getMovementProgress() + progressIncrement;
            if (newProgress > 1.0) {
                newProgress = 1.0;
            }
            packet.setMovementProgress(newProgress);
            Point2D newPosition = wire.getPositionAtProgress(newProgress);
            
            // For standard packets, also don't add deflection during normal movement
            packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
        }
    }
    
    private static void completeMovement(Packet packet) {
        Wire wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }
        
        // Set position to destination center for precise positioning
        Point2D destPos = wire.getDest().getPosition();
        packet.setPosition(new Point2D(destPos.getX(), destPos.getY()));
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
            
        } else if (destinationSystem instanceof model.entity.systems.DDosSystem) {
            model.entity.systems.DDosSystem ddosSystem = (model.entity.systems.DDosSystem) destinationSystem;
            
            manager.systems.DDosSystemManager manager = 
                new manager.systems.DDosSystemManager(ddosSystem);
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(ddosSystem.getPosition());
            
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

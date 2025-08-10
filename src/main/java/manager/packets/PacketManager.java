package manager.packets;

import model.entity.ports.Port;
import model.entity.packets.Packet;
import model.entity.packets.HexagonPacket;
import model.entity.packets.ConfidentialPacket;
import model.entity.packets.MassivePacket;
import model.wire.Wire;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import model.levels.Level;
import controller.PacketController;
// imports trimmed

public class PacketManager {
    private static final List<Packet> movingPackets = new ArrayList<>();
    // During updates, defer structural modifications to avoid CME
    private static boolean isUpdating = false;
    private static final List<Packet> pendingAdds = new ArrayList<>();
    private static final List<Packet> pendingRemovals = new ArrayList<>();
    private static Level level;
    private static PacketController packetController;
    
    // Track confidential packet managers for special movement handling
    private static final Map<Packet, ConfidentialPacketManager> confidentialManagers = new HashMap<>();

    public static void setLevel(Level lvl) {
        level = lvl;
    }

    public static void setPacketController(PacketController controller) {
        packetController = controller;
    }
    
    public static boolean sendPacket(Port sourcePort, Packet packet) {
        return sendPacket(sourcePort, packet, false);
    }
    
    public static boolean sendPacket(Port sourcePort, Packet packet, boolean preserveCompatibility) {
        Wire wire = sourcePort.getWire();
        
        if (wire == null || !sourcePort.isConnected() || !wire.isActive()) {
            return false;
        }
        // Ensure packet is in level list before visualization/movement starts
        if (level != null && packet != null && !level.getPackets().contains(packet)) {
            level.addPacket(packet);
        }
        
        boolean result = startMovement(packet, wire, preserveCompatibility);
        return result;
    }
    
    public static boolean startMovement(Packet packet, Wire wire) {
        return startMovement(packet, wire, false);
    }
    
    public static boolean startMovement(Packet packet, Wire wire, boolean preserveCompatibility) {
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
        
        boolean isCompatible = packet.isCompatibleWithCurrentPort();
        if (!preserveCompatibility) {
            isCompatible = isPortCompatibleWithPacket(wire.getSource(), packet);
            packet.setCompatibleWithCurrentPort(isCompatible);
        }
        
        if (packet instanceof HexagonPacket) {
            HexagonPacketManager.initOnStart((HexagonPacket) packet, wire);
        }
        
        if (packet instanceof ConfidentialPacket) {
            ConfidentialPacketManager confidentialManager = new ConfidentialPacketManager(packet, wire);
            confidentialManagers.put(packet, confidentialManager);
        }

        
        if (packet instanceof model.entity.packets.TrianglePacket) {
            TrianglePacketManager.onStart((model.entity.packets.TrianglePacket) packet, isCompatible);
        }
        wire.setActive(false);
        if (isUpdating) {
            pendingAdds.add(packet);
        } else {
            movingPackets.add(packet);
        }
        if (packetController != null) {
            packetController.addPacket(packet);
        }
        
        return true;
    }

    private static boolean isPortCompatibleWithPacket(Port port, Packet packet) {
        if (port == null || packet == null) return false;
        return port.isCompatible(packet);
    }
    
    public static void updateMovingPackets(double deltaTimeSeconds) {
        if (level != null && level.isPaused()) return;
        isUpdating = true;
        Iterator<Packet> iterator = movingPackets.iterator();
        while (iterator.hasNext()) {
            Packet packet = iterator.next();
            updatePacketMovement(packet, deltaTimeSeconds);
            if (packetController != null) {
                packetController.updatePacket(packet);
            }
            
            boolean shouldComplete = false;
            if (packet instanceof HexagonPacket) {
                HexagonPacket hexPacket = (HexagonPacket) packet;
                shouldComplete = (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD && 
                                packet.getMovementProgress() >= 1.0);
            } else {
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

        manager.game.CollisionManager.cleanupOffWire(movingPackets, packetController);

        if (level != null) {
            for (model.entity.systems.System sys : level.getSystems()) {
                if (sys instanceof model.entity.systems.DistributorSystem) {
                    manager.systems.DistributorSystemManager mgr = new manager.systems.DistributorSystemManager((model.entity.systems.DistributorSystem) sys);
                    mgr.forwardPackets();
                }
            }
        }

        isUpdating = false;
        if (!pendingRemovals.isEmpty()) {
            movingPackets.removeAll(pendingRemovals);
            pendingRemovals.clear();
        }
        if (!pendingAdds.isEmpty()) {
            for (Packet p : pendingAdds) {
                if (p != null && !movingPackets.contains(p)) {
                    movingPackets.add(p);
                }
            }
            pendingAdds.clear();
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
        
        if (packet instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) packet;
            boolean inputCompatible = (wire.getDest() != null) && isPortCompatibleWithPacket(wire.getDest(), packet);
            double distanceTraveled = manager.game.MovementManager.updateHexagonDistance(hexPacket, wire, deltaTimeSeconds, inputCompatible);
            double progress = distanceTraveled / wireLength;

            if (level != null && !level.getAergiaMarks().isEmpty()) {
                long now = java.lang.System.nanoTime();
                for (model.logic.Shop.Aergia.AergiaMark mark : level.getAergiaMarks()) {
                    if (mark.wire == wire && mark.effectEndNanos > now && progress >= mark.progress) {
                        packet.setAergiaFreeze(hexPacket.getSpeed(), mark.effectEndNanos);
                        break;
                    }
                }
            }
            
            if (progress < 0.0) progress = 0.0;
            if (progress > 1.0) progress = 1.0;
            
            packet.setMovementProgress(progress);
            Point2D newPosition = wire.getPositionAtProgress(progress);
            
            packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
            
        } else if (packet instanceof ConfidentialPacket) {
            ConfidentialPacketManager confidentialManager = confidentialManagers.get(packet);
            if (confidentialManager != null) {
                confidentialManager.updateMovement(deltaTimeSeconds);
            } else {
                standardPacketMovement(packet, deltaTimeSeconds, wireLength);
            }
        } else {
            manager.game.MovementManager.moveStandard(packet, deltaTimeSeconds, wireLength, level);
        }
    }
    
    private static void standardPacketMovement(Packet packet, double deltaTimeSeconds, double wireLength) {
        manager.game.MovementManager.moveStandard(packet, deltaTimeSeconds, wireLength, level);
    }

    
    private static void completeMovement(Packet packet) {
        Wire wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }
        
        Point2D destPos = wire.getDest().getPosition();
        packet.setPosition(new Point2D(destPos.getX(), destPos.getY()));
        packet.setInSystem(true);
        packet.setMoving(false);
        packet.setCurrentWire(null);
        packet.setMovementProgress(0.0);
        packet.resetDeflection();
        
        wire.setActive(true);
        
        if (packet instanceof MassivePacket) {
            MassivePacketManager.changeInputPort(wire, packetController);
        }
        
        model.entity.systems.System destinationSystem = wire.getDest().getSystem();
        
        manager.game.MovementManager.deliver(packet, destinationSystem, level, packetController);

        MassivePacketManager.onComplete(wire, packet, packetController);
    }

    
    public static void removePacket(Packet packet) {
        if (isUpdating) {
            pendingRemovals.add(packet);
        } else {
            movingPackets.remove(packet);
        }
        
        if (packet instanceof ConfidentialPacket) {
            ConfidentialPacketManager manager = confidentialManagers.remove(packet);
            if (manager != null) {
                manager.cleanup();
            }
        }
        
        if (packetController != null) {
            packetController.removePacket(packet);
        }
    }
    
    public static List<Packet> getMovingPackets() {
        return new ArrayList<>(movingPackets);
    }
    
    public static boolean hasMovingPackets() {
        if (isUpdating) {
            int size = movingPackets.size() + pendingAdds.size() - pendingRemovals.size();
            return size > 0;
        }
        return !movingPackets.isEmpty();
    }
    
    public static void replacePacket(Packet oldPacket, Packet newPacket) {
        if (oldPacket == null || newPacket == null) return;
        boolean hadOld = movingPackets.contains(oldPacket) || pendingAdds.contains(oldPacket);
        if (hadOld) {
            if (isUpdating) {
                pendingRemovals.add(oldPacket);
                pendingAdds.add(newPacket);
            } else {
                movingPackets.remove(oldPacket);
                movingPackets.add(newPacket);
            }
            if (packetController != null) {
                packetController.removePacket(oldPacket);
                packetController.addPacket(newPacket);
            }
        }
    }
}

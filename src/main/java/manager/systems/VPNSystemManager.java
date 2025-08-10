package manager.systems;

import model.entity.systems.VPNSystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;
import model.entity.packets.ConfidentialPacket;
import model.entity.packets.PacketType;
import model.entity.ports.Port;
import manager.packets.PacketManager;
import model.levels.Level;

public class VPNSystemManager extends SystemManager<VPNSystem> {
    
    public interface VPNVisualUpdater {
        void updateVPNSystemVisuals();
    }
    private static VPNVisualUpdater visualUpdater;

    public VPNSystemManager(VPNSystem vpnSystem) {
        super(vpnSystem);
    }
    
    public static void setVisualUpdater(VPNVisualUpdater updater) {
        visualUpdater = updater;
    }

    public void receivePacket(Packet packet) {
        if (system.isDisabled()) {
            forwardPacketAsNormal(packet);
            return;
        }

        if (system.shouldDisableFromPacket(packet)) {
            disableAllVPNSystemsInLevel();
            convertAllProtectedPacketsGlobally();
            forwardPacketAsNormal(packet);
            return;
        }

        if (packet.getType() == PacketType.CONFIDENTIAL_TYPE1) {
            ConfidentialPacket.Type1 type1Packet = (ConfidentialPacket.Type1) packet;
            ConfidentialPacket.Type2 type2Packet = ConfidentialPacket.Type2.fromType1(type1Packet);
            
            processConfidentialType2Packet(type2Packet);
        }
        else if (packet.getType() == PacketType.TRIANGLE || 
            packet.getType() == PacketType.SQUARE || 
            packet.getType() == PacketType.HEXAGON) {

            ProtectedPacket protectedPacket = new ProtectedPacket(
                packet.getId(), 
                packet.getPosition(), 
                packet.getDirection(), 
                packet.getType()
            );
            
            protectedPacket.setCurrentHealth(packet.getCurrentHealth() * 2); // Double the current health
            
            processProtectedPacket(protectedPacket);
        } else {
            forwardPacketAsNormal(packet);
        }
    }

    private void processConfidentialType2Packet(ConfidentialPacket.Type2 packet) {
        enqueueWithOverflow(packet);
    }

    private void processProtectedPacket(Packet packet) {
        enqueueWithOverflow(packet);
    }

    private void forwardPacketAsNormal(Packet packet) {
        enqueueWithOverflow(packet);
    }
    
    private void enqueueWithOverflow(Packet packet) {
        if (!system.isFull()) {
            system.enqueuePacket(packet);
        } else {
            system.removeOldestPacket();
            system.enqueuePacket(packet);
        }
    }

    public void forwardPackets() {
        if (system.getStorageSize() == 0) {
            return;
        }

        Packet packet = system.peekNextPacket();
        if (packet == null) {
            return;
        }

        Port availablePort = findAvailableOutputPort(packet);
        if (availablePort != null) {
            system.dequeuePacket(); // Remove from storage
            PacketManager.sendPacket(availablePort, packet);
        }
    }

    private Port findAvailableOutputPort(Packet packet) {

        for (Port outPort : system.getOutPorts()) {
            if (outPort.isConnected() && outPort.getWire() != null && outPort.getWire().isActive()
                && isPacketCompatibleWithPort(packet, outPort)) {
                return outPort;
            }
        }
        
        for (Port outPort : system.getOutPorts()) {
            if (outPort.isConnected() && outPort.getWire() != null && outPort.getWire().isActive()) {
                return outPort;
            }
        }
        
        return null;
    }
    
    private boolean isPacketCompatibleWithPort(Packet packet, Port port) {
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            PacketType originalType = protectedPacket.getOriginalType();
            
            String portClassName = port.getClass().getSimpleName().toLowerCase();
            switch (originalType) {
                case SQUARE:
                    return portClassName.contains("square");
                case TRIANGLE:
                    return portClassName.contains("triangle");
                case HEXAGON:
                    return portClassName.contains("hexagon");
                default:
                    return false;
            }
        } else if (packet instanceof ConfidentialPacket) {
            return port.isCompatible(packet);
        } else {
            return port.isCompatible(packet);
        }
    }

    private void disableAllVPNSystemsInLevel() {
        if (level == null) return;
        disableAllVPNSystemsInLevelInternal(level);
        
        triggerVPNVisualUpdates();
    }
    
    private void triggerVPNVisualUpdates() {
        if (level == null) return;
        
        if (visualUpdater != null) {
            visualUpdater.updateVPNSystemVisuals();
        }
    }

    private void convertAllProtectedPacketsGlobally() {
        if (level == null) return;
        
        java.util.List<Packet> packetsToConvert = new java.util.ArrayList<>();
        java.util.List<Packet> convertedPackets = new java.util.ArrayList<>();

        for (Packet packet : level.getPackets()) {
            if (packet instanceof ProtectedPacket) {
                packetsToConvert.add(packet);
            }
        }

        for (model.entity.systems.System sys : level.getSystems()) {
            if (sys instanceof VPNSystem) {
                VPNSystem vs = (VPNSystem) sys;
                for (Packet packet : vs.getPackets()) {
                    if (packet instanceof ProtectedPacket && !packetsToConvert.contains(packet)) {
                        packetsToConvert.add(packet);
                    }
                }
            }
        }

        for (Packet packet : PacketManager.getMovingPackets()) {
            if (packet instanceof ProtectedPacket && !packetsToConvert.contains(packet)) {
                packetsToConvert.add(packet);
            }
        }

        for (Packet protectedPacket : packetsToConvert) {
            ProtectedPacket pPacket = (ProtectedPacket) protectedPacket;
            Packet originalPacket = convertProtectedToOriginalFull(pPacket);
            convertedPackets.add(originalPacket);
        }

        for (int i = 0; i < packetsToConvert.size(); i++) {
            Packet oldPacket = packetsToConvert.get(i);
            Packet newPacket = convertedPackets.get(i);

            if (level.getPackets().contains(oldPacket)) {
                level.removePacket(oldPacket);
                level.addPacket(newPacket);
            }

            for (model.entity.systems.System sys : level.getSystems()) {
                if (sys instanceof VPNSystem) {
                    VPNSystem vs = (VPNSystem) sys;
                    if (vs.getPackets().contains(oldPacket)) {
                        vs.getPackets().remove(oldPacket);
                        vs.enqueuePacket(newPacket);
                    }
                }
            }

            manager.packets.ProtectedPacketManager.convert(oldPacket, newPacket);
        }
        
    }

    public static void disableAllVPNSystemsInLevel(Level level) {
        if (level == null) return;
        disableAllVPNSystemsInLevelInternal(level);
    }

    public static void convertAllProtectedPacketsInLevel(Level level) {
        if (level == null) return;
        
        java.util.List<Packet> packetsToConvert = new java.util.ArrayList<>();
        java.util.List<Packet> convertedPackets = new java.util.ArrayList<>();
        
        for (Packet packet : level.getPackets()) {
            if (packet instanceof ProtectedPacket) {
                packetsToConvert.add(packet);
            }
        }
        
        for (Packet protectedPacket : packetsToConvert) {
            ProtectedPacket pPacket = (ProtectedPacket) protectedPacket;
            Packet originalPacket = convertProtectedToOriginalBasic(pPacket);
            convertedPackets.add(originalPacket);
            
        }
        
        for (int i = 0; i < packetsToConvert.size(); i++) {
            level.removePacket(packetsToConvert.get(i));
            level.addPacket(convertedPackets.get(i));
        }
    }

    private static void disableAllVPNSystemsInLevelInternal(Level level) {
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof VPNSystem) {
                VPNSystem vpnSystem = (VPNSystem) system;
                if (!vpnSystem.isDisabled()) {
                    vpnSystem.disable();
                }
            }
        }
    }

    private static Packet convertProtectedToOriginalBasic(ProtectedPacket protectedPacket) {
        Packet originalPacket = protectedPacket.convertToOriginalType();
        originalPacket.setPosition(protectedPacket.getPosition());
        originalPacket.setDirection(protectedPacket.getDirection());
        originalPacket.setMoving(protectedPacket.isMoving());
        originalPacket.setCurrentWire(protectedPacket.getCurrentWire());
        originalPacket.setMovementProgress(protectedPacket.getMovementProgress());
        originalPacket.setStartPosition(protectedPacket.getStartPosition());
        originalPacket.setTargetPosition(protectedPacket.getTargetPosition());
        return originalPacket;
    }

    private static Packet convertProtectedToOriginalFull(ProtectedPacket protectedPacket) {
        Packet originalPacket = convertProtectedToOriginalBasic(protectedPacket);
        originalPacket.setInSystem(protectedPacket.isInSystem());
        originalPacket.applyDeflection(protectedPacket.getDeflectedX(), protectedPacket.getDeflectedY());
        return originalPacket;
    }
} 
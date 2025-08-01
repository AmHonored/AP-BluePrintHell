package manager.systems;

import model.entity.systems.VPNSystem;
import model.entity.packets.Packet;
import model.entity.packets.ProtectedPacket;
import model.entity.packets.ConfidentialPacket;
import model.entity.packets.PacketType;
import model.entity.ports.Port;
import manager.packets.PacketManager;
import model.levels.Level;

public class VPNSystemManager {
    
    /**
     * Interface for updating VPN system visuals
     */
    public interface VPNVisualUpdater {
        void updateVPNSystemVisuals();
    }
    private final VPNSystem vpnSystem;
    private Level level; // For global VPN failure handling
    private static VPNVisualUpdater visualUpdater; // For triggering visual updates

    public VPNSystemManager(VPNSystem vpnSystem) {
        this.vpnSystem = vpnSystem;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
    
    /**
     * Set the visual updater for VPN systems
     */
    public static void setVisualUpdater(VPNVisualUpdater updater) {
        visualUpdater = updater;
    }

    /**
     * Receive a packet into the VPN system
     * Check if packet should disable the system, then convert or forward
     */
    public void receivePacket(Packet packet) {
        if (vpnSystem.isDisabled()) {
            // If VPN is disabled, act like a normal intermediate system
            forwardPacketAsNormal(packet);
            return;
        }

        // Check if this packet should disable the VPN system
        if (vpnSystem.shouldDisableFromPacket(packet)) {
            System.out.println("🚨 VPN SYSTEM DISABLED by high-speed packet: " + packet.getId() + " (speed: " + packet.getSpeed() + ")");
            
            // Disable ALL VPN systems in the level when any one fails
            disableAllVPNSystemsInLevel();
            
            // Convert all protected packets globally when VPN fails
            convertAllProtectedPacketsGlobally();
            
            // Forward this packet normally (since VPN is now disabled)
            forwardPacketAsNormal(packet);
            return;
        }

        // Convert confidential Type 1 to Type 2
        if (packet.getType() == PacketType.CONFIDENTIAL_TYPE1) {
            ConfidentialPacket.Type1 type1Packet = (ConfidentialPacket.Type1) packet;
            ConfidentialPacket.Type2 type2Packet = ConfidentialPacket.Type2.fromType1(type1Packet);
            
            System.out.println("🔒 CONFIDENTIAL PACKET TRANSFORMED: " + packet.getId() + " (TYPE1 → TYPE2) through VPN");
            
            // Process the transformed packet
            processConfidentialType2Packet(type2Packet);
        }
        // Convert packet to protected packet if it's a supported type
        else if (packet.getType() == PacketType.TRIANGLE || 
            packet.getType() == PacketType.SQUARE || 
            packet.getType() == PacketType.HEXAGON) {
            
            // Create protected packet with same ID, position, direction
            ProtectedPacket protectedPacket = new ProtectedPacket(
                packet.getId(), 
                packet.getPosition(), 
                packet.getDirection(), 
                packet.getType()
            );
            
            // Copy current health (might be reduced from damage)
            protectedPacket.setCurrentHealth(packet.getCurrentHealth() * 2); // Double the current health
            
            System.out.println("🛡️ PACKET PROTECTED: " + packet.getId() + " (" + packet.getType() + 
                " → PROTECTED with " + protectedPacket.getInheritedMovement() + " movement)");
            
            // Process the protected packet
            processProtectedPacket(protectedPacket);
        } else {
            // Forward non-convertible packets normally
            forwardPacketAsNormal(packet);
        }
    }

    /**
     * Process a confidential Type 2 packet (add to storage and forward)
     */
    private void processConfidentialType2Packet(ConfidentialPacket.Type2 packet) {
        if (!vpnSystem.isFull()) {
            vpnSystem.enqueuePacket(packet);
        } else {
            // Remove oldest packet if storage is full
            vpnSystem.removeOldestPacket();
            vpnSystem.enqueuePacket(packet);
        }
    }

    /**
     * Process a protected packet (add to storage and forward)
     */
    private void processProtectedPacket(Packet packet) {
        if (!vpnSystem.isFull()) {
            vpnSystem.enqueuePacket(packet);
        } else {
            // Remove oldest packet if storage is full
            vpnSystem.removeOldestPacket();
            vpnSystem.enqueuePacket(packet);
        }
    }

    /**
     * Forward packet without conversion (for disabled VPN or non-convertible packets)
     */
    private void forwardPacketAsNormal(Packet packet) {
        if (!vpnSystem.isFull()) {
            vpnSystem.enqueuePacket(packet);
        } else {
            // Remove oldest packet if storage is full
            vpnSystem.removeOldestPacket();
            vpnSystem.enqueuePacket(packet);
        }
    }

    /**
     * Forward packets from VPN system to output ports
     */
    public void forwardPackets() {
        if (vpnSystem.getStorageSize() == 0) {
            return;
        }

        Packet packet = vpnSystem.peekNextPacket();
        if (packet == null) {
            return;
        }

        // Find available output port
        Port availablePort = findAvailableOutputPort(packet);
        if (availablePort != null) {
            vpnSystem.dequeuePacket(); // Remove from storage
            PacketManager.sendPacket(availablePort, packet);
        }
    }

    /**
     * Find an available output port for forwarding packets
     * For protected packets, consider their original type for compatibility
     */
    private Port findAvailableOutputPort(Packet packet) {
        // First pass: compatible and available ports
        for (Port outPort : vpnSystem.getOutPorts()) {
            if (outPort.isConnected() && outPort.getWire().isAvailable() && 
                isPacketCompatibleWithPort(packet, outPort)) {
                return outPort;
            }
        }
        
        // Second pass: any available port (fallback)
        for (Port outPort : vpnSystem.getOutPorts()) {
            if (outPort.isConnected() && outPort.getWire().isAvailable()) {
                return outPort;
            }
        }
        
        return null;
    }
    
    /**
     * Check if a packet is compatible with a port, considering protected packets' original types
     */
    private boolean isPacketCompatibleWithPort(Packet packet, Port port) {
        // For protected packets, check compatibility based on original type
        if (packet instanceof ProtectedPacket) {
            ProtectedPacket protectedPacket = (ProtectedPacket) packet;
            PacketType originalType = protectedPacket.getOriginalType();
            
            // Check if port type matches the original packet type
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
            // Confidential packets are generally compatible with most ports
            // but avoid ports that might be incompatible with their pentagon shape
            return port.isCompatible(packet);
        } else {
            // For regular packets, use the port's standard compatibility check
            return port.isCompatible(packet);
        }
    }

    /**
     * Disable all VPN systems in the level when any VPN system fails
     * VPN systems are part of a connected network - if one fails, they all fail
     */
    private void disableAllVPNSystemsInLevel() {
        if (level == null) return;
        
        // Find and disable all VPN systems in the level
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof VPNSystem) {
                VPNSystem vpnSystem = (VPNSystem) system;
                if (!vpnSystem.isDisabled()) {
                    vpnSystem.disable();
                    System.out.println("🚨 CASCADING VPN FAILURE: VPN system at " + vpnSystem.getPosition() + " disabled");
                }
            }
        }
        
        // Trigger visual updates for all VPN systems 
        triggerVPNVisualUpdates();
    }
    
    /**
     * Trigger visual updates for all VPN systems in the level
     */
    private void triggerVPNVisualUpdates() {
        if (level == null) return;
        
        System.out.println("🔄 VISUAL UPDATE: Triggering VPN system visual updates");
        
        // Call the visual updater if available
        if (visualUpdater != null) {
            visualUpdater.updateVPNSystemVisuals();
            System.out.println("✅ VPN VISUAL UPDATE: Successfully updated VPN system visual indicators");
        } else {
            System.out.println("⚠️ VPN VISUAL UPDATE: No visual updater available - VPN indicators may not reflect disabled state");
        }
    }

    /**
     * Convert all protected packets globally when VPN system fails
     * This should be called when any VPN system gets disabled
     */
    private void convertAllProtectedPacketsGlobally() {
        if (level == null) return;
        
        // Create a list to hold conversions to avoid concurrent modification
        java.util.List<Packet> packetsToConvert = new java.util.ArrayList<>();
        java.util.List<Packet> convertedPackets = new java.util.ArrayList<>();
        
        // Find all protected packets in the level's packet list
        for (Packet packet : level.getPackets()) {
            if (packet instanceof ProtectedPacket) {
                packetsToConvert.add(packet);
            }
        }
        
        // Also find protected packets in VPN system storage
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof VPNSystem) {
                VPNSystem vpnSystem = (VPNSystem) system;
                for (Packet packet : vpnSystem.getPackets()) {
                    if (packet instanceof ProtectedPacket && !packetsToConvert.contains(packet)) {
                        packetsToConvert.add(packet);
                    }
                }
            }
        }
        
        // Find protected packets in moving packets (PacketManager)
        for (Packet packet : PacketManager.getMovingPackets()) {
            if (packet instanceof ProtectedPacket && !packetsToConvert.contains(packet)) {
                packetsToConvert.add(packet);
            }
        }
        
        System.out.println("🔍 VPN FAILURE: Found " + packetsToConvert.size() + " protected packets to convert");
        
        // Convert each protected packet to its original type
        for (Packet protectedPacket : packetsToConvert) {
            ProtectedPacket pPacket = (ProtectedPacket) protectedPacket;
            Packet originalPacket = pPacket.convertToOriginalType();
            
            // Copy movement state and important properties
            originalPacket.setPosition(protectedPacket.getPosition());
            originalPacket.setDirection(protectedPacket.getDirection());
            originalPacket.setMoving(protectedPacket.isMoving());
            originalPacket.setCurrentWire(protectedPacket.getCurrentWire());
            originalPacket.setMovementProgress(protectedPacket.getMovementProgress());
            originalPacket.setStartPosition(protectedPacket.getStartPosition());
            originalPacket.setTargetPosition(protectedPacket.getTargetPosition());
            originalPacket.setInSystem(protectedPacket.isInSystem());
            
            // Copy deflection state
            originalPacket.applyDeflection(protectedPacket.getDeflectedX(), protectedPacket.getDeflectedY());
            
            convertedPackets.add(originalPacket);
            
            System.out.println("🔄 VPN FAILURE - PROTECTED PACKET REVERTED: " + protectedPacket.getId() + 
                " (PROTECTED → " + pPacket.getOriginalType() + ") - Original movement restored");
        }
        
        // Replace protected packets with original packets in all locations
        for (int i = 0; i < packetsToConvert.size(); i++) {
            Packet oldPacket = packetsToConvert.get(i);
            Packet newPacket = convertedPackets.get(i);
            
            // Replace in level's packet list
            if (level.getPackets().contains(oldPacket)) {
                level.removePacket(oldPacket);
                level.addPacket(newPacket);
            }
            
            // Replace in VPN system storage
            for (model.entity.systems.System system : level.getSystems()) {
                if (system instanceof VPNSystem) {
                    VPNSystem vpnSystem = (VPNSystem) system;
                    if (vpnSystem.getPackets().contains(oldPacket)) {
                        vpnSystem.getPackets().remove(oldPacket);
                        vpnSystem.enqueuePacket(newPacket);
                    }
                }
            }
            
            // Notify PacketManager about the conversion for visual updates
            PacketManager.convertProtectedPacket(oldPacket, newPacket);
        }
        
        System.out.println("🔄 VPN FAILURE COMPLETE: " + packetsToConvert.size() + " protected packets reverted to original types");
    }

    /**
     * Static method to disable all VPN systems in a level
     * This can be called from any VPN system manager when a VPN fails
     */
    public static void disableAllVPNSystemsInLevel(Level level) {
        if (level == null) return;
        
        // Find and disable all VPN systems in the level
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof VPNSystem) {
                VPNSystem vpnSystem = (VPNSystem) system;
                if (!vpnSystem.isDisabled()) {
                    vpnSystem.disable();
                    System.out.println("🚨 GLOBAL VPN FAILURE: VPN system at " + vpnSystem.getPosition() + " disabled");
                }
            }
        }
    }

    /**
     * Static method to convert all protected packets globally from any VPN system
     * This can be called by any VPN system manager when a VPN fails
     */
    public static void convertAllProtectedPacketsInLevel(Level level) {
        if (level == null) return;
        
        // Create a list to hold conversions to avoid concurrent modification
        java.util.List<Packet> packetsToConvert = new java.util.ArrayList<>();
        java.util.List<Packet> convertedPackets = new java.util.ArrayList<>();
        
        // Find all protected packets in the level
        for (Packet packet : level.getPackets()) {
            if (packet instanceof ProtectedPacket) {
                packetsToConvert.add(packet);
            }
        }
        
        // Convert each protected packet to its original type
        for (Packet protectedPacket : packetsToConvert) {
            ProtectedPacket pPacket = (ProtectedPacket) protectedPacket;
            Packet originalPacket = pPacket.convertToOriginalType();
            
            // Copy movement state
            originalPacket.setPosition(protectedPacket.getPosition());
            originalPacket.setDirection(protectedPacket.getDirection());
            originalPacket.setMoving(protectedPacket.isMoving());
            originalPacket.setCurrentWire(protectedPacket.getCurrentWire());
            originalPacket.setMovementProgress(protectedPacket.getMovementProgress());
            originalPacket.setStartPosition(protectedPacket.getStartPosition());
            originalPacket.setTargetPosition(protectedPacket.getTargetPosition());
            
            convertedPackets.add(originalPacket);
            
            System.out.println("🔄 GLOBAL VPN FAILURE - PROTECTED PACKET REVERTED: " + protectedPacket.getId() + 
                " (PROTECTED → " + pPacket.getOriginalType() + ")");
        }
        
        // Replace protected packets with original packets in the level
        for (int i = 0; i < packetsToConvert.size(); i++) {
            level.removePacket(packetsToConvert.get(i));
            level.addPacket(convertedPackets.get(i));
        }
    }
} 
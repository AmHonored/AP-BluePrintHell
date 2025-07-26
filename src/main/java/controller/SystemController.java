package controller;

import model.entity.systems.System;
import model.entity.systems.StartSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.EndSystem;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.levels.Level;
import manager.packets.PacketManager;
import controller.PacketController;
import java.util.List;
import java.util.Random;

public class SystemController {
    private final Level level;
    private final PacketController packetController;
    private final Random random = new Random();
    private long lastPacketGenerationTime = 0;
    private static final long PACKET_GENERATION_INTERVAL = 1000; // Reduced to 1 second for more frequent collisions

    public SystemController(Level level, PacketController packetController) {
        this.level = level;
        this.packetController = packetController;
    }

    /**
     * Check if all systems are ready (all ports connected)
     */
    public boolean areAllSystemsReady() {
        for (System system : level.getSystems()) {
            if (!system.isReady()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Update all system ready states based on their connection status
     */
    public void updateAllSystemsReadyState() {
        for (System system : level.getSystems()) {
            system.updateReady();
        }
    }

    /**
     * Process all systems in the level
     */
    public void processSystems() {
        if (level.isPaused()) return;
        
        long currentTime = java.lang.System.currentTimeMillis();
        
        for (System system : level.getSystems()) {
            if (system instanceof StartSystem) {
                processStartSystem((StartSystem) system, currentTime);
            } else if (system instanceof IntermediateSystem) {
                processIntermediateSystem((IntermediateSystem) system);
            } else if (system instanceof EndSystem) {
                processEndSystem((EndSystem) system);
            }
        }
    }

    /**
     * Process StartSystem - generate packets at intervals
     */
    private void processStartSystem(StartSystem startSystem, long currentTime) {
        // Only generate packets if the game has been started
        if (!level.isGameStarted()) {
            return;
        }
        
        if (currentTime - lastPacketGenerationTime < PACKET_GENERATION_INTERVAL) {
            return;
        }

        // Try to generate packets from each output port
        for (Port outPort : startSystem.getOutPorts()) {
            if (outPort.isConnected() && outPort.getWire().isAvailable()) {
                Packet packet = startSystem.generatePacketIfPossible(outPort);
                if (packet != null) {
                    level.addPacket(packet);
                    level.incrementPacketsGenerated();
                    PacketManager.sendPacket(outPort, packet);
                    lastPacketGenerationTime = currentTime;
                    break; // Only generate one packet per cycle
                }
            }
        }
    }

    /**
     * Process IntermediateSystem - move packets through
     */
    private void processIntermediateSystem(IntermediateSystem intermediateSystem) {

        
        // REMOVED: Duplicate packet arrival processing - now handled by PacketManager.deliverToDestinationSystem()
        // The PacketManager automatically delivers packets when they complete movement
        
        // Use IntermediateSystemManager for consistent packet forwarding
        manager.systems.IntermediateSystemManager manager = 
            new manager.systems.IntermediateSystemManager(intermediateSystem);
        manager.forwardPackets();
    }

    /**
     * Process EndSystem - collect arriving packets
     * Note: EndSystem packet delivery is now handled directly in PacketManager.deliverToDestinationSystem()
     * when packets complete their movement, so this method is simplified.
     */
    private void processEndSystem(EndSystem endSystem) {
        // EndSystem processing is now handled automatically when packets complete movement
        // No additional processing needed here since deliverToDestinationSystem handles it
    }

    /**
     * Update system ready states
     */
    public void updateSystemStates() {
        for (System system : level.getSystems()) {
            system.updateReady();
        }
    }
}

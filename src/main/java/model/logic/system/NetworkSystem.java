package model.logic.system;

import model.entity.systems.System;
import model.entity.systems.StartSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.DDosSystem;
import model.entity.systems.SpySystem;
import model.entity.systems.EndSystem;
import model.entity.systems.VPNSystem;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.levels.Level;
import manager.packets.PacketManager;
import java.util.List;
import java.util.Random;

public class NetworkSystem {
    private final Level level;
    private final Random random = new Random();
    private long lastPacketGenerationTime = 0;
    private static final long PACKET_GENERATION_INTERVAL = 1000; // Reduced to 1 second for more frequent collisions

    public NetworkSystem(Level level) {
        this.level = level;
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
     * Process all systems in the network
     */
    public void processSystems() {
        if (level.isPaused()) return;
        
        long currentTime = java.lang.System.currentTimeMillis();
        
        for (System system : level.getSystems()) {
            if (system instanceof StartSystem) {
                processStartSystem((StartSystem) system, currentTime);
            } else if (system instanceof IntermediateSystem) {
                processIntermediateSystem((IntermediateSystem) system);
            } else if (system instanceof DDosSystem) {
                processDDosSystem((DDosSystem) system);
            } else if (system instanceof SpySystem) {
                processSpySystem((SpySystem) system);
            } else if (system instanceof VPNSystem) {
                processVPNSystem((VPNSystem) system);
            } else if (system instanceof model.entity.systems.AntiVirusSystem) {
                processAntiVirusSystem((model.entity.systems.AntiVirusSystem) system);
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
        // Use IntermediateSystemManager for consistent packet forwarding
        manager.systems.IntermediateSystemManager manager = 
            new manager.systems.IntermediateSystemManager(intermediateSystem);
        manager.forwardPackets();
    }

    /**
     * Process DDosSystem - move packets through with DDoS logic
     */
    private void processDDosSystem(DDosSystem ddosSystem) {
        // Use DDosSystemManager for consistent packet forwarding with DDoS behavior
        manager.systems.DDosSystemManager manager = 
            new manager.systems.DDosSystemManager(ddosSystem);
        manager.forwardPackets();
    }

    /**
     * Process SpySystem - move packets through with spy system logic
     * Note: Individual spy system processing is now handled at the network level
     * in GameController.forwardPacketsFromAnySpySystem()
     */
    private void processSpySystem(SpySystem spySystem) {
        // Individual spy system processing is now handled at the network level
        // This method is kept for compatibility but the actual logic is in
        // SpySystemManager.forwardPacketsFromAnySpySystem()
    }

    /**
     * Process VPNSystem - move packets through with VPN conversion logic
     */
    private void processVPNSystem(VPNSystem vpnSystem) {
        // Use VPNSystemManager for packet forwarding with VPN behavior
        manager.systems.VPNSystemManager manager = 
            new manager.systems.VPNSystemManager(vpnSystem);
        manager.setLevel(level);
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
     * Process AntiVirusSystem - clean trojan packets in range
     */
    private void processAntiVirusSystem(model.entity.systems.AntiVirusSystem antivirusSystem) {
        // Process any trojan packets within range
        manager.systems.AntiVirusSystemManager manager = 
            new manager.systems.AntiVirusSystemManager(antivirusSystem);
        manager.processActiveTrojanPackets(level.getPackets());
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

package model.logic.system;

import model.entity.systems.System;
import model.entity.systems.StartSystem;
import model.entity.systems.IntermediateSystem;
import model.entity.systems.DDosSystem;
import model.entity.systems.VPNSystem;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.levels.Level;
import manager.packets.PacketManager;

public class NetworkSystem {
    private final Level level;
    private long lastPacketGenerationTime = 0;
    private static final long PACKET_GENERATION_INTERVAL = 1000; 

    public NetworkSystem(Level level) {
        this.level = level;
    }

    public boolean areAllSystemsReady() {
        for (System system : level.getSystems()) {
            if (!system.isReady()) {
                return false;
            }
        }
        return true;
    }
    
    public void updateAllSystemsReadyState() {
        for (System system : level.getSystems()) {
            system.updateReady();
        }
    }

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
            } else if (system instanceof VPNSystem) {
                processVPNSystem((VPNSystem) system);
            } else if (system instanceof model.entity.systems.AntiVirusSystem) {
                processAntiVirusSystem((model.entity.systems.AntiVirusSystem) system);
            }
        }
    }

    private void processStartSystem(StartSystem startSystem, long currentTime) {

        if (!level.isGameStarted()) {
            return;
        }

        if (!startSystem.canGenerateMore()) {
            return;
        }
        
        if (currentTime - lastPacketGenerationTime < PACKET_GENERATION_INTERVAL) {
            return;
        }

        for (Port outPort : startSystem.getOutPorts()) {
            if (outPort.isConnected() && outPort.getWire().isActive()) {
                Packet packet = startSystem.generatePacketIfPossible(outPort);
                if (packet != null) {
                    level.addPacket(packet);
                    level.incrementPacketsGenerated();
                    startSystem.onPacketGenerated();
                    PacketManager.sendPacket(outPort, packet);
                    lastPacketGenerationTime = currentTime;
                    break;
                }
            }
        }
    }

    private void processIntermediateSystem(IntermediateSystem intermediateSystem) {
        manager.systems.IntermediateSystemManager manager = 
            new manager.systems.IntermediateSystemManager(intermediateSystem);
        manager.forwardPackets();
    }

    private void processDDosSystem(DDosSystem ddosSystem) {
        manager.systems.DDosSystemManager manager = 
            new manager.systems.DDosSystemManager(ddosSystem);
        manager.forwardPackets();
    }

    private void processVPNSystem(VPNSystem vpnSystem) {
        manager.systems.VPNSystemManager manager = 
            new manager.systems.VPNSystemManager(vpnSystem);
        manager.setLevel(level);
        manager.forwardPackets();
    }

    private void processAntiVirusSystem(model.entity.systems.AntiVirusSystem antivirusSystem) {
        manager.systems.AntiVirusSystemManager manager = 
            new manager.systems.AntiVirusSystemManager(antivirusSystem);
        manager.processActiveTrojanPackets(level.getPackets());
    }

    public void updateSystemStates() {
        updateAllSystemsReadyState();
    }
}

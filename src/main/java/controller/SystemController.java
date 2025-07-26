package controller;

import model.logic.system.NetworkSystem;
import model.levels.Level;
import controller.PacketController;

public class SystemController {
    private final Level level;
    private final PacketController packetController;
    private final NetworkSystem networkSystem;

    public SystemController(Level level, PacketController packetController) {
        this.level = level;
        this.packetController = packetController;
        this.networkSystem = new NetworkSystem(level);
    }

    /**
     * Check if all systems are ready (all ports connected)
     */
    public boolean areAllSystemsReady() {
        return networkSystem.areAllSystemsReady();
    }
    
    /**
     * Update all system ready states based on their connection status
     */
    public void updateAllSystemsReadyState() {
        networkSystem.updateAllSystemsReadyState();
    }

    /**
     * Process all systems in the level
     */
    public void processSystems() {
        networkSystem.processSystems();
    }

    /**
     * Update system ready states
     */
    public void updateSystemStates() {
        networkSystem.updateSystemStates();
    }
}

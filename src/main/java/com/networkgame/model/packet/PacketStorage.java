package com.networkgame.model.packet;

import javafx.application.Platform;
import javafx.geometry.Point2D;

import java.util.List;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.packettype.messenger.SquarePacket;
import com.networkgame.model.state.GameState;

/**
 * Handles all packet storage operations including capacity management
 */
public class PacketStorage {
    private static final String ARRIVAL_TIME_PROPERTY = "arrivalTime";
    private static final String STORED_IN_SYSTEM_PROPERTY = "storedInSystem";
    private static final String STORAGE_SYSTEM_LABEL = "Storage System";
    private static final int MAX_QUEUE_SIZE = 5;
    private static final double STORAGE_SYSTEM_LEVEL1_WAIT_TIME = 3.0;
    private static final double LEVEL1_DEFAULT_WAIT_TIME = 1.5;
    private static final double DEFAULT_WAIT_TIME = 1.0;
    
    private final NetworkSystem parentSystem;
    private final List<Packet> packets;
    private int storageCapacity;
    private final GameState gameState;

    public PacketStorage(NetworkSystem parentSystem, List<Packet> packets, int storageCapacity, GameState gameState) {
        this.parentSystem = parentSystem;
        this.packets = packets;
        this.storageCapacity = storageCapacity;
        this.gameState = gameState;
    }

    public void reset() { packets.clear(); }

    /** Get the current capacity usage of the system */
    public int getCurrentCapacityUsed() {
        return packets.size();
    }
    
    /** Check if adding this packet would exceed system capacity */
    public boolean wouldExceedCapacity(Packet packet) {
        return !(parentSystem.isStartSystem() || parentSystem.isEndSystem()) 
               && packets.size() >= MAX_QUEUE_SIZE;
    }

    /** Get minimum processing time based on level */
    private double getMinimumProcessingTime() {
        if (gameState.getCurrentLevel() == 1) {
            return STORAGE_SYSTEM_LABEL.equals(parentSystem.getLabel()) 
                   ? STORAGE_SYSTEM_LEVEL1_WAIT_TIME : LEVEL1_DEFAULT_WAIT_TIME;
        }
        return DEFAULT_WAIT_TIME;
    }

    /** Stores a packet in this system's inner box */
    public void storePacket(Packet packet) {
        if (packets.contains(packet)) {
            parentSystem.getVisualManager().positionPacketInInnerBox(packet);
            return;
        }
        
        boolean isLevel1StorageSystem = isLevel1StorageSystem();
        
        handleCapacityOverflow(isLevel1StorageSystem);
        preparePacketForStorage(packet);
        packets.add(packet);
        updateVisuals(packet, isLevel1StorageSystem);
        
        parentSystem.setActive(true);
        logPacketStored(packet);
    }

    private boolean isLevel1StorageSystem() {
        return gameState != null && gameState.getCurrentLevel() == 1 
               && STORAGE_SYSTEM_LABEL.equals(parentSystem.getLabel());
    }
    
    private void handleCapacityOverflow(boolean isLevel1StorageSystem) {
        if (packets.size() >= storageCapacity && !isLevel1StorageSystem && !packets.isEmpty()) {
            Packet oldestPacket = packets.remove(0);
            
            if (gameState != null) {
                gameState.incrementLostPackets();
                gameState.getActivePackets().remove(oldestPacket);
            }
            
            System.out.println("Removed oldest packet due to system capacity overflow");
        }
    }
    
    private void preparePacketForStorage(Packet packet) {
        if (!packet.hasProperty(ARRIVAL_TIME_PROPERTY)) {
            packet.setProperty(ARRIVAL_TIME_PROPERTY, System.currentTimeMillis());
        }
        packet.setInsideSystem(true);
    }
    
    private void updateVisuals(Packet packet, boolean isLevel1StorageSystem) {
        parentSystem.getVisualManager().updateCapacityVisual();
        
        if (gameState != null) {
            gameState.updateCapacityUsed();
                    if (gameState.getUIUpdateListener() != null) {
            Platform.runLater(() -> gameState.getUIUpdateListener().render());
        }
        }
        
        parentSystem.getVisualManager().positionPacketInInnerBox(packet);
        
        if (isLevel1StorageSystem && packet instanceof SquarePacket) {
            double centerX = parentSystem.getPosition().getX() + parentSystem.getWidth()/2;
            double centerY = parentSystem.getPosition().getY() + parentSystem.getHeight()/2;
            
            parentSystem.getVisualManager().enhanceStoredPacketVisibility(packet, centerX, centerY);
            packet.setProperty(STORED_IN_SYSTEM_PROPERTY, true);
            System.out.println("Enhancing stored square packet visibility for Level 1 demonstration");
        }
    }
    
    private void logPacketStored(Packet packet) {
        System.out.println(parentSystem.getLabel() + ": Stored packet, now have " + 
                           packets.size() + " packets (type: " + packet.getType() + 
                           "), capacity used: " + getCurrentCapacityUsed() + "/" + storageCapacity);
    }

    /** Process and return the next packet, or null if no packets */
    public Packet processDeliveredPacket() {
        if (packets.isEmpty()) return null;
        
        while (packets.size() > MAX_QUEUE_SIZE) {
            Packet removedPacket = packets.remove(packets.size() - 1);
            if (gameState != null) {
                gameState.getActivePackets().remove(removedPacket);
                gameState.incrementLostPackets();
            }
            System.out.println("Removed excess packet from system");
        }
        
        return packets.remove(0);
    }
    
    /** Safely remove a packet */
    public void removeStoredPacket(Packet packet) {
        packets.remove(packet);
        parentSystem.getVisualManager().updateCapacityVisual();
        if (gameState != null) gameState.updateCapacityUsed();
    }
    
    // Accessor methods
    public List<Packet> getPackets() { return packets; }
    public boolean isFull() { return getCurrentCapacityUsed() >= storageCapacity; }
    public boolean isEmpty() { return packets.isEmpty(); }
    public void clearPackets() { packets.clear(); }
    public void setStorageCapacity(int capacity) { this.storageCapacity = capacity; }
    public int getStorageCapacity() { return storageCapacity; }
}

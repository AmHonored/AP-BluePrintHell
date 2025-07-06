package com.networkgame.model.entity.system;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Interface for callbacks that managers can use to modify NetworkSystem state.
 * This breaks the circular dependency between NetworkSystem and managers.
 */
public interface NetworkSystemCallbacks {
    // Packet operations
    void addStoredPacket(Packet packet);
    void removeStoredPacket(Packet packet);
    void clearStoredPackets();
    void processStoredPacketsAfterNetworkChange();
    
    // Port operations
    void addInputPort(Port port);
    void addOutputPort(Port port);
    void updatePortPositions();
    
    // System state operations
    void setActive(boolean active);
    void setLabel(String label);
    void setPlayButton(Group playButton);
    void setStorageCapacity(int capacity);
    
    // Visual operations
    void moveBy(double dx, double dy);
    void updateIndicatorLamp();
    void updateCapacityVisual();
    void updateStoredPacketsVisibility();
    
    // Visual element setters
    void setShape(Rectangle shape);
    void setIndicatorLamp(Circle lamp);
    void setInnerBox(Rectangle innerBox);
    
    // Timeline operations
    void startPacketTransferTimeline();
    void stopPacketTransferTimeline();
    void startSendingPackets(double interval);
    void stopSendingPackets();
    void updatePacketGenerationSpeed(double speedMultiplier);
    
    // Cleanup
    void cleanup();
} 
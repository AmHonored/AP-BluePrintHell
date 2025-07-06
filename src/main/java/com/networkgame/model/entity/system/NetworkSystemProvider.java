package com.networkgame.model.entity.system;

import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameStateProvider;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.util.List;

/**
 * Interface providing read-only access to NetworkSystem properties.
 * Used by managers to avoid direct NetworkSystem dependencies.
 */
public interface NetworkSystemProvider {
    // Position and dimensions
    Point2D getPosition();
    double getWidth();
    double getHeight();
    
    // Ports
    List<Port> getInputPorts();
    List<Port> getOutputPorts();
    List<Port> getAllPorts();
    
    // System properties
    boolean isReference();
    boolean isActive();
    boolean isStartSystem();
    boolean isEndSystem();
    String getLabel();
    
    // Packet storage
    List<Packet> getStoredPackets();
    boolean isFull();
    boolean hasEmptyOutputPort();
    int getCurrentCapacityUsed();
    
    // Visual elements
    Rectangle getShape();
    Circle getIndicatorLamp();
    Rectangle getInnerBox();
    Group getPlayButton();
    boolean isIndicatorOn();
    boolean isFixedPosition();
    
    // Game state access
    GameStateProvider getGameStateProvider();
} 
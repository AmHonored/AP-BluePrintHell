package com.networkgame.model.entity; 

import javafx.geometry.Point2D;
import javafx.scene.shape.Shape;
import com.networkgame.model.entity.system.NetworkSystem;

public abstract class Port {
    protected Shape shape;
    protected Point2D position;
    protected boolean isInput;
    protected Packet.PacketType type;
    protected boolean isConnected;
    protected Connection connection;
    protected NetworkSystem system;
    
    public Port(Point2D position, boolean isInput, Packet.PacketType type, NetworkSystem system) {
        this.isInput = isInput;
        this.type = type;
        this.system = system;
        this.isConnected = false;
        
        // If position is null, it will be set when added to system
        this.position = position != null ? position : new Point2D(0, 0);
        
        // Creating the shape is deferred to subclasses
    }
    
    public Shape getShape() {
        return shape;
    }
    
    public Point2D getPosition() {
        return position;
    }
    
    public void setPosition(Point2D position) {
        this.position = position;
        updateShapePosition();
    }
    
    protected abstract void updateShapePosition();
    
    public boolean isInput() {
        return isInput;
    }
    
    public Packet.PacketType getType() {
        return type;
    }
    
    public boolean isConnected() {
        return isConnected;
    }
    
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
    
    public Connection getConnection() {
        return connection;
    }
    
    public void setConnection(Connection connection) {
        this.connection = connection;
        this.isConnected = (connection != null);
    }
    
    public NetworkSystem getSystem() {
        return system;
    }
    
    public boolean isCompatible(Packet packet) {
        return packet.getType() == this.type;
    }
    
    public boolean isInputPort() {
        return isInput;
    }
    
    public boolean isOutputPort() {
        return !isInput;
    }
} 

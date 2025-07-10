package com.networkgame.model.entity.packettype.messenger;

import javafx.geometry.Point2D;
import com.networkgame.model.entity.Packet;

/**
 * High-speed triangle packet that triggers VPN system failures
 * These packets have speed 120, which exceeds the VPN threshold of 100
 */
public class HighSpeedTrianglePacket extends TrianglePacket {
    
    private static final double HIGH_SPEED = 120.0;
    
    public HighSpeedTrianglePacket(Point2D position) {
        super(position);
        
        // Override speed to be high enough to trigger VPN failure
        setSpeed(HIGH_SPEED);
        
        // Mark this as a high-speed packet for identification
        setProperty("isHighSpeedPacket", true);
        setProperty("originalSpeed", HIGH_SPEED);
    }
    
    @Override
    public double getSpeed() {
        return HIGH_SPEED;
    }
    
    @Override
    public void setSpeed(double speed) {
        // Don't allow speed to be changed for high-speed packets
        super.setSpeed(HIGH_SPEED);
    }
    
    @Override
    public String toString() {
        return "HighSpeedTrianglePacket{id=" + getId() + ", speed=" + getSpeed() + "}";
    }
} 
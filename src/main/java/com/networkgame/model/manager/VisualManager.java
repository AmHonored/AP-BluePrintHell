package com.networkgame.model.manager; 

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Text;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;


/**
 * Handles all visual elements of the network system including shapes, colors, and visual updates
 */
public class VisualManager {
    // Constants
    private static final double INTERMEDIATE_WIDTH = 80.0;
    private static final double INTERMEDIATE_HEIGHT = 100.0;
    private static final double PADDING = 10.0;
    private static final double STANDARD_PACKET_SIZE = 12.0;
    private static final double LAMP_RADIUS = 5.0;
    private static final double INNER_BOX_PADDING = 3.0;
    private static final double INNER_BOX_USABLE_HEIGHT_RATIO = 0.2;
    private static final int MAX_CAPACITY = 5;
    private static final long VISIBILITY_UPDATE_INTERVAL = 83L; // ~12 updates per second
    private static final long PACKET_UPDATE_INTERVAL = 250L; // 1/4 second

    // Colors
    private static final Color MAIN_SHAPE_COLOR = Color.rgb(50, 50, 50);
    private static final Color STORAGE_BASE_COLOR = Color.rgb(80, 80, 80);
    private static final Color WARNING_COLOR = Color.rgb(220, 180, 0);
    private static final Color CRITICAL_COLOR = Color.rgb(220, 60, 0);
    private static final Color LAMP_ON_COLOR = Color.rgb(0, 119, 255);
    private static final Color LAMP_OFF_COLOR = Color.BLACK;
    private static final Color VPN_LAMP_ON_COLOR = Color.CYAN; // Cyan blue for VPN systems
    private static final Color VPN_LAMP_FAILED_COLOR = Color.RED; // Red for failed VPN systems
    private static final Color ENHANCED_STORAGE_COLOR = Color.rgb(60, 70, 120);

    // Class fields
    private final NetworkSystem parentSystem;
    private Rectangle shape;
    private Circle indicatorLamp;
    private Rectangle innerBox;
    private long lastVisibilityUpdateTime = 0L;

    public VisualManager(NetworkSystem parentSystem, Point2D position, double width, double height) {
        this.parentSystem = parentSystem;
        
        // Use system-specific preferred dimensions
        double systemWidth = parentSystem.getPreferredWidth(width);
        double systemHeight = parentSystem.getPreferredHeight(height);
        
        if (systemWidth != width || systemHeight != height) {
            System.out.println("Forcing system dimensions to " + systemWidth + "x" + systemHeight + " for: " + parentSystem.getLabel());
        }
        
        createMainShape(position, systemWidth, systemHeight);
        createInnerBox(position, systemWidth, systemHeight);
        createIndicatorLamp(position, systemWidth, systemHeight);
    }
    
    private void createMainShape(Point2D position, double width, double height) {
        this.shape = new Rectangle(position.getX(), position.getY(), width, height);
        this.shape.setFill(MAIN_SHAPE_COLOR);
        this.shape.setStroke(Color.BLACK);
        this.shape.setStrokeWidth(2);
        this.shape.setArcWidth(0);
        this.shape.setArcHeight(0);
        
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.rgb(0, 0, 0, 0.5));
        dropShadow.setRadius(5);
        dropShadow.setOffsetX(2);
        dropShadow.setOffsetY(2);
        this.shape.setEffect(dropShadow);
    }
    
    private void createInnerBox(Point2D position, double width, double height) {
        double indicatorPadding = height/4 + PADDING;
        this.innerBox = new Rectangle(
            position.getX() + PADDING, 
            position.getY() + indicatorPadding, 
            width - (PADDING * 2), 
            height - (indicatorPadding + PADDING)
        );
        
        applyStorageStyle(innerBox, STORAGE_BASE_COLOR);
    }
    
    private void applyStorageStyle(Rectangle box, Color baseColor) {
        LinearGradient storageGradient = createGradient(baseColor);
        box.setFill(storageGradient);
        box.setStroke(Color.rgb(100, 100, 100));
        box.setStrokeWidth(1.5);
        box.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
        box.setArcWidth(8);
        box.setArcHeight(8);
        
        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setRadius(4);
        innerShadow.setChoke(0.2);
        innerShadow.setColor(Color.rgb(0, 0, 0, 0.6));
        box.setEffect(innerShadow);
    }
    
    private LinearGradient createGradient(Color baseColor) {
        return new LinearGradient(
            0, 0, 0, 1, true, 
            CycleMethod.NO_CYCLE,
            new Stop(0, baseColor.brighter()),
            new Stop(1, baseColor)
        );
    }
    
    private void createIndicatorLamp(Point2D position, double width, double height) {
        this.indicatorLamp = new Circle(position.getX() + width/2, position.getY() + height/4, LAMP_RADIUS);
        this.indicatorLamp.setFill(LAMP_OFF_COLOR);
        this.indicatorLamp.setStroke(Color.GRAY);
        this.indicatorLamp.setStrokeWidth(1);
        this.indicatorLamp.getStyleClass().add("indicator-lamp");
        this.indicatorLamp.getStyleClass().add("indicator-lamp-off");
    }
    
    /**
     * Re-arranges stored packets in the inner box after a packet has been removed
     */
    public void rearrangeStoredPackets() {
        if (parentSystem == null || parentSystem.getPacketManager() == null) {
            System.out.println("Cannot rearrange packets: parent system or packet manager is null");
            return;
        }

        List<Packet> packets = parentSystem.getPacketManager().getPackets();
        if (innerBox == null || packets == null || packets.isEmpty()) {
            return;
        }
        
        // Calculate grid layout parameters
        int columns = Math.min(3, packets.size());
        int rows = (packets.size() + columns - 1) / columns;
        
        double cellWidth = innerBox.getWidth() / Math.max(columns, 1);
        double cellHeight = innerBox.getHeight() / Math.max(rows, 1);
        
        for (int i = 0; i < packets.size(); i++) {
            Packet packet = packets.get(i);
            if (packet == null) continue;
            
            int row = i / columns;
            int col = i % columns;
            
            // Calculate center position of this cell
            double centerX = innerBox.getX() + (col + 0.5) * cellWidth;
            double centerY = innerBox.getY() + (row + 0.5) * cellHeight;
            
            // Set packet position and update visibility
            packet.setPosition(new Point2D(centerX, centerY));
            enhanceStoredPacketVisibility(packet, centerX, centerY);
        }
    }
    
    /**
     * Position a packet visually inside the inner box
     */
    public void positionPacketInInnerBox(Packet packet) {
        if (packet == null || parentSystem == null || parentSystem.getPacketManager() == null) {
            System.out.println("Cannot position packet: packet, parent system, or packet manager is null");
            return;
        }

        List<Packet> packets = parentSystem.getPacketManager().getPackets();
        boolean hasManuallySetPosition = packet.hasProperty("manuallyPositionedInInnerBox");
        
        if (parentSystem.shouldPositionPacketsInInnerBox() && innerBox != null && !hasManuallySetPosition) {
            packet.setProperty("manuallyPositionedInInnerBox", true);
            
            int packetIndex = packets.indexOf(packet);
            int totalPackets = packets.size();
            
            Point2D position = calculatePacketPosition(packetIndex, totalPackets);
            packet.setPosition(position);
            enhanceStoredPacketVisibility(packet, position.getX(), position.getY());
            
            // Force a capacity update
            updateCapacityIfNeeded();
            
            System.out.println("Positioned packet in inner box at " + position.getX() + "," + position.getY());
        }
    }
    
    private Point2D calculatePacketPosition(int packetIndex, int totalPackets) {
        int cols = Math.max(1, (int)Math.ceil(Math.sqrt(totalPackets)));
        int row = packetIndex / cols;
        int col = packetIndex % cols;
        
        double innerBoxX = innerBox.getX();
        double innerBoxY = innerBox.getY();
        double innerBoxWidth = innerBox.getWidth();
        
        double padding = 5.0;
        double usableHeight = innerBox.getHeight() * 0.2; // Only use top 20% of inner box
        double topOffset = innerBoxY + 3.0; // Minimal top padding
        
        double cellWidth = (innerBoxWidth - 2 * padding) / cols;
        double cellHeight = (usableHeight - 3.0) / Math.max(1, (int)Math.ceil(totalPackets / (double)cols));
        
        double centerX = innerBoxX + padding + (col + 0.5) * cellWidth;
        double centerY = topOffset + (row + 0.5) * cellHeight;
        
        return new Point2D(centerX, centerY);
    }
    
    private void updateCapacityIfNeeded() {
        if (parentSystem.getGameState() != null) {
            Platform.runLater(() -> {
                parentSystem.getGameState().updateCapacityUsed();
            });
        }
    }
    
    /**
     * Enhance the visibility of a stored packet
     */
    public void enhanceStoredPacketVisibility(Packet packet, double centerX, double centerY) {
        Shape packetShape = packet.getShape();
        if (packetShape == null) return;
        
        packetShape.toFront();
        packetShape.setOpacity(1.0);
        
        applyPacketStyle(packet, packetShape, centerX, centerY);
        
        packet.setProperty("visuallyUpdated", System.currentTimeMillis());
    }
    
    private void applyPacketStyle(Packet packet, Shape packetShape, double centerX, double centerY) {
        Glow glow = new Glow(0.3);
        
        if (packet.getType() == Packet.PacketType.SQUARE) {
            Rectangle rect = (Rectangle) packetShape;
            rect.setWidth(STANDARD_PACKET_SIZE);
            rect.setHeight(STANDARD_PACKET_SIZE);
            rect.setX(centerX - STANDARD_PACKET_SIZE/2);
            rect.setY(centerY - STANDARD_PACKET_SIZE/2);
            rect.setEffect(glow);
            
        } else if (packet.getType() == Packet.PacketType.TRIANGLE) {
            Polygon triangle = (Polygon) packetShape;
            triangle.getPoints().clear();
            double height = STANDARD_PACKET_SIZE * 0.866; // height of equilateral triangle
            
            triangle.getPoints().addAll(
                centerX, centerY - height/2,
                centerX - STANDARD_PACKET_SIZE/2, centerY + height/2,
                centerX + STANDARD_PACKET_SIZE/2, centerY + height/2
            );
            triangle.setEffect(glow);
        }
    }
    
    /**
     * Updates the visual appearance of the system based on its storage usage
     */
    public void updateCapacityVisual() {
        // Get current capacity used
        int capacity = parentSystem.getPacketManager().getCurrentCapacityUsed();
        double capacityPercentage = capacity / (double)MAX_CAPACITY;
        
        // Check if this is the special storage system in Level 1
        boolean isSpecialStorage = isSpecialStorageSystem();
        Color baseColor = isSpecialStorage ? ENHANCED_STORAGE_COLOR : STORAGE_BASE_COLOR;
        
        // Determine fill color based on capacity
        Color fillColor = determineCapacityColor(capacityPercentage, baseColor);
        
        // Apply visual updates
        innerBox.setFill(createGradient(fillColor));
        innerBox.setEffect(null); // No glow effects regardless of capacity
        
        updateStrokeBasedOnCapacity(capacityPercentage);
    }
    
    private boolean isSpecialStorageSystem() {
        return parentSystem.getGameState() != null && 
               parentSystem.getGameState().getCurrentLevel() == 1 && 
               "Storage System".equals(parentSystem.getLabel());
    }
    
    private Color determineCapacityColor(double percentage, Color baseColor) {
        if (percentage < 0.4) {
            return baseColor;
        } else if (percentage < 0.7) {
            return baseColor.interpolate(WARNING_COLOR, (percentage - 0.4) / 0.3);
        } else if (percentage < 0.9) {
            return WARNING_COLOR;
        } else {
            return WARNING_COLOR.interpolate(CRITICAL_COLOR, (percentage - 0.9) / 0.1);
        }
    }
    
    private void updateStrokeBasedOnCapacity(double percentage) {
        if (percentage >= 0.9) {
            innerBox.setStroke(Color.rgb(250, 100, 0));
            innerBox.setStrokeWidth(2.5);
        } else if (percentage >= 0.7) {
            innerBox.setStroke(Color.rgb(220, 180, 0));
            innerBox.setStrokeWidth(2.0);
        } else if (percentage >= 0.4) {
            innerBox.setStroke(Color.rgb(180, 180, 100));
            innerBox.setStrokeWidth(1.8);
        } else {
            innerBox.setStroke(Color.rgb(100, 100, 100));
            innerBox.setStrokeWidth(1.5);
        }
    }
    
    /**
     * Updates the visibility of stored packets to ensure they are clearly visible
     * This is called from the GameScene update method
     */
    public void updateStoredPacketsVisibility() {
        List<Packet> packets = parentSystem.getPacketManager().getPackets();
        if (packets.isEmpty()) return;
        
        // Throttle updates to reduce overhead
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVisibilityUpdateTime < VISIBILITY_UPDATE_INTERVAL) {
            return;
        }
        lastVisibilityUpdateTime = currentTime;
        
        for (Packet packet : packets) {
            updatePacketVisibility(packet, currentTime);
        }
    }
    
    private void updatePacketVisibility(Packet packet, long currentTime) {
        Shape packetShape = packet.getShape();
        if (packetShape == null) return;
        
        // Skip if already processed recently
        if (packet.hasProperty("lastVisualUpdate")) {
            long lastUpdate = (long)packet.getProperty("lastVisualUpdate", 0L);
            if (currentTime - lastUpdate < PACKET_UPDATE_INTERVAL) {
                return;
            }
        }
        
        // Ensure visibility
        if (packetShape.getParent() != null) {
            packetShape.toFront();
        }
        packetShape.setVisible(true);
        packetShape.setOpacity(1.0);
        
        packet.setProperty("lastVisualUpdate", currentTime);
    }
    
    /**
     * Update indicator lamp based on port connection status
     */
    public void updateIndicatorLamp() {
        // Check if this is a VPN system with special indicator behavior
        boolean isVpnSystem = "vpn-system".equals(parentSystem.getSystemStyleClass());
        
        if (isVpnSystem) {
            updateVpnIndicatorLamp();
        } else {
            updateStandardIndicatorLamp();
        }
    }
    
    /**
     * Update indicator lamp for VPN systems (cyan when operational, red when failed, black when off)
     */
    private void updateVpnIndicatorLamp() {
        // Check if VPN system has failed
        try {
            java.lang.reflect.Method isFailedMethod = parentSystem.getClass().getMethod("isFailed");
            boolean isFailed = (Boolean) isFailedMethod.invoke(parentSystem);
            
            if (isFailed) {
                // VPN failed - show red
                indicatorLamp.setFill(VPN_LAMP_FAILED_COLOR);
                indicatorLamp.getStyleClass().removeAll("indicator-lamp-off", "indicator-lamp-on", "vpn-lamp-on");
                indicatorLamp.getStyleClass().add("vpn-lamp-failed");
                return;
            }
        } catch (Exception e) {
            // If we can't check failure status, fall back to normal behavior
        }
        
        boolean isCorrectlyConnected = checkCorrectConnections();
        
        if (isCorrectlyConnected) {
            // VPN operational - show cyan
            indicatorLamp.setFill(VPN_LAMP_ON_COLOR);
            indicatorLamp.getStyleClass().removeAll("indicator-lamp-off", "indicator-lamp-on", "vpn-lamp-failed");
            indicatorLamp.getStyleClass().add("vpn-lamp-on");
        } else {
            // VPN off - show black
            indicatorLamp.setFill(LAMP_OFF_COLOR);
            indicatorLamp.getStyleClass().removeAll("indicator-lamp-on", "vpn-lamp-on", "vpn-lamp-failed");
            indicatorLamp.getStyleClass().add("indicator-lamp-off");
        }
    }
    
    /**
     * Update indicator lamp for standard systems (blue when on, black when off)
     */
    private void updateStandardIndicatorLamp() {
        boolean isCorrectlyConnected = checkCorrectConnections();
        
        if (isCorrectlyConnected) {
            indicatorLamp.setFill(LAMP_ON_COLOR);
            indicatorLamp.getStyleClass().remove("indicator-lamp-off");
            indicatorLamp.getStyleClass().add("indicator-lamp-on");
        } else {
            indicatorLamp.setFill(LAMP_OFF_COLOR);
            indicatorLamp.getStyleClass().remove("indicator-lamp-on");
            indicatorLamp.getStyleClass().add("indicator-lamp-off");
        }
    }
    
    private boolean checkCorrectConnections() {
        return parentSystem.arePortsCorrectlyConnected();
    }
    
    /**
     * Move the visual elements by dx, dy
     * Ensures all system components (shape, inner box, indicator) move as one cohesive object
     */
    public void moveBy(double dx, double dy) {
        shape.setX(shape.getX() + dx);
        shape.setY(shape.getY() + dy);
        
        if (innerBox != null) {
            innerBox.setX(innerBox.getX() + dx);
            innerBox.setY(innerBox.getY() + dy);
        }
        
        indicatorLamp.setCenterX(indicatorLamp.getCenterX() + dx);
        indicatorLamp.setCenterY(indicatorLamp.getCenterY() + dy);
    }
    
    // Getters
    public Rectangle getShape() {
        return shape;
    }
    
    public Circle getIndicatorLamp() {
        return indicatorLamp;
    }
    
    public Rectangle getInnerBox() {
        return innerBox;
    }
    
    public boolean isIndicatorOn() {
        return indicatorLamp.getStyleClass().contains("indicator-lamp-on");
    }
    
    /**
     * Force the system to have specific dimensions, updating all visual components
     * @param width The new width
     * @param height The new height
     */
    public void forceSystemDimensions(double width, double height) {
        // Store original position
        double x = shape.getX();
        double y = shape.getY();
        
        // Update the main shape
        shape.setWidth(width);
        shape.setHeight(height);
        
        // Update inner box
        if (innerBox != null) {
            double indicatorPadding = height/4 + PADDING;
            
            innerBox.setX(x + PADDING);
            innerBox.setY(y + indicatorPadding);
            innerBox.setWidth(width - (PADDING * 2));
            innerBox.setHeight(height - (indicatorPadding + PADDING));
        }
        
        // Update indicator lamp - keep it centered horizontally
        if (indicatorLamp != null) {
            indicatorLamp.setCenterX(x + width/2);
            indicatorLamp.setCenterY(y + height/4);
        }
        
        updateSystemDimensionsReflectively(width, height);
    }
    
    private void updateSystemDimensionsReflectively(double width, double height) {
        if (parentSystem == null) return;
        
        java.lang.reflect.Field widthField = null;
        java.lang.reflect.Field heightField = null;
        
        try {
            widthField = NetworkSystem.class.getDeclaredField("width");
            heightField = NetworkSystem.class.getDeclaredField("height");
            
            widthField.setAccessible(true);
            heightField.setAccessible(true);
            
            widthField.set(parentSystem, width);
            heightField.set(parentSystem, height);
            
            // Update port positions for the new dimensions
            parentSystem.getPortManager().updatePortPositions();
            
            System.out.println("Successfully updated system dimensions to " + width + "x" + height);
        } catch (Exception e) {
            System.err.println("Failed to update system dimensions: " + e.getMessage());
        } finally {
            if (widthField != null) widthField.setAccessible(false);
            if (heightField != null) heightField.setAccessible(false);
        }
    }
} 

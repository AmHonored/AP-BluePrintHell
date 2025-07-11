package com.networkgame.model.entity.packettype.secret;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.system.NetworkSystem;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

/**
 * PentagonPacket is a private packet that moves at fixed speed on communication 
 * connections but implements collision avoidance when approaching systems that 
 * already contain other packets. It enhances the network capacity when entering systems.
 * 
 * Features:
 * - Size (health points): 4
 * - Coin value when reaching end systems: 3
 * - Red pentagon shape
 * - Collision avoidance: slows down when approaching occupied systems
 * - Network enhancement: adds 3 units to network when entering systems
 * - Fixed speed movement except under Impact effects or collision avoidance
 */
public class PentagonPacket extends Packet {
    
    // Constants for appearance and behavior
    private static final int DEFAULT_SIZE = 4;
    private static final double DEFAULT_SPEED = 85.0;
    private static final double VISUAL_SIZE = 16.0;
    private static final double COLLISION_DETECTION_DISTANCE = 80.0; // Distance to start detecting target system
    private static final int NETWORK_ENHANCEMENT_VALUE = 3; // Units added to network when entering systems
    
    // Different slow speeds for different situations (constant within each situation)
    private static final double SLOW_SPEED_SINGLE_OUTPUT = 25.0; // When system has 1 output wire
    private static final double SLOW_SPEED_MULTI_OUTPUT = 35.0;  // When system has multiple output wires
    private static final double SLOW_SPEED_NO_OUTPUT = 15.0;     // When system has no available outputs (emergency)
    private static final double SLOW_SPEED_FAST_PROCESSING = 45.0; // When system processes packets quickly
    
    // State management for collision avoidance
    private boolean avoidingCollision = false;
    private NetworkSystem targetSystem = null;
    
    /**
     * Creates a new PentagonPacket at the specified position.
     * 
     * @param position The initial position of the packet
     */
    public PentagonPacket(Point2D position) {
        super(position, PacketType.PENTAGON, DEFAULT_SIZE);
        initializeShape(position);
        this.currentSpeed = DEFAULT_SPEED;
        
        // Set properties for identification
        setProperty("isPentagonPacket", true);
        setProperty("networkEnhancementValue", NETWORK_ENHANCEMENT_VALUE);
    }
    
    /**
     * Initialize the pentagon shape for the packet.
     */
    private void initializeShape(Point2D position) {
        Polygon pentagon = new Polygon();
        updatePentagonPoints(pentagon, position);
        
        pentagon.setFill(getBaseColor());
        pentagon.setStroke(Color.BLACK);
        pentagon.setStrokeWidth(1.5);
        
        this.shape = pentagon;
    }
    
    /**
     * Updates the pentagon shape points based on position.
     * Creates a regular pentagon with the tip pointing up.
     */
    private void updatePentagonPoints(Polygon pentagon, Point2D center) {
        pentagon.getPoints().clear();
        
        double radius = VISUAL_SIZE / 2;
        
        // Create pentagon points (5 sides, starting from top)
        for (int i = 0; i < 5; i++) {
            double angle = -Math.PI / 2 + (2 * Math.PI * i / 5); // Start from top (-90 degrees)
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + radius * Math.sin(angle);
            pentagon.getPoints().addAll(x, y);
        }
    }
    
    @Override
    protected void updateShapePosition() {
        if (shape != null && shape instanceof Polygon) {
            updatePentagonPoints((Polygon) shape, position);
        }
    }
    
    @Override
    protected Color getBaseColor() {
        return Color.CRIMSON; // Red pentagon as specified
    }
    
    @Override
    public void update(double deltaTime) {
        // Perform collision avoidance check before standard update
        performCollisionAvoidanceCheck();
        
        // Call parent update method
        super.update(deltaTime);
    }
    
    /**
     * Checks if we're approaching a system that has packets stored and adjusts speed to time arrival perfectly.
     */
    private void performCollisionAvoidanceCheck() {
        Connection connection = getCurrentConnection();
        if (connection == null || isInsideSystem() || hasReachedEndSystem()) {
            resetCollisionAvoidance();
            return;
        }
        
        // Get target system from current connection
        NetworkSystem currentTargetSystem = connection.getTargetPort().getSystem();
        
        // Skip collision detection for non-storable systems (start/end systems)
        if (currentTargetSystem.isStartSystem() || currentTargetSystem.isEndSystem()) {
            resetCollisionAvoidance();
            return;
        }
        
        // Calculate distance to target system
        Point2D targetPos = connection.getTargetPort().getPosition();
        double distanceToTarget = position.distance(targetPos);
        
        // Log scanning when approaching detection range
        if (distanceToTarget <= COLLISION_DETECTION_DISTANCE + 20 && distanceToTarget > COLLISION_DETECTION_DISTANCE) {
            System.out.println("PentagonPacket " + getId() + ": Approaching collision detection range. Distance: " + 
                             String.format("%.1f", distanceToTarget) + " to system " + currentTargetSystem.getLabel());
        }
        
        // Check if we're within collision detection range
        if (distanceToTarget <= COLLISION_DETECTION_DISTANCE) {
            // Check if target system is busy (stored packets, input wire traffic, or output wire congestion)
            boolean systemIsBusy = isSystemBusy(currentTargetSystem);
            
            int storedPacketCount = (currentTargetSystem.getPacketManager() != null) ? 
                                   currentTargetSystem.getPacketManager().getPackets().size() : 0;
            
            int inputWireTraffic = countInputWireTraffic(currentTargetSystem);
            int occupiedOutputWires = countOccupiedOutputWires(currentTargetSystem);
            
            System.out.println("PentagonPacket " + getId() + ": Within detection range (distance: " + 
                             String.format("%.1f", distanceToTarget) + ") - System " + currentTargetSystem.getLabel());
            System.out.println("PentagonPacket " + getId() + ":     Stored packets: " + storedPacketCount + 
                             ", Input traffic: " + inputWireTraffic + ", Occupied outputs: " + occupiedOutputWires);
            System.out.println("PentagonPacket " + getId() + ":     System busy: " + systemIsBusy + 
                             ", Current speed: " + String.format("%.1f", currentSpeed));
            
            if (systemIsBusy && !avoidingCollision) {
                System.out.println("PentagonPacket " + getId() + ": *** BUSY SYSTEM DETECTED *** - Starting collision avoidance analysis");
                // Analyze system and calculate appropriate slow speed
                double optimalSlowSpeed = calculateOptimalSlowSpeed(currentTargetSystem, distanceToTarget);
                startCollisionAvoidance(currentTargetSystem, optimalSlowSpeed);
            } else if (!systemIsBusy && avoidingCollision && 
                      currentTargetSystem.equals(targetSystem)) {
                // System is now clear, resume normal speed
                System.out.println("PentagonPacket " + getId() + ": *** SYSTEM NOW CLEAR *** - Resuming normal speed");
                stopCollisionAvoidance();
            } else if (!systemIsBusy && !avoidingCollision) {
                System.out.println("PentagonPacket " + getId() + ": System is clear - maintaining normal speed " + DEFAULT_SPEED);
            }
        } 
        // If we're slowed down but far from target, reset to normal speed
        else if (avoidingCollision && distanceToTarget > COLLISION_DETECTION_DISTANCE) {
            System.out.println("PentagonPacket " + getId() + ": Now far from target (distance: " + 
                             String.format("%.1f", distanceToTarget) + ") - resetting collision avoidance");
            resetCollisionAvoidance();
        }
    }
    
    /**
     * Analyzes the target system to determine the optimal slow speed for timing arrival perfectly.
     */
    private double calculateOptimalSlowSpeed(NetworkSystem system, double currentDistance) {
        System.out.println("PentagonPacket " + getId() + ": ==> ANALYZING SYSTEM " + system.getLabel() + " for optimal speed");
        
        // Count available output connections
        int availableOutputs = 0;
        int totalOutputs = 0;
        
        for (Port outputPort : system.getOutputPorts()) {
            totalOutputs++;
            boolean isEmpty = outputPort.getConnection() != null && outputPort.getConnection().isEmpty();
            if (isEmpty) {
                availableOutputs++;
            }
            System.out.println("PentagonPacket " + getId() + ":     Output port " + outputPort.getType() + 
                             " - " + (isEmpty ? "AVAILABLE" : "OCCUPIED"));
        }
        
        // Estimate processing speed based on system characteristics
        boolean hasMultipleOutputs = totalOutputs > 1;
        boolean hasAvailableOutputs = availableOutputs > 0;
        int storedPacketsCount = system.getPacketManager().getPackets().size();
        int inputTraffic = countInputWireTraffic(system);
        int occupiedOutputs = countOccupiedOutputWires(system);
        
        System.out.println("PentagonPacket " + getId() + ":     Analysis: " + storedPacketsCount + " stored, " + 
                         inputTraffic + " incoming, " + occupiedOutputs + "/" + totalOutputs + " outputs busy, " +
                         availableOutputs + " available");
        
        double selectedSpeed;
        String speedReason;
        
        if (!hasAvailableOutputs) {
            // No available outputs - use slowest speed to give system time
            selectedSpeed = SLOW_SPEED_NO_OUTPUT;
            speedReason = "no available outputs (EMERGENCY SLOW)";
        } else if (inputTraffic > 2 || storedPacketsCount > 1) {
            // High traffic or multiple stored packets - use moderate speed
            selectedSpeed = SLOW_SPEED_SINGLE_OUTPUT;
            speedReason = "high traffic or multiple packets (MODERATE SLOW)";
        } else if (hasMultipleOutputs && occupiedOutputs < totalOutputs / 2) {
            // Multiple outputs with some free - can process faster
            selectedSpeed = SLOW_SPEED_MULTI_OUTPUT;
            speedReason = "multiple outputs partially available (MULTI-WIRE SLOW)";
        } else if (inputTraffic <= 1 && storedPacketsCount <= 1) {
            // Low traffic - expect fast processing
            selectedSpeed = SLOW_SPEED_FAST_PROCESSING;
            speedReason = "low traffic, fast processing expected (FAST PROCESSING)";
        } else {
            // Default moderate speed
            selectedSpeed = SLOW_SPEED_SINGLE_OUTPUT;
            speedReason = "default moderate speed (MODERATE SLOW)";
        }
        
        double timeToArrival = currentDistance / selectedSpeed;
        
        System.out.println("PentagonPacket " + getId() + ": ==> SPEED DECISION: " + selectedSpeed + 
                         " (" + speedReason + ")");
        System.out.println("PentagonPacket " + getId() + ":     Estimated arrival time: " + String.format("%.2f", timeToArrival) + " seconds");
        
        return selectedSpeed;
    }
    
    /**
     * Starts collision avoidance mode - reduces speed to time arrival when system becomes empty.
     */
    private void startCollisionAvoidance(NetworkSystem system, double slowSpeed) {
        if (!avoidingCollision) {
            double previousSpeed = this.currentSpeed;
            avoidingCollision = true;
            targetSystem = system;
            this.currentSpeed = slowSpeed;
            
            // Update velocity with new slower speed, maintaining direction
            if (velocity.magnitude() > 0) {
                velocity = velocity.normalize().multiply(currentSpeed);
            }
            
            double speedReduction = ((previousSpeed - slowSpeed) / previousSpeed) * 100;
            
            System.out.println("PentagonPacket " + getId() + ": *** COLLISION AVOIDANCE ACTIVATED ***");
            System.out.println("PentagonPacket " + getId() + ":     Speed reduced from " + String.format("%.1f", previousSpeed) + 
                             " to " + String.format("%.1f", slowSpeed) + " (" + String.format("%.1f", speedReduction) + "% reduction)");
            System.out.println("PentagonPacket " + getId() + ":     Target system: " + system.getLabel() + 
                             " (currently has " + system.getPacketManager().getPackets().size() + " packets)");
        }
    }
    
    /**
     * Stops collision avoidance mode - returns to normal speed.
     */
    private void stopCollisionAvoidance() {
        if (avoidingCollision) {
            double previousSpeed = this.currentSpeed;
            avoidingCollision = false;
            NetworkSystem previousTarget = targetSystem;
            targetSystem = null;
            this.currentSpeed = DEFAULT_SPEED;
            
            // Update velocity with new speed, maintaining direction
            if (velocity.magnitude() > 0) {
                velocity = velocity.normalize().multiply(currentSpeed);
            }
            
            double speedIncrease = ((DEFAULT_SPEED - previousSpeed) / previousSpeed) * 100;
            
            System.out.println("PentagonPacket " + getId() + ": *** COLLISION AVOIDANCE DEACTIVATED ***");
            System.out.println("PentagonPacket " + getId() + ":     Speed increased from " + String.format("%.1f", previousSpeed) + 
                             " to " + String.format("%.1f", DEFAULT_SPEED) + " (" + String.format("%.1f", speedIncrease) + "% increase)");
            System.out.println("PentagonPacket " + getId() + ":     Previous target: " + 
                             (previousTarget != null ? previousTarget.getLabel() : "none"));
        }
    }
    
    /**
     * Resets collision avoidance state completely.
     */
    private void resetCollisionAvoidance() {
        if (avoidingCollision) {
            stopCollisionAvoidance();
        }
    }
    
    /**
     * Determines if a system is busy by checking stored packets, input wire traffic, and output wire congestion.
     */
    private boolean isSystemBusy(NetworkSystem system) {
        // Check for stored packets
        boolean hasStoredPackets = system.getPacketManager() != null && 
                                  !system.getPacketManager().getPackets().isEmpty();
        
        // Check for input wire traffic (packets approaching the system)
        int inputTraffic = countInputWireTraffic(system);
        
        // Check for output wire congestion (multiple outputs occupied)
        int occupiedOutputs = countOccupiedOutputWires(system);
        int totalOutputs = system.getOutputPorts().size();
        
        // System is busy if:
        // 1. Has stored packets, OR
        // 2. Has incoming traffic on input wires, OR  
        // 3. Multiple output wires are occupied (congestion)
        boolean isBusy = hasStoredPackets || 
                        inputTraffic > 0 || 
                        (totalOutputs > 1 && occupiedOutputs >= totalOutputs / 2);
        
        return isBusy;
    }
    
    /**
     * Counts packets currently traveling on input wires towards this system.
     */
    private int countInputWireTraffic(NetworkSystem system) {
        int trafficCount = 0;
        
        for (Port inputPort : system.getInputPorts()) {
            Connection connection = inputPort.getConnection();
            if (connection != null && !connection.isEmpty()) {
                trafficCount += connection.getPackets().size();
            }
        }
        
        return trafficCount;
    }
    
    /**
     * Counts how many output wires currently have packets traveling on them.
     */
    private int countOccupiedOutputWires(NetworkSystem system) {
        int occupiedCount = 0;
        
        for (Port outputPort : system.getOutputPorts()) {
            Connection connection = outputPort.getConnection();
            if (connection != null && !connection.isEmpty()) {
                occupiedCount++;
            }
        }
        
        return occupiedCount;
    }
    
    @Override
    public boolean isCompatible(Port port) {
        // Pentagon packets are compatible with any port type
        return true;
    }
    
    /**
     * Override setCurrentConnection to track when we enter new connections.
     */
    @Override
    public void setCurrentConnection(Connection connection) {
        super.setCurrentConnection(connection);
        
        // Reset collision avoidance when entering a new connection
        if (connection != null) {
            resetCollisionAvoidance();
        }
    }
    
    /**
     * Override setInsideSystem to handle network enhancement when entering systems.
     */
    @Override
    public void setInsideSystem(boolean inside) {
        boolean wasOutside = !isInsideSystem();
        super.setInsideSystem(inside);
        
        // If we just entered a system, apply network enhancement
        if (inside && wasOutside) {
            applyNetworkEnhancement();
        }
        
        // Reset collision avoidance when entering a system
        if (inside) {
            resetCollisionAvoidance();
        }
    }
    
    /**
     * Applies network enhancement effect when entering a system.
     * Adds 3 units to the network (implementation depends on game mechanics).
     */
    private void applyNetworkEnhancement() {
        // Set property to indicate network enhancement has been applied
        setProperty("networkEnhanced", true);
        setProperty("enhancementAppliedAt", System.currentTimeMillis());
        
        System.out.println("PentagonPacket " + getId() + ": Applied network enhancement (+3 units)");
        
        // TODO: Implement actual network enhancement mechanism
        // This could involve:
        // - Increasing system capacity
        // - Boosting network throughput
        // - Enhancing connection efficiency
        // - Adding to global network metrics
    }
    
    /**
     * Get the collision avoidance status for debugging/monitoring.
     */
    public boolean isAvoidingCollision() {
        return avoidingCollision;
    }
    
    /**
     * Get the target system we're avoiding collision with.
     */
    public NetworkSystem getTargetSystem() {
        return targetSystem;
    }
    
    /**
     * Get the network enhancement value.
     */
    public int getNetworkEnhancementValue() {
        return NETWORK_ENHANCEMENT_VALUE;
    }
    
    @Override
    public String toString() {
        return "PentagonPacket{" +
               "id=" + getId() +
               ", size=" + getSize() +
               ", health=" + getHealth() +
               ", speed=" + getSpeed() +
               ", avoidingCollision=" + avoidingCollision +
               ", position=" + getPosition() +
               '}';
    }
} 
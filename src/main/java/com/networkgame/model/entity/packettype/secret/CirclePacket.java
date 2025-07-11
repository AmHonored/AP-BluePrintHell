package com.networkgame.model.entity.packettype.secret;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.system.NetworkSystem;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * CirclePacket is a secret packet created when a pentagon packet passes through a VPN system.
 * It implements advanced collision avoidance by maintaining distance toward all directions
 * with respect to the rear of network communications, preserving all other existing packets
 * on network systems.
 * 
 * Features:
 * - Size (health points): 5
 * - Coin value when reaching end systems: 4
 * - Cyan blue circle shape
 * - Enhanced collision avoidance: maintains distance from all other packets
 * - Bidirectional movement: can move backward or straight forward
 * - Preservation behavior: protects other packets in the network
 * - Origin: Created when pentagon packet passes through VPN system
 */
public class CirclePacket extends Packet {
    
    // Constants for appearance and behavior
    private static final int DEFAULT_SIZE = 5;
    private static final double DEFAULT_SPEED = 75.0;
    private static final double VISUAL_RADIUS = 10.0;
    private static final double COLLISION_DETECTION_DISTANCE = 100.0;
    
    // Adaptive preservation distance (set at creation time)
    private double targetPreservationDistance = 0; // Target average distance to maintain
    private boolean targetDistanceInitialized = false;
    
    // Different speeds for different movement behaviors
    private static final double BACKWARD_SPEED = 40.0;        // Speed when moving backward
    private static final double PRESERVATION_SPEED = 30.0;    // Speed when preserving other packets
    private static final double NORMAL_FORWARD_SPEED = 75.0;  // Normal forward speed
    private static final double FAST_FORWARD_SPEED = 95.0;    // Fast forward when path is clear
    
    // State management for preservation behavior
    private boolean preservingOtherPackets = false;
    private boolean movingBackward = false;
    private NetworkSystem targetSystem = null;
    private Point2D preservationTarget = null;
    
    /**
     * Analysis result for moving packets in the network
     */
    private static class MovingPacketAnalysis {
        boolean hasNearbyMovingPackets = false;
        double nearestPacketDistance = Double.MAX_VALUE;
        int packetsAhead = 0;
        int packetsBehind = 0;
        double averageDistanceAhead = 0.0;
        double averageDistanceBehind = 0.0;
    }
    
    /**
     * Creates a new CirclePacket at the specified position.
     * Typically created when a pentagon packet passes through a VPN system.
     * 
     * @param position The initial position of the packet
     */
    public CirclePacket(Point2D position) {
        super(position, PacketType.CIRCLE, DEFAULT_SIZE);
        initializeShape(position);
        this.currentSpeed = DEFAULT_SPEED;
        
        // Set properties for identification
        setProperty("isCirclePacket", true);
        setProperty("originType", "pentagon_vpn_enhanced");
        setProperty("preservationCapable", true);
        setProperty("bidirectionalMovement", true);
    }
    
    /**
     * Initializes the target preservation distance based on current network traffic.
     * This should be called immediately after the packet is created and placed in the network.
     */
    public void initializeTargetPreservationDistance() {
        if (targetDistanceInitialized) {
            System.out.println("🔵 CirclePacket " + getId() + ": Target distance already initialized (" + targetPreservationDistance + ")");
            return;
        }
        
        System.out.println("🔵 CirclePacket " + getId() + ": Initializing target preservation distance...");
        
        // Get all active packets from the game state
        Object gameStateObj = getProperty("gameState");
        if (!(gameStateObj instanceof com.networkgame.model.state.GameState)) {
            System.out.println("🔵 CirclePacket " + getId() + ": No GameState available, using default distance 120.0");
            targetPreservationDistance = 120.0;
            targetDistanceInitialized = true;
            return;
        }
        
        com.networkgame.model.state.GameState gameState = (com.networkgame.model.state.GameState) gameStateObj;
        double totalDistance = 0.0;
        int movingPacketCount = 0;
        
        for (Packet otherPacket : gameState.getActivePackets()) {
            if (otherPacket != this && otherPacket.getCurrentConnection() != null && !otherPacket.isInsideSystem()) {
                double distance = position.distance(otherPacket.getPosition());
                totalDistance += distance;
                movingPacketCount++;
                System.out.println("🔵 CirclePacket " + getId() + ": Found moving packet " + otherPacket.getId() + " at distance " + String.format("%.1f", distance));
            }
        }
        
        if (movingPacketCount > 0) {
            targetPreservationDistance = totalDistance / movingPacketCount;
            System.out.println("🔵 CirclePacket " + getId() + ": Calculated target distance: " + String.format("%.1f", targetPreservationDistance) + " (from " + movingPacketCount + " packets)");
        } else {
            targetPreservationDistance = 120.0; // Default fallback
            System.out.println("🔵 CirclePacket " + getId() + ": No moving packets found, using default distance 120.0");
        }
        
        targetDistanceInitialized = true;
    }
    
    /**
     * Calculates the current average distance to all moving packets in the network.
     * @return Average distance to moving packets, or 0 if no moving packets found
     */
    private double calculateCurrentAverageDistance() {
        Object gameStateObj = getProperty("gameState");
        if (!(gameStateObj instanceof com.networkgame.model.state.GameState)) {
            return 0;
        }
        
        com.networkgame.model.state.GameState gameState = (com.networkgame.model.state.GameState) gameStateObj;
        double totalDistance = 0.0;
        int movingPacketCount = 0;
        
        for (Packet otherPacket : gameState.getActivePackets()) {
            if (otherPacket != this && otherPacket.getCurrentConnection() != null && !otherPacket.isInsideSystem()) {
                double distance = position.distance(otherPacket.getPosition());
                totalDistance += distance;
                movingPacketCount++;
            }
        }
        
        return movingPacketCount > 0 ? totalDistance / movingPacketCount : 0;
    }
    
    /**
     * Recalculates the target preservation distance based on current network conditions.
     * This adapts to changing network traffic as packets enter/leave the system.
     */
    private void recalculateTargetPreservationDistance() {
        System.out.println("🔵 CirclePacket " + getId() + ": *** RECALCULATING TARGET PRESERVATION DISTANCE ***");
        
        double currentAverage = calculateCurrentAverageDistance();
        double oldTarget = targetPreservationDistance;
        
        if (currentAverage > 0) {
            // Smoothly adjust target distance (weighted average to avoid sudden changes)
            double weight = 0.3; // 30% new, 70% old
            this.targetPreservationDistance = (weight * currentAverage) + ((1 - weight) * targetPreservationDistance);
            
            System.out.println("🔵 CirclePacket " + getId() + ": Target distance updated from " + 
                             String.format("%.1f", oldTarget) + " to " + String.format("%.1f", targetPreservationDistance));
            System.out.println("🔵 CirclePacket " + getId() + ": Current network average: " + String.format("%.1f", currentAverage));
        } else {
            System.out.println("🔵 CirclePacket " + getId() + ": No other packets in network, keeping target distance: " + String.format("%.1f", targetPreservationDistance));
        }
    }
    
    /**
     * Initialize the circular shape for the packet.
     */
    private void initializeShape(Point2D position) {
        Circle circle = new Circle(position.getX(), position.getY(), VISUAL_RADIUS);
        
        circle.setFill(getBaseColor());
        circle.setStroke(Color.DARKBLUE);
        circle.setStrokeWidth(2.0);
        
        this.shape = circle;
    }
    
    @Override
    protected void updateShapePosition() {
        if (shape != null && shape instanceof Circle) {
            Circle circle = (Circle) shape;
            circle.setCenterX(position.getX());
            circle.setCenterY(position.getY());
        }
    }
    
    @Override
    protected Color getBaseColor() {
        return Color.CYAN; // Cyan blue as specified
    }
    
    @Override
    public void update(double deltaTime) {
        // Perform preservation and collision avoidance check before standard update
        performPreservationBehavior();
        
        // Call parent update method
        super.update(deltaTime);
    }
    
    /**
     * Implements the adaptive preservation behavior - maintains the target average distance
     * to other moving packets in the network.
     */
    private void performPreservationBehavior() {
        System.out.println("🔵 CirclePacket " + getId() + ": performPreservationBehavior() called");
        
        Connection connection = getCurrentConnection();
        System.out.println("🔵 CirclePacket " + getId() + ": Connection = " + (connection != null ? "YES" : "NULL"));
        System.out.println("🔵 CirclePacket " + getId() + ": Inside system = " + isInsideSystem());
        System.out.println("🔵 CirclePacket " + getId() + ": Reached end = " + hasReachedEndSystem());
        
        if (connection == null || isInsideSystem() || hasReachedEndSystem()) {
            System.out.println("🔵 CirclePacket " + getId() + ": Skipping preservation - not on connection");
            resetPreservationState();
            return;
        }
        
        // Initialize target distance if not done yet, or recalculate periodically
        if (!targetDistanceInitialized) {
            initializeTargetPreservationDistance();
        } else {
            // Recalculate target distance every 120 frames (~2 seconds at 60fps)
            if (System.currentTimeMillis() % 120 == 0) {
                recalculateTargetPreservationDistance();
            }
        }
        
        System.out.println("🔵 CirclePacket " + getId() + ": *** ADAPTIVE DISTANCE PRESERVATION ***");
        System.out.println("🔵 CirclePacket " + getId() + ": Target distance: " + String.format("%.1f", targetPreservationDistance));
        
        // Calculate current average distance to moving packets
        double currentAverageDistance = calculateCurrentAverageDistance();
        System.out.println("🔵 CirclePacket " + getId() + ": Current avg distance: " + String.format("%.1f", currentAverageDistance));
        
        if (currentAverageDistance > 0) { // We have other packets to consider
            double distanceDifference = currentAverageDistance - targetPreservationDistance;
            System.out.println("🔵 CirclePacket " + getId() + ": Distance difference: " + String.format("%.1f", distanceDifference));
            
            // Determine behavior based on distance difference
            if (distanceDifference < -20.0) { // Packets are getting too close
                System.out.println("🔵 CirclePacket " + getId() + ": *** DECISION: PACKETS TOO CLOSE ***");
                System.out.println("🔵 CirclePacket " + getId() + ": Currently moving backward: " + movingBackward);
                System.out.println("🔵 CirclePacket " + getId() + ": Currently preserving: " + preservingOtherPackets);
                
                if (!movingBackward) {
                    System.out.println("🔵 CirclePacket " + getId() + ": *** INITIATING BACKWARD MOVEMENT ***");
                    initiateBackwardMovement();
                } else {
                    System.out.println("🔵 CirclePacket " + getId() + ": *** ALREADY MOVING BACKWARD ***");
                }
                
                if (!preservingOtherPackets) {
                    System.out.println("🔵 CirclePacket " + getId() + ": *** STARTING PRESERVATION MODE ***");
                    startPreservationModeForMovingPackets(currentAverageDistance);
                } else {
                    System.out.println("🔵 CirclePacket " + getId() + ": *** ALREADY IN PRESERVATION MODE ***");
                }
            } else if (distanceDifference > 20.0) { // Packets are too far, safe to speed up
                System.out.println("🔵 CirclePacket " + getId() + ": *** DECISION: DISTANCE SUFFICIENT ***");
                if (movingBackward) {
                    System.out.println("🔵 CirclePacket " + getId() + ": *** RESUMING FORWARD MOVEMENT ***");
                    resumeForwardMovement();
                }
                if (preservingOtherPackets) {
                    stopPreservationMode();
                }
            } else { // Distance is within acceptable range
                System.out.println("🔵 CirclePacket " + getId() + ": *** DECISION: DISTANCE OPTIMAL - MAINTAINING CURRENT BEHAVIOR ***");
                System.out.println("🔵 CirclePacket " + getId() + ": Moving backward: " + movingBackward + ", Preserving: " + preservingOtherPackets);
            }
        } else {
            // No other moving packets, return to normal behavior
            if (preservingOtherPackets || movingBackward) {
                System.out.println("🔵 CirclePacket " + getId() + ": *** NO OTHER PACKETS - NORMAL BEHAVIOR ***");
                stopPreservationMode();
                resumeForwardMovement();
            }
        }
        
        // Adjust speed based on current behavior
        adjustSpeedBasedOnBehavior();
    }
    
    /**
     * Analyzes all moving packets in the network to determine preservation behavior.
     */
    private MovingPacketAnalysis analyzeMovingPacketsInNetwork() {
        System.out.println("🔵 CirclePacket " + getId() + ": analyzeMovingPacketsInNetwork() called");
        
        MovingPacketAnalysis analysis = new MovingPacketAnalysis();
        
        if (getCurrentConnection() == null) {
            System.out.println("🔵 CirclePacket " + getId() + ": No connection - returning empty analysis");
            return analysis;
        }
        
        // Get all active packets from the game state
        Object gameStateObj = getProperty("gameState");
        System.out.println("🔵 CirclePacket " + getId() + ": GameState property = " + (gameStateObj != null ? gameStateObj.getClass().getSimpleName() : "NULL"));
        
        int movingPacketsFound = 0;
        
        if (gameStateObj instanceof com.networkgame.model.state.GameState) {
             com.networkgame.model.state.GameState gameState = (com.networkgame.model.state.GameState) gameStateObj;
             System.out.println("🔵 CirclePacket " + getId() + ": Found GameState with " + gameState.getActivePackets().size() + " active packets");
             
             for (Packet otherPacket : gameState.getActivePackets()) {
                if (otherPacket != this && otherPacket.getCurrentConnection() != null && !otherPacket.isInsideSystem()) {
                    movingPacketsFound++;
                    System.out.println("🔵 CirclePacket " + getId() + ": Found moving packet " + otherPacket.getId() + " (" + otherPacket.getType() + ")");
                    // This packet is moving on a connection
                    double distance = position.distance(otherPacket.getPosition());
                    
                    // Use adaptive distance or default fallback
                    double preservationThreshold = targetDistanceInitialized ? (targetPreservationDistance * 1.5) : 120.0;
                    
                    if (distance <= preservationThreshold) {
                        analysis.hasNearbyMovingPackets = true;
                        
                        if (distance < analysis.nearestPacketDistance) {
                            analysis.nearestPacketDistance = distance;
                        }
                        
                        // Determine if packet is ahead or behind based on movement direction
                        if (isPacketAhead(otherPacket)) {
                            analysis.packetsAhead++;
                            analysis.averageDistanceAhead += distance;
                        } else {
                            analysis.packetsBehind++;
                            analysis.averageDistanceBehind += distance;
                        }
                        
                        System.out.println("CirclePacket " + getId() + ": Found moving packet " + 
                                         otherPacket.getId() + " at distance " + String.format("%.1f", distance) +
                                         " (" + (isPacketAhead(otherPacket) ? "ahead" : "behind") + ")");
                    }
                }
            }
        }
        
        // Calculate averages
        if (analysis.packetsAhead > 0) {
            analysis.averageDistanceAhead /= analysis.packetsAhead;
        }
        if (analysis.packetsBehind > 0) {
            analysis.averageDistanceBehind /= analysis.packetsBehind;
        }
        
        System.out.println("🔵 CirclePacket " + getId() + ": Analysis complete - Moving packets found: " + movingPacketsFound);
        System.out.println("🔵 CirclePacket " + getId() + ": Has nearby packets: " + analysis.hasNearbyMovingPackets);
        System.out.println("🔵 CirclePacket " + getId() + ": Packets ahead: " + analysis.packetsAhead + ", behind: " + analysis.packetsBehind);
        
        return analysis;
    }
    
    /**
     * Determines if another packet is ahead of this packet in terms of movement direction.
     */
    private boolean isPacketAhead(Packet otherPacket) {
        Connection myConnection = getCurrentConnection();
        Connection otherConnection = otherPacket.getCurrentConnection();
        
        if (myConnection == null || otherConnection == null) {
            return false;
        }
        
        // If on the same connection, check which is closer to the target
        if (myConnection == otherConnection) {
            Point2D targetPos = myConnection.getTargetPort().getPosition();
            double myDistanceToTarget = position.distance(targetPos);
            double otherDistanceToTarget = otherPacket.getPosition().distance(targetPos);
            return otherDistanceToTarget < myDistanceToTarget; // Other packet is closer to target = ahead
        }
        
        // If on different connections, use a more general spatial relationship
        // Check if the other packet is in the general forward direction
        double[] myDirection = getUnitVector();
        Point2D toOtherPacket = otherPacket.getPosition().subtract(position);
        double dotProduct = myDirection[0] * toOtherPacket.getX() + myDirection[1] * toOtherPacket.getY();
        return dotProduct > 0; // Positive dot product means ahead in movement direction
    }
    
    /**
     * Determines if backward movement should be initiated based on moving packets.
     */
    private boolean shouldMoveBackwardBasedOnMovingPackets(MovingPacketAnalysis analysis) {
        // Move backward if there are packets very close ahead
        if (analysis.packetsAhead > 0 && analysis.averageDistanceAhead < 60.0) {
            System.out.println("CirclePacket " + getId() + ": Moving backward - packets too close ahead (avg: " + 
                             String.format("%.1f", analysis.averageDistanceAhead) + ")");
            return true;
        }
        
        // Move backward if there are multiple packets ahead within medium range
        if (analysis.packetsAhead >= 2 && analysis.averageDistanceAhead < 90.0) {
            System.out.println("CirclePacket " + getId() + ": Moving backward - multiple packets ahead (count: " + 
                             analysis.packetsAhead + ", avg distance: " + String.format("%.1f", analysis.averageDistanceAhead) + ")");
            return true;
        }
        
        // Move backward if the nearest packet is very close regardless of direction
        if (analysis.nearestPacketDistance < 40.0) {
            System.out.println("CirclePacket " + getId() + ": Moving backward - nearest packet too close (" + 
                             String.format("%.1f", analysis.nearestPacketDistance) + ")");
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if a system needs preservation (has packets that should be protected).
     */
    private boolean checkIfSystemNeedsPreservation(NetworkSystem system) {
        if (system == null || system.isStartSystem() || system.isEndSystem()) {
            return false;
        }
        
        // Check if system has stored packets that need preservation
        boolean hasStoredPackets = system.getPacketManager() != null && 
                                  !system.getPacketManager().getPackets().isEmpty();
        
        // Check for congestion that CirclePacket can help alleviate
        int inputTraffic = countInputWireTraffic(system);
        int occupiedOutputs = countOccupiedOutputWires(system);
        int totalOutputs = system.getOutputPorts().size();
        
        boolean systemCongested = (totalOutputs > 1 && occupiedOutputs >= totalOutputs * 0.75);
        
        return hasStoredPackets || systemCongested || inputTraffic > 2;
    }
    
    /**
     * Determines if backward movement is necessary for preservation.
     */
    private boolean determineBackwardMovement(Connection connection, double distanceToTarget) {
        // Move backward if very close to a congested system
        if (distanceToTarget < 50.0 && checkIfSystemNeedsPreservation(connection.getTargetPort().getSystem())) {
            return true;
        }
        
        // Move backward if there are multiple packets very close ahead
        int packetsAhead = 0;
        for (Packet otherPacket : connection.getPackets()) {
            if (otherPacket != this) {
                Point2D targetPos = connection.getTargetPort().getPosition();
                double otherDistance = otherPacket.getPosition().distance(targetPos);
                double myDistance = position.distance(targetPos);
                
                if (otherDistance < myDistance && (myDistance - otherDistance) < 30.0) {
                    packetsAhead++;
                }
            }
        }
        
        return packetsAhead >= 2;
    }
    
    /**
     * Starts preservation mode with appropriate speed adjustment.
     */
    private void startPreservationMode(NetworkSystem system, double distance) {
        if (!preservingOtherPackets) {
            preservingOtherPackets = true;
            targetSystem = system;
            
            System.out.println("CirclePacket " + getId() + ": *** PRESERVATION MODE ACTIVATED ***");
            System.out.println("CirclePacket " + getId() + ":     Target system: " + 
                             (system != null ? system.getLabel() : "unknown"));
            System.out.println("CirclePacket " + getId() + ":     Distance to target: " + String.format("%.1f", distance));
        }
    }
    
    /**
     * Starts preservation mode based on proximity to moving packets.
     */
    private void startPreservationModeForMovingPackets(double nearestDistance) {
        if (!preservingOtherPackets) {
            preservingOtherPackets = true;
            targetSystem = null; // No specific target system when preserving for moving packets
            
            System.out.println("CirclePacket " + getId() + ": *** PRESERVATION MODE ACTIVATED (MOVING PACKETS) ***");
            System.out.println("CirclePacket " + getId() + ":     Nearest moving packet distance: " + String.format("%.1f", nearestDistance));
            System.out.println("CirclePacket " + getId() + ":     Maintaining distance from other moving packets");
        }
    }
    
    /**
     * Initiates backward movement to preserve network communications.
     */
    private void initiateBackwardMovement() {
        System.out.println("🔵 CirclePacket " + getId() + ": initiateBackwardMovement() called");
        System.out.println("🔵 CirclePacket " + getId() + ": Current movingBackward state: " + movingBackward);
        
        if (!movingBackward) {
            System.out.println("🔵 CirclePacket " + getId() + ": Setting movingBackward = true");
            movingBackward = true;
            
            System.out.println("🔵 CirclePacket " + getId() + ": Original unit vector: [" + unitVector[0] + ", " + unitVector[1] + "]");
            
            // Reverse the unit vector to move backward
            this.unitVector[0] = -this.unitVector[0];
            this.unitVector[1] = -this.unitVector[1];
            
            System.out.println("🔵 CirclePacket " + getId() + ": Reversed unit vector: [" + unitVector[0] + ", " + unitVector[1] + "]");
            
            // Update velocity with backward direction
            this.velocity = new Point2D(this.unitVector[0], this.unitVector[1]).multiply(BACKWARD_SPEED);
            this.currentSpeed = BACKWARD_SPEED;
            
            // CRITICAL: Prevent PacketRouter from overriding our direction
            setProperty("customDirectionControl", true);
            
            System.out.println("🔵 CirclePacket " + getId() + ": *** BACKWARD MOVEMENT INITIATED ***");
            System.out.println("🔵 CirclePacket " + getId() + ":     Speed set to " + BACKWARD_SPEED + " (backward)");
            System.out.println("🔵 CirclePacket " + getId() + ":     New velocity: " + velocity);
            System.out.println("🔵 CirclePacket " + getId() + ":     Custom direction control: ENABLED");
        } else {
            System.out.println("🔵 CirclePacket " + getId() + ": Already moving backward, no change needed");
        }
    }
    
    /**
     * Resumes forward movement when preservation allows.
     */
    private void resumeForwardMovement() {
        if (movingBackward) {
            movingBackward = false;
            
            // Restore forward direction
            this.unitVector[0] = -this.unitVector[0];
            this.unitVector[1] = -this.unitVector[1];
            
            // Update velocity with forward direction
            this.velocity = new Point2D(this.unitVector[0], this.unitVector[1]).multiply(NORMAL_FORWARD_SPEED);
            this.currentSpeed = NORMAL_FORWARD_SPEED;
            
            // Allow PacketRouter to control direction again
            setProperty("customDirectionControl", false);
            
            System.out.println("CirclePacket " + getId() + ": *** FORWARD MOVEMENT RESUMED ***");
            System.out.println("CirclePacket " + getId() + ":     Speed set to " + NORMAL_FORWARD_SPEED + " (forward)");
            System.out.println("CirclePacket " + getId() + ":     Custom direction control: DISABLED");
        }
    }
    
    /**
     * Stops preservation mode and returns to normal behavior.
     */
    private void stopPreservationMode() {
        if (preservingOtherPackets) {
            preservingOtherPackets = false;
            targetSystem = null;
            preservationTarget = null;
            
            // Ensure we're moving forward
            if (movingBackward) {
                resumeForwardMovement();
            }
            
            System.out.println("CirclePacket " + getId() + ": *** PRESERVATION MODE DEACTIVATED ***");
            System.out.println("CirclePacket " + getId() + ":     Returning to normal forward movement");
        }
    }
    
    /**
     * Resets all preservation state.
     */
    private void resetPreservationState() {
        if (preservingOtherPackets || movingBackward) {
            stopPreservationMode();
        }
    }
    
    /**
     * Adjusts speed based on current behavior mode.
     */
    private void adjustSpeedBasedOnBehavior() {
        System.out.println("🔵 CirclePacket " + getId() + ": adjustSpeedBasedOnBehavior() called");
        System.out.println("🔵 CirclePacket " + getId() + ": movingBackward=" + movingBackward + ", preservingOtherPackets=" + preservingOtherPackets);
        
        double previousSpeed = this.currentSpeed;
        Point2D previousVelocity = this.velocity;
        
        if (movingBackward) {
            this.currentSpeed = BACKWARD_SPEED;
            System.out.println("🔵 CirclePacket " + getId() + ": Using BACKWARD_SPEED: " + BACKWARD_SPEED);
        } else if (preservingOtherPackets) {
            this.currentSpeed = PRESERVATION_SPEED;
            System.out.println("🔵 CirclePacket " + getId() + ": Using PRESERVATION_SPEED: " + PRESERVATION_SPEED);
        } else {
            // Check if path is clear for fast movement
            Connection connection = getCurrentConnection();
            boolean pathClear = connection != null && connection.getPackets().size() <= 1; // Only this packet
            
            this.currentSpeed = pathClear ? FAST_FORWARD_SPEED : NORMAL_FORWARD_SPEED;
            System.out.println("🔵 CirclePacket " + getId() + ": Using normal speed: " + this.currentSpeed + " (pathClear=" + pathClear + ")");
        }
        
        // Update velocity with current speed using the unit vector (which already has correct direction)
        if (unitVector != null) {
            velocity = new Point2D(unitVector[0], unitVector[1]).multiply(currentSpeed);
            System.out.println("🔵 CirclePacket " + getId() + ": Speed changed from " + previousSpeed + " to " + currentSpeed);
            System.out.println("🔵 CirclePacket " + getId() + ": Direction: " + (movingBackward ? "BACKWARD" : "FORWARD"));
            System.out.println("🔵 CirclePacket " + getId() + ": Unit vector: [" + unitVector[0] + ", " + unitVector[1] + "]");
            System.out.println("🔵 CirclePacket " + getId() + ": Velocity changed from " + previousVelocity + " to " + velocity);
        } else {
            System.out.println("🔵 CirclePacket " + getId() + ": WARNING: No unit vector available");
        }
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
        // Circle packets are compatible with any port type (like pentagon packets)
        return true;
    }
    
    /**
     * Override setCurrentConnection to reset preservation state when entering new connections.
     */
    @Override
    public void setCurrentConnection(Connection connection) {
        super.setCurrentConnection(connection);
        
        // Reset preservation state when entering a new connection
        if (connection != null) {
            resetPreservationState();
        }
    }
    
    /**
     * Override setInsideSystem to handle preservation when entering systems.
     */
    @Override
    public void setInsideSystem(boolean inside) {
        super.setInsideSystem(inside);
        
        // Reset preservation state when entering a system
        if (inside) {
            resetPreservationState();
        }
    }
    
    /**
     * Get the preservation status for debugging/monitoring.
     */
    public boolean isPreservingOtherPackets() {
        return preservingOtherPackets;
    }
    
    /**
     * Get the backward movement status.
     */
    public boolean isMovingBackward() {
        return movingBackward;
    }
    
    /**
     * Get the target system for preservation.
     */
    public NetworkSystem getTargetSystem() {
        return targetSystem;
    }
    
    @Override
    public String toString() {
        return "CirclePacket{" +
               "id=" + getId() +
               ", size=" + getSize() +
               ", health=" + getHealth() +
               ", speed=" + getSpeed() +
               ", preserving=" + preservingOtherPackets +
               ", movingBackward=" + movingBackward +
               ", position=" + getPosition() +
               '}';
    }
} 
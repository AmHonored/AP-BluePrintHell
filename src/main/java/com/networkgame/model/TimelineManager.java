package com.networkgame.model;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manages all timelines and animations for the network system
 */
public class TimelineManager {
    private NetworkSystem parentSystem;
    private Timeline packetGenerationTimeline;
    private Timeline level1ScriptTimeline;
    private Timeline packetTransferTimeline;
    
    // Packet generation parameters
    private int packetWave = 0;
    private boolean inHighTrafficMode = false;
    private int visiblePacketCount = 0;
    private boolean hasStoredSquarePacket = false;
    
    public TimelineManager(NetworkSystem parentSystem) {
        this.parentSystem = parentSystem;
    }
    
    /**
     * Start the packet transfer timeline for periodic packet processing
     */
    public void startPacketTransferTimeline() {
        stopTimeline(packetTransferTimeline);
        
        packetTransferTimeline = createTimeline(
            Duration.millis(100), 
            e -> parentSystem.getPacketManager().transferPacket(),
            Timeline.INDEFINITE
        );
        packetTransferTimeline.play();
    }
    
    /**
     * Start sending packets from this system
     * @param interval The interval between packet generation in seconds
     */
    public void startSendingPackets(double interval) {
        if (!parentSystem.isStartSystem()) {
            System.out.println("Warning: startSendingPackets called on a non-start system: " + parentSystem.getLabel());
            return;
        }
        
        stopSendingPackets();
        
        int currentLevel = parentSystem.getGameState().getCurrentLevel();
        
        if (currentLevel == 1) {
            startLevel1ScriptedPackets();
            return;
        }
        
        if (currentLevel == 2) {
            startLevel2CollisionPackets(interval);
            return;
        }
        
        // Default packet generation for other levels
        packetGenerationTimeline = createTimeline(
            Duration.seconds(interval),
            event -> {
                if (packetWave % 3 == 0) {
                    generateSquarePacket();
                } else if (packetWave % 3 == 1) {
                    generateTrianglePacket();
                } else {
                    generateStorageDemonstrationBurst();
                }
                
                packetWave++;
                
                if (packetWave == 20 && !inHighTrafficMode) {
                    inHighTrafficMode = true;
                    stopSendingPackets();
                    startSendingPackets(interval * 0.7); // 30% faster packet generation
                }
            },
            Timeline.INDEFINITE
        );
        
        packetGenerationTimeline.play();
        System.out.println("Packet generation timeline started");
        
        startPacketTransferTimeline();
    }
    
    /**
     * Stop sending packets from this system
     */
    public void stopSendingPackets() {
        stopTimeline(packetGenerationTimeline);
        stopTimeline(level1ScriptTimeline);
        
        packetGenerationTimeline = null;
        level1ScriptTimeline = null;
        inHighTrafficMode = false;
        
        System.out.println("Stopped sending packets from system");
    }
    
    /**
     * Update packet generation speed based on game speed multiplier
     * @param speedMultiplier The speed multiplier to apply
     */
    public void updatePacketGenerationSpeed(double speedMultiplier) {
        if (packetGenerationTimeline != null) {
            packetGenerationTimeline.stop();
            
            double baseInterval = 2.0;
            double adjustedInterval = baseInterval / speedMultiplier;
            
            packetGenerationTimeline = createTimeline(
                Duration.seconds(adjustedInterval),
                event -> generateRandomPacket(),
                Timeline.INDEFINITE
            );
            
            packetGenerationTimeline.play();
            
            System.out.println("Updated packet generation speed for system: " + 
                              (parentSystem.getLabel() != null ? parentSystem.getLabel() : "unnamed") + 
                              " to " + speedMultiplier + "x");
        }
    }
    
    /**
     * Generate a random packet from this system's output ports
     */
    private void generateRandomPacket() {
        if (!parentSystem.isStartSystem() || parentSystem.getOutputPorts().isEmpty()) {
            return;
        }
        
        int portIndex = (int)(Math.random() * parentSystem.getOutputPorts().size());
        Port outputPort = parentSystem.getOutputPorts().get(portIndex);
        
        Packet packet = createPacketForPort(outputPort);
        
        if (packet != null && outputPort.getConnection() != null) {
            generatePacket(packet, outputPort);
        }
    }
    
    /**
     * Create a packet of the appropriate type for the given port
     */
    private Packet createPacketForPort(Port port) {
        if (port.getType() == Packet.PacketType.SQUARE) {
            return new SquarePacket(port.getPosition());
        } else if (port.getType() == Packet.PacketType.TRIANGLE) {
            return new TrianglePacket(port.getPosition());
        }
        return null;
    }
    
    /**
     * Find available ports of a specific type
     */
    private List<Port> findAvailablePortsByType(Packet.PacketType type) {
        List<Port> availablePorts = parentSystem.getPortManager().findAvailableOutputPorts();
        return availablePorts.stream()
            .filter(port -> port.getType() == type)
            .collect(Collectors.toList());
    }
    
    /**
     * Create a visible packet of specified type and generate it
     */
    private void createAndGenerateVisiblePacket(Packet.PacketType type, String logPrefix) {
        List<Port> typedPorts = findAvailablePortsByType(type);
        
        if (typedPorts.isEmpty()) {
            System.out.println("Warning: No " + type.toString().toLowerCase() + " ports available for packet generation");
            return;
        }
        
        Port outputPort = typedPorts.get(0);
        Packet packet = createPacketForType(type, outputPort.getPosition());
        
        System.out.println("Generating " + type + " packet from " + type + " port at " + outputPort.getPosition());
        
        packet.setProperty("isVisiblePacket", true);
        packet.setProperty("creationTime", System.currentTimeMillis());
        
        generatePacket(packet, outputPort);
        
        parentSystem.getGameState().incrementTotalPackets();
        System.out.println(logPrefix + " packet created and added to total count");
    }
    
    /**
     * Create packet of specified type at position
     */
    private Packet createPacketForType(Packet.PacketType type, Point2D position) {
        return type == Packet.PacketType.SQUARE ? new SquarePacket(position) : new TrianglePacket(position);
    }
    
    /**
     * Generate a square packet on an available output port of square type
     */
    private void generateSquarePacket() {
        createAndGenerateVisiblePacket(Packet.PacketType.SQUARE, "Square");
    }
    
    /**
     * Generate a triangle packet on an available output port of triangle type
     */
    private void generateTrianglePacket() {
        createAndGenerateVisiblePacket(Packet.PacketType.TRIANGLE, "Triangle");
    }
    
    /**
     * Create a Timeline with the specified parameters
     */
    private Timeline createTimeline(Duration duration, Consumer<javafx.event.ActionEvent> action, int cycleCount) {
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(duration, action::accept);
        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(cycleCount);
        return timeline;
    }
    
    /**
     * Stop a timeline if not null
     */
    private void stopTimeline(Timeline timeline) {
        if (timeline != null) {
            timeline.stop();
        }
    }
    
    /**
     * Add a keyframe to a timeline with the specified parameters
     */
    private void addKeyFrame(Timeline timeline, double seconds, Runnable action) {
        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(seconds), e -> action.run()));
    }
    
    /**
     * Generate a burst of packets specifically designed to demonstrate the storage mechanism
     */
    private void generateStorageDemonstrationBurst() {
        stopTimeline(level1ScriptTimeline);
        
        visiblePacketCount = 0;
        level1ScriptTimeline = new Timeline();
        
        // Create a series of packet generations with specific timings
        addKeyFrame(level1ScriptTimeline, 0.1, () -> {
            generateSquarePacket();
            System.out.println("Burst: First packet sent");
        });
        
        addKeyFrame(level1ScriptTimeline, 0.3, () -> {
            generateSquarePacket();
            System.out.println("Burst: Second packet sent");
        });
        
        addKeyFrame(level1ScriptTimeline, 0.5, () -> {
            generateSquarePacket();
            System.out.println("Burst: Third packet sent");
        });
        
        addKeyFrame(level1ScriptTimeline, 0.7, () -> {
            generateTrianglePacket();
            System.out.println("Burst: Fourth packet (triangle) sent");
        });
        
        addKeyFrame(level1ScriptTimeline, 0.9, () -> {
            generateSquarePacket();
            System.out.println("Burst: Fifth packet sent - approaching capacity");
        });
        
        addKeyFrame(level1ScriptTimeline, 3.0, () -> {
            startLevel1ScriptedPackets();
            System.out.println("Burst complete, resuming normal packet generation");
        });
        
        level1ScriptTimeline.play();
    }
    
    /**
     * Start a specialized packet generation pattern for level 2
     * that creates opportunities for collisions
     * @param interval base interval for packet generation
     */
    private void startLevel2CollisionPackets(double interval) {
        System.out.println("DEBUG: Starting Level 2 collision-optimized packet generation for " + parentSystem.getLabel());
        
        packetGenerationTimeline = new Timeline();
        packetGenerationTimeline.setCycleCount(Timeline.INDEFINITE);
        
        // First generation pattern: simultaneous packets from different ports
        KeyFrame burstFrame = new KeyFrame(
            Duration.seconds(interval * 1.5),
            event -> createBurstPackets(interval)
        );
        
        // Second pattern: alternating single packets at regular intervals
        KeyFrame regularFrame = new KeyFrame(
            Duration.seconds(interval * 0.6),
            event -> createRegularPackets(interval)
        );
        
        // Add a third frame specifically for collision testing
        KeyFrame collisionTestFrame = new KeyFrame(
            Duration.seconds(interval * 3.0),
            event -> createCollisionTestPackets()
        );
        
        packetGenerationTimeline.getKeyFrames().addAll(burstFrame, regularFrame, collisionTestFrame);
        packetGenerationTimeline.play();
    }
    
    private void createBurstPackets(double interval) {
        List<Port> availablePorts = parentSystem.getPortManager().findAvailableOutputPorts();
        
        if (availablePorts.isEmpty()) {
            System.out.println("DEBUG: No available output ports for burst generation");
            return;
        }
        
        List<Port> squarePorts = findAvailablePortsByType(Packet.PacketType.SQUARE);
        List<Port> trianglePorts = findAvailablePortsByType(Packet.PacketType.TRIANGLE);
        
        System.out.println("DEBUG: Available ports for burst - Square: " + squarePorts.size() + 
                         ", Triangle: " + trianglePorts.size());
        
        // Generate burst of packets to cause collisions
        if (!squarePorts.isEmpty()) {
            for (Port port : squarePorts) {
                Packet packet = new SquarePacket(port.getPosition());
                packet.setProperty("isVisiblePacket", true);
                packet.setProperty("burstPacket", true); 
                generatePacket(packet, port);
                parentSystem.getGameState().incrementTotalPackets();
            }
            System.out.println("DEBUG: Generated square packet burst (" + squarePorts.size() + 
                             " packets) from " + parentSystem.getLabel());
        }
        
        // Schedule triangle packets to follow shortly after
        Timeline delayedTriangles = createTimeline(
            Duration.seconds(0.2),
            e -> {
                if (!trianglePorts.isEmpty()) {
                    for (Port port : trianglePorts) {
                        Packet packet = new TrianglePacket(port.getPosition());
                        packet.setProperty("isVisiblePacket", true);
                        packet.setProperty("burstPacket", true);
                        generatePacket(packet, port);
                        parentSystem.getGameState().incrementTotalPackets();
                    }
                    System.out.println("DEBUG: Generated triangle packet burst (" + 
                                     trianglePorts.size() + " packets) from " + parentSystem.getLabel());
                }
            },
            1
        );
        delayedTriangles.play();
        
        packetWave++;
    }
    
    private void createRegularPackets(double interval) {
        int portIndex = packetWave % 4; // Cycle through up to 4 ports
        List<Port> availablePorts = parentSystem.getPortManager().findAvailableOutputPorts();
        
        if (!availablePorts.isEmpty()) {
            Port port = portIndex < availablePorts.size() ? availablePorts.get(portIndex) : availablePorts.get(0);
            
            if (port != null) {
                Packet packet = createPacketForPort(port);
                
                packet.setProperty("isVisiblePacket", true);
                packet.setProperty("regularPacket", true);
                generatePacket(packet, port);
                parentSystem.getGameState().incrementTotalPackets();
                System.out.println("DEBUG: Generated regular " + 
                                 (port.getType() == Packet.PacketType.SQUARE ? "square" : "triangle") + 
                                 " packet from " + parentSystem.getLabel() + " (port " + portIndex + ")");
            }
        } else {
            System.out.println("DEBUG: No available ports for regular packet generation");
        }
        
        packetWave++;
        
        // Every 15 waves, increase packet generation rate
        if (packetWave % 15 == 0 && !inHighTrafficMode) {
            inHighTrafficMode = true;
            System.out.println("DEBUG: Switching to high traffic mode");
            
            stopSendingPackets();
            startLevel2CollisionPackets(interval * 0.7);
            
            System.out.println("DEBUG: High traffic mode activated - packet generation speed increased by 30%");
        }
    }
    
    private void createCollisionTestPackets() {
        List<Port> availablePorts = parentSystem.getPortManager().findAvailableOutputPorts();
        if (availablePorts.size() >= 2) {
            Port port1 = availablePorts.get(0);
            Port port2 = availablePorts.get(availablePorts.size() > 1 ? 1 : 0);
            
            Packet packet1 = new SquarePacket(port1.getPosition());
            Packet packet2 = new TrianglePacket(port2.getPosition());
            
            setPacketProperties(packet1, "isVisiblePacket", true, "collisionTestPacket", true);
            setPacketProperties(packet2, "isVisiblePacket", true, "collisionTestPacket", true);
            
            generatePacket(packet1, port1);
            generatePacket(packet2, port2);
            
            parentSystem.getGameState().incrementTotalPackets();
            parentSystem.getGameState().incrementTotalPackets();
            
            System.out.println("DEBUG: Generated special collision test packet pair from " + parentSystem.getLabel());
        }
    }
    
    private void setPacketProperties(Packet packet, String key1, Object value1, String key2, Object value2) {
        packet.setProperty(key1, value1);
        packet.setProperty(key2, value2);
    }
    
    /**
     * Start a scripted packet generation pattern for level 1
     */
    private void startLevel1ScriptedPackets() {
        level1ScriptTimeline = new Timeline();
        level1ScriptTimeline.setCycleCount(Timeline.INDEFINITE);
        
        if ("Source".equals(parentSystem.getLabel())) {
            handleSourceSystemPacketGeneration();
        }
        else if (parentSystem.isStartSystem()) {
            handleDefaultSystemPacketGeneration();
        }
        
        level1ScriptTimeline.play();
    }
    
    private void handleSourceSystemPacketGeneration() {
        final int[] sequenceCounter = {0};
        
        KeyFrame scriptedPatternFrame = new KeyFrame(
            Duration.seconds(3.0),
            event -> {
                List<Port> availablePorts = parentSystem.getPortManager().findAvailableOutputPorts();
                if (availablePorts.isEmpty()) {
                    System.out.println("No available output ports");
                    return;
                }
                
                List<Port> squarePorts = findAvailablePortsByType(Packet.PacketType.SQUARE);
                List<Port> trianglePorts = findAvailablePortsByType(Packet.PacketType.TRIANGLE);
                
                if (squarePorts.size() < 2 || trianglePorts.isEmpty()) {
                    System.out.println("Not enough ports for the scripted pattern");
                    return;
                }
                
                if (sequenceCounter[0] == 0) {
                    generateSequenceZeroPackets(squarePorts, trianglePorts);
                } else if (sequenceCounter[0] == 1) {
                    generateSequenceOnePackets(squarePorts, trianglePorts);
                }
                
                sequenceCounter[0] = (sequenceCounter[0] + 1) % 2;
            }
        );
        
        level1ScriptTimeline.getKeyFrames().add(scriptedPatternFrame);
    }
    
    private void generateSequenceZeroPackets(List<Port> squarePorts, List<Port> trianglePorts) {
        // Generate a square packet from the top wire
        Port topSquarePort = squarePorts.get(0);
        generateVisiblePacket(topSquarePort, "Generated square packet from top wire");
        
        // Generate a triangle packet from the bottom wire
        Port trianglePort = trianglePorts.get(0);
        generateVisiblePacket(trianglePort, "Generated triangle packet from bottom wire");
        
        // Schedule the next part of the sequence
        Timeline delayedPacket = createTimeline(
            Duration.seconds(1.5),
            e -> {
                if (squarePorts.size() > 1) {
                    Port middleSquarePort = squarePorts.get(1);
                    generateVisiblePacket(middleSquarePort, "Generated delayed square packet from middle wire (will be stored)");
                }
            },
            1
        );
        delayedPacket.play();
    }
    
    private void generateSequenceOnePackets(List<Port> squarePorts, List<Port> trianglePorts) {
        // Generate a triangle packet
        Port trianglePort = trianglePorts.get(0);
        generateVisiblePacket(trianglePort, "Generated triangle packet");
        
        // Schedule two square packets in sequence
        Timeline sequentialPackets = new Timeline();
        
        addKeyFrame(sequentialPackets, 0.8, () -> {
            if (!squarePorts.isEmpty()) {
                Port squarePort = squarePorts.get(0);
                generateVisiblePacket(squarePort, "Generated first sequential square packet");
            }
        });
        
        addKeyFrame(sequentialPackets, 1.6, () -> {
            if (squarePorts.size() > 1) {
                Port squarePort = squarePorts.get(1);
                generateVisiblePacket(squarePort, "Generated second sequential square packet");
            }
        });
        
        sequentialPackets.play();
    }
    
    private void generateVisiblePacket(Port port, String logMessage) {
        Packet packet = createPacketForPort(port);
        packet.setProperty("isVisiblePacket", true);
        generatePacket(packet, port);
        parentSystem.getGameState().incrementTotalPackets();
        System.out.println(logMessage);
    }
    
    private void handleDefaultSystemPacketGeneration() {
        KeyFrame defaultFrame = new KeyFrame(
            Duration.seconds(0.8),
            event -> {
                if (parentSystem.getPortManager().findAvailableOutputPorts().size() > 0) {
                    if (Math.random() < 0.6) {
                        generateSquarePacket();
                    } else {
                        generateTrianglePacket();
                    }
                }
            }
        );
        level1ScriptTimeline.getKeyFrames().add(defaultFrame);
    }
    
    /**
     * Generate a packet (for reference systems only)
     * @param packet The packet to generate
     * @param outputPort The port to use for output
     */
    public void generatePacket(Packet packet, Port outputPort) {
        if (!outputPort.isConnected() || !parentSystem.getOutputPorts().contains(outputPort)) {
            System.out.println("Cannot generate packet: port is not connected or not an output port");
            return;
        }
        
        Connection connection = outputPort.getConnection();
        packet.setProperty("gameState", parentSystem.getGameState());
        
        // Only store packet if ALL output wires are busy
        if (!connection.isEmpty() && parentSystem.getPacketManager().areAllOutputWiresFull()) {
            storePacketInSystem(packet);
            return;
        } else if (!connection.isEmpty()) {
            System.out.println("This connection busy but others free - waiting to retry");
            return;
        }
        
        // Set packet at output port position and configure it
        configurePacketForTransfer(packet, outputPort, connection);
        
        // Add packet to connection and game state
        connection.addPacket(packet);
        addPacketToGameState(packet);
        
        System.out.println("Packet sent on wire from " + outputPort.getPosition() + 
                          " to " + connection.getTargetPort().getPosition() + 
                          " with speed " + packet.getSpeed());
    }
    
    private void storePacketInSystem(Packet packet) {
        System.out.println("All output wires busy - storing packet in system");
        
        if (!parentSystem.getPacketManager().getPackets().contains(packet)) {
            parentSystem.getPacketManager().getPackets().add(packet);
            packet.setInsideSystem(true);
            addPacketToGameState(packet);
            System.out.println("Packet stored in system, waiting for wire to clear");
        }
    }
    
    private void configurePacketForTransfer(Packet packet, Port outputPort, Connection connection) {
        packet.setPosition(outputPort.getPosition());
        
        // Set speed based on compatibility
        if (packet instanceof SquarePacket) {
            ((SquarePacket) packet).adjustSpeedForPort(outputPort);
        } else if (packet instanceof TrianglePacket) {
            ((TrianglePacket) packet).adjustSpeedForPort(outputPort);
        }
        
        // Use consistent slow speed in level 1 for clear visibility
        if (parentSystem.getGameState().getCurrentLevel() == 1) {
            packet.setSpeedMultiplier(0.4);
        }
        
        // Set velocity aligned with the wire
        Point2D sourcePos = outputPort.getPosition();
        Point2D targetPos = connection.getTargetPort().getPosition();
        packet.alignVelocityToWire(sourcePos.getX(), sourcePos.getY(), 
                                  targetPos.getX(), targetPos.getY());
        
        packet.setProperty("progress", 0.0);
    }
    
    private void addPacketToGameState(Packet packet) {
        if (!parentSystem.getGameState().getActivePackets().contains(packet)) {
            parentSystem.getGameState().addActivePacket(packet);
        }
        parentSystem.setActive(true);
    }
    
    /**
     * Clean up all resources to prevent memory leaks
     */
    public void cleanup() {
        stopTimeline(packetGenerationTimeline);
        stopTimeline(level1ScriptTimeline);
        stopTimeline(packetTransferTimeline);
        
        packetGenerationTimeline = null;
        level1ScriptTimeline = null;
        packetTransferTimeline = null;
        
        System.out.println("TimelineManager resources cleaned up");
    }
    
    // Getters for timelines
    public Timeline getPacketGenerationTimeline() {
        return packetGenerationTimeline;
    }
    
    public Timeline getLevel1ScriptTimeline() {
        return level1ScriptTimeline;
    }
    
    public Timeline getPacketTransferTimeline() {
        return packetTransferTimeline;
    }
} 
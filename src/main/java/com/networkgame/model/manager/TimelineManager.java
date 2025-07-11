package com.networkgame.model.manager; 

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.util.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.Port;
import com.networkgame.model.entity.Packet;
import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.packettype.messenger.SquarePacket;
import com.networkgame.model.entity.packettype.messenger.TrianglePacket;
import com.networkgame.model.entity.packettype.messenger.HexagonPacket;
import com.networkgame.model.entity.packettype.messenger.ProtectedPacket;
import com.networkgame.model.entity.packettype.secret.PentagonPacket;

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
            e -> {
                // Use a safer approach to get the packet manager
                PacketManager packetManager = parentSystem.getPacketManager();
                if (packetManager != null) {
                    packetManager.transferPacket();
                }
            },
            Timeline.INDEFINITE
        );
        packetTransferTimeline.play();
    }
    
    /**
     * Start sending packets from this system
     * @param interval The interval between packet generation in seconds
     */
    public void startSendingPackets(double interval) {
        if (!parentSystem.canGeneratePackets()) {
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
        
        if (currentLevel == 3) {
            startLevel3HexagonPackets(interval);
            return;
        }
        
        if (currentLevel == 4) {
            startLevel4VpnTestPackets(interval);
            return;
        }
        
        if (currentLevel == 5) {
            startLevel5PentagonTestPackets(interval);
            return;
        }
        
        if (currentLevel == 6) {
            startLevel6CircleTestPackets(interval);
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
        }
    }
    
    /**
     * Generate a random packet from this system's output ports
     */
    private void generateRandomPacket() {
        if (!parentSystem.canGeneratePackets() || parentSystem.getOutputPorts().isEmpty()) {
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
        } else if (port.getType() == Packet.PacketType.HEXAGON) {
            return new HexagonPacket(port.getPosition());

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
            return;
        }
        
        Port outputPort = typedPorts.get(0);
        Packet packet = createPacketForType(type, outputPort.getPosition());
        
        packet.setProperty("isVisiblePacket", true);
        packet.setProperty("creationTime", System.currentTimeMillis());
        
        generatePacket(packet, outputPort);
        
        parentSystem.getGameState().incrementTotalPackets();
    }
    
    /**
     * Create packet of specified type at position
     */
    private Packet createPacketForType(Packet.PacketType type, Point2D position) {
        if (type == Packet.PacketType.SQUARE) {
            return new SquarePacket(position);
        } else if (type == Packet.PacketType.TRIANGLE) {
            return new TrianglePacket(position);
        } else if (type == Packet.PacketType.HEXAGON) {
            return new HexagonPacket(position);
        } else if (type == Packet.PacketType.PROTECTED) {
            return new ProtectedPacket(position);

        }
        return null;
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
     * Generate a hexagon packet on an available output port of hexagon type
     */
    private void generateHexagonPacket() {
        createAndGenerateVisiblePacket(Packet.PacketType.HEXAGON, "Hexagon");
    }
    
    /**
     * Generate a protected packet on an available output port
     */
    private void generateProtectedPacket() {
        createAndGenerateVisiblePacket(Packet.PacketType.PROTECTED, "Protected");
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
        });
        
        addKeyFrame(level1ScriptTimeline, 0.3, () -> {
            generateSquarePacket();
        });
        
        addKeyFrame(level1ScriptTimeline, 0.5, () -> {
            generateSquarePacket();
        });
        
        addKeyFrame(level1ScriptTimeline, 0.7, () -> {
            generateTrianglePacket();
        });
        
        addKeyFrame(level1ScriptTimeline, 0.9, () -> {
            generateSquarePacket();
        });
        
        addKeyFrame(level1ScriptTimeline, 3.0, () -> {
            startLevel1ScriptedPackets();
        });
        
        level1ScriptTimeline.play();
    }
    
    /**
     * Start packet generation for level 3 (DDoS system test level)
     * @param interval base interval for packet generation
     */
    private void startLevel3HexagonPackets(double interval) {
        packetGenerationTimeline = new Timeline();
        packetGenerationTimeline.setCycleCount(Timeline.INDEFINITE);
        
        // Generate packets of all three types cyclically for DDoS system test
        KeyFrame packetFrame = new KeyFrame(
            Duration.seconds(interval),
            event -> {
                // Cycle through packet types: Square -> Triangle -> Hexagon
                int packetType = packetWave % 3;
                switch (packetType) {
                    case 0:
                        generateSquarePacket();
                        break;
                    case 1:
                        generateTrianglePacket();
                        break;
                    case 2:
                        generateHexagonPacket();
                        break;
                }
                packetWave++;
            }
        );
        
        packetGenerationTimeline.getKeyFrames().add(packetFrame);
        packetGenerationTimeline.play();
        
        startPacketTransferTimeline();
    }
    
    /**
     * Start specialized packet generation for level 4 VPN test
     * HIGH_SPEED systems generate high-speed triangle packets
     * Normal START systems generate regular triangle packets
     */
    private void startLevel4VpnTestPackets(double interval) {
        packetGenerationTimeline = new Timeline();
        packetGenerationTimeline.setCycleCount(Timeline.INDEFINITE);
        
        // Determine packet generation strategy based on system label
        String systemLabel = parentSystem.getLabel();
        
        if ("HIGH_SPEED".equals(systemLabel)) {
            // High-speed start system: Generate high-speed triangle packets every 5 seconds
            KeyFrame highSpeedFrame = new KeyFrame(
                Duration.seconds(5.0),
                event -> generateHighSpeedTrianglePacket()
            );
            packetGenerationTimeline.getKeyFrames().add(highSpeedFrame);
            System.out.println("TimelineManager: Initialized HIGH_SPEED system to generate high-speed packets every 5 seconds");
        } else if ("START".equals(systemLabel)) {
            // Normal start system: Generate regular triangle packets every 1.5 seconds
            KeyFrame normalFrame = new KeyFrame(
                Duration.seconds(1.5),
                event -> generateTrianglePacket()
            );
            packetGenerationTimeline.getKeyFrames().add(normalFrame);
            System.out.println("TimelineManager: Initialized START system to generate normal triangle packets every 1.5 seconds");
        }
        
        packetGenerationTimeline.play();
        startPacketTransferTimeline();
    }
    
    /**
     * Generate a high-speed triangle packet for VPN failure testing
     */
    private void generateHighSpeedTrianglePacket() {
        List<Port> trianglePorts = findAvailablePortsByType(Packet.PacketType.TRIANGLE);
        
        if (trianglePorts.isEmpty()) {
            System.out.println("TimelineManager: No available triangle ports for high-speed packet generation");
            return;
        }
        
        Port outputPort = trianglePorts.get(0);
        
        // Create high-speed triangle packet
        com.networkgame.model.entity.packettype.messenger.HighSpeedTrianglePacket highSpeedPacket = 
            new com.networkgame.model.entity.packettype.messenger.HighSpeedTrianglePacket(outputPort.getPosition());
        
        highSpeedPacket.setProperty("isVisiblePacket", true);
        highSpeedPacket.setProperty("creationTime", System.currentTimeMillis());
        highSpeedPacket.setProperty("isHighSpeedTrigger", true);
        
        System.out.println("TimelineManager: Generated high-speed triangle packet " + highSpeedPacket.getId() + 
                         " with speed " + highSpeedPacket.getSpeed() + " to trigger VPN failure");
        
        generatePacket(highSpeedPacket, outputPort);
        parentSystem.getGameState().incrementTotalPackets();
    }
    
    /**
     * Start specialized packet generation for level 5 Pentagon collision avoidance test
     * FAST_GEN systems generate Square and Triangle packets rapidly
     * PENTAGON_GEN systems generate Pentagon packets at a slower rate
     */
    private void startLevel5PentagonTestPackets(double interval) {
        packetGenerationTimeline = new Timeline();
        packetGenerationTimeline.setCycleCount(Timeline.INDEFINITE);
        
        // Determine packet generation strategy based on system label
        String systemLabel = parentSystem.getLabel();
        
        if ("FAST_GEN".equals(systemLabel)) {
            // Fast generator: Generate Square and Triangle packets extremely rapidly (every 0.15 seconds, alternating)
            KeyFrame fastFrame = new KeyFrame(
                Duration.seconds(0.15),
                event -> {
                    // Alternate between Square and Triangle packets to create massive congestion
                    if (packetWave % 2 == 0) {
                        generateSquarePacket();
                    } else {
                        generateTrianglePacket();
                    }
                    packetWave++;
                }
            );
            packetGenerationTimeline.getKeyFrames().add(fastFrame);
            System.out.println("TimelineManager: Initialized FAST_GEN system to generate Square/Triangle packets every 0.15 seconds (massive congestion)");
        } else if ("PENTAGON_GEN".equals(systemLabel)) {
            // Pentagon generator: Generate Pentagon packets at a much slower rate (every 5.0 seconds)
            KeyFrame pentagonFrame = new KeyFrame(
                Duration.seconds(5.0),
                event -> generatePentagonPacket()
            );
            packetGenerationTimeline.getKeyFrames().add(pentagonFrame);
            System.out.println("TimelineManager: Initialized PENTAGON_GEN system to generate Pentagon packets every 5.0 seconds (slow for collision testing)");
        }
        
        packetGenerationTimeline.play();
        startPacketTransferTimeline();
    }
    
    /**
     * Generate a Pentagon packet for testing collision avoidance
     * Pentagon packets are compatible with any port, so use any available port
     */
    private void generatePentagonPacket() {
        List<Port> availablePorts = parentSystem.getPortManager().findAvailableOutputPorts();
        
        if (availablePorts.isEmpty()) {
            return;
        }
        
        Port outputPort = availablePorts.get(0);
        Packet packet = new com.networkgame.model.entity.packettype.secret.PentagonPacket(outputPort.getPosition());
        
        packet.setProperty("isVisiblePacket", true);
        packet.setProperty("creationTime", System.currentTimeMillis());
        
        generatePacket(packet, outputPort);
        
        parentSystem.getGameState().incrementTotalPackets();
    }
    
    /**
     * Start specialized packet generation for level 6 Circle transformation test
     * START systems generate different packets based on port index:
     * - Port 0 (Square): Pentagon packets (slow rate)
     * - Port 1 (Square): Square packets (fast rate)  
     * - Port 2 (Triangle): Triangle packets (fast rate)
     */
    private void startLevel6CircleTestPackets(double interval) {
        packetGenerationTimeline = new Timeline();
        packetGenerationTimeline.setCycleCount(Timeline.INDEFINITE);
        
        // Determine packet generation strategy based on system label
        String systemLabel = parentSystem.getLabel();
        
        if ("START".equals(systemLabel)) {
            // START system: Generate different packets from different ports based on index
            
            // Pentagon packets from port 0 (first Square port) - slow rate (every 3 seconds)
            KeyFrame pentagonFrame = new KeyFrame(
                Duration.seconds(3.0),
                event -> generateLevel6PentagonPacket()
            );
            
            // Square packets from port 1 (second Square port) - fast rate (every 0.4 seconds)
            KeyFrame squareFrame = new KeyFrame(
                Duration.seconds(0.4),
                event -> generateLevel6SquarePacket()
            );
            
            // Triangle packets from port 2 (Triangle port) - fast rate (every 0.6 seconds)
            KeyFrame triangleFrame = new KeyFrame(
                Duration.seconds(0.6),
                event -> generateLevel6TrianglePacket()
            );
            
            packetGenerationTimeline.getKeyFrames().addAll(pentagonFrame, squareFrame, triangleFrame);
            System.out.println("TimelineManager: Initialized Level 6 START system:");
            System.out.println("  - Port 0 (Square): Pentagon packets every 3.0s");
            System.out.println("  - Port 1 (Square): Square packets every 0.4s (FAST)");
            System.out.println("  - Port 2 (Triangle): Triangle packets every 0.6s (FAST)");
        }
        
        packetGenerationTimeline.play();
        startPacketTransferTimeline();
    }
    
    /**
     * Generate a Pentagon packet from the first Square port (index 0) for VPN transformation
     */
    private void generateLevel6PentagonPacket() {
        List<Port> squarePorts = findAvailablePortsByType(Packet.PacketType.SQUARE);
        
        if (squarePorts.isEmpty()) {
            return;
        }
        
        // Use the first Square port (index 0) for Pentagon packets
        Port outputPort = squarePorts.get(0);
        Packet packet = new com.networkgame.model.entity.packettype.secret.PentagonPacket(outputPort.getPosition());
        
        packet.setProperty("isVisiblePacket", true);
        packet.setProperty("creationTime", System.currentTimeMillis());
        packet.setProperty("level6PentagonPacket", true);
        
        System.out.println("TimelineManager: Generated Pentagon packet " + packet.getId() + " from port 0 (first Square port) -> VPN");
        
        generatePacket(packet, outputPort);
        parentSystem.getGameState().incrementTotalPackets();
    }
    
    /**
     * Generate a Square packet from the second Square port (index 1) for direct connection
     */
    private void generateLevel6SquarePacket() {
        List<Port> squarePorts = findAvailablePortsByType(Packet.PacketType.SQUARE);
        
        if (squarePorts.size() < 2) {
            return;
        }
        
        // Use the second Square port (index 1) for regular Square packets
        Port outputPort = squarePorts.get(1);
        Packet packet = new com.networkgame.model.entity.packettype.messenger.SquarePacket(outputPort.getPosition());
        
        packet.setProperty("isVisiblePacket", true);
        packet.setProperty("creationTime", System.currentTimeMillis());
        packet.setProperty("level6SquarePacket", true);
        
        generatePacket(packet, outputPort);
        parentSystem.getGameState().incrementTotalPackets();
    }
    
    /**
     * Generate a Triangle packet from the Triangle port for direct connection
     */
    private void generateLevel6TrianglePacket() {
        List<Port> trianglePorts = findAvailablePortsByType(Packet.PacketType.TRIANGLE);
        
        if (trianglePorts.isEmpty()) {
            return;
        }
        
        Port outputPort = trianglePorts.get(0);
        Packet packet = new com.networkgame.model.entity.packettype.messenger.TrianglePacket(outputPort.getPosition());
        
        packet.setProperty("isVisiblePacket", true);
        packet.setProperty("creationTime", System.currentTimeMillis());
        packet.setProperty("level6TrianglePacket", true);
        
        generatePacket(packet, outputPort);
        parentSystem.getGameState().incrementTotalPackets();
    }
    
    /**
     * Start a specialized packet generation pattern for level 2
     * that creates opportunities for collisions
     * @param interval base interval for packet generation
     */
    private void startLevel2CollisionPackets(double interval) {
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
            return;
        }
        
        List<Port> squarePorts = findAvailablePortsByType(Packet.PacketType.SQUARE);
        List<Port> trianglePorts = findAvailablePortsByType(Packet.PacketType.TRIANGLE);
        
        // Generate burst of packets to cause collisions
        if (!squarePorts.isEmpty()) {
            for (Port port : squarePorts) {
                Packet packet = new SquarePacket(port.getPosition());
                packet.setProperty("isVisiblePacket", true);
                packet.setProperty("burstPacket", true); 
                generatePacket(packet, port);
            }
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
                    }
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
            }
        }
        
        packetWave++;
        
        // Every 15 waves, increase packet generation rate
        if (packetWave % 15 == 0 && !inHighTrafficMode) {
            inHighTrafficMode = true;
            
            stopSendingPackets();
            startLevel2CollisionPackets(interval * 0.7);
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
        else if (parentSystem.canGeneratePackets()) {
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
                    return;
                }
                
                List<Port> squarePorts = findAvailablePortsByType(Packet.PacketType.SQUARE);
                List<Port> trianglePorts = findAvailablePortsByType(Packet.PacketType.TRIANGLE);
                
                if (squarePorts.size() < 2 || trianglePorts.isEmpty()) {
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
            return;
        }
        
        Connection connection = outputPort.getConnection();
        packet.setProperty("gameState", parentSystem.getGameState());
        
        // Only store packet if ALL output wires are busy
        if (!connection.isEmpty() && parentSystem.getPacketManager().areAllOutputWiresFull()) {
            storePacketInSystem(packet);
            return;
        } else if (!connection.isEmpty()) {
            return;
        }
        
        // Set packet at output port position and configure it
        configurePacketForTransfer(packet, outputPort, connection);
        
        // Add packet to connection and game state
        connection.addPacket(packet);
        addPacketToGameState(packet);
    }
    
    private void storePacketInSystem(Packet packet) {
        if (!parentSystem.getPacketManager().getPackets().contains(packet)) {
            parentSystem.getPacketManager().getPackets().add(packet);
            packet.setInsideSystem(true);
            addPacketToGameState(packet);
        }
    }
    
    private void configurePacketForTransfer(Packet packet, Port outputPort, Connection connection) {
        packet.setPosition(outputPort.getPosition());
        
        // Set speed based on compatibility
        if (packet instanceof SquarePacket) {
            ((SquarePacket) packet).adjustSpeedForPort(outputPort);
        } else if (packet instanceof TrianglePacket) {
            ((TrianglePacket) packet).adjustSpeedForPort(outputPort);
        } else if (packet instanceof HexagonPacket) {
            ((HexagonPacket) packet).adjustSpeedForPort(outputPort);
        } else if (packet instanceof ProtectedPacket) {
            ((ProtectedPacket) packet).adjustSpeedForPort(outputPort);
        
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

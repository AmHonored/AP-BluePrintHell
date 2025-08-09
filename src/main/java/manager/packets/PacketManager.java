package manager.packets;

import model.entity.ports.Port;
import model.entity.packets.Packet;
import model.entity.packets.HexagonPacket;
import model.entity.packets.ConfidentialPacket;
import model.entity.packets.MassivePacket;
import model.wire.Wire;
import javafx.geometry.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import model.levels.Level;
import controller.PacketController;
import javafx.scene.layout.Pane;
import view.components.ports.PortView;
import view.components.ports.SquarePortView;
import view.components.ports.TrianglePortView;
import view.components.ports.HexagonPortView;
import view.components.systems.SystemView;
import view.components.systems.MergeSystemView;

public class PacketManager {
    private static final List<Packet> movingPackets = new ArrayList<>();
    private static Level level;
    private static PacketController packetController;
    
    // Track confidential packet managers for special movement handling
    private static final Map<Packet, ConfidentialPacketManager> confidentialManagers = new HashMap<>();

    public static void setLevel(Level lvl) {
        level = lvl;
    }

    public static void setPacketController(PacketController controller) {
        packetController = controller;
    }
    
    public static boolean sendPacket(Port sourcePort, Packet packet) {
        return sendPacket(sourcePort, packet, false);
    }
    
    public static boolean sendPacket(Port sourcePort, Packet packet, boolean preserveCompatibility) {
        Wire wire = sourcePort.getWire();
        
        if (wire == null || !sourcePort.isConnected() || !wire.isAvailable()) {
            return false;
        }
        // Ensure packet is in level list before visualization/movement starts
        if (level != null && packet != null && !level.getPackets().contains(packet)) {
            level.addPacket(packet);
        }
        
        boolean result = startMovement(packet, wire, preserveCompatibility);
        return result;
    }
    
    public static boolean startMovement(Packet packet, Wire wire) {
        return startMovement(packet, wire, false);
    }
    
    public static boolean startMovement(Packet packet, Wire wire, boolean preserveCompatibility) {
        if (packet.isMoving()) {
            return false;
        }
        
        // CRITICAL FIX: Show packet if it was hidden (coming out of storage)
        if (packetController != null) {
            packetController.showPacket(packet);
        }
        
        // Set initial position to source center for precise positioning
        Point2D sourcePos = wire.getSource().getPosition();
        packet.setPosition(new Point2D(sourcePos.getX(), sourcePos.getY()));
        
        packet.setStartPosition(wire.getSource().getPosition());
        packet.setTargetPosition(wire.getDest().getPosition());
        packet.setCurrentWire(wire);
        packet.setMovementProgress(0.0);
        packet.setMovementStartTime(java.lang.System.nanoTime());
        packet.setMoving(true);
        packet.setInSystem(false);
        
        // Only recalculate compatibility if not preserving it
        boolean isCompatible = packet.isCompatibleWithCurrentPort(); // Use current state
        if (!preserveCompatibility) {
            isCompatible = isPortCompatibleWithPacket(wire.getSource(), packet);
            packet.setCompatibleWithCurrentPort(isCompatible);
        }
        
        // Special initialization for HexagonPacket
        if (packet instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) packet;
            hexPacket.setTotalPathLength(wire.getLength());
            hexPacket.setDistanceTraveled(0.0);
            java.lang.System.out.println("🚀 HEXAGON PACKET MOVEMENT STARTED: " + packet.getId() + " on wire of length " + String.format("%.1f", wire.getLength()));
        }
        
        // Special initialization for ConfidentialPacket
        if (packet instanceof ConfidentialPacket) {
            ConfidentialPacketManager confidentialManager = new ConfidentialPacketManager(packet, wire);
            confidentialManagers.put(packet, confidentialManager);
            java.lang.System.out.println("🔒 CONFIDENTIAL PACKET MOVEMENT STARTED: " + packet.getId() + " (" + packet.getType() + ") on wire of length " + String.format("%.1f", wire.getLength()));
        }

        // Debug for Massive packets
        if (packet instanceof MassivePacket) {
            java.lang.System.out.println("🟤 MASSIVE PACKET MOVEMENT STARTED: " + packet.getId() + " (" + packet.getType() + ") on wire " + wire.getId() + " length=" + String.format("%.1f", wire.getLength()));
        }
        
        if (packet instanceof model.entity.packets.TrianglePacket && !isCompatible) {
            ((model.entity.packets.TrianglePacket) packet).resetSpeed();
        }
        wire.setAvailable(false);
        movingPackets.add(packet);
        if (packetController != null) {
            packetController.addPacket(packet);
        }
        
        return true;
    }

    // Determine compatibility using the port's current (possibly dynamic) shape kind.
    private static boolean isPortCompatibleWithPacket(Port port, Packet packet) {
        if (port == null || packet == null) return false;
        return port.isCompatible(packet);
    }
    
    public static void updateMovingPackets(double deltaTimeSeconds) {
        if (level != null && level.isPaused()) return;
        Iterator<Packet> iterator = movingPackets.iterator();
        while (iterator.hasNext()) {
            Packet packet = iterator.next();
            updatePacketMovement(packet, deltaTimeSeconds);
            if (packetController != null) {
                packetController.updatePacket(packet);
            }
            
            // Check for movement completion
            boolean shouldComplete = false;
            if (packet instanceof HexagonPacket) {
                HexagonPacket hexPacket = (HexagonPacket) packet;
                // Complete when reaching destination in FORWARD state
                shouldComplete = (hexPacket.getMovementState() == model.logic.packet.PacketState.FORWARD && 
                                packet.getMovementProgress() >= 1.0);
            } else {
                // Standard completion check for other packets
                shouldComplete = packet.getMovementProgress() >= 1.0;
            }
            
            if (shouldComplete) {
                completeMovement(packet);
                iterator.remove();
                if (packetController != null) {
                    packetController.updatePacket(packet);
                }
            }
        }

        // After movement updates, handle collisions and off-wire losses
        handleCollisionsAndOffWireLoss();

        // After movement/collision handling, process distributor forwarding
        if (level != null) {
            for (model.entity.systems.System sys : level.getSystems()) {
                if (sys instanceof model.entity.systems.DistributorSystem) {
                    manager.systems.DistributorSystemManager mgr = new manager.systems.DistributorSystemManager((model.entity.systems.DistributorSystem) sys);
                    mgr.forwardPackets();
                }
            }
        }
    }
    
    private static void updatePacketMovement(Packet packet, double deltaTimeSeconds) {
        Wire wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }
        double wireLength = wire.getLength();
        if (wireLength <= 0) {
            return;
        }
        
        packet.updateMovement(deltaTimeSeconds, packet.isCompatibleWithCurrentPort());
        
        // Special handling for HexagonPacket using distance-based movement
        if (packet instanceof HexagonPacket) {
            HexagonPacket hexPacket = (HexagonPacket) packet;
            double distanceTraveled = hexPacket.getDistanceTraveled();
            // Apply input-port incompatibility speed doubling for hexagon
            boolean inputCompatible = (wire.getDest() != null) && isPortCompatibleWithPacket(wire.getDest(), packet);
            if (!inputCompatible && !packet.isAergiaFrozenActive()) {
                // Add extra distance equal to current speed step to effectively double speed
                distanceTraveled += hexPacket.getSpeed() * deltaTimeSeconds;
                hexPacket.setDistanceTraveled(distanceTraveled);
            }
            
            // Debug log for hexagon speed freezing (reduced frequency)
            if (Math.random() < 0.016 && packet.isAergiaFrozenActive()) { // ~1/60 chance
                java.lang.System.out.println("DEBUG: HEXAGON MOVEMENT (FROZEN) → packet=" + packet.getId() + 
                    ", frozenSpeed=" + String.format("%.2f", packet.getAergiaFrozenSpeedOrNegative()) + 
                    ", normalSpeed=" + String.format("%.2f", hexPacket.getSpeed()) + 
                    ", speedDoubleSkipped=" + (!inputCompatible));
            }
            double progress = distanceTraveled / wireLength;
            // Check Aergia mark crossing and freeze speed if applicable for the remaining effect time
            if (level != null && !level.getAergiaMarks().isEmpty()) {
                long now = java.lang.System.nanoTime();
                for (model.logic.Shop.AergiaLogic.AergiaMark mark : level.getAergiaMarks()) {
                    if (mark.wire == wire && mark.effectEndNanos > now && progress >= mark.progress) {
                        packet.setAergiaFreeze(hexPacket.getSpeed(), mark.effectEndNanos);
                        java.lang.System.out.println("DEBUG: AERGIA MARK CROSSED (hexagon) → packet=" + packet.getId() +
                            ", wire=" + wire.getId() + ", progress=" + String.format("%.3f", progress) +
                            ", speedFrozenAt=" + String.format("%.2f", hexPacket.getSpeed()));
                        break;
                    }
                }
            }
            
            // Clamp progress to valid range
            if (progress < 0.0) progress = 0.0;
            if (progress > 1.0) progress = 1.0;
            
            packet.setMovementProgress(progress);
            Point2D newPosition = wire.getPositionAtProgress(progress);
            
            // For precise wire following, don't add deflection during normal movement
            // Deflection should only be a visual effect, not affect the actual path
            packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
            
            // Debug position updates for hexagon packets (reduced frequency)
            if (hexPacket.getMovementState() == model.logic.packet.PacketState.RETURNING && 
                Math.random() < 0.1) { // Only log 10% of the time to reduce spam
                java.lang.System.out.println("⬅️ HEXAGON VISUAL UPDATE: " + packet.getId() + " - Distance: " + String.format("%.1f", distanceTraveled) + 
                                           ", Progress: " + String.format("%.2f", progress) + ", Position: (" + 
                                           String.format("%.1f", newPosition.getX()) + ", " + String.format("%.1f", newPosition.getY()) + ")");
            }
        } else if (packet instanceof ConfidentialPacket) {
            // Special handling for ConfidentialPacket using custom movement logic
            ConfidentialPacketManager confidentialManager = confidentialManagers.get(packet);
            if (confidentialManager != null) {
                confidentialManager.updateMovement(deltaTimeSeconds);
            } else {
                // Fallback to standard movement if manager not found
                standardPacketMovement(packet, deltaTimeSeconds, wireLength);
            }
        } else if (packet instanceof MassivePacket.Type2) {
            // Type2 has constant speed but deflection is handled in packet.updateMovement()
            standardPacketMovement(packet, deltaTimeSeconds, wireLength);
        } else if (packet instanceof MassivePacket.Type1) {
            // Type1 accelerates on curved wires and constant on straight; updateMovement sets speed
            standardPacketMovement(packet, deltaTimeSeconds, wireLength);
        } else {
            // Standard progress-based movement for other packets
            standardPacketMovement(packet, deltaTimeSeconds, wireLength);
        }
    }
    
    /**
     * Standard movement logic for regular packets
     */
    private static void standardPacketMovement(Packet packet, double deltaTimeSeconds, double wireLength) {
        // Respect Aergia freeze if active
        double frozen = packet.getAergiaFrozenSpeedOrNegative();
        double speed = (frozen >= 0.0) ? frozen : packet.getSpeed();
        
        // Debug log every 60 frames (reduce spam) to show speed being used
        if (Math.random() < 0.016) { // ~1/60 chance
            if (frozen >= 0.0) {
                java.lang.System.out.println("DEBUG: PACKET MOVEMENT (FROZEN) → packet=" + packet.getId() + 
                    ", usingSpeed=" + String.format("%.2f", speed) + " (frozen), normalSpeed=" + String.format("%.2f", packet.getSpeed()));
            }
        }
        // If entering an incompatible input port, double the speed for square/triangle
        Wire wire = packet.getCurrentWire();
        if (wire != null && frozen < 0.0 && (packet instanceof model.entity.packets.SquarePacket || packet instanceof model.entity.packets.TrianglePacket || packet instanceof model.entity.packets.ProtectedPacket)) {
            boolean inputCompatible = (wire.getDest() != null) && isPortCompatibleWithPacket(wire.getDest(), packet);
            if (!inputCompatible) {
                speed *= 2.0;
            }
        }
        // Apply Aergia suppression past marked points: once a packet passes
        // any active mark on this wire, freeze its speed at current value for the mark duration
        if (wire != null && frozen < 0.0 && level != null && !level.getAergiaMarks().isEmpty()) {
            double progress = packet.getMovementProgress();
            long now = java.lang.System.nanoTime();
            for (model.logic.Shop.AergiaLogic.AergiaMark mark : level.getAergiaMarks()) {
                if (mark.wire == wire && mark.effectEndNanos > now && progress >= mark.progress) {
                    packet.setAergiaFreeze(speed, mark.effectEndNanos);
                    java.lang.System.out.println("DEBUG: AERGIA MARK CROSSED (standard) → packet=" + packet.getId() +
                        ", wire=" + wire.getId() + ", progress=" + String.format("%.3f", progress) +
                        ", speedFrozenAt=" + String.format("%.2f", speed));
                    break;
                }
            }
        }
        double distanceToMove = speed * deltaTimeSeconds;
        double progressIncrement = distanceToMove / wireLength;
        double newProgress = packet.getMovementProgress() + progressIncrement;
        if (newProgress > 1.0) {
            newProgress = 1.0;
        }
        packet.setMovementProgress(newProgress);
        Point2D newPosition = packet.getCurrentWire().getPositionAtProgress(newProgress);
        
        // For standard packets, also don't add deflection during normal movement
        packet.setPosition(new Point2D(newPosition.getX(), newPosition.getY()));
    }

    // === Collision handling and off-wire removal ===
    private static final double COLLISION_DEFLECT_MIN = 8.0;
    private static final double COLLISION_DEFLECT_MAX = 14.0;
    private static final double COLLISION_TRIGGER_PROBABILITY = 0.7; // not consistent

    private static void handleCollisionsAndOffWireLoss() {
        int n = movingPackets.size();
        if (n <= 1) return;

        java.util.Random rng = new java.util.Random();
        java.util.List<Packet> toRemove = new java.util.ArrayList<>();

        for (int i = 0; i < n; i++) {
            Packet a = movingPackets.get(i);
            Wire wireA = a.getCurrentWire();
            if (wireA == null) continue;

            // Off-wire removal check for packet a
            if (isPacketOffWire(a)) {
                toRemove.add(a);
                continue;
            }

            for (int j = i + 1; j < n; j++) {
                Packet b = movingPackets.get(j);
                if (b.getCurrentWire() != wireA) continue; // Only collide on same wire
                if (a.isInSystem() || b.isInSystem()) continue;

                // Broad phase: distance threshold to avoid heavy Shape.intersect if far apart
                double d = a.getPosition().distance(b.getPosition());
                if (d > 20.0) continue;

                // Narrow phase: collision shapes
                if (!a.intersects(b)) continue;

                // Not consistent: random chance to apply deflection
                if (rng.nextDouble() > COLLISION_TRIGGER_PROBABILITY) continue;

                // Compute local perpendicular to wire at mid-progress to push apart smoothly
                double progressMid = 0.5 * (a.getMovementProgress() + b.getMovementProgress());
                Point2D perp = computeWirePerpendicular(wireA, progressMid);
                if (perp == null) continue;

                double magnitude = COLLISION_DEFLECT_MIN + rng.nextDouble() * (COLLISION_DEFLECT_MAX - COLLISION_DEFLECT_MIN);

                // Push packets to opposite sides
                a.applyDeflection(perp.getX() * magnitude, perp.getY() * magnitude);
                b.applyDeflection(-perp.getX() * magnitude, -perp.getY() * magnitude);

                // Immediate off-wire check after deflection
                if (isPacketOffWire(a)) toRemove.add(a);
                if (isPacketOffWire(b)) toRemove.add(b);
            }
        }

        // Remove off-wire packets and count as loss
        if (!toRemove.isEmpty()) {
            for (Packet p : new java.util.HashSet<>(toRemove)) {
                movingPackets.remove(p);
                // Clean up confidential manager if present
                if (p instanceof ConfidentialPacket) {
                    ConfidentialPacketManager manager = confidentialManagers.remove(p);
                    if (manager != null) manager.cleanup();
                }
                if (packetController != null) {
                    packetController.removePacket(p); // counts as packet loss
                }
            }
        }
    }

    private static boolean isPacketOffWire(Packet packet) {
        double dx = packet.getDeflectedX();
        double dy = packet.getDeflectedY();
        double radial = Math.hypot(dx, dy);
        // Consider completely out of wire beyond this threshold
        final double OFF_WIRE_THRESHOLD = 18.0;
        return radial > OFF_WIRE_THRESHOLD || packet.isDeflectionTooLarge();
    }

    private static Point2D computeWirePerpendicular(Wire wire, double progress) {
        if (wire == null) return null;
        double h = 0.003;
        double p0 = Math.max(0.0, progress - h);
        double p1 = Math.min(1.0, progress + h);
        Point2D a = wire.getPositionAtProgress(p0);
        Point2D b = wire.getPositionAtProgress(p1);
        Point2D tangent = b.subtract(a);
        if (tangent.magnitude() <= 1e-6) return null;
        return new Point2D(-tangent.getY(), tangent.getX()).normalize();
    }
    
    private static void completeMovement(Packet packet) {
        Wire wire = packet.getCurrentWire();
        if (wire == null) {
            return;
        }
        
        // Set position to destination center for precise positioning
        Point2D destPos = wire.getDest().getPosition();
        packet.setPosition(new Point2D(destPos.getX(), destPos.getY()));
        packet.setInSystem(true);
        packet.setMoving(false);
        packet.setCurrentWire(null);
        packet.setMovementProgress(0.0);
        // Reset deflection after movement completes
        packet.resetDeflection();
        
        wire.setAvailable(true);
        
        // If a massive packet enters through an input port, morph that input port's shape kind
        try {
            if (packet instanceof MassivePacket && wire.getDest() != null && wire.getDest().getType() == model.entity.ports.PortType.INPUT) {
                java.util.Random rng = new java.util.Random();
                double r = rng.nextDouble();
                model.entity.ports.Port.ShapeKind newKind;
                if (r < 1.0 / 3.0) {
                    newKind = model.entity.ports.Port.ShapeKind.SQUARE;
                } else if (r < 2.0 / 3.0) {
                    newKind = model.entity.ports.Port.ShapeKind.TRIANGLE;
                } else {
                    newKind = model.entity.ports.Port.ShapeKind.HEXAGON;
                }
                wire.getDest().setShapeKind(newKind);
                java.lang.System.out.println("🔄 INPUT PORT MORPHED by MASSIVE packet at port " + wire.getDest().getId() + " → " + newKind);
                // Update visuals to match new shape
                refreshInputPortViewVisual(wire.getDest());
            }
        } catch (Throwable t) {
            // Fail-safe: do not interrupt delivery if morphing fails
        }
        
        model.entity.systems.System destinationSystem = wire.getDest().getSystem();
        
        deliverToDestinationSystem(packet, destinationSystem);

        // Track massive packet runs and remove wire after 3rd massive packet completes
        if (packet instanceof MassivePacket) {
            wire.incrementMassivePacketRunCount();
            java.lang.System.out.println("🟤 MASSIVE RUN COMPLETED on wire " + wire.getId() + " → count=" + wire.getMassivePacketRunCount());
            if (wire.hasReachedMassiveRunLimit()) {
                java.lang.System.out.println("🛑 MASSIVE RUN LIMIT REACHED (3). Detaching and deactivating wire " + wire.getId());
                // Remove/deactivate wire and detach ports
                wire.detachAndDeactivate();
                java.lang.System.out.println("🛑 Wire " + wire.getId() + " active=" + wire.isActive() + ", source.wire=" + (wire.getSource().getWire() != null) + ", dest.wire=" + (wire.getDest().getWire() != null));

                // Visual: mark the disabled wire red
                try {
                    view.components.wires.WireView.markDisabled(wire);
                } catch (Throwable t) {
                    // Ignore if view not available
                }

                // Visual: mark connected systems warning (yellow indicators)
                try {
                    markSystemsWarning(wire);
                } catch (Throwable t) {
                    // Ignore if view not available
                }
            }
        }
    }

    /**
     * Replace the PortView node for a given input port with a new view matching its current shape kind.
     */
    private static void refreshInputPortViewVisual(Port inputPort) {
        if (packetController == null || inputPort == null) return;
        try {
            Pane pane = packetController.getPacketLayer();
            if (pane == null) return;

            PortView oldView = null;
            SystemView targetSystemView = null;
            for (javafx.scene.Node node : new java.util.ArrayList<>(pane.getChildren())) {
                if (node instanceof PortView) {
                    PortView pv = (PortView) node;
                    if (pv.getModelPort() == inputPort) {
                        oldView = pv;
                    }
                } else if (node instanceof SystemView) {
                    SystemView sv = (SystemView) node;
                    if (sv.getSystem() == inputPort.getSystem()) {
                        targetSystemView = sv;
                    }
                }
            }

            double x = 0.0, y = 0.0;
            if (oldView != null) {
                x = oldView.getLayoutX();
                y = oldView.getLayoutY();
                pane.getChildren().remove(oldView);
            }

            boolean isInput = inputPort.getType() == model.entity.ports.PortType.INPUT;
            PortView newView;
            switch (inputPort.getShapeKind()) {
                case SQUARE:
                    newView = new SquarePortView(inputPort, isInput);
                    break;
                case TRIANGLE:
                    newView = new TrianglePortView(inputPort, isInput);
                    break;
                case HEXAGON:
                default:
                    newView = new HexagonPortView(inputPort, isInput);
                    break;
            }
            newView.setLayoutX(x);
            newView.setLayoutY(y);
            pane.getChildren().add(newView);

            if (targetSystemView != null) {
                java.util.List<PortView> inputs = targetSystemView.getInputPortViews();
                for (int i = 0; i < inputs.size(); i++) {
                    if (inputs.get(i).getModelPort() == inputPort) {
                        inputs.set(i, newView);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            // Ignore failures in visual replacement
        }
    }

    // Try to set indicator lamp of systems connected to the disabled wire to yellow
    private static void markSystemsWarning(Wire wire) {
        if (wire == null) return;
        model.entity.systems.System srcSys = wire.getSource().getSystem();
        model.entity.systems.System dstSys = wire.getDest().getSystem();
        javafx.scene.layout.Pane pane = (packetController != null) ? packetController.getPacketLayer() : null;
        if (pane == null) return;
        for (javafx.scene.Node node : pane.getChildren()) {
            if (node instanceof view.components.systems.SystemView) {
                view.components.systems.SystemView sv = (view.components.systems.SystemView) node;
                if (sv.getSystem() == srcSys || sv.getSystem() == dstSys) {
                    sv.setIndicatorWarning();
                }
            }
        }
    }
    
    private static void deliverToDestinationSystem(Packet packet, model.entity.systems.System destinationSystem) {
        if (destinationSystem instanceof model.entity.systems.IntermediateSystem) {
            model.entity.systems.IntermediateSystem intermediateSystem = (model.entity.systems.IntermediateSystem) destinationSystem;
            
            manager.systems.IntermediateSystemManager manager = 
                new manager.systems.IntermediateSystemManager(intermediateSystem);
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(intermediateSystem.getPosition());
            
            // Hide the packet visually since it's in internal storage
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.DDosSystem) {
            model.entity.systems.DDosSystem ddosSystem = (model.entity.systems.DDosSystem) destinationSystem;
            
            manager.systems.DDosSystemManager manager = 
                new manager.systems.DDosSystemManager(ddosSystem);
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(ddosSystem.getPosition());
            
            // Hide the packet visually since it's in internal storage
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.SpySystem) {
            model.entity.systems.SpySystem spySystem = (model.entity.systems.SpySystem) destinationSystem;
            
            manager.systems.SpySystemManager manager = 
                new manager.systems.SpySystemManager(spySystem);
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(spySystem.getPosition());
            
            // Hide the packet visually since it's in internal storage
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.VPNSystem) {
            model.entity.systems.VPNSystem vpnSystem = (model.entity.systems.VPNSystem) destinationSystem;
            
            manager.systems.VPNSystemManager manager = 
                new manager.systems.VPNSystemManager(vpnSystem);
            manager.setLevel(level);  // Required for global VPN failure handling
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(vpnSystem.getPosition());
            
            // Hide the packet visually since it's in internal storage
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.AntiVirusSystem) {
            model.entity.systems.AntiVirusSystem antivirusSystem = (model.entity.systems.AntiVirusSystem) destinationSystem;
            
            manager.systems.AntiVirusSystemManager manager = 
                new manager.systems.AntiVirusSystemManager(antivirusSystem);
            manager.receivePacket(packet);
            
            // CRITICAL FIX: Update packet visual position to be inside the system and hide it
            // since it's now in internal storage
            
            // Move packet to system center position
            packet.setPosition(antivirusSystem.getPosition());
            
            // Hide the packet visually since it's in internal storage
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.DistributorSystem) {
            model.entity.systems.DistributorSystem distributorSystem = (model.entity.systems.DistributorSystem) destinationSystem;
            manager.systems.DistributorSystemManager manager =
                new manager.systems.DistributorSystemManager(distributorSystem);
            
            // Move packet to system center position
            packet.setPosition(distributorSystem.getPosition());
            
            // Hide all packets entering the distributor (they're stored in FIFO queue)
            if (packetController != null) {
                packetController.hidePacket(packet);
            }
            
            // Process the packet (will be forwarded when ports are available)
            manager.receivePacket(packet);
            
            // Only remove massive packets from level (they get split)
            // Non-massive packets stay in level but hidden
            if (packet instanceof MassivePacket && level != null) {
                level.removePacket(packet);
            }
            
        } else if (destinationSystem instanceof model.entity.systems.MergeSystem) {
            model.entity.systems.MergeSystem mergeSystem = (model.entity.systems.MergeSystem) destinationSystem;
            manager.systems.MergeSystemManager manager =
                new manager.systems.MergeSystemManager(mergeSystem);
            manager.receivePacket(packet);

            // Update MergeSystemView labels immediately after storage change
            if (packetController != null) {
                try {
                    Pane pane = packetController.getPacketLayer();
                    if (pane != null) {
                        for (javafx.scene.Node node : pane.getChildren()) {
                            if (node instanceof MergeSystemView) {
                                MergeSystemView msv = (MergeSystemView) node;
                                if (msv.getMergeSystem() == mergeSystem) {
                                    manager.updateView(msv);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            
        } else if (destinationSystem instanceof model.entity.systems.EndSystem) {
            model.entity.systems.EndSystem endSystem = (model.entity.systems.EndSystem) destinationSystem;
            endSystem.claimPacket(packet, level);
            // Don't call deliverPacket here - the iterator in updateMovingPackets will handle removal
            if (packetController != null) {
                packetController.deliverPacket(packet);
            }
        }
    }
    
    public static void removePacket(Packet packet) {
        movingPackets.remove(packet);
        
        // Clean up confidential packet manager if exists
        if (packet instanceof ConfidentialPacket) {
            ConfidentialPacketManager manager = confidentialManagers.remove(packet);
            if (manager != null) {
                manager.cleanup();
            }
        }
        
        if (packetController != null) {
            packetController.removePacket(packet);
        }
    }
    
    /**
     * Remove packet from view only (for successful delivery).
     * This method should be used when packets successfully reach their destination
     * and should not be counted as packet loss.
     * Note: This method no longer removes from movingPackets as that's handled by the iterator.
     */
    public static void deliverPacket(Packet packet) {
        // Don't remove from movingPackets here - the iterator in updateMovingPackets handles that
        if (packetController != null) {
            packetController.deliverPacket(packet);
        }
    }
    
    public static List<Packet> getMovingPackets() {
        return new ArrayList<>(movingPackets);
    }
    
    public static boolean hasMovingPackets() {
        return !movingPackets.isEmpty();
    }
    
    /**
     * Convert a protected packet to its original type and update visuals
     */
    public static void convertProtectedPacket(Packet oldPacket, Packet newPacket) {
        // Replace in moving packets list
        if (movingPackets.contains(oldPacket)) {
            movingPackets.remove(oldPacket);
            movingPackets.add(newPacket);
            
            // Update packet controller for visual changes
            if (packetController != null) {
                packetController.removePacket(oldPacket);
                packetController.addPacket(newPacket);
                java.lang.System.out.println("🔄 PACKET VISUAL CONVERSION: " + oldPacket.getId() + " visual updated from PROTECTED to " + newPacket.getType());
            }
        }
    }
}

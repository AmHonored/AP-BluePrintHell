package manager.packets;

import model.entity.packets.HexagonPacket;
import model.entity.packets.Packet;
import model.logic.packet.PacketState;
import model.wire.Wire;
import javafx.geometry.Point2D;

public class HexagonPacketManager {
    private final HexagonPacket packet;
    private final Wire wire;

    public HexagonPacketManager(HexagonPacket packet, Wire wire) {
        this.packet = packet;
        this.wire = wire;
        initializePacket();
    }

    private void initializePacket() {
        // Set the total path length for this wire
        packet.setTotalPathLength(wire.getLength());
        packet.setDistanceTraveled(0.0);
    }

    public void updateMovement(double deltaTimeSeconds) {
        // Update packet movement based on current state
        packet.updateMovement(deltaTimeSeconds, isCompatibleWithCurrentPort());
        
        // Check if packet has reached destination
        if (packet.hasReachedDestination()) {
            handleDestinationReached();
        }
    }

    private boolean isCompatibleWithCurrentPort() {
        // Check if packet type is compatible with the wire's port type
        // This would need to be implemented based on your port compatibility logic
        return true; // Placeholder - implement actual compatibility check
    }

    private void handleDestinationReached() {
        // Check if destination system is disabled
        if (isDestinationSystemDisabled()) {
            // Start returning to source
            packet.setMovementState(PacketState.RETURNING);
        } else {
            // Normal packet delivery
            deliverPacketToDestination();
        }
    }

    private boolean isDestinationSystemDisabled() {
        // Check if the destination system is disabled
        // This would need to be implemented based on your system state logic
        return false; // Placeholder - implement actual system state check
    }

    private void deliverPacketToDestination() {
        // Deliver packet to destination system
        // This would integrate with your existing packet delivery system
        System.out.println("HexagonPacket " + packet.getId() + " delivered to destination");
    }

    public void handleCollision() {
        // Hexagon packets change direction when colliding
        packet.changeDirection();
        System.out.println("HexagonPacket " + packet.getId() + " changed direction due to collision");
    }

    public Point2D getCurrentPosition() {
        // Calculate current position based on distance traveled and wire
        double progress = packet.getDistanceTraveled() / wire.getLength();
        if (packet.getMovementState() == PacketState.RETURNING) {
            // When returning, reverse the progress
            progress = 1.0 - progress;
        }
        return wire.getPositionAtProgress(progress);
    }

    public boolean isReturning() {
        return packet.getMovementState() == PacketState.RETURNING;
    }

    public boolean hasCompletedJourney() {
        // Packet has completed its journey when it's back at the start after returning
        return packet.getMovementState() == PacketState.RETURNING && 
               packet.getDistanceTraveled() <= 0;
    }
} 
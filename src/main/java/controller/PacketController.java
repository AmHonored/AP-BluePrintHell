package controller;

import model.entity.packets.Packet;
import model.entity.packets.SquarePacket;
import model.entity.packets.TrianglePacket;
import model.entity.packets.HexagonPacket;
import model.entity.packets.ProtectedPacket;
import model.entity.packets.MassivePacket;
import model.entity.packets.ConfidentialPacket;
import view.components.packets.PacketView;
import view.components.packets.SquarePacketView;
import view.components.packets.TrianglePacketView;
import view.components.packets.HexagonPacketView;
import view.components.packets.ProtectedPacketView;
import view.components.packets.ConfidentialPacketView;
import view.components.packets.MassivePacketView;
import javafx.scene.layout.Pane;
import model.levels.Level;
import java.util.HashMap;
import java.util.Map;

public class PacketController {
    private Level level;
    private Pane packetLayer; // The pane to which PacketViews are added/removed
    private final Map<Packet, PacketView> packetViewMap = new HashMap<>();

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setPacketLayer(Pane packetLayer) {
        this.packetLayer = packetLayer;
    }

    public void addPacket(Packet packet) {
        if (packet == null || packetViewMap.containsKey(packet) || packetLayer == null) return;
        PacketView view;
        if (packet instanceof ProtectedPacket) {
            view = new ProtectedPacketView((ProtectedPacket) packet);
        } else if (packet instanceof ConfidentialPacket) {
            view = new ConfidentialPacketView((ConfidentialPacket) packet);
        } else if (packet instanceof MassivePacket) {
            view = new MassivePacketView((MassivePacket) packet);
        } else if (packet instanceof SquarePacket) {
            view = new SquarePacketView((SquarePacket) packet);
        } else if (packet instanceof TrianglePacket) {
            view = new TrianglePacketView((TrianglePacket) packet);
        } else if (packet instanceof HexagonPacket) {
            view = new HexagonPacketView((HexagonPacket) packet);
        } else {
            return;
        }
        packetViewMap.put(packet, view);
        packetLayer.getChildren().add(view);
    }

    public void updatePacket(Packet packet) {
        PacketView view = packetViewMap.get(packet);
        if (view != null) {
            view.updatePosition();
            view.updateHealth();
            view.updateDeflection();
            
            // Update hexagon packet movement state if applicable
            if (packet instanceof HexagonPacket && view instanceof HexagonPacketView) {
                ((HexagonPacketView) view).updateMovementState();
            }
        }
    }

    public void removePacket(Packet packet) {
        PacketView view = packetViewMap.remove(packet);
        if (view != null && packetLayer != null) {
            packetLayer.getChildren().remove(view);
        }
        if (level != null && packet != null) {
            level.removePacket(packet);
            level.incrementPacketLoss();
            if (level.isGameOver() && !level.getGameOverFlag()) {
                level.setGameOver(true);
                // Optionally: notify UI or stop timers here
            }
        }
    }

    /**
     * Remove packet from view only (for successful deliveries).
     * This method should be used when packets successfully reach their destination
     * and should not be counted as packet loss.
     */
    public void deliverPacket(Packet packet) {
        java.lang.System.out.println("DEBUG: PacketController.deliverPacket called for packet " + packet.getId());
        PacketView view = packetViewMap.remove(packet);
        java.lang.System.out.println("DEBUG: PacketView found and removed: " + (view != null));
        if (view != null && packetLayer != null) {
            boolean removed = packetLayer.getChildren().remove(view);
            java.lang.System.out.println("DEBUG: Removed from packetLayer: " + removed + " (packetLayer children count: " + packetLayer.getChildren().size() + ")");
        } else {
            java.lang.System.out.println("DEBUG: view=" + (view != null) + ", packetLayer=" + (packetLayer != null));
        }
        // Note: Don't remove from level or increment packet loss - 
        // the receiving system (EndSystem.claimPacket) handles that
        java.lang.System.out.println("DEBUG: PacketController.deliverPacket completed");
    }

    // Restore for compatibility with ImpactManager and other usages
    public void killPacket(Packet packet) {
        removePacket(packet);
    }

    public PacketView getPacketView(Packet packet) {
        return packetViewMap.get(packet);
    }
    
    /**
     * Get the packet layer for visual effects
     */
    public Pane getPacketLayer() {
        return packetLayer;
    }
    
    /**
     * Show a packet (make it visible)
     */
    public void showPacket(Packet packet) {
        PacketView view = packetViewMap.get(packet);
        if (view != null) {
            view.setPacketVisible(true);
        }
    }
    
    /**
     * Hide a packet (make it invisible)
     */
    public void hidePacket(Packet packet) {
        PacketView view = packetViewMap.get(packet);
        if (view != null) {
            view.setPacketVisible(false);
        }
    }

    public void clearAll() {
        if (packetLayer != null) {
            packetLayer.getChildren().removeAll(packetViewMap.values());
        }
        packetViewMap.clear();
    }
}

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
import view.components.packets.BitCirclePacketView;
import view.components.packets.BitRectPacketView;
import javafx.scene.layout.Pane;
import model.levels.Level;
import java.util.HashMap;
import java.util.Map;

public class PacketController {
    private Level level;
    private Pane packetLayer; 
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
        } else if (packet instanceof model.entity.packets.bits.BitCirclePacket) {
            view = new BitCirclePacketView((model.entity.packets.bits.BitCirclePacket) packet);
        } else if (packet instanceof model.entity.packets.bits.BitRectPacket) {
            view = new BitRectPacketView((model.entity.packets.bits.BitRectPacket) packet);
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
        }
    }


    public void deliverPacket(Packet packet) {
        PacketView view = packetViewMap.remove(packet);
        if (view != null && packetLayer != null) {
            packetLayer.getChildren().remove(view);
        }

    }

    public void killPacket(Packet packet) {
        removePacket(packet);
    }

    public PacketView getPacketView(Packet packet) {
        return packetViewMap.get(packet);
    }
    
    public Pane getPacketLayer() {
        return packetLayer;
    }
    
    public void showPacket(Packet packet) {
        PacketView view = packetViewMap.get(packet);
        if (view != null) {
            view.setPacketVisible(true);
        }
    }
    
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

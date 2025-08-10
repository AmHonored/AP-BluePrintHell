package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.packets.Packet;
import model.levels.Level;

public class EndSystem extends System {
    public EndSystem(Point2D position) {
        super(position, SystemType.EndSystem);
    }

    public void claimPacket(Packet packet, Level lvl) {
        if (packet == null) return;

        if (packet.isBitFragment()) {
            lvl.incrementPacketLoss();
            lvl.removePacket(packet);
            return;
        }
        lvl.addCoins(packet.getSize());
        lvl.incrementPacketsCollected();
        lvl.removePacket(packet);
    }

    @Override
    public boolean isDraggableWithSisyphus() {
        return false;
    }
}

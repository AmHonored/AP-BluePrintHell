package model.entity.ports;

import javafx.geometry.Point2D;
import model.entity.systems.System;
import model.entity.packets.Packet;
import model.entity.packets.HexagonPacket;

public class HexagonPort extends Port {
    public HexagonPort(String id, System system, PortType type, Point2D position) {
        super(id, system, type, position);
    }

    @Override
    public boolean isCompatible(Packet packet) {
        return packet instanceof HexagonPacket;
    }
}

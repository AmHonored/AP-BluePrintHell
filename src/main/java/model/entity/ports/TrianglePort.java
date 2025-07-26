package model.entity.ports;

import javafx.geometry.Point2D;
import model.entity.systems.System;
import model.entity.packets.Packet;
import model.entity.packets.TrianglePacket;

public class TrianglePort extends Port {
    public TrianglePort(String id, System system, PortType type, Point2D position) {
        super(id, system, type, position);
    }

    @Override
    public boolean isCompatible(Packet packet) {
        return packet instanceof TrianglePacket;
    }
}

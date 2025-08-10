package model.entity.ports;

import javafx.geometry.Point2D;
import model.entity.systems.System;
import model.entity.packets.Packet;

public class SquarePort extends Port {
    public SquarePort(String id, System system, PortType type, Point2D position) {
        super(id, system, type, position);
        setShapeKind(ShapeKind.SQUARE);
    }

    @Override
    public boolean isCompatible(Packet packet) {
        return isCompatibleByShapeKind(packet);
    }
}

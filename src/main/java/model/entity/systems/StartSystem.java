package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.ports.Port;
import model.entity.packets.SquarePacket;
import model.entity.packets.TrianglePacket;
import model.entity.packets.Packet;

public class StartSystem extends System {
    public StartSystem(Point2D position) {
        super(position, SystemType.StartSystem);
    }

    public void addOutPort(Port port) {
        outPorts.add(port);
    }

    public Packet generatePacketIfPossible(Port port) {
        if (!port.isConnected()) {
            return null;
        }
        // Determine packet type by port class or property
        String portClass = port.getClass().getSimpleName().toLowerCase();
        if (portClass.contains("square")) {
            return new SquarePacket("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
        } else if (portClass.contains("triangle")) {
            return new TrianglePacket("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
        }
        return null;
    }
}

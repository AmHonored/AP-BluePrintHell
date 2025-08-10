package model.entity.systems;

import javafx.geometry.Point2D;
import model.entity.ports.Port;
import model.entity.packets.SquarePacket;
import model.entity.packets.TrianglePacket;
import model.entity.packets.HexagonPacket;
import model.entity.packets.ConfidentialPacket;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;

public class StartSystem extends System {

    private int maxPacketsToGenerate = 8; 
    private int generatedPacketCount = 0;

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
        if (!canGenerateMore()) {
            return null;
        }
        
        if (port.shouldGenerateConfidentialPacket()) {
            return new ConfidentialPacket.Type1("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
        }

        if (port.shouldGenerateMassivePacket()) {
            if (Math.random() < 0.5) {
                return new MassivePacket.Type1("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
            } else {
                return new MassivePacket.Type2("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
            }
        }
        
        String portClass = port.getClass().getSimpleName().toLowerCase();
        if (portClass.contains("square")) {
            return new SquarePacket("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
        } else if (portClass.contains("triangle")) {
            return new TrianglePacket("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
        } else if (portClass.contains("hexagon")) {
            return new HexagonPacket("pkt-" + java.lang.System.nanoTime(), port.getPosition(), port.getPosition());
        }
        return null;
    }

    public void onPacketGenerated() {
        generatedPacketCount++;
    }

    public boolean canGenerateMore() {
        return maxPacketsToGenerate < 0 || generatedPacketCount < maxPacketsToGenerate;
    }

    public boolean isGenerationComplete() {
        return maxPacketsToGenerate >= 0 && generatedPacketCount >= maxPacketsToGenerate;
    }

    public int getGeneratedPacketCount() {
        return generatedPacketCount;
    }

    public int getMaxPacketsToGenerate() {
        return maxPacketsToGenerate;
    }

    public void setMaxPacketsToGenerate(int maxPacketsToGenerate) {
        this.maxPacketsToGenerate = maxPacketsToGenerate;
    }

    @Override
    public boolean isDraggableWithSisyphus() {
        return false;
    }
}

package manager.systems;

import manager.packets.PacketManager;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;
import model.entity.packets.PacketType;
import model.entity.ports.Port;
import model.entity.systems.MergeSystem;
import view.components.systems.MergeSystemView;
import javafx.geometry.Point2D;

import java.util.UUID;

public class MergeSystemManager {
    private final MergeSystem system;

    public MergeSystemManager(MergeSystem system) {
        this.system = system;
    }

    public void receivePacket(Packet packet) {

        if (packet.isBitFragment()) {
            system.addBitPacket(packet);

            checkAndCreateMassivePacket();
        } else {
            forwardPacket(packet);
        }
    }

    private void checkAndCreateMassivePacket() {
        if (!system.canCreateMassivePacket()) {
            return;
        }

        PacketType massiveType = system.getMassivePacketType();
        if (massiveType == null) {
            return;
        }

        system.removeBitsForMassive(massiveType);
        
        MassivePacket newMassive = null;
        String id = "MP-" + UUID.randomUUID().toString().substring(0, 8);
        Point2D position = system.getPosition();
        Point2D direction = new Point2D(1, 0);

        if (massiveType == PacketType.MASSIVE_TYPE1) {
            newMassive = new MassivePacket.Type1(id, position, direction);
            System.out.println("T1 Created");
        } else if (massiveType == PacketType.MASSIVE_TYPE2) {
            newMassive = new MassivePacket.Type2(id, position, direction);
            System.out.println("T2 Created");
        }

        if (newMassive != null) {
            double packetLoss = system.calculatePacketLoss();
            if (packetLoss > 0) {
                int healthReduction = (int) Math.round(packetLoss);
                int newHealth = Math.max(1, newMassive.getHealth() - healthReduction);
                newMassive.setCurrentHealth(newHealth);
                
            }

            forwardPacket(newMassive);
        }
    }


    private void forwardPacket(Packet packet) {
        Port bestPort = system.findBestOutPort(packet);
        if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isActive()) {
            packet.setPosition(bestPort.getPosition());
            
            boolean sent = PacketManager.sendPacket(bestPort, packet);
            if (sent) {
                
            }
        } else {
            
        }
    }

    public void updateView(MergeSystemView view) {
        if (view != null) {
            view.updateCounts(system.getCircleBitCount(), system.getRectBitCount());
        }
    }
}

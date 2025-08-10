package manager.systems;

import manager.packets.PacketManager;
import model.entity.packets.MassivePacket;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import model.entity.systems.DistributorSystem;

import java.util.List;

public class DistributorSystemManager {
    private final DistributorSystem system;
    private int lastUsedPortIndex = -1; 

    public DistributorSystemManager(DistributorSystem system) {
        this.system = system;
    }

    public void receivePacket(Packet packet) {
        if (packet instanceof MassivePacket) {
            MassivePacket massive = (MassivePacket) packet;

            List<Packet> bits = massive.splitIntoBits();

            for (Packet bit : bits) {
                bit.setPosition(system.getPosition());
                system.processPacket(bit);
            }

            forwardPackets();
        } else {
            system.processPacket(packet);
            
            java.util.Queue<Packet> queue = system.getPackets();
            if (queue.peek() == packet) { 
                Port best = findBestAvailableOutPort(packet);
                if (best != null && best.getWire() != null && best.getWire().isActive()) {
                    packet.setPosition(best.getPosition());
                    
                    boolean sent = PacketManager.sendPacket(best, packet, true);
                    if (sent) {
                        queue.poll(); 
                    }
                } else {
                }
            }
        }
    }

    public void forwardPackets() {
        java.util.Queue<Packet> queue = system.getPackets();
        while (!queue.isEmpty()) {
            Packet next = queue.peek();
            Port best = findBestAvailableOutPort(next);
            if (best != null && best.getWire() != null && best.getWire().isActive()) {
                next.setPosition(best.getPosition());
                
                boolean preserveCompatibility = !next.isBitFragment() && !(next instanceof MassivePacket);
                
                boolean sent = PacketManager.sendPacket(best, next, preserveCompatibility);
                if (sent) {
                    queue.poll();
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }

    private Port findBestAvailableOutPort(Packet packet) {
        java.util.List<Port> outPorts = system.getAvailableOutPorts(packet);
        if (outPorts.isEmpty()) {
            return null;
        }
        // round robin
        lastUsedPortIndex = (lastUsedPortIndex + 1) % outPorts.size();
        return outPorts.get(lastUsedPortIndex);
    }
}



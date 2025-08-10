package manager.systems;

import model.entity.systems.System;
import model.levels.Level;
import model.entity.packets.Packet;
import model.entity.ports.Port;
import manager.packets.PacketManager;
import java.util.function.Function;

public abstract class SystemManager<T extends System> {
    protected final T system;
    protected Level level;

    protected SystemManager(T system) {
        this.system = system;
    }

    protected SystemManager(T system, Level level) {
        this.system = system;
        this.level = level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    protected boolean forwardNextFromQueue(java.util.Queue<Packet> queue) {
        if (queue == null || queue.isEmpty()) return false;
        Packet packet = queue.peek();
        Port bestPort = system.findBestOutPort(packet);
        if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isActive()) {
            packet.setPosition(bestPort.getPosition());
            boolean sent = PacketManager.sendPacket(bestPort, packet);
            if (sent) {
                queue.poll();
                return true;
            }
        }
        return false;
    }

    protected void forwardAllFromQueue(java.util.Queue<Packet> queue) {
        while (queue != null && !queue.isEmpty()) {
            boolean forwarded = forwardNextFromQueue(queue);
            if (!forwarded) break;
        }
    }

    protected void forwardAllFromQueue(java.util.Queue<Packet> queue, Function<Packet, Port> portSelector) {
        if (portSelector == null) {
            forwardAllFromQueue(queue);
            return;
        }
        while (queue != null && !queue.isEmpty()) {
            Packet packet = queue.peek();
            Port bestPort = portSelector.apply(packet);
            if (bestPort != null && bestPort.getWire() != null && bestPort.getWire().isActive()) {
                packet.setPosition(bestPort.getPosition());
                boolean sent = PacketManager.sendPacket(bestPort, packet);
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
} 
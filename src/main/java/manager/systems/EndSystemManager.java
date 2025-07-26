package manager.systems;

import model.entity.systems.EndSystem;
import model.entity.packets.Packet;
import model.levels.Level;

public class EndSystemManager {
    private final EndSystem system;
    private final Level level;

    public EndSystemManager(EndSystem system, Level level) {
        this.system = system;
        this.level = level;
    }

    public void receivePacket(Packet packet) {
        if (packet != null) {
            system.claimPacket(packet, level);
        }
    }
}

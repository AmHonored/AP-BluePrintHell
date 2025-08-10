package model.entity.systems;

import javafx.geometry.Point2D;
import model.levels.Level;

public class EndSystem extends System {
    public EndSystem(Point2D position) {
        super(position, SystemType.EndSystem);
    }

    public void claimPacket(Object packet, Level lvl) {
        if (packet instanceof model.entity.packets.Packet) {
            model.entity.packets.Packet pkt = (model.entity.packets.Packet) packet;
            // Bit fragments count as packet loss (no coins)
            if (pkt.isBitFragment()) {
                lvl.incrementPacketLoss();
                lvl.removePacket(pkt);
                java.lang.System.out.println("DEBUG: EndSystem.claimPacket - Bit fragment reached end. Counted as loss. id=" + pkt.getId());
                return;
            }
            int coinsToAdd = pkt.getSize();
            int coinsBefore = lvl.getCoins();
            java.lang.System.out.println("DEBUG: EndSystem.claimPacket - Adding " + coinsToAdd + " coins for packet " + pkt.getId());
            java.lang.System.out.println("DEBUG: Coins before: " + coinsBefore);
            lvl.addCoins(coinsToAdd);
            int coinsAfter = lvl.getCoins();
            java.lang.System.out.println("DEBUG: Coins after: " + coinsAfter + " (difference: " + (coinsAfter - coinsBefore) + ")");
            
            // Increment packets collected counter
            lvl.incrementPacketsCollected();
            java.lang.System.out.println("DEBUG: EndSystem.claimPacket - Incremented packetsCollected to: " + lvl.getPacketsCollected());
            
            lvl.removePacket(pkt);
            java.lang.System.out.println("DEBUG: EndSystem.claimPacket completed for packet " + pkt.getId());
        }
    }

    @Override
    public boolean isDraggableWithSisyphus() {
        return false;
    }
}

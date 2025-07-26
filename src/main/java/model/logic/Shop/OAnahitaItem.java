package model.logic.Shop;

import model.levels.Level;
import model.entity.packets.Packet;

public class OAnahitaItem implements ShopItem {
    @Override
    public String getName() { return "O' Anahita"; }
    @Override
    public int getPrice() { return 5; }
    @Override
    public int getDurationSeconds() { return 0; }
    @Override
    public void apply(Level level) {
        System.out.println("DEBUG: OAnahitaItem.apply() - Removing noise from all packets");
        int packetCount = level.getPackets().size();
        System.out.println("DEBUG: OAnahitaItem.apply() - Found " + packetCount + " packets to process");
        
        int processedPackets = 0;
        for (Packet packet : level.getPackets()) {
            double oldNoise = packet.getNoise();
            packet.setNoise(0);
            double newNoise = packet.getNoise();
            processedPackets++;
            System.out.println("DEBUG: OAnahitaItem - Packet " + processedPackets + " noise: " + oldNoise + " -> " + newNoise);
        }
        
        System.out.println("DEBUG: OAnahitaItem.apply() - Processed " + processedPackets + " packets");
    }
} 
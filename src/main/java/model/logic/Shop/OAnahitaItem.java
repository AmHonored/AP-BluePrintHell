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
        for (Packet packet : level.getPackets()) {
            packet.setNoise(0);
        }
    }
} 
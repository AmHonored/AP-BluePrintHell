package model.logic.Shop;

import model.levels.Level;

/**
 * Scroll of Eliphas: By paying 20 coins, the player can place a temporary mark
 * on a wire. For the duration (e.g., 30 seconds), packets that pass this point
 * will continuously re-center their deflection to the wire path to avoid tunneling
 * and keep collision logic stable.
 */
public class OEliphasItem implements ShopItem {
    @Override
    public String getName() { return "Scroll of Eliphas"; }

    @Override
    public int getPrice() { return 20; }

    @Override
    public int getDurationSeconds() { return 0; } // inventory item - placement on use

    @Override
    public void apply(Level level) {
        // Increase inventory count by 1
        level.addEliphasScrolls(1);
        System.out.println("DEBUG: OEliphasItem.apply() - Added 1 Eliphas scroll. Total = " + level.getEliphasScrolls());
    }
}




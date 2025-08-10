package model.logic.Shop;

import model.levels.Level;

/**
 * Scroll of Aergia: Purchasing this item increases the player's
 * available Aergia scrolls by one. Using a scroll in-game lets the
 * player place a temporary mark on a wire that suppresses packet
 * acceleration after crossing that mark.
 */
public class OAergiaItem implements ShopItem {
    @Override
    public String getName() { return "Scroll of Aergia"; }

    @Override
    public int getPrice() { return 10; }

    @Override
    public int getDurationSeconds() { return 0; } // inventory item

    @Override
    public void apply(Level level) {
        // Increase inventory count by 1
        level.addAergiaScrolls(1);
        System.out.println("DEBUG: OAergiaItem.apply() - Added 1 Aergia scroll. Total = " + level.getAergiaScrolls());
    }
}






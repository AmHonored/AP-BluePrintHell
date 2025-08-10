package model.logic.Shop;

import model.levels.Level;

/**
 * Scroll of Sisyphus: By paying 15 coins, the player can move one of the 
 * non-reference systems (any system except start or end systems) within a 
 * specified radius. This ability requires that wire lengths don't exceed 
 * the available wire length and that moving systems doesn't cause wires 
 * to pass through any other systems.
 */
public class OSisyphusItem implements ShopItem {
    @Override
    public String getName() { 
        return "Scroll of Sisyphus"; 
    }

    @Override
    public int getPrice() { 
        return 15; 
    }

    @Override
    public int getDurationSeconds() { 
        return 0; // inventory item - instant use
    }

    @Override
    public void apply(Level level) {
        // Increase inventory count by 1
        level.addSisyphusScrolls(1);
        System.out.println("DEBUG: OSisyphusItem.apply() - Added 1 Sisyphus scroll. Total = " + level.getSisyphusScrolls());
    }
}


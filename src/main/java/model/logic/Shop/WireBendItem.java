package model.logic.Shop;

import model.levels.Level;

public class WireBendItem implements ShopItem {
    
    @Override
    public String getName() {
        return "Wire Bend";
    }
    
    @Override
    public int getPrice() {
        return 1;
    }
    
    @Override
    public int getDurationSeconds() {
        return 0; // Instant effect
    }
    
    @Override
    public void apply(Level level) {
        // This item doesn't directly modify the level
        // The actual bend point addition is handled by the WireView when purchased
        // This just deducts the coin cost which is handled by ShopManager.purchase()
        System.out.println("DEBUG: WireBendItem.apply() - Bend point purchase processed");
    }
}
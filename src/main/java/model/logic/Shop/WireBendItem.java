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
        return 0; 
    }
    
    @Override
    public void apply(Level level) {
    }
}
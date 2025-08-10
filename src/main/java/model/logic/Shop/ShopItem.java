package model.logic.Shop;

import model.levels.Level;

public interface ShopItem {
    String getName();
    int getPrice();
    int getDurationSeconds(); 
    void apply(Level level);
}

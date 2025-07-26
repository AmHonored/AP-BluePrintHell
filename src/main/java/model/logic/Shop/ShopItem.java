package model.logic.Shop;

import model.levels.Level;

public interface ShopItem {
    String getName();
    int getPrice();
    int getDurationSeconds(); // 0 for instant
    void apply(Level level);
}

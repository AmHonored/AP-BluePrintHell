package manager.game;

import model.wire.Wire;
import model.levels.Level;
import manager.game.ShopManager;

public class WireBendManager {
    private final Level level;
    private final ShopManager shopManager;
    private static final int BEND_POINT_COST = 1;
    
    public WireBendManager(Level level, ShopManager shopManager) {
        this.level = level;
        this.shopManager = shopManager;
    }
    
    /**
     * Attempts to purchase a bend point for the given wire
     * @param wire The wire to add a bend point to
     * @return true if purchase was successful, false otherwise
     */
    public boolean purchaseBendPoint(Wire wire) {
        if (!wire.canAddBendPoint()) {
            return false;
        }
        
        if (level.getCoins() < BEND_POINT_COST) {
            return false;
        }
        
        // Deduct coins
        level.addCoins(-BEND_POINT_COST);
        
        // Play purchase sound
        service.AudioManager.playShopPurchase();
        
        return true;
    }
    
    /**
     * Check if a bend point can be purchased for the given wire
     * @param wire The wire to check
     * @return true if bend point can be purchased, false otherwise
     */
    public boolean canPurchaseBendPoint(Wire wire) {
        return wire.canAddBendPoint() && level.getCoins() >= BEND_POINT_COST;
    }
    
    /**
     * Get the cost of a bend point
     * @return The cost in coins
     */
    public int getBendPointCost() {
        return BEND_POINT_COST;
    }
}
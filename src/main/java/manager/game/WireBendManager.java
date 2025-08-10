package manager.game;

import model.levels.Level;
import model.wire.Wire;

public class WireBendManager {
    private final Level level;
    private static final int BEND_POINT_COST = 1;

    public WireBendManager(Level level) {
        this.level = level;
    }

    public boolean purchaseBendPoint(Wire wire) {
        if (!wire.canAddBendPoint()) {
            return false;
        }
        if (level.getCoins() < BEND_POINT_COST) {
            return false;
        }
        level.addCoins(-BEND_POINT_COST);
        service.AudioManager.playShopPurchase();
        return true;
    }

    public boolean refundBendPoint(Wire wire) {
        level.addCoins(BEND_POINT_COST);
        return true;
    }
}
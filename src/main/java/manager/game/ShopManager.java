package manager.game;

import model.logic.Shop.ShopItem;
import model.logic.Shop.OAtarItem;
import model.logic.Shop.OAiryamanItem;
import model.logic.Shop.OAnahitaItem;
import model.logic.Shop.Aergia;
import model.logic.Shop.Sisyphus;
import model.logic.Shop.Eliphas;
import model.levels.Level;
import java.util.ArrayList;
import java.util.List;

public class ShopManager {
    private final List<ShopItem> items = new ArrayList<>();
    private Level level;

    public ShopManager(Level level) {
        this.level = level;
        items.add(new OAtarItem());
        items.add(new OAiryamanItem());
        items.add(new OAnahitaItem());
        items.add(new Aergia());
        items.add(new Sisyphus());
        items.add(new Eliphas());
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public boolean canPurchase(ShopItem item) {
        return level.getCoins() >= item.getPrice();
    }

    public boolean purchase(ShopItem item) {
        if (!canPurchase(item)) {
            return false;
        }

        level.addCoins(-item.getPrice());

        item.apply(level);

        service.AudioManager.playShopPurchase();

        return true;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}

package manager.game;

import model.logic.Shop.ShopItem;
import model.logic.Shop.OAtarItem;
import model.logic.Shop.OAiryamanItem;
import model.logic.Shop.OAnahitaItem;
import model.logic.Shop.OAergiaItem;
import model.logic.Shop.OSisyphusItem;
import model.logic.Shop.OEliphasItem;
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
        items.add(new OAergiaItem());
        items.add(new OSisyphusItem());
        items.add(new OEliphasItem());
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public boolean canPurchase(ShopItem item) {
        return level.getCoins() >= item.getPrice();
    }

    public boolean purchase(ShopItem item) {
        System.out.println("DEBUG: ShopManager.purchase() - Attempting to buy " + item.getName());
        System.out.println("DEBUG: ShopManager.purchase() - Item price: " + item.getPrice() + ", Player coins: " + level.getCoins());
        
        if (!canPurchase(item)) {
            System.out.println("DEBUG: ShopManager.purchase() - Purchase FAILED - Insufficient funds");
            return false;
        }
        
        int oldCoins = level.getCoins();
        level.addCoins(-item.getPrice());
        int newCoins = level.getCoins();
        
        System.out.println("DEBUG: ShopManager.purchase() - Coins: " + oldCoins + " -> " + newCoins);
        System.out.println("DEBUG: ShopManager.purchase() - Applying item effect...");
        
        item.apply(level);
        
        System.out.println("DEBUG: ShopManager.purchase() - Purchase SUCCESS - " + item.getName() + " purchased");
        
        // Play shop purchase sound
        service.AudioManager.playShopPurchase();
        
        return true;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}

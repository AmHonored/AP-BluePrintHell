package model.logic.Shop;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import model.levels.Level;

public class OAtarItem implements ShopItem {
    @Override
    public String getName() { return "O' Atar"; }
    @Override
    public int getPrice() { return 3; }
    @Override
    public int getDurationSeconds() { return 10; }
    @Override
    public void apply(Level level) {
        System.out.println("DEBUG: OAtarItem.apply() - Disabling impact waves for " + getDurationSeconds() + " seconds");
        System.out.println("DEBUG: OAtarItem.apply() - Impact disabled before: " + level.isImpactDisabled());
        level.setImpactDisabled(true);
        System.out.println("DEBUG: OAtarItem.apply() - Impact disabled after: " + level.isImpactDisabled());
        
        PauseTransition delay = new PauseTransition(Duration.seconds(getDurationSeconds()));
        delay.setOnFinished(event -> {
            level.setImpactDisabled(false);
            System.out.println("DEBUG: OAtarItem - Impact waves re-enabled after " + getDurationSeconds() + " seconds");
            System.out.println("DEBUG: OAtarItem - Impact disabled now: " + level.isImpactDisabled());
        });
        delay.play();
    }
} 
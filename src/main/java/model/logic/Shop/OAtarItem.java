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
        level.setImpactDisabled(true);
        level.getLevelState().setImpactDisableEndNanos(java.lang.System.nanoTime() + getDurationSeconds() * 1_000_000_000L);
        
        PauseTransition delay = new PauseTransition(Duration.seconds(getDurationSeconds()));
        delay.setOnFinished(event -> {
            level.setImpactDisabled(false);
            level.getLevelState().setImpactDisableEndNanos(0L);
        });
        delay.play();
    }
} 
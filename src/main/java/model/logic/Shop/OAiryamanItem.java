package model.logic.Shop;

import javafx.animation.PauseTransition;
import javafx.util.Duration;
import model.levels.Level;

public class OAiryamanItem implements ShopItem {
    @Override
    public String getName() { return "O’ Airyaman"; }
    @Override
    public int getPrice() { return 4; }
    @Override
    public int getDurationSeconds() { return 5; }
    @Override
    public void apply(Level level) {
        level.setCollisionsDisabled(true);
        level.getLevelState().setCollisionsDisableEndNanos(java.lang.System.nanoTime() + getDurationSeconds() * 1_000_000_000L);
        
        PauseTransition delay = new PauseTransition(Duration.seconds(getDurationSeconds()));
        delay.setOnFinished(event -> {
            level.setCollisionsDisabled(false);
            level.getLevelState().setCollisionsDisableEndNanos(0L);
        });
        delay.play();
    }
} 
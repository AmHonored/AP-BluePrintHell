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
        System.out.println("DEBUG: OAiryamanItem.apply() - Disabling collisions for " + getDurationSeconds() + " seconds");
        System.out.println("DEBUG: OAiryamanItem.apply() - Collisions disabled before: " + level.isCollisionsDisabled());
        level.setCollisionsDisabled(true);
        System.out.println("DEBUG: OAiryamanItem.apply() - Collisions disabled after: " + level.isCollisionsDisabled());
        
        PauseTransition delay = new PauseTransition(Duration.seconds(getDurationSeconds()));
        delay.setOnFinished(event -> {
            level.setCollisionsDisabled(false);
            System.out.println("DEBUG: OAiryamanItem - Collisions re-enabled after " + getDurationSeconds() + " seconds");
            System.out.println("DEBUG: OAiryamanItem - Collisions disabled now: " + level.isCollisionsDisabled());
        });
        delay.play();
    }
} 
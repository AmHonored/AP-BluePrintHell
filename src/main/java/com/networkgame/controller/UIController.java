package com.networkgame.controller;

import com.networkgame.model.GameState;
import com.networkgame.service.GameCleanupService;
import com.networkgame.view.ShopScene;
import javafx.scene.Scene;

/**
 * Controller responsible for handling UI navigation.
 */
public class UIController {
    private final GameController mainController;
    private final GameCleanupService cleanupService;
    
    public UIController(GameController mainController) {
        this.mainController = mainController;
        this.cleanupService = new GameCleanupService();
    }
    
    /**
     * Returns to the main menu, stopping all game operations.
     */
    public void returnToMainMenu() {
        cleanupService.stopAllGameOperations(mainController);
        cleanupService.cleanupGameResources(mainController.getGameState());
        
        switchScene(mainController.getMainMenuScene().getScene());
    }
    
    /**
     * Shows the level select screen.
     */
    public void showLevelSelect() {
        switchScene(mainController.getLevelSelectScene().getScene());
    }
    
    /**
     * Shows the settings screen.
     */
    public void showSettings() {
        switchScene(mainController.getSettingsScene().getScene());
    }
    
    /**
     * Shows the shop screen and pauses the game.
     */
    public void showShop() {
        mainController.pauseGame();
        
        if (mainController.getShopScene() == null) {
            mainController.setShopScene(new ShopScene(mainController, mainController.getGameState()));
        }
        
        switchScene(mainController.getShopScene().getScene());
        mainController.setShopOpen(true);
    }
    
    /**
     * Hides the shop and returns to the game.
     */
    public void hideShop() {
        switchScene(mainController.getGameScene().getScene());
        mainController.setShopOpen(false);
        mainController.resumeGame();
    }
    
    /**
     * Helper method to switch scenes.
     * 
     * @param scene The scene to switch to
     */
    private void switchScene(Scene scene) {
        mainController.getPrimaryStage().setScene(scene);
    }
} 
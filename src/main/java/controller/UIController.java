package controller;

import view.game.GameScene;
import view.game.HUDScene;
import view.game.GameButtons;
import view.game.ShopScene;
import view.components.levels.LevelView;
import model.levels.Level;
import manager.game.ShopManager;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class UIController {
    private final GameScene gameScene;
    private final LevelView levelView;
    private final Level level;
    private final ShopManager shopManager;
    private boolean timeForwardPressed = false;
    private boolean timeBackwardPressed = false;

    public UIController(GameScene gameScene, Level level, ShopManager shopManager) {
        this.gameScene = gameScene;
        this.levelView = null;
        this.level = level;
        this.shopManager = shopManager;
        setupEventHandlers();
    }
    
    public UIController(LevelView levelView, Level level, ShopManager shopManager) {
        this.gameScene = null;
        this.levelView = levelView;
        this.level = level;
        this.shopManager = shopManager;
        setupEventHandlers();
    }

    /**
     * Setup all UI event handlers
     */
    private void setupEventHandlers() {
        setupGameButtons();
        setupKeyboardControls();
        setupHUDControls();
    }

    /**
     * Setup game button event handlers
     */
    private void setupGameButtons() {
        // Note: GameButtons are set up in GameScene constructor
        // This method will be called by GameController after setup
    }

    /**
     * Setup keyboard controls for time progression
     */
    private void setupKeyboardControls() {
        if (gameScene != null) {
            gameScene.setOnKeyPressed(this::handleKeyPressed);
            gameScene.setOnKeyReleased(this::handleKeyReleased);
            gameScene.setFocusTraversable(true);
        } else if (levelView != null) {
            levelView.setOnKeyPressed(this::handleKeyPressed);
            levelView.setOnKeyReleased(this::handleKeyReleased);
            levelView.setFocusTraversable(true);
        }
    }

    /**
     * Setup HUD interaction controls
     */
    private void setupHUDControls() {
        // Note: HUD controls will be set up by GameController
    }

    /**
     * Handle key press events
     */
    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
            case W:
                timeForwardPressed = true;
                handleTimeProgression();
                break;
            case DOWN:
            case S:
                timeBackwardPressed = true;
                handleTimeProgression();
                break;
            case SPACE:
                handlePauseButton();
                break;
            case ESCAPE:
                handleMenuButton();
                break;
        }
    }

    /**
     * Handle key release events
     */
    private void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
            case W:
                timeForwardPressed = false;
                break;
            case DOWN:
            case S:
                timeBackwardPressed = false;
                break;
        }
    }

    /**
     * Handle time progression based on key states
     */
    private void handleTimeProgression() {
        if (timeForwardPressed && !timeBackwardPressed) {
            // Move time forward
            level.setCurrentTime(level.getCurrentTime() + 1);
        } else if (timeBackwardPressed && !timeForwardPressed) {
            // Move time backward
            int newTime = Math.max(0, level.getCurrentTime() - 1);
            level.setCurrentTime(newTime);
        }
        // If both pressed, no change (as per requirements)
    }

    /**
     * Handle shop button click
     */
    private void handleShopButton() {
        if (!level.isPaused()) {
            level.setPaused(true);
            if (gameScene != null) {
                gameScene.openShop();
            } else if (levelView != null) {
                levelView.openShop();
            }
        }
    }

    /**
     * Handle pause button click
     */
    private void handlePauseButton() {
        level.setPaused(!level.isPaused());
        updatePauseButtonText();
    }

    /**
     * Handle menu button click
     */
    private void handleMenuButton() {
        // This will be handled by GameController for scene transition
    }



    /**
     * Update pause button text based on game state
     */
    private void updatePauseButtonText() {
        // Will be implemented by GameController
    }

    /**
     * Update HUD with current game state
     */
    public void updateHUD() {
        if (gameScene != null) {
            // GameScene doesn't have updateHUD, but we can get the HUD directly
            HUDScene hud = gameScene.getHUDScene();
            if (hud != null) {
                updateHUDStats(hud);
            }
        } else if (levelView != null) {
            levelView.updateHUD();
        }
        
        // Update capacity labels for intermediate systems
        updateIntermediateSystemCapacities();
    }
    
    /**
     * Update HUD stats with current level data
     */
    private void updateHUDStats(HUDScene hud) {
        // Update wire length
        hud.getWireBox().setValue(String.format("%.1f", level.getRemainingWireLength()));
        
        // Update packet loss percentage
        double lossPercentage = (level.getPacketsGenerated() == 0) ? 0.0 : 
            ((double) level.getPacketLoss() / level.getPacketsGenerated()) * 100.0;
        hud.getLossBox().setValue(String.format("%.1f%%", lossPercentage));
        
        // Update coins with debug logging
        int currentCoins = level.getCoins();
        String coinsText = String.valueOf(currentCoins);
        java.lang.System.out.println("DEBUG: UIController.updateHUDStats - Setting coins to: " + coinsText);
        hud.getCoinsBox().setValue(coinsText);
        
        // Update packets collected (actual count from end systems)
        int packetsCollected = level.getPacketsCollected();
        hud.getPacketsBox().setValue(String.valueOf(packetsCollected));
        java.lang.System.out.println("DEBUG: UIController.updateHUDStats - Updated HUD: coins=" + currentCoins + ", packets=" + packetsCollected + ", loss=" + String.format("%.1f%%", lossPercentage));
    }
    
    /**
     * Update capacity labels for all intermediate systems
     */
    private void updateIntermediateSystemCapacities() {
        // Get all systems and find IntermediateSystemViews to update
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof model.entity.systems.IntermediateSystem) {
                model.entity.systems.IntermediateSystem intermediateSystem = (model.entity.systems.IntermediateSystem) system;
                int currentCapacity = intermediateSystem.getStorageSize();
                
                // Find the corresponding view and update capacity
                // We need to access the view through the level view or game scene
                if (gameScene != null) {
                    updateCapacityInGameScene(system, currentCapacity);
                } else if (levelView != null) {
                    updateCapacityInLevelView(system, currentCapacity);
                }
            }
        }
    }
    
    /**
     * Update capacity in GameScene
     */
    private void updateCapacityInGameScene(model.entity.systems.System system, int currentCapacity) {
        // Find the system view in the game scene's game pane
        javafx.scene.layout.Pane gamePane = gameScene.getGamePane();
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.systems.IntermediateSystemView) {
                view.components.systems.IntermediateSystemView systemView = 
                    (view.components.systems.IntermediateSystemView) node;
                if (systemView.getSystem() == system) {
                    systemView.updateCapacity(currentCapacity);
                    break;
                }
            }
        }
    }
    
    /**
     * Update capacity in LevelView
     */
    private void updateCapacityInLevelView(model.entity.systems.System system, int currentCapacity) {
        // Find the system view in the level view's game pane
        javafx.scene.layout.Pane gamePane = levelView.getGamePane();
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.systems.IntermediateSystemView) {
                view.components.systems.IntermediateSystemView systemView = 
                    (view.components.systems.IntermediateSystemView) node;
                if (systemView.getSystem() == system) {
                    systemView.updateCapacity(currentCapacity);
                    break;
                }
            }
        }
    }
}

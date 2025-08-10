package view.components.levels;

import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import view.game.HUDScene;
import view.game.GameButtons;
import view.game.ShopScene;
import view.game.GameOverScene;
import view.game.LevelCompleteScene;
import model.levels.Level;
import manager.game.ShopManager;
import manager.game.VisualManager;

public abstract class LevelView extends StackPane {
    protected final Level level;
    protected final VisualManager visualManager;
    
    // Common UI components
    protected Pane gamePane;
    protected HUDScene hud;
    protected GameButtons controls;
    protected ShopScene shopOverlay;
    protected GameOverScene gameOverOverlay;
    protected LevelCompleteScene levelCompleteOverlay;
    
    // Managers
    protected ShopManager shopManager;

    public LevelView(Level level, VisualManager visualManager) {
        this.level = level;
        this.visualManager = visualManager;
        
        setupCommonUI();
        setupOverlays();
        // Event handlers will be set up by GameController
        
        // Initialize HUD with current values
        updateHUD();
    }

    /**
     * Setup common UI components (HUD, controls, game pane)
     */
    private void setupCommonUI() {
        BorderPane mainLayout = new BorderPane();

        // Center game area
        gamePane = new Pane();
        gamePane.setPrefSize(800, 500);
        gamePane.getStyleClass().add("game-pane");
        mainLayout.setCenter(gamePane);

        // Top HUD
        hud = new HUDScene(level);
        hud.getStyleClass().add("hud-pane");
        mainLayout.setTop(hud);
        // Enable/disable Aergia button based on connections and inventory
        updateAergiaButtonState();

        // Bottom controls
        controls = new GameButtons();
        controls.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controls);

        this.getChildren().add(mainLayout);
    }

    /**
     * Setup overlay components
     */
    private void setupOverlays() {
        // Shop overlay
        shopManager = new ShopManager(level);
        shopOverlay = new ShopScene(shopManager, level);
        // Update HUD Aergia button when items change (e.g., Aergia purchased)
        shopOverlay.setOnItemsChanged(() -> {
            if (hud != null) {
                // Update Aergia
                String aergiaText = "Aergia (" + level.getAergiaScrolls() + ")";
                if (level.isAergiaOnCooldown()) aergiaText += " \u23F3"; // hourglass
                hud.getAergiaButton().setText(aergiaText);
                boolean aergiaEnabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
                hud.getAergiaButton().setDisable(!aergiaEnabled);

                // Update Sisyphus
                String sisyphusText = "Sisyphus (" + level.getSisyphusScrolls() + ")";
                hud.getSisyphusButton().setText(sisyphusText);
                boolean sisyphusEnabled = level.getSisyphusScrolls() > 0; // no cooldown/conditions
                hud.getSisyphusButton().setDisable(!sisyphusEnabled);

                // Update Eliphas
                String eliphasText = "Eliphas (" + level.getEliphasScrolls() + ")";
                hud.getEliphasButton().setText(eliphasText);
                boolean eliphasEnabled = level.getEliphasScrolls() > 0; // no cooldown
                hud.getEliphasButton().setDisable(!eliphasEnabled);
            }
        });
        shopOverlay.setVisible(false);
        shopOverlay.getCloseButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            closeShop();
        });

        // Game over overlay
        gameOverOverlay = new GameOverScene();
        gameOverOverlay.setVisible(false);

        // Level complete overlay
        levelCompleteOverlay = new LevelCompleteScene();
        levelCompleteOverlay.setVisible(false);

        // Add overlays to stack
        this.getChildren().addAll(shopOverlay, gameOverOverlay, levelCompleteOverlay);
    }

    private boolean areAllPortsConnected() {
        for (model.entity.systems.System sys : level.getSystems()) {
            for (model.entity.ports.Port p : sys.getInPorts()) {
                if (!p.isConnected()) return false;
            }
            for (model.entity.ports.Port p : sys.getOutPorts()) {
                if (!p.isConnected()) return false;
            }
        }
        return true;
    }

    /**
     * Setup common event handlers (now handled by GameController)
     */
    private void setupEventHandlers() {
        // Event handlers are now set up by GameController
        // This method is kept for compatibility but does nothing
        
        // Setup mouse events for wire dragging on game pane
        setupWireDraggingEvents();
    }
    
    /**
     * Setup mouse events for wire dragging
     */
    private void setupWireDraggingEvents() {
        gamePane.setOnMouseDragged(event -> {
            // This will be handled by the WireController when it's set up
            // The GameController will set up the wire controller and connect these events
        });
        
        gamePane.setOnMouseReleased(event -> {
            // This will be handled by the WireController when it's set up
        });
    }

    /**
     * Open shop overlay
     */
    public void openShop() {
        // Always pause the level and show shop, regardless of current pause state
        if (!level.isPaused()) {
            level.setPaused(true);
        }
        shopOverlay.setVisible(true);
    }

    /**
     * Close shop overlay
     */
    protected void closeShop() {
        shopOverlay.setVisible(false);
        level.setPaused(false);
    }

    /**
     * Toggle pause state
     */
    protected void togglePause() {
        level.setPaused(!level.isPaused());
        updatePauseButtonText();
    }

    /**
     * Go to main menu
     */
    protected void goToMenu() {
        visualManager.showMenu();
    }



    /**
     * Update pause button text
     */
    protected void updatePauseButtonText() {
        if (level.isPaused()) {
            controls.getPauseButton().setText("Resume");
        } else {
            controls.getPauseButton().setText("Pause");
        }
    }

    /**
     * Show game over overlay
     */
    protected void showGameOver() {
        gameOverOverlay.setVisible(true);
        gameOverOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        gameOverOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
    }

    /**
     * Show level complete overlay
     */
    protected void showLevelComplete() {
        levelCompleteOverlay.setVisible(true);
        
        // Play level complete sound
        service.AudioManager.playLevelComplete();
        
        levelCompleteOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        levelCompleteOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
        levelCompleteOverlay.getNextLevelButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToNextLevel();
        });
    }

    /**
     * Restart current level
     */
    protected abstract void restartLevel();

    /**
     * Go to next level
     */
    protected abstract void goToNextLevel();

    /**
     * Update HUD with current game state
     */
    public void updateHUD() {
        // Update wire length
        hud.getWireBox().setValue(String.format("%.1f", level.getRemainingWireLength()));
        
        // Update packet loss percentage
        double lossPercentage = (level.getPacketsGenerated() == 0) ? 0.0 : 
            ((double) level.getPacketLoss() / level.getPacketsGenerated()) * 100.0;
        hud.getLossBox().setValue(String.format("%.1f%%", lossPercentage));
        
        // Update coins
        hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
        
        // Update packets collected (actual count from end systems)
        hud.getPacketsBox().setValue(String.valueOf(level.getPacketsCollected()));
        
        // Update temporal progress
        if (hud.getTemporalProgress() != null) {
            hud.getTemporalProgress().getProgressBar().setProgress(level.getCurrentTime() / 100.0); // Normalize to 0-1
            hud.getTemporalProgress().getTimeLabel().setText("Time: " + level.getCurrentTime());
        }
        updateAergiaButtonState();
    }

    private void updateAergiaButtonState() {
        String text = "Aergia (" + level.getAergiaScrolls() + ")";
        if (level.isAergiaOnCooldown()) text += " \u23F3";
        hud.getAergiaButton().setText(text);
        // Also enforce enabled/disabled here when running in LevelView mode
        boolean enabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
        hud.getAergiaButton().setDisable(!enabled);
        // Silent in production: no per-tick logging
    }

    /**
     * Get game pane for adding game elements
     */
    public Pane getGamePane() {
        return gamePane;
    }

    /**
     * Get HUD scene
     */
    public HUDScene getHUDScene() {
        return hud;
    }

    /**
     * Get game buttons
     */
    public GameButtons getGameButtons() {
        return controls;
    }

    /**
     * Get shop manager
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Get level
     */
    public Level getLevel() {
        return level;
    }
}

package view.game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
// import javafx.scene.control.Label; // kept for potential future HUD binding
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import model.levels.Level;
import controller.GameController;
// Same-package types don't need imports
import manager.game.VisualManager;
import controller.PacketController;
import manager.packets.PacketManager;
import manager.game.ShopManager;

public class GameScene extends StackPane {
    // private Scene scene; // reserved for future use
    private GameController gameController;
    private Level level;

    private Pane gamePane;
    private HUDScene hud;
    private GameButtons controls;
    private GameOverScene gameOverOverlay;
    private LevelCompleteScene levelCompleteOverlay;
    private ShopScene shopOverlay;
    private TemporalProgress temporalProgress;
    private Timeline levelTimer;
    private int levelDurationSeconds = 60; // Example: 60 seconds per level
    private int elapsedSeconds = 0;
    private boolean completedOrFailed = false;

    // Track overlays for future use
    private List<VBox> overlays = new ArrayList<>();

    private VisualManager visualManager;
    private PacketController packetController;
    private ShopManager shopManager;

    public GameScene(Level level, VisualManager visualManager) {
        this.level = level;
        this.visualManager = visualManager;

        // Main layout
        BorderPane mainLayout = new BorderPane();

        // Center game area
        gamePane = new Pane();
        gamePane.setPrefSize(800, 500);
        gamePane.getStyleClass().add("game-pane");
        mainLayout.setCenter(gamePane);

        // Initialize GameController and related components
        initializeGameController();

        // Add existing packets to the view (if any)
        for (model.entity.packets.Packet packet : level.getPackets()) {
            packetController.addPacket(packet);
        }

        // Set up ShopManager and ShopScene
        shopManager = new ShopManager(level);
        shopOverlay = new ShopScene(shopManager, level);
        shopOverlay.setOnItemsChanged(() -> {
            // Refresh Aergia, Sisyphus, and Eliphas HUD buttons immediately after purchase
            updateAergiaButtonText();
            updateSisyphusButtonText();
            updateEliphasButtonText();
            if (hud != null) {
                // Sisyphus: enable if any scrolls > 0
                boolean sisyphusEnabled = level.getSisyphusScrolls() > 0;
                hud.getSisyphusButton().setDisable(!sisyphusEnabled);
                // Eliphas: enable if any scrolls > 0
                boolean eliphasEnabled = level.getEliphasScrolls() > 0;
                hud.getEliphasButton().setDisable(!eliphasEnabled);
            }
        });
        shopOverlay.setVisible(false);
        shopOverlay.getCloseButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            closeShop();
        });

        // Top HUD
        hud = new HUDScene(level);
        hud.getStyleClass().add("hud-pane");
        mainLayout.setTop(hud);

        // Bottom controls (without Aergia; moved to HUD)
        controls = new GameButtons();
        controls.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controls);

        // Aergia button behavior is fully managed by GameController

        // Shop button will be set up by GameController
        
        // Set up event handlers after GameController is initialized
        setupEventHandlers();

        // Overlays (hidden by default)
        gameOverOverlay = new GameOverScene();
        gameOverOverlay.setVisible(false);
        levelCompleteOverlay = new LevelCompleteScene();
        levelCompleteOverlay.setVisible(false);
        // shopOverlay is now managed by ShopManager and ShopScene

        // Temporal progress bar (optional, can be added to HUD)
        temporalProgress = new TemporalProgress(level);

        // Add overlays to overlays list for easy management
        overlays.add(gameOverOverlay);
        overlays.add(levelCompleteOverlay);
        overlays.add(shopOverlay);

        // StackPane: main layout at bottom, overlays on top
        this.getChildren().addAll(mainLayout, gameOverOverlay, levelCompleteOverlay, shopOverlay);
        
        // Start background music for the game scene
        service.AudioManager.playBackgroundMusic();
        
        // Ensure event handlers are set up
        if (gameController != null) {
            gameController.setupEventHandlersManually();
        }

        // Reflect initial Aergia count on button
        updateAergiaButtonText();
        // Reflect initial Sisyphus count on button
        updateSisyphusButtonText();
        // Reflect initial Eliphas count on button
        updateEliphasButtonText();

        // Start the level timer
        startLevelTimer();
    }

    public void updateAergiaButtonText() {
        String text = "Aergia (" + level.getAergiaScrolls() + ")";
        if (level.isAergiaOnCooldown()) {
            text += " ⏳";
        }
        if (hud != null) {
            hud.getAergiaButton().setText(text);
            // Let GameController control enabled/disabled state; only update label here
        }
    }

    public void updateSisyphusButtonText() {
        if (hud != null) {
            String text = "Sisyphus (" + level.getSisyphusScrolls() + ")";
            hud.getSisyphusButton().setText(text);
        }
    }

    public void updateEliphasButtonText() {
        if (hud != null) {
            String text = "Eliphas (" + level.getEliphasScrolls() + ")";
            hud.getEliphasButton().setText(text);
        }
    }

    private void startLevelTimer() {
        elapsedSeconds = 0;
        completedOrFailed = false;
        updateHUDTime();
        if (levelTimer != null) levelTimer.stop();
        levelTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            elapsedSeconds++;
            updateHUDTime();
            if (elapsedSeconds >= levelDurationSeconds) {
                onLevelTimerEnd();
            }
        }));
        levelTimer.setCycleCount(levelDurationSeconds);
        levelTimer.play();
    }

    private void updateHUDTime() {
        int remaining = levelDurationSeconds - elapsedSeconds;
        int min = remaining / 60;
        int sec = remaining % 60;
        String timeStr = String.format("%02d:%02d", min, sec);
        // Update HUD and temporal progress bar
        if (hud != null) {
            // If HUD has a time label, update it
            // (Assume HUDScene has a method or access to time label)
        }
        if (temporalProgress != null) {
            temporalProgress.getTimeLabel().setText(timeStr);
            temporalProgress.getProgressBar().setProgress((double)elapsedSeconds / levelDurationSeconds);
        }
    }

    // Display a brief hint overlay guiding the player to click a wire
    public void showAergiaPlacementHint() {
        javafx.scene.control.Label hint = new javafx.scene.control.Label("Click on a wire to place the ❌ mark");
        hint.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-text-fill: #00d4ff; -fx-font-weight: bold; -fx-padding: 8 12; -fx-background-radius: 8; -fx-border-color: #00d4ff; -fx-border-radius: 8; -fx-border-width: 2;");
        hint.setMouseTransparent(true);
        StackPane.setAlignment(hint, javafx.geometry.Pos.TOP_CENTER);
        this.getChildren().add(hint);
        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2.5));
        delay.setOnFinished(ev -> this.getChildren().remove(hint));
        delay.play();
    }

    private void onLevelTimerEnd() {
        if (completedOrFailed) return;
        completedOrFailed = true;
        double lossRatio = (level.getPacketsGenerated() == 0) ? 0 : ((double)level.getPacketLoss() / level.getPacketsGenerated());
        if (lossRatio <= 0.5) {
            showLevelCompleteOverlay();
        } else {
            showGameOverOverlay();
        }
    }

    private void showLevelCompleteOverlay() {
        levelCompleteOverlay.setVisible(true);
        // Hide other overlays
        gameOverOverlay.setVisible(false);
        shopOverlay.setVisible(false);
        
        // Play level complete sound
        service.AudioManager.playLevelComplete();
        
        // Set up overlay buttons
        levelCompleteOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        levelCompleteOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
        // Show next level button always in config-driven flow
        levelCompleteOverlay.getNextLevelButton().setVisible(true);
        levelCompleteOverlay.getNextLevelButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToNextLevel();
        });
    }

    private void showGameOverOverlay() {
        gameOverOverlay.setVisible(true);
        // Hide other overlays
        levelCompleteOverlay.setVisible(false);
        shopOverlay.setVisible(false);
        // Set up overlay buttons
        gameOverOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        gameOverOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
    }

    private void restartLevel() {
        if (visualManager != null) {
            // Delegate restart to LevelManager in config-driven flow
            visualManager.showMenu();
        }
    }

    private void goToNextLevel() {
        if (visualManager != null) {
            // Delegate to LevelManager's next logic via menu path
            visualManager.showMenu();
        }
    }

    private void goToMenu() {
        if (visualManager != null) {
            visualManager.showMenu();
        }
    }

    public void openShop() {
        level.setPaused(true);
        shopOverlay.setVisible(true);
    }

    private void closeShop() {
        shopOverlay.setVisible(false);
        level.setPaused(false);
    }

    public Pane getGamePane() {
        return gamePane;
    }

    /**
     * Initialize GameController and related components
     */
    private void initializeGameController() {
        // Initialize GameController
        this.gameController = new GameController(level, this, visualManager);
        
        // Set up PacketController and PacketManager
        this.packetController = gameController.getPacketController();
        this.packetController.setPacketLayer(gamePane);
        PacketManager.setPacketController(packetController);
        PacketManager.setLevel(level);
        
        // Set up ShopManager
        this.shopManager = gameController.getShopManager();
        
        // Add existing packets to the view (if any)
        for (model.entity.packets.Packet packet : level.getPackets()) {
            packetController.addPacket(packet);
        }
    }

    /**
     * Start the game
     */
    public void startGame() {
        if (gameController != null) {
            gameController.startGame();
        }
    }

    /**
     * Get HUD scene for external access
     */
    public HUDScene getHUDScene() {
        return hud;
    }

    /**
     * Get GameButtons for external access
     */
    public GameButtons getGameButtons() {
        return controls;
    }

    /**
     * Get VisualManager for external access
     */
    public VisualManager getVisualManager() {
        return visualManager;
    }

    /**
     * Get GameController for external access
     */
    public GameController getGameController() {
        return gameController;
    }

    /**
     * Get ShopManager for external access
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Setup event handlers for UI components
     */
    private void setupEventHandlers() {
        if (gameController != null) {
            // The GameController will handle setting up its own event handlers
            // This method is called after GameController is initialized
        }
    }
}

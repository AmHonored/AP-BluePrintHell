package view.game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import java.util.ArrayList;
import java.util.List;
import model.levels.Level;
import controller.GameController;
import view.game.HUDScene;
import view.game.GameButtons;
import view.game.GameOverScene;
import view.game.LevelCompleteScene;
import view.game.ShopScene;
import view.game.TemporalProgress;
import manager.game.VisualManager;
import controller.PacketController;
import manager.packets.PacketManager;
import manager.game.ShopManager;

public class GameScene extends StackPane {
    private Scene scene;
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
        shopOverlay.setVisible(false);
        shopOverlay.getCloseButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            closeShop();
        });

        // Top HUD
        hud = new HUDScene(level);
        hud.getStyleClass().add("hud-pane");
        mainLayout.setTop(hud);

        // Bottom controls
        controls = new GameButtons();
        controls.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controls);

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

        // Start the level timer
        startLevelTimer();
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
        // Only show next level if this is Level 1
        boolean isLevel1 = level instanceof model.levels.Level1;
        levelCompleteOverlay.getNextLevelButton().setVisible(isLevel1);
        if (isLevel1) {
            levelCompleteOverlay.getNextLevelButton().setOnAction(e -> {
                service.AudioManager.playButtonClick();
                goToNextLevel();
            });
            // Unlock Level 2
            if (visualManager != null) visualManager.unlockLevel2();
        }
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
            int levelNum = (level instanceof model.levels.Level1) ? 1 : 2;
            visualManager.showGame(levelNum);
        }
    }

    private void goToNextLevel() {
        if (visualManager != null) {
            visualManager.showGame(2);
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

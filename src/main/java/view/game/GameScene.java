package view.game;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import model.levels.Level;
import controller.GameController;
import manager.game.VisualManager;
import controller.PacketController;
import manager.packets.PacketManager;
import manager.game.ShopManager;

public class GameScene extends StackPane {
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
    private int levelDurationSeconds = 60;
    private int elapsedSeconds = 0;
    private boolean completedOrFailed = false;

    private List<VBox> overlays = new ArrayList<>();

    private VisualManager visualManager;
    private PacketController packetController;
    private ShopManager shopManager;

    public GameScene(Level level, VisualManager visualManager) {
        this.level = level;
        this.visualManager = visualManager;

        BorderPane mainLayout = new BorderPane();

        gamePane = new Pane();
        gamePane.setPrefSize(800, 500);
        gamePane.getStyleClass().add("game-pane");
        mainLayout.setCenter(gamePane);

        initializeGameController();

        for (model.entity.packets.Packet packet : level.getPackets()) {
            packetController.addPacket(packet);
        }

        shopManager = new ShopManager(level);
        shopOverlay = new ShopScene(shopManager, level);
        shopOverlay.setOnItemsChanged(() -> {
            if (hud != null) {
                hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
            }
            updateAergiaButtonText();
            updateSisyphusButtonText();
            updateEliphasButtonText();
            if (hud != null) {
                boolean sisyphusEnabled = level.getSisyphusScrolls() > 0;
                hud.getSisyphusButton().setDisable(!sisyphusEnabled);
                boolean eliphasEnabled = level.getEliphasScrolls() > 0;
                hud.getEliphasButton().setDisable(!eliphasEnabled);
            }
        });
        shopOverlay.setVisible(false);
        shopOverlay.getCloseButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            closeShop();
        });

        hud = new HUDScene(level);
        hud.getStyleClass().add("hud-pane");
        mainLayout.setTop(hud);

        controls = new GameButtons();
        controls.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controls);

        setupEventHandlers();

        gameOverOverlay = new GameOverScene();
        gameOverOverlay.setVisible(false);
        levelCompleteOverlay = new LevelCompleteScene();
        levelCompleteOverlay.setVisible(false);

        temporalProgress = new TemporalProgress(level);

        overlays.add(gameOverOverlay);
        overlays.add(levelCompleteOverlay);
        overlays.add(shopOverlay);

        this.getChildren().addAll(mainLayout, gameOverOverlay, levelCompleteOverlay, shopOverlay);
        
        service.AudioManager.playBackgroundMusic();
        
        updateAergiaButtonText();
        updateSisyphusButtonText();
        updateEliphasButtonText();

        startLevelTimer();
    }

    public void updateAergiaButtonText() {
        String text = "Aergia (" + level.getAergiaScrolls() + ")";
        if (level.isAergiaOnCooldown()) {
            text += " ⏳";
        }
        if (hud != null) {
            hud.getAergiaButton().setText(text);
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
        if (hud != null) {
        }
        if (temporalProgress != null) {
            temporalProgress.getTimeLabel().setText(timeStr);
            temporalProgress.getProgressBar().setProgress((double)elapsedSeconds / levelDurationSeconds);
        }
    }

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
        gameOverOverlay.setVisible(false);
        shopOverlay.setVisible(false);
        
        service.AudioManager.playLevelComplete();
        
        levelCompleteOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            try {
                new service.SaveService().deleteSave("default", "level-" + getCurrentLevelNumber());
            } catch (Throwable ignored) {}
            restartLevel();
        });
        levelCompleteOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
        levelCompleteOverlay.getNextLevelButton().setVisible(true);
        levelCompleteOverlay.getNextLevelButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToNextLevel();
        });
    }

    public void endLevelByNetwork() {
        if (completedOrFailed) return;
        completedOrFailed = true;
        double lossRatio = (level.getPacketsGenerated() == 0) ? 0 : ((double) level.getPacketLoss() / level.getPacketsGenerated());
        if (lossRatio <= 0.5) {
            showLevelCompleteOverlay();
        } else {
            showGameOverOverlay();
        }
    }

    private void showGameOverOverlay() {
        gameOverOverlay.setVisible(true);
        levelCompleteOverlay.setVisible(false);
        shopOverlay.setVisible(false);
        
        service.AudioManager.cleanup();
        
        gameOverOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            try {
                new service.SaveService().deleteSave("default", "level-" + getCurrentLevelNumber());
            } catch (Throwable ignored) {}
            restartLevel();
        });
        gameOverOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
    }

    private void restartLevel() {
        if (visualManager != null) {
            try {
                manager.game.LevelManager lm = new manager.game.LevelManager(visualManager, visualManager.getPrimaryStage(), visualManager.getCssFile());
                lm.showLevel(getCurrentLevelNumber());
            } catch (Throwable t) {
                visualManager.showMenu();
            }
        }
    }

    private void goToNextLevel() {
        if (visualManager != null) {
            try {
                manager.game.LevelManager lm = new manager.game.LevelManager(visualManager, visualManager.getPrimaryStage(), visualManager.getCssFile());
                lm.showLevel(getCurrentLevelNumber() + 1);
            } catch (Throwable t) {
                visualManager.showMenu();
            }
        }
    }

    private void goToMenu() {
        if (visualManager != null) {
            visualManager.showMenu();
        }
    }

    private int getCurrentLevelNumber() {
        try {
            manager.game.LevelManager lm = new manager.game.LevelManager(visualManager, visualManager.getPrimaryStage(), visualManager.getCssFile());
            java.lang.reflect.Field f = lm.getClass().getDeclaredField("currentLevelNumber");
            f.setAccessible(true);
            Object v = f.get(lm);
            if (v instanceof Integer) return (Integer) v;
        } catch (Throwable ignored) {}
        return 1;
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

    private void initializeGameController() {
        this.gameController = new GameController(level, this, visualManager);
        
        this.packetController = gameController.getPacketController();
        this.packetController.setPacketLayer(gamePane);
        PacketManager.setPacketController(packetController);
        PacketManager.setLevel(level);
        
        this.shopManager = gameController.getShopManager();
        
        for (model.entity.packets.Packet packet : level.getPackets()) {
            packetController.addPacket(packet);
        }
    }

    public void startGame() {
        if (gameController != null) {
            gameController.startGame();
        }
    }

    public HUDScene getHUDScene() {
        return hud;
    }

    public GameButtons getGameButtons() {
        return controls;
    }

    public VisualManager getVisualManager() {
        return visualManager;
    }

    public GameController getGameController() {
        return gameController;
    }


    public ShopManager getShopManager() {
        return shopManager;
    }


    private void setupEventHandlers() {
        if (gameController != null) {
        }
    }
}

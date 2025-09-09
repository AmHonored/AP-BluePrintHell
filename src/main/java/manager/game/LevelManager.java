package manager.game;

import config.levels.LevelConfigLoader;
import config.levels.LevelDefinition;
import config.levels.LevelFactory;
import controller.GameController;
import service.SaveService;
import service.CrashAutosaveManager;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.levels.Level;
import view.components.levels.DataDrivenLevelView;
import view.components.levels.LevelView;

public class LevelManager {
    private final VisualManager visualManager;
    private final Stage primaryStage;
    private final String cssFile;
    private boolean level2Unlocked = true; // Unlocked by default for testing
    private GameController currentGameController;
    private final LevelConfigLoader configLoader = new LevelConfigLoader();
    private final LevelFactory levelFactory = new LevelFactory();
    private int currentLevelNumber = -1;
    private final SaveService saveService = new SaveService();
    private final CrashAutosaveManager crashAutosave = new CrashAutosaveManager();
    private Level currentLevel;

    public LevelManager(VisualManager visualManager, Stage primaryStage, String cssFile) {
        this.visualManager = visualManager;
        this.primaryStage = primaryStage;
        this.cssFile = cssFile;
    }

    /**
     * Create and show a specific level
     */
    public void showLevel(int levelNumber) {
        this.currentLevelNumber = levelNumber;
        String levelId = "level-" + levelNumber;

        // Resume prompt: if crash autosave exists, ask user
        crashAutosave.tryLoad(levelId).ifPresentOrElse(saved -> {
            boolean resume = showResumePrompt(saved);
            if (resume) {
                // Create fresh level then apply saved state fully
                Level level = createLevel(levelNumber);
                this.currentLevel = level;
                // Apply model state prior to view creation
                saveService.applyBasicToLevel(level, saved.level);
                saveService.applySystemPositions(level, saved.level);

                LevelView levelView = createLevelView(level, levelNumber);
                Scene scene = new Scene(levelView, 800, 600);
                scene.getStylesheets().add(cssFile);
                primaryStage.setScene(scene);

                currentGameController = new GameController(level, levelView, visualManager);

                // Restore wires/marks/packets after pane exists
                try {
                    javafx.scene.layout.Pane pane = levelView.getGamePane();
                    saveService.restoreWires(level, saved.level, pane);
                    saveService.restoreMarks(level, saved.level);
                    saveService.restorePackets(level, saved.level);
                    if (currentGameController != null) currentGameController.updateSystemIndicators();
                } catch (Throwable ignored) {}

                // Brief frozen preview (enhanced with countdown)
                showFrozenPreview(levelView, saved);

                // Begin gameplay and crash-autosave
                currentGameController.startGame();
                // Start crash-only autosave every 3 seconds
                crashAutosave.startAutosave(level, levelId, 3.0);
                // Delete temp file only after user explicitly rejects; keep while playing
            } else {
                // User rejected resume: delete autosave and start fresh
                crashAutosave.deleteAutosave(levelId);
                startFreshLevel(levelNumber, levelId);
            }
        }, () -> {
            // No autosave -> start fresh
            startFreshLevel(levelNumber, levelId);
        });
    }

    private void startFreshLevel(int levelNumber, String levelId) {
        Level level = createLevel(levelNumber);
        this.currentLevel = level;

        // Load basic saved state and system positions BEFORE creating the view
        saveService.tryLoadLevelSave("default", levelId)
            .ifPresent(save -> {
                saveService.applyBasicToLevel(level, save);
                saveService.applySystemPositions(level, save);
            });

        LevelView levelView = createLevelView(level, levelNumber);
        
        Scene scene = new Scene(levelView, 800, 600);
        scene.getStylesheets().add(cssFile);
        primaryStage.setScene(scene);
        
        // Initialize game controller for the level
        currentGameController = new GameController(level, levelView, visualManager);
        
        // Transition audio: stop menu music and start background
        service.AudioManager.stopMenuMusic();
        service.AudioManager.playBackgroundMusic();
        
        // Start crash-only autosave every 3 seconds
        crashAutosave.startAutosave(level, levelId, 3.0);

        // After scene is set and pane exists, restore wires visually
        saveService.tryLoadLevelSave("default", levelId)
            .ifPresent(save -> {
                try {
                    javafx.scene.layout.Pane pane = levelView.getGamePane();
                    saveService.restoreWires(level, save, pane);
                    saveService.restoreMarks(level, save);
                    saveService.restorePackets(level, save);
                    // Refresh indicators and enable play buttons based on restored connections
                    if (currentGameController != null) {
                        currentGameController.updateSystemIndicators();
                    }
                } catch (Throwable ignored) {}
            });

        currentGameController.startGame();
        // Notify server about run start if connected (online mode)
        try {
            if (net.NetworkService.getInstance().isConnected()) {
                net.NetworkService.getInstance().startRun(levelId);
            }
        } catch (Throwable ignored) {}
        // Notify server about run start if connected (online mode)
        try {
            if (net.NetworkService.getInstance().isConnected()) {
                net.NetworkService.getInstance().startRun(levelId);
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Create level model based on level number
     */
    private Level createLevel(int levelNumber) {
        ensureIndexLoaded();
        String levelId = "level-" + levelNumber;
        LevelDefinition def = configLoader.findLevelById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown level id: " + levelId));
        return levelFactory.createLevel(def);
    }

    /**
     * Expose current level code like "level-1" for networking/leaderboard.
     */
    public String getCurrentLevelId() {
        if (currentLevelNumber <= 0) return null;
        return "level-" + currentLevelNumber;
    }

    /**
     * Create level view based on level number
     */
    private LevelView createLevelView(Level level, int levelNumber) {
        ensureIndexLoaded();
        String levelId = "level-" + levelNumber;
        LevelDefinition def = configLoader.findLevelById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown level id: " + levelId));
        return new DataDrivenLevelView(level, visualManager, def);
    }

    /**
     * Restart current level
     */
    public void restartCurrentLevel() {
        if (currentGameController != null) {
            // Save before stopping and restarting
            if (currentLevelNumber > 0 && currentLevel != null) {
                saveService.saveNow(currentLevel, "default", "level-" + currentLevelNumber);
            }
            crashAutosave.stopAutosave();
            // Restart is an intentional action; clear crash autosave
            if (currentLevelNumber > 0) {
                crashAutosave.deleteAutosave("level-" + currentLevelNumber);
            }
            currentGameController.stopGame();
        }
        
        // Restart the same level number
        if (currentLevelNumber > 0) {
            showLevel(currentLevelNumber);
        }
    }

    /**
     * Go to next level
     */
    public void goToNextLevel() {
        if (currentGameController != null) {
            // Save before transitioning
            if (currentLevelNumber > 0 && currentLevel != null) {
                saveService.saveNow(currentLevel, "default", "level-" + currentLevelNumber);
            }
            crashAutosave.stopAutosave();
            // Intentional transition; clear crash autosave for prior level
            if (currentLevelNumber > 0) {
                crashAutosave.deleteAutosave("level-" + currentLevelNumber);
            }
            currentGameController.stopGame();
        }
        
        int next = currentLevelNumber + 1;
        if (next >= 1 && next <= 9) {
            showLevel(next);
        } else {
            visualManager.showMenu();
        }
    }

    private void ensureIndexLoaded() {
        // Load index once
        try {
            // Attempt to load only if not already loaded
            if (!configLoader.findLevelById("__probe__").isPresent()) {
                configLoader.loadIndex("levels/levels-index.json");
            }
        } catch (IllegalStateException e) {
            // levelIndex not loaded -> load it
            configLoader.loadIndex("levels/levels-index.json");
        }
    }

    /**
     * Unlock Level 2
     */
    public void unlockLevel2() {
        level2Unlocked = true;
        visualManager.unlockLevel2();
    }

    /**
     * Check if Level 2 is unlocked
     */
    public boolean isLevel2Unlocked() {
        return level2Unlocked;
    }

    /**
     * Get current game controller
     */
    public GameController getCurrentGameController() {
        return currentGameController;
    }

    /**
     * Stop current game
     */
    public void stopCurrentGame() {
        if (currentGameController != null) {
            if (currentLevelNumber > 0 && currentLevel != null) {
                saveService.saveNow(currentLevel, "default", "level-" + currentLevelNumber);
            }
            crashAutosave.stopAutosave();
            // Menu-driven exit or external stop: delete crash autosave
            if (currentLevelNumber > 0) {
                crashAutosave.deleteAutosave("level-" + currentLevelNumber);
            }
            currentGameController.stopGame();
            currentGameController = null;
        }
    }

    private boolean showResumePrompt(serialization.save.SaveGame saved) {
        // Styled confirmation dialog that matches game look; buttons are Yes/No
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Resume Interrupted Game");
        alert.setHeaderText(null);

        // Apply game stylesheet to dialog
        try { alert.getDialogPane().getStylesheets().add(cssFile); } catch (Throwable ignored) {}
        alert.getDialogPane().setStyle("-fx-background-color: #0f1319; -fx-text-fill: #e9edf1; -fx-padding: 16;");

        String when = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(saved.savedAtEpochMillis));
        String ago = humanizeMillis(System.currentTimeMillis() - saved.savedAtEpochMillis);

        javafx.scene.layout.VBox contentBox = new javafx.scene.layout.VBox(8);
        contentBox.setStyle("-fx-alignment: center-left;");

        javafx.scene.control.Label title = new javafx.scene.control.Label("Do you want to continue where you left off?");
        title.setStyle("-fx-text-fill: #e9edf1; -fx-font-size: 16px; -fx-font-weight: bold;");

        javafx.scene.control.Label meta = new javafx.scene.control.Label("Level: " + saved.levelId + "\nSaved at: " + when + " (" + ago + " ago)");
        meta.setStyle("-fx-text-fill: #9fb3c8; -fx-font-size: 12px;");

        javafx.scene.control.Label hint = new javafx.scene.control.Label("Yes: resume from last autosave\nNo: discard autosave and start fresh");
        hint.setStyle("-fx-text-fill: #8dd3ff; -fx-font-size: 12px;");

        contentBox.getChildren().addAll(title, meta, hint);
        alert.getDialogPane().setContent(contentBox);

        javafx.scene.control.ButtonType yesBtn = new javafx.scene.control.ButtonType("Yes", javafx.scene.control.ButtonBar.ButtonData.YES);
        javafx.scene.control.ButtonType noBtn = new javafx.scene.control.ButtonType("No", javafx.scene.control.ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesBtn, noBtn);

        // Subtle button styling to align with game accent
        try {
            alert.getDialogPane().lookupButton(yesBtn).setStyle("-fx-background-color: #00d4ff; -fx-text-fill: #0b0f14; -fx-font-weight: bold;");
            alert.getDialogPane().lookupButton(noBtn).setStyle("-fx-background-color: #2a3441; -fx-text-fill: #e9edf1;");
        } catch (Throwable ignored) {}

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == yesBtn;
    }

    private String humanizeMillis(long ms) {
        if (ms < 60_000) return (ms / 1000) + "s";
        if (ms < 3_600_000) return (ms / 60_000) + "m";
        if (ms < 86_400_000) return (ms / 3_600_000) + "h";
        return (ms / 86_400_000) + "d";
    }

    private void showFrozenPreview(view.components.levels.LevelView levelView, serialization.save.SaveGame saved) {
        // Enhanced frozen overlay with countdown and metadata; skippable
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Label title = new javafx.scene.control.Label("Restoring saved game...");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        String when = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(saved.savedAtEpochMillis));
        javafx.scene.control.Label meta = new javafx.scene.control.Label("Level: " + saved.levelId + "   •   Saved at: " + when);
        meta.setStyle("-fx-text-fill: #d5e0ea; -fx-font-size: 12px;");
        javafx.scene.control.Label countdown = new javafx.scene.control.Label();
        countdown.setStyle("-fx-text-fill: #c0ffee; -fx-font-size: 14px;");
        box.getChildren().addAll(title, meta, countdown);
        overlay.getChildren().add(box);
        ((javafx.scene.layout.StackPane) levelView).getChildren().add(overlay);

        final int[] remaining = new int[]{4}; // show more seconds
        countdown.setText("Starting in " + remaining[0] + "…  (click to skip)");
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                remaining[0]--;
                if (remaining[0] <= 0) {
                    ((javafx.scene.layout.StackPane) levelView).getChildren().remove(overlay);
                } else {
                    countdown.setText("Starting in " + remaining[0] + "…  (click to skip)");
                }
            })
        );
        tl.setCycleCount(4);
        tl.play();

        overlay.setOnMouseClicked(e -> {
            tl.stop();
            ((javafx.scene.layout.StackPane) levelView).getChildren().remove(overlay);
        });
    }
} 
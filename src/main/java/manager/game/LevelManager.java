package manager.game;

import config.levels.LevelConfigLoader;
import config.levels.LevelDefinition;
import config.levels.LevelFactory;
import controller.GameController;
import service.SaveService;
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
        
        // Start autosave every 2 seconds
        saveService.attachAutosave(level, levelId, 2.0);

        // After scene is set and pane exists, restore wires visually
        saveService.tryLoadLevelSave("default", levelId)
            .ifPresent(save -> {
                try {
                    javafx.scene.layout.Pane pane = levelView.getGamePane();
                    saveService.restoreWires(level, save, pane);
                    saveService.restorePackets(level, save);
                    // Refresh indicators and enable play buttons based on restored connections
                    if (currentGameController != null) {
                        currentGameController.updateSystemIndicators();
                    }
                } catch (Throwable ignored) {}
            });

        currentGameController.startGame();
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
            saveService.stopAutosave();
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
            saveService.stopAutosave();
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
            saveService.stopAutosave();
            currentGameController.stopGame();
            currentGameController = null;
        }
    }
} 
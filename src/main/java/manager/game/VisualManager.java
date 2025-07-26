package manager.game;

import javafx.scene.Scene;
import javafx.stage.Stage;
import view.menu.MenuScene;
import view.menu.LevelSelectScene;
import view.menu.SettingsScene;
import view.game.GameScene;
import model.levels.Level;
import controller.GameController;
import manager.game.LevelManager;

/**
 * VisualManager handles all scene and navigation logic for Blueprint Hell.
 * Follows SOLID and clean code principles. No business logic, just navigation/state.
 */
public class VisualManager {
    private final Stage primaryStage;
    private final String cssFile;
    private boolean level2Unlocked = true; // Unlocked by default for testing
    private double soundVolume = 100.0;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private LevelManager levelManager;

    public VisualManager(Stage primaryStage, String cssFile) {
        this.primaryStage = primaryStage;
        this.cssFile = cssFile;
        this.levelManager = new LevelManager(this, primaryStage, cssFile);
    }

    /**
     * Show the main menu scene.
     */
    public void showMenu() {
        MenuScene menuRoot = new MenuScene();
        Scene menuScene = new Scene(menuRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        menuScene.getStylesheets().add(cssFile);
        primaryStage.setScene(menuScene);
        
        // Start menu music
        service.AudioManager.playMenuMusic();

        menuRoot.getStartGameButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(1);
        });
        menuRoot.getLevelSelectButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showLevelSelect();
        });
        menuRoot.getSettingsButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showSettings();
        });
        menuRoot.getExitButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            primaryStage.close();
        });
    }

    /**
     * Show the level select scene.
     */
    public void showLevelSelect() {
        LevelSelectScene levelSelectRoot = new LevelSelectScene(level2Unlocked);
        Scene levelSelectScene = new Scene(levelSelectRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        levelSelectScene.getStylesheets().add(cssFile);
        primaryStage.setScene(levelSelectScene);

        levelSelectRoot.getLevel1Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(1);
        });
        levelSelectRoot.getLevel2Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            if (level2Unlocked) levelManager.showLevel(2);
        });
        levelSelectRoot.getLevel3Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(3);
        });
        levelSelectRoot.getBackButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showMenu();
        });
    }

    /**
     * Show the settings scene.
     */
    public void showSettings() {
        SettingsScene settingsRoot = new SettingsScene(soundVolume);
        Scene settingsScene = new Scene(settingsRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        settingsScene.getStylesheets().add(cssFile);
        primaryStage.setScene(settingsScene);

        settingsRoot.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            soundVolume = newVal.doubleValue();
            // Connect to AudioManager
            service.AudioManager.setVolume(soundVolume / 100.0);
        });
        settingsRoot.getBackButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showMenu();
        });
    }

    /**
     * Show the game scene for the given level number.
     * @deprecated Use levelManager.showLevel() instead
     */
    @Deprecated
    public void showGame(int levelNumber) {
        levelManager.showLevel(levelNumber);
    }

    /**
     * Unlock Level 2 (call this after Level 1 is completed).
     */
    public void unlockLevel2() {
        level2Unlocked = true;
    }

    /**
     * Get the current sound volume (0-100).
     */
    public double getSoundVolume() {
        return soundVolume;
    }
} 
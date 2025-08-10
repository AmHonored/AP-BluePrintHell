package manager.game;

import javafx.scene.Scene;
import javafx.stage.Stage;
import view.menu.MenuScene;
import view.menu.LevelSelectScene;
import view.menu.SettingsScene;

public class VisualManager {
    private final Stage primaryStage;
    private final String cssFile;
    private double soundVolume = 100.0;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private LevelManager levelManager;

    public VisualManager(Stage primaryStage, String cssFile) {
        this.primaryStage = primaryStage;
        this.cssFile = cssFile;
        this.levelManager = new LevelManager(this, primaryStage, cssFile);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public String getCssFile() {
        return cssFile;
    }

    public void showLevelView(view.components.levels.LevelView levelView) {
        Scene scene = new Scene(levelView, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(cssFile);
        primaryStage.setScene(scene);
    }

    public void showMenu() {

        if (levelManager != null) {
            levelManager.stopCurrentGame();
        }
        MenuScene menuRoot = new MenuScene();
        Scene menuScene = new Scene(menuRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        menuScene.getStylesheets().add(cssFile);
        primaryStage.setScene(menuScene);
        
        service.AudioManager.playMenuMusic();

        menuRoot.getStartGameButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showResumeOrFirstLevel();
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
            if (levelManager != null) {
                levelManager.stopCurrentGame();
            }
            primaryStage.close();
        });
    }

    public void showLevelSelect() {
        LevelSelectScene levelSelectRoot = new LevelSelectScene();
        Scene levelSelectScene = new Scene(levelSelectRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        levelSelectScene.getStylesheets().add(cssFile);
        primaryStage.setScene(levelSelectScene);

        levelSelectRoot.getLevel1Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(1);
        });
        levelSelectRoot.getLevel2Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(2);
        });
        levelSelectRoot.getLevel3Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(3);
        });
        levelSelectRoot.getLevel4Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(4);
        });
        levelSelectRoot.getLevel5Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(5);
        });
        levelSelectRoot.getLevel6Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(6);
        });
        levelSelectRoot.getDistributeAndMergeButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(7);
        });
        levelSelectRoot.getBackButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showMenu();
        });
    }

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
}
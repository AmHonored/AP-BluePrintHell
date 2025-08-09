package manager.game;

import model.levels.Level;
import model.levels.Level1;
import model.levels.Level2;
import model.levels.Level3;
import model.levels.Level4;
import model.levels.Level5;
import model.levels.Level6;
import model.levels.Level7;
import model.levels.Level9;
import view.components.levels.LevelView;
import view.components.levels.Level1View;
import view.components.levels.Level2View;
import view.components.levels.Level3View;
import view.components.levels.Level4View;
import view.components.levels.Level5View;
import view.components.levels.Level6View;
import view.components.levels.Level7View;
import view.components.levels.Level9View;
import controller.GameController;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LevelManager {
    private final VisualManager visualManager;
    private final Stage primaryStage;
    private final String cssFile;
    private boolean level2Unlocked = true; // Unlocked by default for testing
    private GameController currentGameController;

    public LevelManager(VisualManager visualManager, Stage primaryStage, String cssFile) {
        this.visualManager = visualManager;
        this.primaryStage = primaryStage;
        this.cssFile = cssFile;
    }

    /**
     * Create and show a specific level
     */
    public void showLevel(int levelNumber) {
        Level level = createLevel(levelNumber);
        LevelView levelView = createLevelView(level, levelNumber);
        
        Scene scene = new Scene(levelView, 800, 600);
        scene.getStylesheets().add(cssFile);
        primaryStage.setScene(scene);
        
        // Initialize game controller for the level
        currentGameController = new GameController(level, levelView, visualManager);
        
        // Start background music for the level
        service.AudioManager.playBackgroundMusic();
        
        currentGameController.startGame();
    }

    /**
     * Create level model based on level number
     */
    private Level createLevel(int levelNumber) {
        switch (levelNumber) {
            case 1:
                return new Level1();
            case 2:
                return new Level2();
            case 3:
                return new Level3();
            case 4:
                return new Level4();
            case 5:
                return new Level5();
            case 6:
                return new Level6();
            case 7:
                return new Level7();
            case 8:
                return new model.levels.Level8();
            case 9:
                return new Level9();
            default:
                throw new IllegalArgumentException("Unknown level number: " + levelNumber);
        }
    }

    /**
     * Create level view based on level number
     */
    private LevelView createLevelView(Level level, int levelNumber) {
        switch (levelNumber) {
            case 1:
                return new Level1View(level, visualManager);
            case 2:
                if (!level2Unlocked) {
                    throw new IllegalStateException("Level 2 is not unlocked yet");
                }
                return new Level2View(level, visualManager);
            case 3:
                return new Level3View(level, visualManager);
            case 4:
                return new Level4View(level, visualManager);
            case 5:
                return new Level5View(level, visualManager);
            case 6:
                return new Level6View(level, visualManager);
            case 7:
                return new Level7View(level, visualManager);
            case 8:
                return new view.components.levels.Level8View(level, visualManager);
            case 9:
                return new Level9View(level, visualManager);
            default:
                throw new IllegalArgumentException("Unknown level number: " + levelNumber);
        }
    }

    /**
     * Restart current level
     */
    public void restartCurrentLevel() {
        if (currentGameController != null) {
            currentGameController.stopGame();
        }
        
        // Get current level number and restart
        Level currentLevel = currentGameController.getLevel();
        if (currentLevel instanceof Level1) {
            showLevel(1);
        } else if (currentLevel instanceof Level2) {
            showLevel(2);
        } else if (currentLevel instanceof Level3) {
            showLevel(3);
        } else if (currentLevel instanceof Level4) {
            showLevel(4);
        } else if (currentLevel instanceof Level5) {
            showLevel(5);
        }
    }

    /**
     * Go to next level
     */
    public void goToNextLevel() {
        if (currentGameController != null) {
            currentGameController.stopGame();
        }
        
        Level currentLevel = currentGameController.getLevel();
        if (currentLevel instanceof Level1) {
            unlockLevel2();
            showLevel(2);
        } else if (currentLevel instanceof Level2) {
            unlockLevel2();
            showLevel(3);
        } else if (currentLevel instanceof Level3) {
            showLevel(4);
        } else {
            // Level 4 completed, go back to menu
            visualManager.showMenu();
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
            currentGameController.stopGame();
            currentGameController = null;
        }
    }
} 
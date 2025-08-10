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

    public void showLevel(int levelNumber) {
        this.currentLevelNumber = levelNumber;
        String levelId = "level-" + levelNumber;

        crashAutosave.tryLoad(levelId).ifPresentOrElse(saved -> {
            boolean resume = view.components.levels.LevelResumeDialog.show(cssFile, saved);
            if (resume) {
                Level level = createLevel(levelNumber);
                this.currentLevel = level;
                saveService.applyBasicToLevel(level, saved.level);
                saveService.applySystemPositions(level, saved.level);

                LevelView levelView = createLevelView(level, levelNumber);
                Scene scene = new Scene(levelView, 800, 600);
                scene.getStylesheets().add(cssFile);
                primaryStage.setScene(scene);

                currentGameController = new GameController(level, levelView, visualManager);

                try {
                    javafx.scene.layout.Pane pane = levelView.getGamePane();
                    saveService.restoreWires(level, saved.level, pane);
                    saveService.restoreMarks(level, saved.level);
                    saveService.restorePackets(level, saved.level);
                    if (currentGameController != null) {
                        currentGameController.updateSystemIndicators();
                        currentGameController.getWireController().setupExistingWireBendCallbacks();
                        levelView.updateHUD();
                    }
                } catch (Throwable ignored) {}

                try { levelView.gameRestoringScene(saved); } catch (Throwable ignored) {}

                currentGameController.startGame();
                crashAutosave.startAutosave(level, levelId, 3.0);
            } else {
                crashAutosave.deleteAutosave(levelId);
                startFreshLevel(levelNumber, levelId);
            }
        }, () -> {
            startFreshLevel(levelNumber, levelId);
        });
    }

    public void showResumeOrFirstLevel() {
        ensureIndexLoaded();
        int candidate = -1;
        try {
            config.levels.LevelIndex index = configLoader.loadIndex("levels/levels-index.json");
            java.util.List<config.levels.LevelIndex.Entry> levels = index.getLevels();

            for (config.levels.LevelIndex.Entry e : levels) {
                String id = e.getId();
                if (id == null || !id.startsWith("level-")) continue;
                int n;
                try { n = Integer.parseInt(id.substring(6)); } catch (NumberFormatException ex) { continue; }
                java.util.Optional<serialization.save.LevelSave> save = saveService.tryLoadLevelSave("default", id);
                if (save.isPresent()) {
                    boolean completed = false;
                    if (save.get().gameState != null) completed = save.get().gameState.levelCompleted;
                    if (!completed) { candidate = n; break; }
                } else {
                    candidate = n; break;
                }
            }
        } catch (Throwable ignored) {}
        if (candidate == -1) candidate = 1;
        showLevel(candidate);
    }

    private void startFreshLevel(int levelNumber, String levelId) {
        Level level = createLevel(levelNumber);
        this.currentLevel = level;

        saveService.tryLoadLevelSave("default", levelId)
            .ifPresent(save -> {
                saveService.applyBasicToLevel(level, save);
                saveService.applySystemPositions(level, save);
            });

        LevelView levelView = createLevelView(level, levelNumber);
        
        Scene scene = new Scene(levelView, 800, 600);
        scene.getStylesheets().add(cssFile);
        primaryStage.setScene(scene);
        
        currentGameController = new GameController(level, levelView, visualManager);
        
        if (level.isLevelCompleted()) {
            try {
                java.lang.reflect.Method m = levelView.getClass().getMethod("endLevelByNetwork");
                m.invoke(levelView);
            } catch (Throwable ignored) {}
            return;
        }

        service.AudioManager.stopMenuMusic();
        service.AudioManager.playBackgroundMusic();
        
        crashAutosave.startAutosave(level, levelId, 3.0);

        saveService.tryLoadLevelSave("default", levelId)
            .ifPresent(save -> {
                try {
                    javafx.scene.layout.Pane pane = levelView.getGamePane();
                    saveService.restoreWires(level, save, pane);
                    saveService.restoreMarks(level, save);
                    saveService.restorePackets(level, save);

                    if (currentGameController != null) {
                        currentGameController.updateSystemIndicators();
                        currentGameController.getWireController().setupExistingWireBendCallbacks();
                        levelView.updateHUD();
                    }
                } catch (Throwable ignored) {}
            });

        currentGameController.startGame();
    }

    private Level createLevel(int levelNumber) {
        ensureIndexLoaded();
        String levelId = "level-" + levelNumber;
        LevelDefinition def = configLoader.findLevelById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown level id: " + levelId));
        return levelFactory.createLevel(def);
    }

    private LevelView createLevelView(Level level, int levelNumber) {
        ensureIndexLoaded();
        String levelId = "level-" + levelNumber;
        LevelDefinition def = configLoader.findLevelById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown level id: " + levelId));
        return new DataDrivenLevelView(level, visualManager, def);
    }

    public void restartCurrentLevel() {
        if (currentGameController != null) {
            if (currentLevelNumber > 0 && currentLevel != null) {
                saveService.saveNow(currentLevel, "default", "level-" + currentLevelNumber);
            }
            crashAutosave.stopAutosave();
            if (currentLevelNumber > 0) {
                crashAutosave.deleteAutosave("level-" + currentLevelNumber);
            }
            currentGameController.stopGame();
        }
        
        if (currentLevelNumber > 0) {
            showLevel(currentLevelNumber);
        }
    }

    public void goToNextLevel() {
        if (currentGameController != null) {
            if (currentLevelNumber > 0 && currentLevel != null) {
                saveService.saveNow(currentLevel, "default", "level-" + currentLevelNumber);
            }
            crashAutosave.stopAutosave();
            if (currentLevelNumber > 0) {
                crashAutosave.deleteAutosave("level-" + currentLevelNumber);
            }
            currentGameController.stopGame();
        }
        
        ensureIndexLoaded();
        int target = -1;
        try {
            config.levels.LevelIndex index = configLoader.loadIndex("levels/levels-index.json");
            for (config.levels.LevelIndex.Entry e : index.getLevels()) {
                String id = e.getId();
                if (id != null && id.startsWith("level-")) {
                    try {
                        int n = Integer.parseInt(id.substring(6));
                        if (n > currentLevelNumber && (target == -1 || n < target)) {
                            target = n;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Throwable ignored) {}

        if (target != -1) {
            showLevel(target);
        } else {
            visualManager.showMenu();
        }
    }

    private void ensureIndexLoaded() {
        try {
            if (!configLoader.findLevelById("__probe__").isPresent()) {
                configLoader.loadIndex("levels/levels-index.json");
            }
        } catch (IllegalStateException e) {
            configLoader.loadIndex("levels/levels-index.json");
        }
    }


    public GameController getCurrentGameController() {
        return currentGameController;
    }

    public void stopCurrentGame() {
        if (currentGameController != null) {
            if (currentLevelNumber > 0 && currentLevel != null) {
                saveService.saveNow(currentLevel, "default", "level-" + currentLevelNumber);
            }
            crashAutosave.stopAutosave();
            if (currentLevelNumber > 0) {
                crashAutosave.deleteAutosave("level-" + currentLevelNumber);
            }
            currentGameController.stopGame();
            currentGameController = null;
        }
    }
} 
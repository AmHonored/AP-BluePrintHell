package controller;

import view.game.GameScene;
import view.game.HUDScene;
import view.components.levels.LevelView;
import model.levels.Level;
import javafx.scene.input.KeyEvent;

public class UIController {
    private final GameScene gameScene;
    private final LevelView levelView;
    private final Level level;
    private boolean timeForwardPressed = false;
    private boolean timeBackwardPressed = false;

    public UIController(GameScene gameScene, Level level) {
        this.gameScene = gameScene;
        this.levelView = null;
        this.level = level;
        setupEventHandlers();
    }
    
    public UIController(LevelView levelView, Level level) {
        this.gameScene = null;
        this.levelView = levelView;
        this.level = level;
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        setupKeyboardControls();
    }

    private void setupKeyboardControls() {
        if (gameScene != null) {
            gameScene.setOnKeyPressed(this::handleKeyPressed);
            gameScene.setOnKeyReleased(this::handleKeyReleased);
            gameScene.setFocusTraversable(true);
        } else if (levelView != null) {
            levelView.setOnKeyPressed(this::handleKeyPressed);
            levelView.setOnKeyReleased(this::handleKeyReleased);
            levelView.setFocusTraversable(true);
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
            case W:
                timeForwardPressed = true;
                handleTimeProgression();
                break;
            case DOWN:
            case S:
                timeBackwardPressed = true;
                handleTimeProgression();
                break;
            case SPACE:
                handlePauseButton();
                break;
            case ESCAPE:
                handleMenuButton();
                break;
            default:
                break;
        }
    }

    private void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case UP:
            case W:
                timeForwardPressed = false;
                break;
            case DOWN:
            case S:
                timeBackwardPressed = false;
                break;
            default:
                break;
        }
    }

    private void handleTimeProgression() {
        if (timeForwardPressed && !timeBackwardPressed) {
            level.setCurrentTime(level.getCurrentTime() + 1);
        } 
        else if (timeBackwardPressed && !timeForwardPressed) {
            int newTime = Math.max(0, level.getCurrentTime() - 1);
            level.setCurrentTime(newTime);
        }
    }

    private void handlePauseButton() {
        level.setPaused(!level.isPaused());
        updatePauseButtonText();
    }

    private void handleMenuButton() {}
    
    private void updatePauseButtonText() {}

    public void updateHUD() {
        if (gameScene != null) {
            HUDScene hud = gameScene.getHUDScene();
            if (hud != null) {
                updateHUDStats(hud);
                String text = "Aergia (" + level.getAergiaScrolls() + ")";
                if (level.isAergiaOnCooldown()) text += " \u23F3";
                hud.getAergiaButton().setText(text);
                boolean enabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
                hud.getAergiaButton().setDisable(!enabled);
                model.logic.Shop.Aergia.pruneExpiredMarks(level);
                model.logic.Shop.Eliphas.pruneExpiredMarks(level);
            }
        } else if (levelView != null) {
            levelView.updateHUD();
        }
        
        updateSystemCapacities();
    }
    
    private void updateHUDStats(HUDScene hud) {
        hud.getWireBox().setValue(String.format("%.1f", level.getRemainingWireLength()));
        
        double lossPercentage = (level.getPacketsGenerated() == 0) ? 0.0 : 
            ((double) level.getPacketLoss() / level.getPacketsGenerated()) * 100.0;
        hud.getLossBox().setValue(String.format("%.1f%%", lossPercentage));
        
        int currentCoins = level.getCoins();
        String coinsText = String.valueOf(currentCoins);
        hud.getCoinsBox().setValue(coinsText);
        
        int packetsCollected = level.getPacketsCollected();
        hud.getPacketsBox().setValue(String.valueOf(packetsCollected));
        
        String sisyphusText = "Sisyphus (" + level.getSisyphusScrolls() + ")";
        hud.getSisyphusButton().setText(sisyphusText);
        
    }

    private void updateSystemCapacities() {
        for (model.entity.systems.System system : level.getSystems()) {
            int currentCapacity = -1;
            if (system instanceof model.entity.systems.IntermediateSystem) {
                currentCapacity = ((model.entity.systems.IntermediateSystem) system).getStorageSize();
            } else if (system instanceof model.entity.systems.AntiVirusSystem) {
                currentCapacity = ((model.entity.systems.AntiVirusSystem) system).getStorageSize();
            } else if (system instanceof model.entity.systems.DDosSystem) {
                currentCapacity = ((model.entity.systems.DDosSystem) system).getStorageSize();
            } else if (system instanceof model.entity.systems.SpySystem) {
                currentCapacity = ((model.entity.systems.SpySystem) system).getStorageSize();
            } else if (system instanceof model.entity.systems.VPNSystem) {
                currentCapacity = ((model.entity.systems.VPNSystem) system).getStorageSize();
            }

            if (currentCapacity >= 0) {
                if (gameScene != null) {
                    updateCapacityInGameScene(system, currentCapacity);
                } else if (levelView != null) {
                    updateCapacityInLevelView(system, currentCapacity);
                }
            }
        }
    }
    
    private void updateCapacityInGameScene(model.entity.systems.System system, int currentCapacity) {

        javafx.scene.layout.Pane gamePane = gameScene.getGamePane();
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.systems.IntermediateSystemView && ((view.components.systems.IntermediateSystemView) node).getSystem() == system) {
                ((view.components.systems.IntermediateSystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.AntiVirusSystemView && ((view.components.systems.AntiVirusSystemView) node).getSystem() == system) {
                ((view.components.systems.AntiVirusSystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.DDosSystemView && ((view.components.systems.DDosSystemView) node).getSystem() == system) {
                ((view.components.systems.DDosSystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.SpySystemView && ((view.components.systems.SpySystemView) node).getSystem() == system) {
                ((view.components.systems.SpySystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.VPNSystemView && ((view.components.systems.VPNSystemView) node).getSystem() == system) {
                ((view.components.systems.VPNSystemView) node).updateCapacity(currentCapacity);
                break;
            }
        }
    }
    
    private void updateCapacityInLevelView(model.entity.systems.System system, int currentCapacity) {

        javafx.scene.layout.Pane gamePane = levelView.getGamePane();
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.systems.IntermediateSystemView && ((view.components.systems.IntermediateSystemView) node).getSystem() == system) {
                ((view.components.systems.IntermediateSystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.AntiVirusSystemView && ((view.components.systems.AntiVirusSystemView) node).getSystem() == system) {
                ((view.components.systems.AntiVirusSystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.DDosSystemView && ((view.components.systems.DDosSystemView) node).getSystem() == system) {
                ((view.components.systems.DDosSystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.SpySystemView && ((view.components.systems.SpySystemView) node).getSystem() == system) {
                ((view.components.systems.SpySystemView) node).updateCapacity(currentCapacity);
                break;
            } else if (node instanceof view.components.systems.VPNSystemView && ((view.components.systems.VPNSystemView) node).getSystem() == system) {
                ((view.components.systems.VPNSystemView) node).updateCapacity(currentCapacity);
                break;
            }
        }
    }
}

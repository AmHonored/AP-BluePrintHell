package controller;

import model.levels.Level;
import view.game.GameScene;
import view.game.HUDScene;
import view.game.GameButtons;
import view.components.levels.LevelView;
import manager.game.VisualManager;
import manager.game.MovementManager;
import manager.game.ConnectionManager;
import manager.game.ShopManager;
import manager.packets.PacketManager;
import javafx.animation.AnimationTimer;

public class GameController {
    private final Level level;
    private final GameScene gameScene;
    private final LevelView levelView;
    private final VisualManager visualManager;
    
    private final SystemController systemController;
    private final AbilityController abilityController;
    private final UIController uiController;
    private final PacketController packetController;
    private final CollisionController collisionController;
    private final WireController wireController;
    
    private final MovementManager movementManager; 
    private final ConnectionManager connectionManager; 
    private final ShopManager shopManager;
    
    private AnimationTimer gameLoop;
    private boolean isRunning = false;

    public GameController(Level level, Object gameView, VisualManager visualManager) {
        this.level = level;
        this.visualManager = visualManager;
        
        if (gameView instanceof view.game.GameScene) {
            this.gameScene = (view.game.GameScene) gameView;
            this.levelView = null;
        } else if (gameView instanceof LevelView) {
            this.gameScene = null;
            this.levelView = (LevelView) gameView;
        } else {
            this.gameScene = null;
            this.levelView = null;
        }
        
        this.connectionManager = new ConnectionManager(level, level.getWireLength());
        this.shopManager = new ShopManager(level);
        this.movementManager = new MovementManager();
        
        this.packetController = new PacketController();
        this.packetController.setLevel(level);
        
        if (gameScene != null) {
            this.packetController.setPacketLayer(gameScene.getGamePane());
        } else if (levelView != null) {
            this.packetController.setPacketLayer(levelView.getGamePane());
        }
        
        this.systemController = new SystemController(level, packetController);
        if (gameScene != null) {
            this.uiController = new UIController(gameScene, level);
        } else {
            this.uiController = new UIController(levelView, level);
        }
        this.collisionController = new CollisionController(level, packetController);
        this.wireController = new WireController();
        this.abilityController = new AbilityController(level, (gameScene != null ? gameScene : levelView), systemController, connectionManager);
        
        setupControllers();
        setupGameLoop();
        setupSystemTimers();
        setupEventHandlers();
        
        setupPortViews();
        
        setupStartSystemPlayButtons();
    }

    private void setupControllers() {

        PacketManager.setLevel(level);
        PacketManager.setPacketController(packetController);
        
        wireController.setLevel(level);
        if (gameScene != null) {
            wireController.setGameScene(gameScene);
        } else if (levelView != null) {
            wireController.setGamePane(levelView.getGamePane());
        }
        wireController.setConnectionManager(connectionManager);
        
        HUDScene hud = getHUDScene();
        if (hud != null) {
            wireController.setHUD(hud);
        }
        
        wireController.setConnectionChangeCallback(() -> {
            updateSystemIndicators();
            if (levelView != null) {
                try {
                    java.lang.reflect.Method method = levelView.getClass().getMethod("setupStartSystemPlayButtons", GameController.class);
                    method.invoke(levelView, this);
                } catch (Exception ignored) {}
            }
        });
        
        try {
            manager.systems.VPNSystemManager.setVisualUpdater(() -> {
                try {
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.layout.Pane pane = null;
                        if (gameScene != null) {
                            pane = gameScene.getGamePane();
                        } else if (levelView != null) {
                            pane = levelView.getGamePane();
                        }
                        if (pane == null) return;
                        int updated = 0;
                        for (javafx.scene.Node node : pane.getChildren()) {
                            if (node instanceof view.components.systems.VPNSystemView) {
                                ((view.components.systems.VPNSystemView) node).updateVPNVisuals();
                                updated++;
                            }
                        }
                        try { System.out.println("[VPN] Visual updater ran, updated " + updated + " VPN views"); } catch (Throwable ignored) {}
                    });
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
    }

    public void updateSystemIndicators() {
        if (levelView != null) {
            try {
                java.lang.reflect.Method method = levelView.getClass().getMethod("updateSystemIndicators");
                method.invoke(levelView);
            } catch (Exception e) {}
        }
    }

    private void setupStartSystemPlayButtons() {
        if (levelView != null) {
            try {
                java.lang.reflect.Method method = levelView.getClass().getMethod("setupStartSystemPlayButtons", GameController.class);
                method.invoke(levelView, this);
            } catch (Exception e) {}
        }
    }

    private void setupPortViews() {
        if (levelView != null) {
            try {
                java.lang.reflect.Method method = levelView.getClass().getMethod("setupWireControllerForPorts", WireController.class);
                method.invoke(levelView, wireController);
            } catch (Exception e) {}
            
            wireController.setupDragHandlers(levelView.getGamePane());
            abilityController.setupAergiaPlacementHandler();
            abilityController.setupSisyphusMovementHandler();
            abilityController.setupEliphasPlacementHandler();
        }
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!level.isPaused()) {
                    movementManager.handle(now); 

                    collisionController.runCollisionCheck(); 
                    
                    uiController.updateHUD();
                    
            updateAergiaMarkPositions();
            updateEliphasMarkPositions();
                    
                    checkGameOver();
                    isLevelCompleted();
                }
            }
        };
    }


    private void setupSystemTimers() { 
        systemController.initTimers();
        systemController.startTimers();
    }

    private void setupEventHandlers() {
        GameButtons buttons = getGameButtons();
        if (buttons == null) return;
        setupCoreButtons(buttons);
        HUDScene hud = getHUDScene();
        if (hud == null) return;
        manager.game.HUDManager hudManager = new manager.game.HUDManager();
        hudManager.wireAergiaButton(hud, level, gameScene, abilityController, connectionManager);
        hudManager.wireSisyphusButton(hud, level, abilityController);
        hudManager.wireEliphasButton(hud, level, abilityController);
    }

    private void setupCoreButtons(GameButtons buttons) {
        buttons.getShopButton().setOnAction(e -> { service.AudioManager.playButtonClick(); handleShopButton(); });
        buttons.getPauseButton().setOnAction(e -> { service.AudioManager.playButtonClick(); handlePauseButton(); });
        buttons.getMenuButton().setOnAction(e -> { service.AudioManager.playButtonClick(); handleMenuButton(); });
    }

    public void handleStartSystemPlayButton() {
        systemController.updateAllSystemsReadyState();
        if (systemController.areAllSystemsReady()) {
            level.setGameStarted(true);
            startGame();
        }
    }

    
    public boolean areAllSystemsReady() {
        systemController.updateAllSystemsReadyState();
        return systemController.areAllSystemsReady();
    }

    public void startGame() {
        if (!isRunning) {
            isRunning = true;
            gameLoop.start();
            movementManager.startMovementUpdates();
            systemController.startTimers();
            updatePauseButtonText();
            
            service.AudioManager.playBackgroundMusic();
        }
    }


    public void stopGame() {
        if (isRunning) {
            isRunning = false;
            gameLoop.stop();
            movementManager.stopMovementUpdates();
            systemController.stopTimers();
        }
    }


    private void handleShopButton() {
        if (!level.isPaused()) {
            level.setPaused(true);
        }
        
        if (gameScene != null) {
            gameScene.openShop();
        } else if (levelView != null) {
            levelView.openShop();
        }
    }

    private void handlePauseButton() {
        level.setPaused(!level.isPaused());
        updatePauseButtonText();
    }

    private void handleMenuButton() {
        stopGame();
        visualManager.showMenu();
    }

    private void updatePauseButtonText() {
        GameButtons buttons = getGameButtons();
        new manager.game.HUDManager().updatePauseButtonText(buttons, level.isPaused());
    }


    private void checkGameOver() {
        if (level.isGameOver()) {
            stopGame();
        }
    }

    private void isLevelCompleted() {

        if (!level.isGameStarted() || level.isGameOver() || level.isLevelCompleted()) return;

        boolean allStartDone = true;
        for (model.entity.systems.System sys : level.getSystems()) {
            if (sys instanceof model.entity.systems.StartSystem) {
                model.entity.systems.StartSystem ss = (model.entity.systems.StartSystem) sys;
                if (!ss.isGenerationComplete()) {
                    allStartDone = false;
                    break;
                }
            }
        }
        if (!allStartDone) return;

        if (manager.packets.PacketManager.hasMovingPackets()) return;

        stopGame();
        level.setLevelCompleted(true);
        if (gameScene != null) {
            gameScene.endLevelByNetwork();
        } else if (levelView != null) {
            try {
                java.lang.reflect.Method method = levelView.getClass().getMethod("endLevelByNetwork");
                method.invoke(levelView);
            } catch (Exception ignored) {}
        }
    }

    private HUDScene getHUDScene() {
        if (gameScene != null) {
            return gameScene.getHUDScene();
        } else if (levelView != null) {
            return levelView.getHUDScene();
        }
        return null;
    }

    private GameButtons getGameButtons() {
        if (gameScene != null) {
            GameButtons buttons = gameScene.getGameButtons();
            return buttons;
        } else if (levelView != null) {
            GameButtons buttons = levelView.getGameButtons();
            return buttons;
        }
        return null;
    }

    public PacketController getPacketController() {
        return packetController;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    
    private void updateAergiaMarkPositions() {
        abilityController.updateAergiaMarkPositions();
    }

    private void updateEliphasMarkPositions() {
        abilityController.updateEliphasMarkPositions();
    }
    
    public WireController getWireController() {
        return wireController;
    }
}
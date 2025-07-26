package controller;

import model.levels.Level;
import view.game.GameScene;
import view.game.HUDScene;
import view.game.GameButtons;
import view.components.levels.LevelView;
import view.components.ports.PortView;
import manager.game.VisualManager;
import manager.game.MovementManager;
import manager.game.ConnectionManager;
import manager.game.ShopManager;
import manager.game.ImpactManager;
import manager.packets.PacketManager;
import javafx.animation.AnimationTimer;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.layout.Pane;
import javafx.geometry.Point2D;

public class GameController {
    private final Level level;
    private final GameScene gameScene;
    private final LevelView levelView;
    private final VisualManager visualManager;
    
    // Controllers
    private final SystemController systemController;
    private final UIController uiController;
    private final PacketController packetController;
    private final CollisionController collisionController;
    private final WireController wireController;
    
    // Managers
    private final MovementManager movementManager;
    private final ConnectionManager connectionManager;
    private final ShopManager shopManager;
    
    // Game loop
    private AnimationTimer gameLoop;
    private Timeline systemUpdateTimer;
    private Timeline continuousTransferTimer;  // NEW: 10ms timer for packet storage transfers
    private boolean isRunning = false;

    public GameController(Level level, Object gameView, VisualManager visualManager) {
        this.level = level;
        this.visualManager = visualManager;
        
        // Handle both GameScene and LevelView
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
        
        // Initialize managers
        this.connectionManager = new ConnectionManager(level, level.getWireLength());
        this.shopManager = new ShopManager(level);
        this.movementManager = new MovementManager();
        
        // Initialize controllers
        this.packetController = new PacketController();
        this.packetController.setLevel(level);
        
        // Set packet layer based on view type
        if (gameScene != null) {
            this.packetController.setPacketLayer(gameScene.getGamePane());
        } else if (levelView != null) {
            this.packetController.setPacketLayer(levelView.getGamePane());
        }
        
        this.systemController = new SystemController(level, packetController);
        if (gameScene != null) {
            this.uiController = new UIController(gameScene, level, shopManager);
        } else {
            this.uiController = new UIController(levelView, level, shopManager);
        }
        this.collisionController = new CollisionController(level, packetController);
        this.wireController = new WireController();
        
        // Setup controllers
        setupControllers();
        setupGameLoop();
        setupSystemTimer();
        setupContinuousTransferTimer(); // Setup the new timer
        setupEventHandlers();
        
        // Setup port views after everything is initialized
        setupPortViews();
        
        // Setup start system play buttons
        setupStartSystemPlayButtons();
    }

    /**
     * Setup all controllers with their dependencies
     */
    private void setupControllers() {
        // Setup PacketManager
        PacketManager.setLevel(level);
        PacketManager.setPacketController(packetController);
        
        // Setup WireController
        wireController.setLevel(level);
        if (gameScene != null) {
            wireController.setGameScene(gameScene);
        } else if (levelView != null) {
            wireController.setGamePane(levelView.getGamePane());
        }
        wireController.setConnectionManager(connectionManager);
        
        // Setup HUD for WireController
        HUDScene hud = getHUDScene();
        if (hud != null) {
            wireController.setHUD(hud);
        }
        
        // Setup connection change callback
        wireController.setConnectionChangeCallback(() -> updateSystemIndicators());
        
        // Setup PortViews with WireController - moved to constructor
    }

    /**
     * Update system indicators when connections change
     */
    public void updateSystemIndicators() {
        // Generic update for all level views
        if (levelView != null) {
            try {
                // Try to call updateSystemIndicators on the level view
                java.lang.reflect.Method method = levelView.getClass().getMethod("updateSystemIndicators");
                method.invoke(levelView);
            } catch (Exception e) {
                // Fallback: handle specific level types
                if (levelView instanceof view.components.levels.Level1View) {
                    ((view.components.levels.Level1View) levelView).updateSystemIndicators();
                } else if (levelView instanceof view.components.levels.Level2View) {
                    ((view.components.levels.Level2View) levelView).updateSystemIndicators();
                }
            }
        }
    }

    /**
     * Setup start system play button actions and states
     */
    private void setupStartSystemPlayButtons() {
        // Generic setup for all level views
        if (levelView != null) {
            // Use reflection or interface to call setupStartSystemPlayButtons on any level view
            try {
                // Try to call setupStartSystemPlayButtons on the level view
                java.lang.reflect.Method method = levelView.getClass().getMethod("setupStartSystemPlayButtons", GameController.class);
                method.invoke(levelView, this);
            } catch (Exception e) {
                // Fallback: handle specific level types
                if (levelView instanceof view.components.levels.Level1View) {
                    ((view.components.levels.Level1View) levelView).setupStartSystemPlayButtons(this);
                } else if (levelView instanceof view.components.levels.Level2View) {
                    ((view.components.levels.Level2View) levelView).setupStartSystemPlayButtons(this);
                }
            }
        }
    }

    /**
     * Setup PortViews with WireController
     */
    private void setupPortViews() {
        // Set up wire controller for all system views
        if (levelView != null) {
            // Generic setup for all level views
            try {
                // Try to call setupWireControllerForPorts on the level view
                java.lang.reflect.Method method = levelView.getClass().getMethod("setupWireControllerForPorts", WireController.class);
                method.invoke(levelView, wireController);
            } catch (Exception e) {
                // Fallback: handle specific level types
                if (levelView instanceof view.components.levels.Level1View) {
                    ((view.components.levels.Level1View) levelView).setupWireControllerForPorts(wireController);
                } else if (levelView instanceof view.components.levels.Level2View) {
                    ((view.components.levels.Level2View) levelView).setupWireControllerForPorts(wireController);
                }
            }
            
            // Setup wire dragging events on the game pane
            setupWireDraggingEvents();
        }
    }
    
    /**
     * Setup wire dragging events on the game pane
     */
    private void setupWireDraggingEvents() {
        if (levelView != null) {
            Pane gamePane = levelView.getGamePane();
            
            // Handle mouse drag events for wire dragging
            gamePane.setOnMouseDragged(event -> {
                if (wireController.isDragging()) {
                    // Find the port under the mouse cursor for visual feedback
                    PortView hoveredPort = findPortAtPosition(event.getSceneX(), event.getSceneY());
                    wireController.updateWireDrag(event.getSceneX(), event.getSceneY(), hoveredPort);
                    event.consume(); // Consume the event to prevent it from being handled by other components
                }
            });
            
            // Handle mouse release events for wire dragging
            gamePane.setOnMouseReleased(event -> {
                if (wireController.isDragging()) {
                    // Find the port under the mouse cursor
                    PortView targetPort = findPortAtPosition(event.getSceneX(), event.getSceneY());
                    wireController.finishWireDrag(targetPort, event.getSceneX(), event.getSceneY());
                    event.consume(); // Consume the event to prevent it from being handled by other components
                }
            });
        }
    }
    
    /**
     * Find a port at the given scene coordinates
     */
    private PortView findPortAtPosition(double sceneX, double sceneY) {
        if (levelView == null) return null;
        
        Pane gamePane = levelView.getGamePane();
        
        // Convert scene coordinates to local coordinates relative to the game pane
        javafx.geometry.Point2D localPoint = gamePane.sceneToLocal(sceneX, sceneY);
        
        for (javafx.scene.Node node : gamePane.getChildren()) {
            if (node instanceof view.components.ports.PortView) {
                view.components.ports.PortView portView = (view.components.ports.PortView) node;
                // Check if the local coordinates are within the port's bounds
                if (portView.getBoundsInParent().contains(localPoint.getX(), localPoint.getY())) {
                    return portView;
                }
            }
        }
        return null;
    }

    /**
     * Setup the main game loop
     */
    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!level.isPaused()) {
                    // Update packet movement
                    movementManager.handle(now);
                    
                    // Update collision detection
                    collisionController.runCollisionCheck();
                    
                    // Update UI
                    uiController.updateHUD();
                    
                    // Check game over condition
                    checkGameOver();
                }
            }
        };
    }

    /**
     * Setup system update timer
     */
    private void setupSystemTimer() {
        systemUpdateTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (!level.isPaused()) {
                systemController.processSystems();
                systemController.updateSystemStates();
            }
        }));
        systemUpdateTimer.setCycleCount(Timeline.INDEFINITE);
    }

    /**
     * Continuous transfer timer for processing intermediate systems
     */
    private void setupContinuousTransferTimer() {
        continuousTransferTimer = new Timeline(
            new KeyFrame(Duration.millis(10), event -> {
                if (level != null && !level.isPaused()) {
                    // Process all intermediate and DDoS systems for packet forwarding
                    for (model.entity.systems.System system : level.getSystems()) {
                        if (system instanceof model.entity.systems.IntermediateSystem) {
                            model.entity.systems.IntermediateSystem intermediateSystem = 
                                (model.entity.systems.IntermediateSystem) system;
                            
                            // Process the intermediate system for packet forwarding
                            manager.systems.IntermediateSystemManager manager = 
                                new manager.systems.IntermediateSystemManager(intermediateSystem);
                            manager.forwardPackets();
                        } else if (system instanceof model.entity.systems.DDosSystem) {
                            model.entity.systems.DDosSystem ddosSystem = 
                                (model.entity.systems.DDosSystem) system;
                            
                            // Process the DDoS system for packet forwarding
                            manager.systems.DDosSystemManager manager = 
                                new manager.systems.DDosSystemManager(ddosSystem);
                            manager.forwardPackets();
                        }
                    }
                }
            })
        );
        continuousTransferTimer.setCycleCount(Timeline.INDEFINITE);
        continuousTransferTimer.play();
    }

    /**
     * Setup event handlers for UI components
     */
    private void setupEventHandlers() {
        GameButtons buttons = getGameButtons();
        System.out.println("DEBUG: GameController.setupEventHandlers - buttons: " + (buttons != null));
        if (buttons != null) {
            System.out.println("DEBUG: GameController.setupEventHandlers - Setting up shop button");
            buttons.getShopButton().setOnAction(e -> {
                System.out.println("DEBUG: Shop button clicked!");
                System.out.println("DEBUG: Shop button event: " + e);
                service.AudioManager.playButtonClick();
                handleShopButton();
            });
            buttons.getPauseButton().setOnAction(e -> {
                service.AudioManager.playButtonClick();
                handlePauseButton();
            });
            buttons.getMenuButton().setOnAction(e -> {
                service.AudioManager.playButtonClick();
                handleMenuButton();
            });
        } else {
            System.out.println("DEBUG: GameController.setupEventHandlers - buttons is null!");
        }
        
        // Note: HUD hide/show button functionality is handled internally by HUDScene
    }

    /**
     * Handle play button click from start system
     */
    public void handleStartSystemPlayButton() {
        // Check if all systems are ready
        systemController.updateAllSystemsReadyState();
        if (systemController.areAllSystemsReady()) {
            // Start the game
            level.setGameStarted(true);
            startGame();
            
            // Start packet generation for all start systems
            startAllStartSystems();
        }
    }
    
    /**
     * Start packet generation for all start systems
     */
    private void startAllStartSystems() {
        for (model.entity.systems.System system : level.getSystems()) {
            if (system instanceof model.entity.systems.StartSystem) {
                // Here we would start the StartSystemManager, but we need to track them
                // For now, the SystemController will handle packet generation
            }
        }
    }
    
    /**
     * Check if all systems are ready and update start system play button states
     */
    public void updateStartSystemPlayButtons() {
        systemController.updateAllSystemsReadyState();
        boolean allReady = systemController.areAllSystemsReady();
        
        // Update play button states if we have access to the views
        // This would need to be called from the view layer
    }
    
    /**
     * Check if all systems are ready
     */
    public boolean areAllSystemsReady() {
        systemController.updateAllSystemsReadyState();
        return systemController.areAllSystemsReady();
    }

    /**
     * Start the game
     */
    public void startGame() {
        if (!isRunning) {
            isRunning = true;
            gameLoop.start();
            movementManager.startMovementUpdates();
            systemUpdateTimer.play();
            continuousTransferTimer.play(); // Start the new timer
            updatePauseButtonText();
            
            // Start background music
            service.AudioManager.playBackgroundMusic();
        }
    }

    /**
     * Stop the game
     */
    public void stopGame() {
        if (isRunning) {
            isRunning = false;
            gameLoop.stop();
            movementManager.stopMovementUpdates();
            systemUpdateTimer.stop();
            if (continuousTransferTimer != null) {
                continuousTransferTimer.stop(); // Stop the new timer
            }
        }
    }

    /**
     * Handle shop button click
     */
    private void handleShopButton() {
        // Always pause the level and open shop, regardless of current pause state
        if (!level.isPaused()) {
            level.setPaused(true);
        }
        
        if (gameScene != null) {
            gameScene.openShop();
        } else if (levelView != null) {
            levelView.openShop();
        }
    }

    /**
     * Handle pause button click
     */
    private void handlePauseButton() {
        level.setPaused(!level.isPaused());
        updatePauseButtonText();
    }

    /**
     * Handle menu button click
     */
    private void handleMenuButton() {
        stopGame();
        visualManager.showMenu();
    }



    /**
     * Update pause button text
     */
    private void updatePauseButtonText() {
        GameButtons buttons = getGameButtons();
        if (buttons != null) {
            if (level.isPaused()) {
                buttons.getPauseButton().setText("Resume");
            } else {
                buttons.getPauseButton().setText("Pause");
            }
        }
    }

    /**
     * Update HUD with current game state
     */
    public void updateHUD() {
        HUDScene hud = getHUDScene();
        if (hud != null) {
            // Update wire length
            hud.getWireBox().setValue(String.format("%.1f", level.getRemainingWireLength()));
            
            // Update packet loss percentage
            double lossPercentage = (level.getPacketsGenerated() == 0) ? 0.0 : 
                ((double) level.getPacketLoss() / level.getPacketsGenerated()) * 100.0;
            hud.getLossBox().setValue(String.format("%.1f%%", lossPercentage));
            
            // Update coins with debug logging
            int currentCoins = level.getCoins();
            String coinsText = String.valueOf(currentCoins);
            java.lang.System.out.println("DEBUG: GameController.updateHUD - Setting coins to: " + coinsText);
            hud.getCoinsBox().setValue(coinsText);
            
            // Update packets collected (actual count from end systems)
            int packetsCollected = level.getPacketsCollected();
            hud.getPacketsBox().setValue(String.valueOf(packetsCollected));
            java.lang.System.out.println("DEBUG: GameController.updateHUD - Updated HUD: coins=" + currentCoins + ", packets=" + packetsCollected + ", loss=" + String.format("%.1f%%", lossPercentage));
        } else {
            java.lang.System.out.println("DEBUG: GameController.updateHUD - HUD scene is null!");
        }
    }

    /**
     * Check game over condition
     */
    private void checkGameOver() {
        if (level.isGameOver() && !level.getGameOverFlag()) {
            level.setGameOver(true);
            stopGame();
            // Game over will be handled by GameScene
        }
    }

    /**
     * Get HUD scene from GameScene or LevelView (helper method)
     */
    private HUDScene getHUDScene() {
        if (gameScene != null) {
            return gameScene.getHUDScene();
        } else if (levelView != null) {
            return levelView.getHUDScene();
        }
        return null;
    }

    /**
     * Get GameButtons from GameScene or LevelView (helper method)
     */
    private GameButtons getGameButtons() {
        System.out.println("DEBUG: GameController.getGameButtons - gameScene: " + (gameScene != null) + ", levelView: " + (levelView != null));
        if (gameScene != null) {
            GameButtons buttons = gameScene.getGameButtons();
            System.out.println("DEBUG: GameController.getGameButtons - from gameScene: " + (buttons != null));
            return buttons;
        } else if (levelView != null) {
            GameButtons buttons = levelView.getGameButtons();
            System.out.println("DEBUG: GameController.getGameButtons - from levelView: " + (buttons != null));
            return buttons;
        }
        return null;
    }

    /**
     * Get wire controller for external access
     */
    public WireController getWireController() {
        return wireController;
    }

    /**
     * Get connection manager for external access
     */
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    /**
     * Get packet controller for external access
     */
    public PacketController getPacketController() {
        return packetController;
    }

    /**
     * Get shop manager for external access
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Get level for external access
     */
    public Level getLevel() {
        return level;
    }
    
    /**
     * Manually setup event handlers (call this if automatic setup fails)
     */
    public void setupEventHandlersManually() {
        System.out.println("DEBUG: GameController.setupEventHandlersManually - called");
        setupEventHandlers();
    }
}
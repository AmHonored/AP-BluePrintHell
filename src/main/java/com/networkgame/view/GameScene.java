package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.model.*;
import com.networkgame.model.entity.Port;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Line;
import javafx.geometry.Point2D;

import javafx.animation.AnimationTimer;
import java.util.ArrayList;
import java.util.List;

import com.networkgame.model.entity.Connection;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.state.GameState;
import com.networkgame.model.state.UIUpdateListener;
import com.networkgame.model.manager.HUDManager;

public class GameScene implements UIUpdateListener {
    private Scene scene;
    private GameController gameController;
    private GameState gameState;
    
    private Pane gamePane;
    private Label wireLabel;
    private Label outOfWireLabel;
    private Label packetLossLabel;
    private Label coinsLabel;
    private Label timeLabel;
    private Label packetsCollectedLabel;
    
    private Button toggleButton;
    private Button startButton;
    
    // Progress bar components
    private ProgressBar temporalProgressBar;
    private ProgressBar timeProgressBar;
    private Circle timeProgressThumb;
    private TextField timeInputField;
    
    // Track whether game timer has started
    private boolean gameTimerStarted = false;
    
    // Game pause and time control UI indicators
    private VBox pauseIndicator;
    private VBox temporalControlIndicator;
    
    // Component for managing animations
    private List<AnimationTimer> animationTimers = new ArrayList<>();
    
    // Extracted components
    private DialogManager dialogManager;
    private UIComponentFactory uiComponentFactory;
    private ConnectionManager connectionManager;
    private PacketVisualizer packetVisualizer;
    private GameEventHandler gameEventHandler;
    private SystemRenderer systemRenderer;
    
    // Track last render time
    private long lastRenderTime = 0;
    
    public GameScene(GameController gameController, GameState gameState) {
        this.gameController = gameController;
        this.gameState = gameState;
        
        // Create main layout
        BorderPane mainLayout = new BorderPane();
        
        // Create game area in center
        gamePane = new Pane();
        gamePane.setPrefSize(800, 500);
        gamePane.getStyleClass().add("game-pane");
        mainLayout.setCenter(gamePane);
        
        // Initialize sub-components
        initializeComponents();
        
        // Initialize UI elements
        wireLabel = new Label(String.format("%.1f", gameState.getRemainingWireLength()));
        outOfWireLabel = new Label("Out of wire!");
        outOfWireLabel.setVisible(false);
        
        timeLabel = new Label("0s");
        packetLossLabel = new Label("0.0%");
        coinsLabel = new Label(String.valueOf(gameState.getCoins()));
        packetsCollectedLabel = new Label("0");
        
        // Initialize time progress components
        timeProgressThumb = new Circle(5, 5, 6);
        timeProgressBar = new ProgressBar(0);
        timeInputField = new TextField("0");
        
        // Initialize temporal progress bar
        temporalProgressBar = new ProgressBar(gameState.getTemporalProgress());
        temporalProgressBar.setPrefWidth(100);
        
        // Create toggle button
        toggleButton = new Button("Pause");
        
        // Create HUD at top with toggle button
        VBox hudContainer = uiComponentFactory.createHUDWithToggle(
            wireLabel, outOfWireLabel, timeLabel, packetLossLabel, coinsLabel, packetsCollectedLabel,
            timeProgressBar, timeProgressThumb, timeInputField);
        mainLayout.setTop(hudContainer);
        
        // Register HUD with HUD manager
        HUDManager hudManager = HUDManager.getInstance();
        if (hudContainer.getChildren().get(0) instanceof HBox) {
            hudManager.setMainHudPane((HBox) hudContainer.getChildren().get(0));
        }
        
        // Create controls at bottom
        HBox controlsPane = uiComponentFactory.createControls(toggleButton);
        controlsPane.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controlsPane);
        
        // Initialize game elements
        initializeGameElements();
        
        // Create scene
        scene = new Scene(mainLayout, 800, 600);
        
        // Apply CSS
        String cssPath = getClass().getResource("/css/style.css").toExternalForm();
        scene.getStylesheets().add(cssPath);
        
        // Setup timer for game loop
        setupGameEvents();
        
        // Set the GameScene in the GameState for callbacks
        gameState.setUIUpdateListener(this);
    }
    
    /**
     * Initialize component classes
     */
    private void initializeComponents() {
        dialogManager = new DialogManager(gamePane, gameController);
        uiComponentFactory = new UIComponentFactory(gameState, gameController);
        connectionManager = new ConnectionManager(gameState, gamePane);
        gameEventHandler = new GameEventHandler(gameState, gameController, gamePane, connectionManager, dialogManager);
        packetVisualizer = new PacketVisualizer(gameState, gamePane);
        systemRenderer = new SystemRenderer(gameState, gamePane, uiComponentFactory, gameEventHandler);
    }
    
    /**
     * Initialize the game elements
     */
    private void initializeGameElements() {
        // Initialize port connection map
        connectionManager.initializePortConnectionMap();
        
        // Initialize and render systems
        systemRenderer.initializeSystemElements();
        
        // Setup event handlers
        gameEventHandler.setupEventHandlers();
        
        // Add connections to the game pane
        for (Connection connection : gameState.getConnections()) {
            Shape connectionShape = connection.getConnectionShape();
            connectionShape.getStyleClass().add("connection");
            
            // Set connection color to green
            if (connectionShape instanceof Line) {
                ((Line) connectionShape).setStroke(javafx.scene.paint.Color.rgb(50, 205, 50));
                ((Line) connectionShape).setStrokeWidth(3.0);
            }
            
            gamePane.getChildren().add(connectionShape);
            
            // Make sure connections are behind ports
            connectionShape.toBack();
        }
        
        // Ensure proper z-ordering
        for (NetworkSystem system : gameState.getSystems()) {
            system.getShape().toBack(); // System shapes at the back
        }
        
        // Register HUD components with the HUD manager
        HUDManager hudManager = HUDManager.getInstance();
        hudManager.setHUDContainer(gamePane);
        
        // Note: HUD components are now registered in UIComponentFactory.createHUD()
    }
    
    /**
     * Setup timer for game loop
     */
    private void setupGameEvents() {
        AnimationTimer gameTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            private long frameCounter = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0; // Convert to seconds
                lastUpdate = now;
                
                // Update UI elements
                packetVisualizer.updatePackets();
                
                // Update game state
                gameState.update(deltaTime);
                
                // Process collisions between packets
                packetVisualizer.processCollisions();
                
                // Process lost packets for visual effects
                packetVisualizer.processPacketLoss();
                
                // Update indicator lamps for all systems
                for (NetworkSystem system : gameState.getSystems()) {
                    system.updateIndicatorLamp();
                    
                    // Update visibility of stored packets in non-start, non-end systems
                    if (!system.isStartSystem() && !system.isEndSystem()) {
                        system.updateStoredPacketsVisibility();
                    }
                }
                
                // Update capacity labels
                systemRenderer.updateCapacityLabels();
                
                // Call render regularly to update the UI
                frameCounter++;
                if (frameCounter % 2 == 0) { // Update UI every 2 frames for better performance
                    render();
                }
            }
        };
        
        // Track the timer for proper cleanup
        animationTimers.add(gameTimer);
        
        // Start the timer
        gameTimer.start();
    }
    
    /**
     * Update the game's UI elements
     */
    public void render() {
        // Always ensure we're on the JavaFX thread
        if (!javafx.application.Platform.isFxApplicationThread()) {
            javafx.application.Platform.runLater(this::render);
            return;
        }
        
        // Throttle updates to prevent UI lag, but skip throttling for critical updates
        long currentTime = System.currentTimeMillis();

        lastRenderTime = currentTime;
        
        // Make sure all packets are properly visible
        packetVisualizer.ensurePacketsVisible();
        
        // Update UI elements with current game state
        String currentText = wireLabel.getText();
        String newText = String.format("%.1f", gameState.getRemainingWireLength());
        
        // Check for out of wire condition
        boolean anyConnectionOutOfWire = false;
        for (Connection connection : gameState.getConnections()) {
            if (connection.isOutOfWire()) {
                anyConnectionOutOfWire = true;
                break;
            }
        }
        
        // Show/hide out of wire warning
        outOfWireLabel.setVisible(anyConnectionOutOfWire || gameState.getRemainingWireLength() <= 0);
        
        // Only animate wire label if there's a significant change
        if (!currentText.equals(newText)) {
            // Animate update with smooth transition
            wireLabel.getStyleClass().add("updating");
            wireLabel.setText(newText);
            
            // Remove updating class after transition completes
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
            pause.setOnFinished(event -> wireLabel.getStyleClass().remove("updating"));
            pause.play();
        }
        
        packetLossLabel.setText(String.format("%.1f%%", gameState.getPacketLossPercentage()));
        
        // Update coins
        int currentCoins = gameState.getCoins();
        coinsLabel.setText(String.format("%d", currentCoins));
        
        // Update time label
        int timeRemaining = gameState.getLevelDuration() - (int)gameState.getElapsedTime();
        timeRemaining = Math.max(0, timeRemaining); // Ensure time doesn't go negative
        int minutes = timeRemaining / 60;
        int seconds = timeRemaining % 60;
        timeLabel.setText(String.format("%02d:%02d", minutes, seconds));
        
        // Update packets collected label to simple counter format
        int packetsDelivered = gameState.getPacketsDelivered();
        String packetsText = String.format("%d", packetsDelivered);
        
        // Always show packets collected label, but with different styling for level 2
        if (gameState.getCurrentLevel() == 2) {
            packetsCollectedLabel.setText(packetsText);
            packetsCollectedLabel.setVisible(true);
            // Add a note to indicate it's just for tracking
            packetsCollectedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #50fa7b; -fx-opacity: 0.8;");
        } else {
            packetsCollectedLabel.setText(packetsText);
            packetsCollectedLabel.setVisible(true);
            packetsCollectedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #50fa7b;");
        }
        
        // Flash effect for packet update
        if (!packetsCollectedLabel.getText().equals(packetsText)) {
            packetsCollectedLabel.getStyleClass().add("packets-updated");
            
            // Remove the style class after animation
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
            pause.setOnFinished(event -> packetsCollectedLabel.getStyleClass().remove("packets-updated"));
            pause.play();
        }
        
        // Update wire colors based on whether they're active or have packets
        connectionManager.updateConnectionColors();
    }
    
    /**
     * Update the temporal progress bar
     */
    public void updateTemporalProgress() {
        temporalProgressBar.setProgress(gameState.getTemporalProgress());
    }
    
    /**
     * Update the time progress display with current time values
     */
    public void updateTimeProgress(double elapsedTime, int totalDuration) {
        // Clamp values to sensible ranges
        double clampedElapsed = Math.max(0, Math.min(elapsedTime, totalDuration));
        int levelDuration = Math.max(10, totalDuration); // Ensure we have a reasonable minimum
        
        // Update time progress based on current elapsed time
        double progress = clampedElapsed / levelDuration;
        
        // Update label to show time
        int remainingTime = levelDuration - (int)clampedElapsed;
        String displayTime = String.format("%d:%02d", remainingTime / 60, remainingTime % 60);
        
        // Format the time label based on remaining time
        if (remainingTime <= 10) {
            // Red color for last 10 seconds
            timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff3333;");
        } else if (remainingTime <= 30) {
            // Orange color for last 30 seconds
            timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff9933;");
        } else {
            // Normal blue color
            timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00aaff;");
        }
        
        // Update controls on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            timeLabel.setText(displayTime);
            timeProgressBar.setProgress(progress);
            
            // Also update the time slider position
            if (timeProgressThumb != null) {
                double thumbPosX = timeProgressBar.getLayoutX() + 
                                   (timeProgressBar.getWidth() * progress);
                timeProgressThumb.setLayoutX(thumbPosX);
            }
            
            // Update time input field if it exists
            if (timeInputField != null) {
                timeInputField.setText(String.valueOf((int)clampedElapsed));
            }
        });
    }
    
    /**
     * Shows a pause indicator overlay on the game screen
     */
    public void showPauseIndicator() {
        if (pauseIndicator == null) {
            pauseIndicator = uiComponentFactory.createPauseIndicator(gamePane.getWidth(), gamePane.getHeight());
        }
        
        if (!gamePane.getChildren().contains(pauseIndicator)) {
            gamePane.getChildren().add(pauseIndicator);
            
            // Ensure it's on top
            pauseIndicator.toFront();
        }
    }
    
    /**
     * Hides the pause indicator
     */
    public void hidePauseIndicator() {
        if (pauseIndicator != null && gamePane.getChildren().contains(pauseIndicator)) {
            gamePane.getChildren().remove(pauseIndicator);
        }
    }
    
    /**
     * Shows a temporal control mode indicator
     */
    public void showTemporalControlIndicator() {
        if (temporalControlIndicator == null) {
            temporalControlIndicator = uiComponentFactory.createTemporalControlIndicator(
                gamePane.getWidth(), gamePane.getHeight());
        }
        
        if (!gamePane.getChildren().contains(temporalControlIndicator)) {
            gamePane.getChildren().add(temporalControlIndicator);
            
            // Ensure it's on top
            temporalControlIndicator.toFront();
        }
    }
    
    /**
     * Hides the temporal control mode indicator
     */
    public void hideTemporalControlIndicator() {
        if (temporalControlIndicator != null && gamePane.getChildren().contains(temporalControlIndicator)) {
            gamePane.getChildren().remove(temporalControlIndicator);
        }
    }
    
    /**
     * Update the packet loss label in the HUD
     * @param lossPercentage The current packet loss percentage
     */
    public void updatePacketLossLabel(double lossPercentage) {
        if (packetLossLabel != null) {
            javafx.application.Platform.runLater(() -> {
                packetLossLabel.setText(String.format("%.1f%%", lossPercentage));
            });
        }
    }
    
    /**
     * Update the packets collected label with current count
     * @param packetsCount The number of packets that reached the end system
     */
    public void updatePacketsCollectedLabel(int packetsCount) {
        if (packetsCollectedLabel != null) {
            javafx.application.Platform.runLater(() -> {
                packetsCollectedLabel.setText(String.format("%d", packetsCount));
            });
        }
    }
    
    /**
     * Update the coins label directly
     * @param coinsCount The current number of coins
     */
    public void updateCoinsLabel(int coinsCount) {
        if (coinsLabel != null) {
            javafx.application.Platform.runLater(() -> {
                coinsLabel.setText(String.format("%d", coinsCount));
            });
        }
    }
    
    /**
     * Show game over screen when system capacity exceeded
     */
    public void showCapacityExceededGameOver(NetworkSystem system) {
        dialogManager.showCapacityExceededGameOver(system);
    }
    
    /**
     * Display game over screen when time's up but not enough packets delivered
     */
    public void showTimeUpGameOver(int packetsDelivered, int requiredPackets) {
        dialogManager.showTimeUpGameOver(packetsDelivered, requiredPackets);
    }
    
    /**
     * Display generic game over screen
     */
    public void showGameOver(double packetLossPercentage, int successfulPackets) {
        dialogManager.showGameOver(packetLossPercentage, successfulPackets);
    }
    
    /**
     * Show level complete dialog
     */
    public void showLevelComplete(int level) {
        dialogManager.showLevelComplete(level, gameState);
    }
    
    /**
     * Create a visual effect for time jump/travel
     */
    public void createTimeJumpEffect() {
        packetVisualizer.createTimeJumpEffect();
    }
    
    /**
     * Cleanup method to properly dispose resources and prevent memory leaks
     */
    public void cleanup() {
        // Stop any running animations
        if (animationTimers != null) {
            for (AnimationTimer timer : animationTimers) {
                timer.stop();
            }
            animationTimers.clear();
        }
        
        // Clear all UI elements that may hold references
        if (gamePane != null) {
            gamePane.getChildren().clear();
        }
        
        // Cleanup packet visualizer
        if (packetVisualizer != null) {
            packetVisualizer.cleanup();
        }
    }
    
    /**
     * Update the capacity labels for all systems
     * Delegates to SystemRenderer
     */
    public void updateCapacityLabels() {
        systemRenderer.updateCapacityLabels();
    }
    
    /**
     * Highlight a port
     * Delegates to GameEventHandler
     */
    public void highlightPort(Port port) {
        gameEventHandler.highlightPort(port);
    }
    
    /**
     * Clear all port highlights
     * Delegates to GameEventHandler
     */
    public void clearHighlights() {
        gameEventHandler.clearHighlights();
    }
    
    /**
     * Get the scene object
     */
    public Scene getScene() {
        return scene;
    }
    
    public Pane getGamePane() {
        return gamePane;
    }
    
    // Implementation of UIUpdateListener interface methods
    
    @Override
    public void createImpactEffect(Point2D position) {
        // TODO: Implement impact effect visualization
        // For now, just trigger a render to update the UI
        render();
    }
    
    @Override
    public void onGameStateUpdated() {
        // Trigger a general UI update when game state changes
        javafx.application.Platform.runLater(() -> {
            render();
        });
    }
} 

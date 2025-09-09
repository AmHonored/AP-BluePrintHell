package view.components.levels;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import view.game.HUDScene;
import view.game.GameButtons;
import view.game.ShopScene;
import view.game.GameOverScene;
import view.game.LevelCompleteScene;
import model.levels.Level;
import manager.game.ShopManager;
import manager.game.VisualManager;

public abstract class LevelView extends StackPane {
    protected final Level level;
    protected final VisualManager visualManager;
    
    // Common UI components
    protected Pane gamePane;
	protected Pane worldContainer;
	protected Group worldGroup;
    protected HUDScene hud;
    protected GameButtons controls;
    protected ShopScene shopOverlay;
    protected GameOverScene gameOverOverlay;
    protected LevelCompleteScene levelCompleteOverlay;
	protected Pane zoomOverlay;
    
    // Managers
    protected ShopManager shopManager;

	// Zoom state
	private Scale worldScale;
	private double currentZoom = 1.0;
	private static final double MIN_ZOOM = 0.5;
	private static final double MAX_ZOOM = 2.0;
	private static final double ZOOM_STEP = 0.1;

    public LevelView(Level level, VisualManager visualManager) {
        this.level = level;
        this.visualManager = visualManager;
        
        setupCommonUI();
        setupOverlays();
        // Event handlers will be set up by GameController
        
        // Initialize HUD with current values
        updateHUD();
    }

    /**
     * Setup common UI components (HUD, controls, game pane)
     */
    private void setupCommonUI() {
        BorderPane mainLayout = new BorderPane();

		// Center game area (wrap in a group so we can apply a scale transform for zoom)
		gamePane = new Pane();
		gamePane.setPrefSize(800, 500);
		gamePane.setStyle("-fx-background-color: transparent;");

		worldGroup = new Group(gamePane);
		worldScale = new Scale(currentZoom, currentZoom, 0, 0);
		worldGroup.getTransforms().add(worldScale);
		worldContainer = new Pane(worldGroup);
		worldContainer.setPrefSize(800, 500);
		worldContainer.getStyleClass().add("game-pane");
		mainLayout.setCenter(worldContainer);

        // Top HUD
        hud = new HUDScene(level);
        hud.getStyleClass().add("hud-pane");
        mainLayout.setTop(hud);
        // Enable/disable Aergia button based on connections and inventory
        updateAergiaButtonState();

		// Bottom controls
		controls = new GameButtons();
        controls.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controls);

		// Floating zoom overlay inside the game scene area (top-left)
		zoomOverlay = createZoomOverlay();
		zoomOverlay.setLayoutX(8);
		zoomOverlay.setLayoutY(8);
		worldContainer.getChildren().add(zoomOverlay);
		zoomOverlay.toFront();
		makeZoomOverlayDraggable();

        this.getChildren().add(mainLayout);
    }

	private void zoomBy(double delta) {
		double target = clamp(currentZoom + delta, MIN_ZOOM, MAX_ZOOM);
		if (Math.abs(target - currentZoom) < 1e-9) return;
		currentZoom = target;
		if (worldScale != null) {
			worldScale.setX(currentZoom);
			worldScale.setY(currentZoom);
		}
	}

	private Pane createZoomOverlay() {
		Pane box = new Pane();
		box.setPickOnBounds(true);
		box.setMouseTransparent(false);
		box.setPrefSize(36, 64);
		box.setMinSize(36, 64);
		box.setMaxSize(36, 64);

		Rectangle bg = new Rectangle(36, 64);
		bg.setArcWidth(8);
		bg.setArcHeight(8);
		bg.setFill(Color.rgb(15, 19, 25, 0.85));
		bg.setStroke(Color.web("#00d4ff"));
		bg.setStrokeWidth(1.0);

		Line divider = new Line(0, 32, 36, 32);
		divider.setStroke(Color.web("#00d4ff"));
		divider.setOpacity(0.6);

		Label plus = new Label("+");
		plus.setTextFill(Color.WHITE);
		plus.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
		plus.setLayoutX(12);
		plus.setLayoutY(4);

		Label minus = new Label("-");
		minus.setTextFill(Color.WHITE);
		minus.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
		minus.setLayoutX(14);
		minus.setLayoutY(36);

		Rectangle topHit = new Rectangle(36, 32);
		topHit.setFill(Color.TRANSPARENT);
		topHit.setOnMouseClicked(e -> zoomBy(ZOOM_STEP));

		Rectangle bottomHit = new Rectangle(36, 32);
		bottomHit.setLayoutY(32);
		bottomHit.setFill(Color.TRANSPARENT);
		bottomHit.setOnMouseClicked(e -> zoomBy(-ZOOM_STEP));

		box.getChildren().addAll(bg, divider, plus, minus, topHit, bottomHit);
		return box;
	}

	private void makeZoomOverlayDraggable() {
		final double[] pressScene = new double[2];
		final double[] startTranslate = new double[2];

		zoomOverlay.setOnMousePressed(e -> {
			pressScene[0] = e.getSceneX();
			pressScene[1] = e.getSceneY();
			startTranslate[0] = zoomOverlay.getTranslateX();
			// If still bound to HUD height, unbind and lock current value so it can move
			if (zoomOverlay.translateYProperty().isBound()) {
				zoomOverlay.translateYProperty().unbind();
				zoomOverlay.setTranslateY(hud.getHeight() + 8);
			}
			startTranslate[1] = zoomOverlay.getTranslateY();
			e.consume();
		});

		zoomOverlay.setOnMouseDragged(e -> {
			double dx = e.getSceneX() - pressScene[0];
			double dy = e.getSceneY() - pressScene[1];
			double targetX = startTranslate[0] + dx;
			double targetY = startTranslate[1] + dy;

			// Clamp within view bounds
			double viewW = Math.max(1.0, this.getWidth());
			double viewH = Math.max(1.0, this.getHeight());
			double boxW = zoomOverlay.getWidth() > 0 ? zoomOverlay.getWidth() : 36;
			double boxH = zoomOverlay.getHeight() > 0 ? zoomOverlay.getHeight() : 64;
			double minY = Math.max(4, hud.getHeight() + 4);
			targetX = clamp(targetX, 4, Math.max(4, viewW - boxW - 4));
			targetY = clamp(targetY, minY, Math.max(minY, viewH - boxH - 4));

			zoomOverlay.setTranslateX(targetX);
			zoomOverlay.setTranslateY(targetY);
			e.consume();
		});
	}

	private double clamp(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}

    /**
     * Setup overlay components
     */
    private void setupOverlays() {
        // Shop overlay
        shopManager = new ShopManager(level);
        shopOverlay = new ShopScene(shopManager, level);
        // Update HUD Aergia button when items change (e.g., Aergia purchased)
        shopOverlay.setOnItemsChanged(() -> {
            if (hud != null) {
                // Update Aergia
                String aergiaText = "Aergia (" + level.getAergiaScrolls() + ")";
                if (level.isAergiaOnCooldown()) aergiaText += " \u23F3"; // hourglass
                hud.getAergiaButton().setText(aergiaText);
                boolean aergiaEnabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
                hud.getAergiaButton().setDisable(!aergiaEnabled);

                // Update Sisyphus
                String sisyphusText = "Sisyphus (" + level.getSisyphusScrolls() + ")";
                hud.getSisyphusButton().setText(sisyphusText);
                boolean sisyphusEnabled = level.getSisyphusScrolls() > 0; // no cooldown/conditions
                hud.getSisyphusButton().setDisable(!sisyphusEnabled);

                // Update Eliphas
                String eliphasText = "Eliphas (" + level.getEliphasScrolls() + ")";
                hud.getEliphasButton().setText(eliphasText);
                boolean eliphasEnabled = level.getEliphasScrolls() > 0; // no cooldown
                hud.getEliphasButton().setDisable(!eliphasEnabled);
            }
        });
        shopOverlay.setVisible(false);
        shopOverlay.getCloseButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            closeShop();
        });

        // Game over overlay
        gameOverOverlay = new GameOverScene();
        gameOverOverlay.setVisible(false);

        // Level complete overlay
        levelCompleteOverlay = new LevelCompleteScene();
        levelCompleteOverlay.setVisible(false);

        // Add overlays to stack
        this.getChildren().addAll(shopOverlay, gameOverOverlay, levelCompleteOverlay);
    }

    private boolean areAllPortsConnected() {
        for (model.entity.systems.System sys : level.getSystems()) {
            for (model.entity.ports.Port p : sys.getInPorts()) {
                if (!p.isConnected()) return false;
            }
            for (model.entity.ports.Port p : sys.getOutPorts()) {
                if (!p.isConnected()) return false;
            }
        }
        return true;
    }

    /**
     * Setup common event handlers (now handled by GameController)
     */
    private void setupEventHandlers() {
        // Event handlers are now set up by GameController
        // This method is kept for compatibility but does nothing
        
        // Setup mouse events for wire dragging on game pane
        setupWireDraggingEvents();
    }
    
    /**
     * Setup mouse events for wire dragging
     */
    private void setupWireDraggingEvents() {
        gamePane.setOnMouseDragged(event -> {
            // This will be handled by the WireController when it's set up
            // The GameController will set up the wire controller and connect these events
        });
        
        gamePane.setOnMouseReleased(event -> {
            // This will be handled by the WireController when it's set up
        });
    }

    /**
     * Open shop overlay
     */
    public void openShop() {
        // Always pause the level and show shop, regardless of current pause state
        if (!level.isPaused()) {
            level.setPaused(true);
        }
        shopOverlay.setVisible(true);
    }

    /**
     * Close shop overlay
     */
    protected void closeShop() {
        shopOverlay.setVisible(false);
        level.setPaused(false);
    }

    /**
     * Toggle pause state
     */
    protected void togglePause() {
        level.setPaused(!level.isPaused());
        updatePauseButtonText();
    }

    /**
     * Go to main menu
     */
    protected void goToMenu() {
        visualManager.showMenu();
    }



    /**
     * Update pause button text
     */
    protected void updatePauseButtonText() {
        if (level.isPaused()) {
            controls.getPauseButton().setText("Resume");
        } else {
            controls.getPauseButton().setText("Pause");
        }
    }

    /**
     * Show game over overlay
     */
    protected void showGameOver() {
        gameOverOverlay.setVisible(true);
        gameOverOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        gameOverOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
    }

    /**
     * Show level complete overlay
     */
    protected void showLevelComplete() {
        levelCompleteOverlay.setVisible(true);
        
        // Play level complete sound
        service.AudioManager.playLevelComplete();
        
        levelCompleteOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        levelCompleteOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToMenu();
        });
        levelCompleteOverlay.getNextLevelButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToNextLevel();
        });
    }

    /**
     * Restart current level
     */
    protected abstract void restartLevel();

    /**
     * Go to next level
     */
    protected abstract void goToNextLevel();

    /**
     * Update HUD with current game state
     */
    public void updateHUD() {
        // Update wire length
        hud.getWireBox().setValue(String.format("%.1f", level.getRemainingWireLength()));
        
        // Update packet loss percentage
        double lossPercentage = (level.getPacketsGenerated() == 0) ? 0.0 : 
            ((double) level.getPacketLoss() / level.getPacketsGenerated()) * 100.0;
        hud.getLossBox().setValue(String.format("%.1f%%", lossPercentage));
        
        // Update coins
        hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
        
        // Update packets collected (actual count from end systems)
        hud.getPacketsBox().setValue(String.valueOf(level.getPacketsCollected()));
        
        // Update temporal progress
        if (hud.getTemporalProgress() != null) {
            hud.getTemporalProgress().getProgressBar().setProgress(level.getCurrentTime() / 100.0); // Normalize to 0-1
            hud.getTemporalProgress().getTimeLabel().setText("Time: " + level.getCurrentTime());
        }
        updateAergiaButtonState();
    }

    private void updateAergiaButtonState() {
        String text = "Aergia (" + level.getAergiaScrolls() + ")";
        if (level.isAergiaOnCooldown()) text += " \u23F3";
        hud.getAergiaButton().setText(text);
        // Also enforce enabled/disabled here when running in LevelView mode
        boolean enabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
        hud.getAergiaButton().setDisable(!enabled);
        // Silent in production: no per-tick logging
    }

    /**
     * Get game pane for adding game elements
     */
    public Pane getGamePane() {
        return gamePane;
    }

    /**
     * Get HUD scene
     */
    public HUDScene getHUDScene() {
        return hud;
    }

    /**
     * Get game buttons
     */
    public GameButtons getGameButtons() {
        return controls;
    }

    /**
     * Get shop manager
     */
    public ShopManager getShopManager() {
        return shopManager;
    }

    /**
     * Get level
     */
    public Level getLevel() {
        return level;
    }
}

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
import model.levels.Level;
import manager.game.ShopManager;
import manager.game.VisualManager;

public abstract class LevelView extends StackPane {
    protected final Level level;
    protected final VisualManager visualManager;
    
    protected Pane gamePane;
	protected Pane worldContainer;
	protected Group worldGroup;
    protected HUDScene hud;
    protected GameButtons controls;
    protected ShopScene shopOverlay;

    protected view.game.GameOverScene gameOverOverlay;
    protected view.game.LevelCompleteScene levelCompleteOverlay;
	protected Pane zoomOverlay;

	// Zoom 
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
        
        updateHUD();
    }

    private void setupCommonUI() {
        BorderPane mainLayout = new BorderPane();

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

        updateAergiaButtonState();

		controls = new GameButtons();
        controls.getStyleClass().add("controls-pane");
        mainLayout.setBottom(controls);

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

    private void setupOverlays() {

        shopOverlay = new ShopScene(new ShopManager(level), level);
        shopOverlay.setOnItemsChanged(() -> {
            if (hud != null) {
                hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
                
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

        gameOverOverlay = new view.game.GameOverScene();
        gameOverOverlay.setVisible(false);
        levelCompleteOverlay = new view.game.LevelCompleteScene();
        levelCompleteOverlay.setVisible(false);

        gameOverOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        gameOverOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            visualManager.showMenu();
        });

        levelCompleteOverlay.getRetryButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            restartLevel();
        });
        levelCompleteOverlay.getMenuButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            visualManager.showMenu();
        });
        levelCompleteOverlay.getNextLevelButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            goToNextLevel();
        });

        this.getChildren().addAll(shopOverlay, gameOverOverlay, levelCompleteOverlay);
    }

    public void openShop() {
        if (!level.isPaused()) {
            level.setPaused(true);
        }
        shopOverlay.setVisible(true);
    }

    protected void closeShop() {
        shopOverlay.setVisible(false);
        level.setPaused(false);
    }

    protected abstract void restartLevel();

    protected abstract void goToNextLevel();

    public void updateHUD() {
        hud.getWireBox().setValue(String.format("%.1f", level.getRemainingWireLength()));
        
        double lossPercentage = (level.getPacketsGenerated() == 0) ? 0.0 : 
            ((double) level.getPacketLoss() / level.getPacketsGenerated()) * 100.0;
        hud.getLossBox().setValue(String.format("%.1f%%", lossPercentage));
        
        hud.getCoinsBox().setValue(String.valueOf(level.getCoins()));
        
        hud.getPacketsBox().setValue(String.valueOf(level.getPacketsCollected()));
        
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
        boolean enabled = level.getAergiaScrolls() > 0 && !level.isAergiaOnCooldown();
        hud.getAergiaButton().setDisable(!enabled);
    }

    public Pane getGamePane() {
        return gamePane;
    }

    public HUDScene getHUDScene() {
        return hud;
    }

    public GameButtons getGameButtons() {
        return controls;
    }

    public Level getLevel() {
        return level;
    }

    public void endLevelByNetwork() {
        double lossRatio = (level.getPacketsGenerated() == 0) ? 0.0 : ((double) level.getPacketLoss() / level.getPacketsGenerated());
        boolean won = lossRatio <= 0.5;
        if (won) {
            try { service.AudioManager.playLevelComplete(); } catch (Throwable ignored) {}
            if (levelCompleteOverlay != null) levelCompleteOverlay.setVisible(true);
            if (gameOverOverlay != null) gameOverOverlay.setVisible(false);
        } else {
            try { service.AudioManager.cleanup(); } catch (Throwable ignored) {}
            if (gameOverOverlay != null) gameOverOverlay.setVisible(true);
            if (levelCompleteOverlay != null) levelCompleteOverlay.setVisible(false);
        }
        if (shopOverlay != null) shopOverlay.setVisible(false);
    }


    public void gameRestoringScene(serialization.save.SaveGame saved) {
        javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.45);");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(8);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.control.Label title = new javafx.scene.control.Label("Restoring saved game...");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        String when = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(saved.savedAtEpochMillis));
        javafx.scene.control.Label meta = new javafx.scene.control.Label("Level: " + saved.levelId + "   •   Saved at: " + when);
        meta.setStyle("-fx-text-fill: #d5e0ea; -fx-font-size: 12px;");
        javafx.scene.control.Label countdown = new javafx.scene.control.Label();
        countdown.setStyle("-fx-text-fill: #c0ffee; -fx-font-size: 14px;");
        box.getChildren().addAll(title, meta, countdown);
        overlay.getChildren().add(box);
        this.getChildren().add(overlay);

        final int[] remaining = new int[]{4};
        countdown.setText("Starting in " + remaining[0] + "…  (click to skip)");
        javafx.animation.Timeline tl = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                remaining[0]--;
                if (remaining[0] <= 0) {
                    this.getChildren().remove(overlay);
                } else {
                    countdown.setText("Starting in " + remaining[0] + "…  (click to skip)");
                }
            })
        );
        tl.setCycleCount(4);
        tl.play();

        overlay.setOnMouseClicked(e -> {
            tl.stop();
            this.getChildren().remove(overlay);
        });
    }
}

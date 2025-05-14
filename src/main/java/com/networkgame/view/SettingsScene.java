package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.model.AudioManager;
import com.networkgame.model.AudioManager.SoundType;
import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SettingsScene {
    private Scene scene;
    private GameController gameController;
    private AudioManager audioManager;
    private Slider volumeSlider;
    
    private boolean leftKeyPressed = false;
    private boolean rightKeyPressed = false;
    private AnimationTimer volumeAdjustTimer;
    
    private final EventHandler<KeyEvent> keyPressHandler;
    private final EventHandler<KeyEvent> keyReleaseHandler;
    
    public SettingsScene(GameController gameController, AudioManager audioManager) {
        this.gameController = gameController;
        this.audioManager = audioManager;
        
        keyPressHandler = this::handleKeyPress;
        keyReleaseHandler = this::handleKeyRelease;
        
        setupScene();
        setupVolumeAdjustmentTimer();
    }
    
    private void setupScene() {
        VBox mainLayout = createMainLayout();
        
        Label titleLabel = new Label("Settings");
        titleLabel.getStyleClass().add("title-label");
        
        mainLayout.getChildren().addAll(
            titleLabel,
            createVolumeControls(),
            createBackButton()
        );
        
        scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        setupKeyboardHandlers();
        setupSceneActivation();
    }
    
    private VBox createMainLayout() {
        VBox mainLayout = new VBox(25);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(40));
        mainLayout.getStyleClass().add("main-menu");
        mainLayout.setStyle("-fx-background-color: #1a1a2e;");
        return mainLayout;
    }
    
    private VBox createVolumeControls() {
        VBox volumeBox = new VBox(10);
        volumeBox.setAlignment(Pos.CENTER);
        
        Label volumeLabel = new Label("Game Volume");
        volumeLabel.getStyleClass().add("settings-label");
        
        volumeSlider = new Slider(0, 1, audioManager.getVolume());
        volumeSlider.setPrefWidth(300);
        volumeSlider.getStyleClass().add("volume-slider");
        volumeSlider.setStyle("-fx-background-color: transparent;");
        
        styleVolumeSlider();
        
        Label volumeValueLabel = new Label(formatVolumeLabel(volumeSlider.getValue()));
        volumeValueLabel.getStyleClass().add("volume-value-label");
        
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            audioManager.setVolume(newVal.doubleValue());
            volumeValueLabel.setText(formatVolumeLabel(newVal.doubleValue()));
            
            if (Math.abs(newVal.doubleValue() - oldVal.doubleValue()) > 0.05) {
                audioManager.playSoundEffect(SoundType.BUTTON_CLICK);
            }
        });
        
        HBox sliderBox = new HBox(10);
        sliderBox.setAlignment(Pos.CENTER);
        sliderBox.getChildren().addAll(volumeSlider, volumeValueLabel);
        
        volumeBox.getChildren().addAll(volumeLabel, sliderBox);
        return volumeBox;
    }
    
    private String formatVolumeLabel(double value) {
        return String.format("%d%%", (int)(value * 100));
    }
    
    private void styleVolumeSlider() {
        volumeSlider.applyCss();
        volumeSlider.layout();
        
        javafx.scene.Node track = volumeSlider.lookup(".track");
        if (track != null) {
            track.setStyle("-fx-background-color: rgba(255, 255, 255, 0.3); -fx-background-radius: 10; -fx-pref-height: 6;");
        }
        
        javafx.scene.Node thumb = volumeSlider.lookup(".thumb");
        if (thumb != null) {
            thumb.setStyle("-fx-background-color: white; -fx-background-radius: 50%; -fx-pref-width: 16; -fx-pref-height: 16; -fx-effect: dropshadow(three-pass-box, rgba(0, 0, 0, 0.4), 5, 0, 0, 0);");
        }
    }
    
    private Button createBackButton() {
        Button backButton = new Button("Back");
        backButton.getStyleClass().add("menu-button");
        backButton.setMaxWidth(200);
        backButton.setPrefHeight(50);
        backButton.setStyle("-fx-background-color: rgba(22, 33, 62, 0.9); -fx-border-color: #e94560; -fx-border-width: 2; -fx-text-fill: white; -fx-font-size: 16px; -fx-background-radius: 8; -fx-border-radius: 8;");
        backButton.setOnAction(e -> {
            audioManager.playSoundEffect(SoundType.BUTTON_CLICK);
            gameController.returnToMainMenu();
        });
        return backButton;
    }
    
    private void setupSceneActivation() {
        scene.windowProperty().addListener((obs, oldWindow, newWindow) -> {
            if (newWindow != null) {
                newWindow.setOnShown(e -> activateControls());
                scene.focusOwnerProperty().addListener((focusObs, oldFocus, newFocus) -> {
                    if (newFocus != null && newFocus.getScene() == scene) {
                        activateControls();
                    }
                });
            }
        });
    }
    
    public void activateControls() {
        leftKeyPressed = false;
        rightKeyPressed = false;
        volumeAdjustTimer.stop();
        reattachKeyboardHandlers();
        javafx.application.Platform.runLater(() -> volumeSlider.requestFocus());
    }
    
    private void reattachKeyboardHandlers() {
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressHandler);  
        scene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleaseHandler);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleaseHandler);
    }
    
    private void handleKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            leftKeyPressed = true;
            volumeAdjustTimer.start();
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            rightKeyPressed = true;
            volumeAdjustTimer.start();
            event.consume();
        }
    }
    
    private void handleKeyRelease(KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            leftKeyPressed = false;
            if (!rightKeyPressed) volumeAdjustTimer.stop();
            event.consume();
        } else if (event.getCode() == KeyCode.RIGHT) {
            rightKeyPressed = false;
            if (!leftKeyPressed) volumeAdjustTimer.stop();
            event.consume();
        }
    }
    
    private void setupVolumeAdjustmentTimer() {
        volumeAdjustTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 33_000_000) {
                    double currentValue = volumeSlider.getValue();
                    double change = 0.005;
                    
                    if (rightKeyPressed && !leftKeyPressed) {
                        volumeSlider.setValue(Math.min(1.0, currentValue + change));
                    } else if (leftKeyPressed && !rightKeyPressed) {
                        volumeSlider.setValue(Math.max(0.0, currentValue - change));
                    }
                    
                    lastUpdate = now;
                }
            }
        };
    }
    
    private void setupKeyboardHandlers() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressHandler);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleaseHandler);
    }
    
    public Scene getScene() {
        javafx.application.Platform.runLater(this::activateControls);
        return scene;
    }
}
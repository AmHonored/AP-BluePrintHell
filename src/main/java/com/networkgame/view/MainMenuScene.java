package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.model.AudioManager;
import com.networkgame.model.AudioManager.SoundType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class MainMenuScene {
    private final Scene scene;
    private final GameController gameController;
    
    public MainMenuScene(GameController gameController) {
        this.gameController = gameController;
        
        VBox mainLayout = createMainLayout();
        Label titleLabel = createTitleLabel();
        
        mainLayout.getChildren().addAll(
            titleLabel,
            createStartButton(),
            createLevelsButton(),
            createSettingsButton(),
            createExitButton()
        );
        
        scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }
    
    private VBox createMainLayout() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(50, 0, 50, 0));
        layout.getStyleClass().add("main-menu");
        return layout;
    }
    
    private Label createTitleLabel() {
        Label label = new Label("Blueprint Hell");
        label.getStyleClass().add("title-label");
        return label;
    }
    
    private Button createMenuButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        return button;
    }
    
    private Button createStartButton() {
        Button button = createMenuButton("Start Game");
        button.setOnAction(e -> {
            playButtonSound();
            gameController.startGame(1);
        });
        return button;
    }
    
    private Button createLevelsButton() {
        Button button = createMenuButton("Game Levels");
        button.setOnAction(e -> {
            playButtonSound();
            gameController.showLevelSelect();
        });
        return button;
    }
    
    private Button createSettingsButton() {
        Button button = createMenuButton("Game Settings");
        button.setOnAction(e -> {
            playButtonSound();
            gameController.showSettings();
        });
        return button;
    }
    
    private Button createExitButton() {
        Button button = createMenuButton("Exit Game");
        button.setOnAction(e -> {
            playButtonSound();
            javafx.application.Platform.exit();
        });
        return button;
    }
    
    private void playButtonSound() {
        AudioManager.getInstance().playSoundEffect(SoundType.BUTTON_CLICK);
    }
    
    public Scene getScene() {
        return scene;
    }
} 
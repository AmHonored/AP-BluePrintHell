package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.service.audio.AudioManager;
import com.networkgame.service.audio.AudioManager.SoundType;
import com.networkgame.model.manager.LevelManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class LevelSelectScene {
    private final Scene scene;
    private final GameController gameController;
    private final LevelManager levelManager;
    
    public LevelSelectScene(GameController gameController, LevelManager levelManager) {
        this.gameController = gameController;
        this.levelManager = levelManager;
        
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));
        mainLayout.getStyleClass().add("main-menu");
        
        Label titleLabel = new Label("Level Select");
        titleLabel.getStyleClass().add("title-label");
        
        GridPane levelGrid = new GridPane();
        levelGrid.setAlignment(Pos.CENTER);
        levelGrid.setHgap(20);
        levelGrid.setVgap(20);
        
        populateLevelGrid(levelGrid);
        
        ScrollPane scrollPane = new ScrollPane(levelGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        Button backButton = new Button("Back to Menu");
        backButton.getStyleClass().add("menu-button");
        backButton.setOnAction(e -> {
            AudioManager.getInstance().playSoundEffect(SoundType.BUTTON_CLICK);
            gameController.returnToMainMenu();
        });
        
        mainLayout.getChildren().addAll(titleLabel, scrollPane, backButton);
        
        scene = new Scene(mainLayout, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
    }
    
    private void populateLevelGrid(GridPane grid) {
        int maxUnlockedLevel = levelManager.getMaxUnlockedLevel();
        int totalLevels = levelManager.getTotalLevels();
        int columns = 3;
        
        for (int i = 1; i <= totalLevels; i++) {
            LevelManager.Level level = levelManager.getLevel(i);
            boolean isUnlocked = i <= maxUnlockedLevel;
            
            VBox levelBox = new VBox(10);
            levelBox.setAlignment(Pos.CENTER);
            levelBox.setPadding(new Insets(15));
            levelBox.setPrefSize(180, 180);
            levelBox.getStyleClass().add("shop-item");
            
            if (!isUnlocked) levelBox.setStyle("-fx-opacity: 0.7;");
            
            Label levelNumber = new Label("Level " + level.getLevelNumber());
            levelNumber.getStyleClass().add("shop-item-title");
            
            Label levelName = new Label(level.getName());
            levelName.getStyleClass().add("shop-item-desc");
            
            Button playButton = new Button(isUnlocked ? "Play" : "Locked");
            playButton.setDisable(!isUnlocked);
            
            if (isUnlocked) {
                playButton.setOnAction(e -> {
                    AudioManager.getInstance().playSoundEffect(SoundType.BUTTON_CLICK);
                    gameController.startGame(level.getLevelNumber());
                });
            }
            
            levelBox.getChildren().addAll(levelNumber, levelName, playButton);
            grid.add(levelBox, (i - 1) % columns, (i - 1) / columns);
        }
    }
    
    public Scene getScene() {
        return scene;
    }
} 

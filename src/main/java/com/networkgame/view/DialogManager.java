package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.model.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;

import com.networkgame.model.entity.Packet;
import com.networkgame.model.state.GameState;
import com.networkgame.model.entity.system.NetworkSystem;
import com.networkgame.model.entity.packettype.messenger.SquarePacket;
import com.networkgame.model.entity.packettype.messenger.TrianglePacket;
import com.networkgame.service.audio.AudioManager;

/**
 * Handles all dialog-related functionality for the game
 */
public class DialogManager {
    private Pane gamePane;
    private GameController gameController;

    public DialogManager(Pane gamePane, GameController gameController) {
        this.gamePane = gamePane;
        this.gameController = gameController;
    }

    /**
     * Show success message and proceed to next level
     */
    public void showSuccessMessage() {
        // Pause the game
        gameController.pauseGame();
        
        // Create semi-transparent background overlay
        javafx.scene.shape.Rectangle overlayBg = new javafx.scene.shape.Rectangle(
            0, 0, gamePane.getWidth(), gamePane.getHeight());
        overlayBg.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.7));
        
        // Create the alert dialog
        VBox successPopup = new VBox(15);
        successPopup.setAlignment(Pos.CENTER);
        successPopup.setPadding(new Insets(30));
        successPopup.setMinWidth(400);
        successPopup.setMaxWidth(400);
        successPopup.setMinHeight(200);
        successPopup.setStyle("-fx-background-color: #1a3a6e; -fx-background-radius: 10; -fx-border-color: #e94560; -fx-border-width: 3; -fx-border-radius: 10;");
        
        Label successLabel = new Label("Success!");
        successLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #50fa7b;");
        
        Label messageLabel = new Label("All systems are connected correctly!");
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        messageLabel.setWrapText(true);
        
        Button nextButton = new Button("Next Level");
        nextButton.setMinWidth(200);
        nextButton.setMinHeight(40);
        nextButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        nextButton.setOnAction(e -> {
            gamePane.getChildren().removeAll(overlayBg, successPopup);
            gameController.levelCompleted();
        });
        
        // Add hover effect to button
        nextButton.setOnMouseEntered(e -> nextButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        nextButton.setOnMouseExited(e -> nextButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        successPopup.getChildren().addAll(successLabel, messageLabel, nextButton);
        
        // Center the popup
        successPopup.setLayoutX(gamePane.getWidth()/2 - 200);
        successPopup.setLayoutY(gamePane.getHeight()/2 - 100);
        
        // Add drop shadow effect
        javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(javafx.scene.paint.Color.BLACK);
        successPopup.setEffect(dropShadow);
        
        gamePane.getChildren().addAll(overlayBg, successPopup);
    }
    
    /**
     * Show error message
     */
    public void showErrorMessage(String message) {
        // Create semi-transparent background overlay
        javafx.scene.shape.Rectangle overlayBg = new javafx.scene.shape.Rectangle(
            0, 0, gamePane.getWidth(), gamePane.getHeight());
        overlayBg.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.7));
        
        // Create the alert dialog
        VBox errorPopup = new VBox(15);
        errorPopup.setAlignment(Pos.CENTER);
        errorPopup.setPadding(new Insets(30));
        errorPopup.setMinWidth(400);
        errorPopup.setMaxWidth(400);
        errorPopup.setMinHeight(200);
        errorPopup.setStyle("-fx-background-color: #1a3a6e; -fx-background-radius: 10; -fx-border-color: #e94560; -fx-border-width: 3; -fx-border-radius: 10;");
        
        Label errorLabel = new Label("Error!");
        errorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #ff5555;");
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        messageLabel.setWrapText(true);
        
        Button okButton = new Button("OK");
        okButton.setMinWidth(200);
        okButton.setMinHeight(40);
        okButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        okButton.setOnAction(e -> {
            gamePane.getChildren().removeAll(overlayBg, errorPopup);
        });
        
        // Add hover effect to button
        okButton.setOnMouseEntered(e -> okButton.setStyle("-fx-background-color: #d63651; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        okButton.setOnMouseExited(e -> okButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        errorPopup.getChildren().addAll(errorLabel, messageLabel, okButton);
        
        // Center the popup
        errorPopup.setLayoutX(gamePane.getWidth()/2 - 200);
        errorPopup.setLayoutY(gamePane.getHeight()/2 - 100);
        
        // Add drop shadow effect
        javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(javafx.scene.paint.Color.BLACK);
        errorPopup.setEffect(dropShadow);
        
        gamePane.getChildren().addAll(overlayBg, errorPopup);
    }
    
    /**
     * Show game over screen when time's up but not enough packets delivered
     */
    public void showTimeUpGameOver(int packetsDelivered, int requiredPackets) {
        // Create semi-transparent background overlay
        javafx.scene.shape.Rectangle overlayBg = new javafx.scene.shape.Rectangle(
            0, 0, gamePane.getWidth(), gamePane.getHeight());
        overlayBg.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.8));
        
        // Create the alert dialog
        VBox gameOverPopup = new VBox(15);
        gameOverPopup.setAlignment(Pos.CENTER);
        gameOverPopup.setPadding(new Insets(30));
        gameOverPopup.setMinWidth(450);
        gameOverPopup.setMaxWidth(450);
        gameOverPopup.setMinHeight(250);
        gameOverPopup.setStyle("-fx-background-color: #1a3a6e; -fx-background-radius: 10; -fx-border-color: #ff0000; -fx-border-width: 3; -fx-border-radius: 10;");
        
        Label gameOverLabel = new Label("Time's Up!");
        gameOverLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #ff5555;");
        
        Label messageLabel = new Label(String.format(
            "You delivered %d out of %d required packets. Try to optimize your network to deliver packets faster!",
            packetsDelivered, requiredPackets
        ));
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        messageLabel.setWrapText(true);
        
        Button retryButton = new Button("Retry Level");
        retryButton.setMinWidth(200);
        retryButton.setMinHeight(40);
        retryButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        retryButton.setOnAction(e -> {
            gamePane.getChildren().removeAll(overlayBg, gameOverPopup);
            gameController.restartCurrentLevel();
        });
        
        Button menuButton = new Button("Main Menu");
        menuButton.setMinWidth(200);
        menuButton.setMinHeight(40);
        menuButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        menuButton.setOnAction(e -> {
            gamePane.getChildren().removeAll(overlayBg, gameOverPopup);
            gameController.returnToMainMenu();
        });
        
        // Add hover effects
        retryButton.setOnMouseEntered(e -> retryButton.setStyle("-fx-background-color: #d63651; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        retryButton.setOnMouseExited(e -> retryButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        menuButton.setOnMouseEntered(e -> menuButton.setStyle("-fx-background-color: #1c2e40; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        menuButton.setOnMouseExited(e -> menuButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        // Create a container for buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(retryButton, menuButton);
        
        gameOverPopup.getChildren().addAll(gameOverLabel, messageLabel, buttonBox);
        
        // Center the popup
        gameOverPopup.setLayoutX(gamePane.getWidth()/2 - 225);
        gameOverPopup.setLayoutY(gamePane.getHeight()/2 - 125);
        
        // Add drop shadow effect
        javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(javafx.scene.paint.Color.BLACK);
        gameOverPopup.setEffect(dropShadow);
        
        // Pause the game
        gameController.pauseGame();
        
        gamePane.getChildren().addAll(overlayBg, gameOverPopup);
    }
    
    /**
     * Display game over screen when system capacity exceeded
     */
    public void showCapacityExceededGameOver(NetworkSystem system) {
        // Create semi-transparent background overlay
        javafx.scene.shape.Rectangle overlayBg = new javafx.scene.shape.Rectangle(
            0, 0, gamePane.getWidth(), gamePane.getHeight());
        overlayBg.setFill(javafx.scene.paint.Color.rgb(0, 0, 0, 0.8));
        
        // Create the alert dialog
        VBox gameOverPopup = new VBox(15);
        gameOverPopup.setAlignment(Pos.CENTER);
        gameOverPopup.setPadding(new Insets(30));
        gameOverPopup.setMinWidth(450);
        gameOverPopup.setMaxWidth(450);
        gameOverPopup.setMinHeight(250);
        gameOverPopup.setStyle("-fx-background-color: #1a3a6e; -fx-background-radius: 10; -fx-border-color: #ff0000; -fx-border-width: 3; -fx-border-radius: 10;");
        
        Label gameOverLabel = new Label("Game Over!");
        gameOverLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #ff5555;");
        
        Label messageLabel = new Label("System capacity exceeded! A system can only hold up to 5 capacity units.");
        messageLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white;");
        messageLabel.setWrapText(true);
        
        Button retryButton = new Button("Retry Level");
        retryButton.setMinWidth(200);
        retryButton.setMinHeight(40);
        retryButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        retryButton.setOnAction(e -> {
            gamePane.getChildren().removeAll(overlayBg, gameOverPopup);
            gameController.restartCurrentLevel();
        });
        
        Button menuButton = new Button("Main Menu");
        menuButton.setMinWidth(200);
        menuButton.setMinHeight(40);
        menuButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        menuButton.setOnAction(e -> {
            gamePane.getChildren().removeAll(overlayBg, gameOverPopup);
            gameController.returnToMainMenu();
        });
        
        // Add hover effects
        retryButton.setOnMouseEntered(e -> retryButton.setStyle("-fx-background-color: #d63651; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        retryButton.setOnMouseExited(e -> retryButton.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        menuButton.setOnMouseEntered(e -> menuButton.setStyle("-fx-background-color: #1c2e40; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        menuButton.setOnMouseExited(e -> menuButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        // Create a container for buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(retryButton, menuButton);
        
        gameOverPopup.getChildren().addAll(gameOverLabel, messageLabel, buttonBox);
        
        // Center the popup
        gameOverPopup.setLayoutX(gamePane.getWidth()/2 - 225);
        gameOverPopup.setLayoutY(gamePane.getHeight()/2 - 125);
        
        // Add drop shadow effect
        javafx.scene.effect.DropShadow dropShadow = new javafx.scene.effect.DropShadow();
        dropShadow.setRadius(10.0);
        dropShadow.setOffsetX(3.0);
        dropShadow.setOffsetY(3.0);
        dropShadow.setColor(javafx.scene.paint.Color.BLACK);
        gameOverPopup.setEffect(dropShadow);
        
        // Pause the game
        gameController.pauseGame();
        
        gamePane.getChildren().addAll(overlayBg, gameOverPopup);
    }
    
    /**
     * Display generic game over screen
     */
    public void showGameOver(double packetLossPercentage, int successfulPackets) {
        // Create game over dialog
        VBox gameOverBox = new VBox(15);
        gameOverBox.setAlignment(Pos.CENTER);
        gameOverBox.setPadding(new Insets(30));
        gameOverBox.getStyleClass().add("game-over-dialog");
        
        // Title
        Label titleLabel = new Label("GAME OVER");
        titleLabel.getStyleClass().add("game-over-title");
        
        // Create packet stats display
        VBox statsBox = new VBox(10);
        statsBox.setAlignment(Pos.CENTER);
        
        // Packet loss percentage (as required in document)
        Label lossLabel = new Label(String.format("Packet Loss: %.1f%%", packetLossPercentage));
        lossLabel.getStyleClass().add("game-over-stat");
        
        // Successful packets (as required in document)
        Label deliveredLabel = new Label("Packets Delivered: " + successfulPackets);
        deliveredLabel.getStyleClass().add("game-over-stat");
        
        statsBox.getChildren().addAll(lossLabel, deliveredLabel);
        
        // Create buttons
        HBox buttonBox = new HBox(20);
        buttonBox.setAlignment(Pos.CENTER);
        
        // Return to menu button (as required in document)
        Button menuButton = new Button("Return to Menu");
        menuButton.getStyleClass().add("game-over-button");
        menuButton.setOnAction(e -> {
            gameController.returnToMainMenu();
        });
        
        // Retry button
        Button retryButton = new Button("Retry Level");
        retryButton.getStyleClass().add("game-over-button");
        retryButton.setOnAction(e -> {
            gameController.restartCurrentLevel();
        });
        
        buttonBox.getChildren().addAll(retryButton, menuButton);
        
        // Add all components to the container
        gameOverBox.getChildren().addAll(titleLabel, statsBox, buttonBox);
        
        // Add the game over box to the game pane as a modal dialog
        gameOverBox.setLayoutX((gamePane.getWidth() - 400) / 2);
        gameOverBox.setLayoutY((gamePane.getHeight() - 300) / 2);
        gameOverBox.setPrefWidth(400);
        gameOverBox.setPrefHeight(300);
        
        // Set a background with semi-transparency
        gameOverBox.setStyle("-fx-background-color: rgba(30, 30, 40, 0.9); -fx-background-radius: 10;");
        
        // Play game over sound
        AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.PACKET_DAMAGE);
        
        // Add to game pane
        gamePane.getChildren().add(gameOverBox);
    }
    
    /**
     * Show level complete dialog
     */
    public void showLevelComplete(int level, GameState gameState) {
        // Stop all game-related audio except the level complete sound
        AudioManager audioManager = AudioManager.getInstance();
        audioManager.stopBackgroundMusic();
        
        // Play level complete sound
        audioManager.playSoundEffect(AudioManager.SoundType.LEVEL_COMPLETE);
        
        // Pause game updates
        gameController.pauseGame();
        
        // Clear active packets to prevent them from showing in the background
        gameState.getActivePackets().clear();
        
        // Ensure timer is completely stopped
        gameState.stopTimer();
        
        // Stop all systems from generating packets
        for (NetworkSystem system : gameState.getSystems()) {
            if (system.isStartSystem()) {
                system.stopSendingPackets();
            }
        }
        
        // Ensure this scene is added at the topmost z-index
        int topZIndex = Integer.MAX_VALUE;
        
        // Create fully opaque overlay background
        javafx.scene.shape.Rectangle overlayBg = new javafx.scene.shape.Rectangle(
            0, 0, gamePane.getWidth(), gamePane.getHeight());
        overlayBg.setFill(javafx.scene.paint.Color.rgb(0, 0, 50, 1.0));
        
        // Set the highest z-index to ensure it covers everything
        javafx.scene.layout.StackPane.setAlignment(overlayBg, Pos.CENTER);
        overlayBg.setViewOrder(-topZIndex); // Lower viewOrder means higher z-index
        
        // Create popup container with clean UI design
        VBox overlay = new VBox(20);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(30));
        overlay.setMinWidth(400);
        overlay.setMaxWidth(400);
        overlay.setMinHeight(350);
        overlay.setStyle("-fx-background-color: #1a3a6e; -fx-background-radius: 10; -fx-border-color: #e94560; -fx-border-width: 3; -fx-border-radius: 10;");
        
        // Add a simple glow effect to the popup
        javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
        glow.setColor(javafx.scene.paint.Color.rgb(233, 69, 96, 0.8));
        glow.setRadius(15);
        overlay.setEffect(glow);
        overlay.setViewOrder(-topZIndex - 1); // Ensure it's above the background
        
        // Create simple level complete title
        Label completeLabel = new Label("Level " + level + " Complete!");
        completeLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #ff79c6;");
        
        // Create coins earned label
        Label coinsEarnedLabel = new Label("Coins Earned: " + gameState.getCoins());
        coinsEarnedLabel.setStyle("-fx-font-size: 20px; -fx-text-fill: #f1fa8c;");
        
        // Create statistics label
        Label statsLabel = new Label(String.format("Packet Loss: %.1f%%", gameState.getPacketLossPercentage()));
        statsLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: white;");
        
        // Create a HBox for buttons to align them in a row
        HBox buttonsBox = new HBox(20);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(15, 0, 0, 0));
        
        // Create next level button
        Button nextLevelButton = new Button("Next Level");
        nextLevelButton.setMinWidth(150);
        nextLevelButton.setMinHeight(40);
        nextLevelButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        
        // Create menu button
        Button menuButton = new Button("Back to Menu");
        menuButton.setMinWidth(150);
        menuButton.setMinHeight(40);
        menuButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;");
        
        // Add hover effects
        nextLevelButton.setOnMouseEntered(e -> nextLevelButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        nextLevelButton.setOnMouseExited(e -> nextLevelButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        menuButton.setOnMouseEntered(e -> menuButton.setStyle("-fx-background-color: #1c2e40; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        menuButton.setOnMouseExited(e -> menuButton.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-background-radius: 5;"));
        
        // Add button handlers
        nextLevelButton.setOnAction(e -> {
            AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.BUTTON_CLICK);
            gameController.startGame(level + 1);
        });
        
        menuButton.setOnAction(e -> {
            AudioManager.getInstance().playSoundEffect(AudioManager.SoundType.BUTTON_CLICK);
            
            // Ensure the next level is unlocked even when going back to menu
            int nextLevel = level + 1;
            if (nextLevel <= gameController.getLevelManager().getTotalLevels()) {
                gameController.getLevelManager().unlockLevel(nextLevel);
            }
            
            gameController.returnToMainMenu();
        });
        
        // Add buttons to button box
        buttonsBox.getChildren().addAll(nextLevelButton, menuButton);
        
        // Add elements to overlay
        overlay.getChildren().addAll(completeLabel, coinsEarnedLabel, statsLabel, buttonsBox);
        
        // Center the popup
        overlay.setLayoutX(gamePane.getWidth()/2 - 200);
        overlay.setLayoutY(gamePane.getHeight()/2 - 175);
        
        // Add overlay background and popup to game pane with proper z-order
        gamePane.getChildren().addAll(overlayBg, overlay);
        
        // Ensure these elements are at the top of the display list
        overlayBg.toFront();
        overlay.toFront();
    }
} 
package com.networkgame.view;

import com.networkgame.controller.GameController;
import com.networkgame.model.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.Cursor;

/**
 * Factory class for creating UI elements in the game
 */
public class UIComponentFactory {
    private GameState gameState;
    private GameController gameController;
    
    // Style constants
    private static final String STATS_BOX_STYLE = "stats-box";
    private static final String STATS_TITLE_STYLE = "stats-title";
    private static final String STATS_VALUE_STYLE = "stats-value";
    private static final String MENU_BUTTON_STYLE = "menu-button";
    private static final int DEFAULT_SPACING = 5;
    private static final int WIDE_SPACING = 20;
    private static final int DEFAULT_PADDING = 10;

    public UIComponentFactory(GameState gameState, GameController gameController) {
        this.gameState = gameState;
        this.gameController = gameController;
    }

    /**
     * Create the HUD (Heads Up Display) at the top of the game
     */
    public HBox createHUD(Label wireLabel, Label outOfWireLabel, Label timeLabel, Label packetLossLabel, 
                          Label coinsLabel, Label packetsCollectedLabel, 
                          ProgressBar timeProgressBar, Circle timeProgressThumb, TextField timeInputField) {
        HBox hudPane = createContainer(WIDE_SPACING, Pos.CENTER, new Insets(DEFAULT_PADDING));
        
        // Create HUD components
        VBox wireBox = createWireIndicator(wireLabel, outOfWireLabel);
        VBox timeBox = createTimeIndicator(timeLabel, timeProgressBar, timeProgressThumb, timeInputField);
        VBox lossBox = createPacketLossIndicator(packetLossLabel);
        VBox coinsBox = createCoinsIndicator(coinsLabel);
        VBox packetsBox = createPacketsCollectedIndicator(packetsCollectedLabel);
        
        // Add all components to HUD pane
        hudPane.getChildren().addAll(wireBox, timeBox, lossBox, coinsBox, packetsBox);
        
        return hudPane;
    }
    
    /**
     * Create controls at the bottom of the game screen
     */
    public HBox createControls(Button toggleButton) {
        HBox controlsPane = createContainer(WIDE_SPACING, Pos.CENTER, new Insets(DEFAULT_PADDING));
        
        // Shop button
        Button shopButton = createStyledButton("Shop", MENU_BUTTON_STYLE);
        shopButton.setOnAction(e -> gameController.showShop());
        
        // Pause/Resume button
        stylePauseResumeButton(toggleButton);
        
        // Menu button
        Button menuButton = createStyledButton("Menu", MENU_BUTTON_STYLE);
        menuButton.setOnAction(e -> gameController.returnToMainMenu());
        
        // Add buttons to controls
        controlsPane.getChildren().addAll(shopButton, toggleButton, menuButton);
        
        return controlsPane;
    }
    
    /**
     * Create a play button icon integrated with the start system
     */
    public Group createPlayButton(double x, double y) {
        Group group = new Group();
        
        // Create button background
        Rectangle background = new Rectangle(x, y, 30, 30);
        background.setFill(Color.rgb(30, 70, 45));
        background.setStroke(Color.rgb(66, 163, 83));
        background.setStrokeWidth(1.5);
        
        // Create play icon (triangle)
        Polygon playIcon = new Polygon();
        playIcon.getPoints().addAll(
            x + 10, y + 8,   // Top left
            x + 10, y + 22,  // Bottom left
            x + 24, y + 15   // Right
        );
        playIcon.setFill(Color.rgb(150, 255, 150));
        
        // Add both shapes to group
        group.getChildren().addAll(background, playIcon);
        
        // Add hover effects
        setupPlayButtonInteractions(group, background, playIcon);
        
        return group;
    }
    
    /**
     * Creates a star shape for visual effects
     */
    public Polygon createStar(double centerX, double centerY, double outerRadius, double innerRadius) {
        Polygon star = new Polygon();
        double angle = Math.PI / 5;
        
        for (int i = 0; i < 10; i++) {
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double x = centerX + radius * Math.sin(i * angle);
            double y = centerY - radius * Math.cos(i * angle);
            star.getPoints().addAll(x, y);
        }
        
        return star;
    }
    
    /**
     * Shows a pause indicator overlay on the game screen
     */
    public VBox createPauseIndicator(double paneWidth, double paneHeight) {
        VBox pauseIndicator = createOverlayContainer(300, 150, paneWidth, paneHeight, 
                                                    "rgba(0, 0, 0, 0.7)", 10);
        
        Label pauseLabel = createStyledLabelWithInlineStyle("GAME PAUSED", 
                                           "-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");
        
        Label instructionLabel = createStyledLabelWithInlineStyle("Press SPACE to resume", 
                                                 "-fx-text-fill: white; -fx-font-size: 16px;");
        
        pauseIndicator.getChildren().addAll(pauseLabel, instructionLabel);
        
        return pauseIndicator;
    }
    
    /**
     * Creates a temporal control mode indicator
     */
    public VBox createTemporalControlIndicator(double paneWidth, double paneHeight) {
        VBox temporalControlIndicator = createOverlayContainer(350, 180, paneWidth, paneHeight,
                                                              "rgba(0, 50, 100, 0.7)", 10);
        
        Label modeLabel = createStyledLabelWithInlineStyle("TEMPORAL CONTROL MODE", 
                                          "-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        
        Label instructionLabel = createStyledLabelWithInlineStyle("Use LEFT/RIGHT ARROW KEYS to control time", 
                                                 "-fx-text-fill: white; -fx-font-size: 14px;");
        
        Label exitLabel = createStyledLabelWithInlineStyle("Press TAB to exit temporal control mode", 
                                          "-fx-text-fill: white; -fx-font-size: 14px;");
        
        temporalControlIndicator.getChildren().addAll(modeLabel, instructionLabel, exitLabel);
        
        return temporalControlIndicator;
    }
    
    // PRIVATE HELPER METHODS
    
    private HBox createContainer(int spacing, Pos alignment, Insets padding) {
        HBox container = new HBox(spacing);
        container.setAlignment(alignment);
        container.setPadding(padding);
        return container;
    }
    
    private VBox createStatsBox(String title, Label valueLabel, String... additionalStyles) {
        VBox statsBox = new VBox(DEFAULT_SPACING);
        statsBox.setAlignment(Pos.CENTER);
        statsBox.getStyleClass().add(STATS_BOX_STYLE);
        
        Label titleLabel = createStyledLabel(title, STATS_TITLE_STYLE);
        
        valueLabel.getStyleClass().add(STATS_VALUE_STYLE);
        for (String style : additionalStyles) {
            valueLabel.getStyleClass().add(style);
        }
        
        statsBox.getChildren().addAll(titleLabel, valueLabel);
        return statsBox;
    }
    
    private Label createStyledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }
    
    private Label createStyledLabelWithInlineStyle(String text, String inlineStyle) {
        Label label = new Label(text);
        label.setStyle(inlineStyle);
        return label;
    }
    
    private Button createStyledButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }
    
    private VBox createOverlayContainer(double width, double height, double paneWidth, double paneHeight, 
                                      String backgroundColor, double radius) {
        VBox overlay = new VBox(DEFAULT_PADDING);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPrefWidth(width);
        overlay.setPrefHeight(height);
        overlay.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: " + radius + ";");
        overlay.setLayoutX((paneWidth - width) / 2);
        overlay.setLayoutY((paneHeight - height) / 2);
        return overlay;
    }
    
    private VBox createWireIndicator(Label wireLabel, Label outOfWireLabel) {
        VBox wireBox = new VBox(DEFAULT_SPACING);
        wireBox.setAlignment(Pos.CENTER);
        wireBox.getStyleClass().add(STATS_BOX_STYLE);
        
        Label wireTitleLabel = createStyledLabel("Remaining Wire", STATS_TITLE_STYLE);
        
        wireLabel.getStyleClass().add("wire-label");
        wireLabel.setLayoutX(10);
        wireLabel.setLayoutY(10);
        
        outOfWireLabel.getStyleClass().add("out-of-wire");
        outOfWireLabel.setVisible(false);
        
        wireBox.getChildren().addAll(wireTitleLabel, wireLabel, outOfWireLabel);
        return wireBox;
    }
    
    private VBox createTimeIndicator(Label timeLabel, ProgressBar timeProgressBar, 
                                   Circle timeProgressThumb, TextField timeInputField) {
        VBox timeBox = new VBox(DEFAULT_SPACING);
        timeBox.setAlignment(Pos.CENTER);
        timeBox.getStyleClass().add(STATS_BOX_STYLE);
        timeBox.setPrefWidth(200);
        
        Label timeTitleLabel = createStyledLabelWithInlineStyle("Level Progress", 
                                               "-fx-text-fill: #42a3ff; -fx-font-weight: bold;");
        timeTitleLabel.getStyleClass().add(STATS_TITLE_STYLE);
        
        timeLabel.getStyleClass().add(STATS_VALUE_STYLE);
        timeLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00aaff;");
        
        VBox timeProgressContainer = createTimeProgressContainer(timeProgressBar, timeProgressThumb, timeInputField, timeLabel);
        
        timeBox.getChildren().addAll(timeTitleLabel, timeProgressContainer, timeLabel);
        return timeBox;
    }
    
    private VBox createTimeProgressContainer(ProgressBar timeProgressBar, Circle timeProgressThumb, 
                                           TextField timeInputField, Label timeLabel) {
        // Create container for the entire time progress component
        VBox timeProgressContainer = new VBox(DEFAULT_SPACING);
        timeProgressContainer.setAlignment(Pos.CENTER);
        
        // Style the progress bar
        timeProgressBar.setPrefWidth(180);
        timeProgressBar.setPrefHeight(10);
        timeProgressBar.setStyle("-fx-accent: linear-gradient(to right, #0088ff, #00aaff); " +
                                "-fx-control-inner-background: #1a2a3a; " + 
                                "-fx-background-radius: 5; -fx-border-radius: 5;");
        
        // Add glow effect to thumb
        DropShadow thumbGlow = new DropShadow();
        thumbGlow.setColor(Color.rgb(0, 170, 255, 0.7));
        thumbGlow.setRadius(6);
        timeProgressThumb.setEffect(thumbGlow);
        
        // Container for progress bar with thumb
        StackPane progressBarContainer = new StackPane();
        progressBarContainer.getChildren().addAll(timeProgressBar, timeProgressThumb);
        progressBarContainer.setPadding(new Insets(5, 0, 0, 0));
        
        // Add tick labels
        BorderPane ticksWithLabels = createTimelineTickLabels();
        
        // Configure time input field
        configureTimeInputField(timeInputField);
        
        // Create HBox for time input
        HBox timeInputContainer = new HBox(DEFAULT_SPACING);
        timeInputContainer.setAlignment(Pos.CENTER);
        
        Label inputLabel = createStyledLabelWithInlineStyle("Time:", "-fx-font-size: 12px; -fx-text-fill: #42a3ff; -fx-font-weight: bold;");
        
        timeInputContainer.getChildren().addAll(inputLabel, timeInputField);
        
        // Add elements to container
        timeProgressContainer.getChildren().addAll(progressBarContainer, ticksWithLabels, timeInputContainer);
        
        // Set up drag functionality
        setupTimeProgressDrag(progressBarContainer, timeProgressBar, timeProgressThumb, timeInputField, timeLabel);
        
        return timeProgressContainer;
    }
    
    private BorderPane createTimelineTickLabels() {
        BorderPane ticksWithLabels = new BorderPane();
        ticksWithLabels.setPrefWidth(180);
        
        String labelStyle = "-fx-font-size: 11px; -fx-font-weight: bold;";
        
        Label startLabel = createStyledLabelWithInlineStyle("Start", labelStyle);
        startLabel.setTextFill(Color.rgb(220, 220, 220));
        
        Label endLabel = createStyledLabelWithInlineStyle("End", labelStyle);
        endLabel.setTextFill(Color.rgb(220, 220, 220));
        
        ticksWithLabels.setLeft(startLabel);
        ticksWithLabels.setRight(endLabel);
        
        return ticksWithLabels;
    }
    
    private void configureTimeInputField(TextField timeInputField) {
        timeInputField.getStyleClass().add("time-input-field");
        
        // Allow only numeric input
        timeInputField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                timeInputField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        
        // Add action handler
        timeInputField.setOnAction(event -> {
            try {
                int timeValue = Integer.parseInt(timeInputField.getText());
                int maxTime = gameState.getLevelDuration();
                if (timeValue >= 0 && timeValue <= maxTime) {
                    gameController.travelInTime(timeValue);
                } else {
                    timeInputField.setText(String.valueOf(Math.min(Math.max(0, timeValue), maxTime)));
                }
            } catch (NumberFormatException e) {
                timeInputField.setText("0");
            }
        });
    }
    
    private VBox createPacketLossIndicator(Label packetLossLabel) {
        VBox lossBox = new VBox(DEFAULT_SPACING);
        lossBox.setAlignment(Pos.CENTER);
        lossBox.getStyleClass().add(STATS_BOX_STYLE);
        
        Label lossTitleLabel = createStyledLabel("Packet Loss", STATS_TITLE_STYLE);
        
        packetLossLabel.getStyleClass().addAll(STATS_VALUE_STYLE, "loss-label");
        packetLossLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ff5555;");
        
        lossBox.getChildren().addAll(lossTitleLabel, packetLossLabel);
        
        // Add note for level 1 win condition
        if (gameState.getCurrentLevel() == 1) {
            Label winConditionNote = createStyledLabelWithInlineStyle("(Keep < 50%)", "-fx-font-size: 10px; -fx-text-fill: #f8f8f2;");
            lossBox.getChildren().add(winConditionNote);
        }
        
        return lossBox;
    }
    
    private VBox createCoinsIndicator(Label coinsLabel) {
        VBox coinsBox = new VBox(DEFAULT_SPACING);
        coinsBox.setAlignment(Pos.CENTER);
        coinsBox.getStyleClass().add(STATS_BOX_STYLE);
        
        Label coinsTitleLabel = createStyledLabel("Coins", STATS_TITLE_STYLE);
        
        coinsLabel.getStyleClass().addAll(STATS_VALUE_STYLE, "coin-label");
        
        coinsBox.getChildren().addAll(coinsTitleLabel, coinsLabel);
        return coinsBox;
    }
    
    private VBox createPacketsCollectedIndicator(Label packetsCollectedLabel) {
        VBox packetsBox = new VBox(DEFAULT_SPACING);
        packetsBox.setAlignment(Pos.CENTER);
        packetsBox.getStyleClass().add(STATS_BOX_STYLE);
        
        Label packetsTitleLabel = createStyledLabel("Packets Collected", STATS_TITLE_STYLE);
        
        packetsCollectedLabel.getStyleClass().addAll(STATS_VALUE_STYLE, "packets-label");
        packetsCollectedLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #50fa7b;");
        
        packetsBox.getChildren().addAll(packetsTitleLabel, packetsCollectedLabel);
        return packetsBox;
    }
    
    private void setupPlayButtonInteractions(Group group, Rectangle background, Polygon playIcon) {
        group.setOnMouseEntered(e -> {
            background.setFill(Color.rgb(40, 90, 55));
            playIcon.setFill(Color.rgb(180, 255, 180));
            group.setCursor(Cursor.HAND);
        });
        
        group.setOnMouseExited(e -> {
            background.setFill(Color.rgb(30, 70, 45));
            playIcon.setFill(Color.rgb(150, 255, 150));
            group.setCursor(Cursor.DEFAULT);
        });
        
        group.setOnMousePressed(e -> {
            background.setFill(Color.rgb(20, 50, 35));
            playIcon.setFill(Color.rgb(120, 220, 120));
        });
        
        group.setOnMouseReleased(e -> {
            background.setFill(Color.rgb(40, 90, 55));
            playIcon.setFill(Color.rgb(180, 255, 180));
        });
    }
    
    private void stylePauseResumeButton(Button toggleButton) {
        toggleButton.getStyleClass().add(MENU_BUTTON_STYLE);
        toggleButton.setOnAction(e -> {
            if (gameController.isRunning()) {
                gameController.pauseGame();
                toggleButton.setText("Resume");
            } else {
                gameController.resumeGame();
                toggleButton.setText("Pause");
            }
        });
    }
    
    /**
     * Set up drag functionality for the time progress bar
     */
    private void setupTimeProgressDrag(StackPane progressBarContainer, ProgressBar timeProgressBar, 
                                      Circle timeProgressThumb, TextField timeInputField, Label timeLabel) {
        progressBarContainer.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                handleTimeProgressInteraction(event.getX(), timeProgressBar, timeProgressThumb, timeInputField, timeLabel);
            }
        });

        progressBarContainer.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                handleTimeProgressInteraction(event.getX(), timeProgressBar, timeProgressThumb, timeInputField, timeLabel);
            }
        });

        progressBarContainer.setOnMouseReleased(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                // When mouse is released, actually travel to the time
                try {
                    int timeValue = Integer.parseInt(timeInputField.getText());
                    gameController.travelInTime(timeValue);
                } catch (NumberFormatException e) {
                    // Ignore invalid values
                }
            }
        });
    }
    
    private void handleTimeProgressInteraction(double mouseX, ProgressBar timeProgressBar, 
                                            Circle timeProgressThumb, TextField timeInputField, Label timeLabel) {
        // Calculate position on the slider based on mouse position
        double trackWidth = timeProgressBar.getPrefWidth();
        
        // Clamp the position to the track bounds (0 to trackWidth)
        double clampedX = Math.max(0, Math.min(mouseX, trackWidth));
        
        // Calculate progress value (0.0 to 1.0)
        double progress = clampedX / trackWidth;
        
        // Update thumb position
        timeProgressThumb.setTranslateX((progress * trackWidth) - trackWidth/2);
        
        // Update progress bar
        timeProgressBar.setProgress(progress);
        
        // Calculate time value
        int timeValue = (int)(progress * gameState.getLevelDuration());
        
        // Update the time label
        updateTimeLabel(timeLabel, timeValue);
        
        // Update the input field
        timeInputField.setText(String.valueOf(timeValue));
    }
    
    private void updateTimeLabel(Label timeLabel, int timeValue) {
        int minutes = timeValue / 60;
        int seconds = timeValue % 60;
        timeLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }
} 
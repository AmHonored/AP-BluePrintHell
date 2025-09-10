package manager.game;

import javafx.scene.Scene;
import javafx.stage.Stage;
import view.menu.MenuScene;
import view.menu.LevelSelectScene;
import view.menu.SettingsScene;
// import view.game.GameScene; // not used
// import model.levels.Level; // not used
// import controller.GameController; // not used
// import manager.game.LevelManager; // class in same package

/**
 * VisualManager handles all scene and navigation logic for Blueprint Hell.
 * Follows SOLID and clean code principles. No business logic, just navigation/state.
 */
public class VisualManager {
    private final Stage primaryStage;
    private final String cssFile;
    private boolean level2Unlocked = true; // Unlocked by default for testing
    private double soundVolume = 100.0;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private LevelManager levelManager;
    
    // Room UI elements for updates
    private javafx.scene.layout.FlowPane currentPlayersFlow;
    private javafx.scene.layout.VBox currentLogsContainer;
    private javafx.scene.control.ScrollPane currentLogsScroll;

    public VisualManager(Stage primaryStage, String cssFile) {
        this.primaryStage = primaryStage;
        this.cssFile = cssFile;
        this.levelManager = new LevelManager(this, primaryStage, cssFile);
        
        // Listen for room updates from server
        try {
            net.NetworkService.getInstance().onRoomUpdate(this::handleRoomUpdate);
        } catch (Throwable ignored) {}
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public String getCssFile() {
        return cssFile;
    }

    /**
     * Show the main menu scene.
     */
    public void showMenu() {
        // Ensure any running game is stopped and crash autosave is halted/deleted
        if (levelManager != null) {
            levelManager.stopCurrentGame();
        }
        // Leave any active multiplayer room when returning to menu
        try { net.NetworkService.getInstance().leaveCurrentRoom(); } catch (Throwable ignored) {}
        MenuScene menuRoot = new MenuScene();
        Scene menuScene = new Scene(menuRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        menuScene.getStylesheets().add(cssFile);
        primaryStage.setScene(menuScene);
        
        // Start menu music
        service.AudioManager.playMenuMusic();

        menuRoot.getStartGameButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            try {
                if (net.NetworkService.getInstance().isConnected()) {
                    showMultiplayerDialog();
                } else {
                    showLevelSelect();
                }
            } catch (Throwable ignored) {
                showLevelSelect();
            }
        });
        // Networking controls (simple):
        try {
            java.lang.reflect.Method m1 = menuRoot.getClass().getMethod("addConnectionControls");
            m1.invoke(menuRoot);
        } catch (Throwable ignored) {}
        menuRoot.getLevelSelectButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showLevelSelect();
        });
        menuRoot.getSettingsButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showSettings();
        });
        menuRoot.getExitButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            // Ensure any running game is cleanly stopped and crash-autosave halted
            if (levelManager != null) {
                levelManager.stopCurrentGame();
            }
            primaryStage.close();
        });
    }

    /**
     * Show the level select scene.
     */
    public void showLevelSelect() {
        LevelSelectScene levelSelectRoot = new LevelSelectScene(level2Unlocked);
        Scene levelSelectScene = new Scene(levelSelectRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        levelSelectScene.getStylesheets().add(cssFile);
        primaryStage.setScene(levelSelectScene);

        levelSelectRoot.getLevel1Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(1);
        });
        levelSelectRoot.getLevel2Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            if (level2Unlocked) levelManager.showLevel(2);
        });
        levelSelectRoot.getLevel3Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(3);
        });
        levelSelectRoot.getLevel4Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(4);
        });
        levelSelectRoot.getLevel5Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(5);
        });
        levelSelectRoot.getLevel6Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(6);
        });
        levelSelectRoot.getLevel7Button().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            levelManager.showLevel(7);
        });
        try {
            java.lang.reflect.Method m = levelSelectRoot.getClass().getMethod("getLevel8Button");
            javafx.scene.control.Button level8Btn = (javafx.scene.control.Button) m.invoke(levelSelectRoot);
            if (level8Btn != null) {
                level8Btn.setOnAction(e -> {
                    service.AudioManager.playButtonClick();
                    levelManager.showLevel(8);
                });
            }
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Method m = levelSelectRoot.getClass().getMethod("getLevel9Button");
            javafx.scene.control.Button level9Btn = (javafx.scene.control.Button) m.invoke(levelSelectRoot);
            if (level9Btn != null) {
                level9Btn.setOnAction(e -> {
                    service.AudioManager.playButtonClick();
                    levelManager.showLevel(9);
                });
            }
        } catch (Throwable ignored) {}
        levelSelectRoot.getBackButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showMenu();
        });
    }

    /**
     * Show the settings scene.
     */
    public void showSettings() {
        SettingsScene settingsRoot = new SettingsScene(soundVolume);
        Scene settingsScene = new Scene(settingsRoot, WINDOW_WIDTH, WINDOW_HEIGHT);
        settingsScene.getStylesheets().add(cssFile);
        primaryStage.setScene(settingsScene);

        settingsRoot.getVolumeSlider().valueProperty().addListener((obs, oldVal, newVal) -> {
            soundVolume = newVal.doubleValue();
            // Connect to AudioManager
            service.AudioManager.setVolume(soundVolume / 100.0);
        });
        settingsRoot.getBackButton().setOnAction(e -> {
            service.AudioManager.playButtonClick();
            showMenu();
        });
    }

    private void showMultiplayerDialog() {
        // Create undecorated modal stage
        javafx.stage.Stage modalStage = new javafx.stage.Stage();
        modalStage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        modalStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modalStage.initOwner(primaryStage);

        // Main container with CSS styling
        javafx.scene.layout.VBox mainContainer = new javafx.scene.layout.VBox(20);
        mainContainer.getStyleClass().add("multiplayer-modal");
        mainContainer.setAlignment(javafx.geometry.Pos.CENTER);

        // Title
        javafx.scene.control.Label title = new javafx.scene.control.Label("Play Online");
        title.getStyleClass().add("multiplayer-title");

        // Text field for room code
        javafx.scene.control.TextField codeField = new javafx.scene.control.TextField();
        codeField.setPromptText("Enter room code");
        codeField.getStyleClass().add("multiplayer-code-field");

        // Button container
        javafx.scene.layout.HBox buttonContainer = new javafx.scene.layout.HBox(12);
        buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);

        // Create Room button (primary action)
        javafx.scene.control.Button createBtn = new javafx.scene.control.Button("Create Room");
        createBtn.getStyleClass().add("multiplayer-create-btn");

        // Join Room button (using level button style for secondary look)
        javafx.scene.control.Button joinBtn = new javafx.scene.control.Button("Join Room");
        joinBtn.getStyleClass().add("multiplayer-join-btn");

        buttonContainer.getChildren().addAll(createBtn, joinBtn);

        // Close button container (bottom right)
        javafx.scene.layout.HBox closeContainer = new javafx.scene.layout.HBox();
        closeContainer.setAlignment(javafx.geometry.Pos.BOTTOM_RIGHT);
        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Close");
        closeBtn.getStyleClass().add("multiplayer-close-btn");
        closeContainer.getChildren().add(closeBtn);

        // Add all components to main container
        mainContainer.getChildren().addAll(title, codeField, buttonContainer, closeContainer);

        // Create scene and apply styles
        javafx.scene.Scene modalScene = new javafx.scene.Scene(mainContainer, 400, 250);
        try { modalScene.getStylesheets().add(cssFile); } catch (Throwable ignored) {}
        modalStage.setScene(modalScene);

        // Center the modal on the parent stage
        modalStage.setX(primaryStage.getX() + (primaryStage.getWidth() - 400) / 2);
        modalStage.setY(primaryStage.getY() + (primaryStage.getHeight() - 250) / 2);

        // Button actions
        createBtn.setOnAction(e -> {
            service.AudioManager.playButtonClick();
            String desired = codeField.getText() == null ? "" : codeField.getText().trim();
            
            // Use server-side room creation
            if (net.NetworkService.getInstance().isConnected()) {
                net.NetworkService.getInstance().createRoom(desired);
                modalStage.close();
            } else {
                try { view.components.Toast.show(mainContainer, "Connect to server first", false); } catch (Throwable ignored2) {}
            }
        });

        joinBtn.setOnAction(e -> {
            service.AudioManager.playButtonClick();
            String code = codeField.getText() == null ? "" : codeField.getText().trim();
            if (code.isEmpty()) {
                return;
            }
            
            // Use server-side room joining
            if (net.NetworkService.getInstance().isConnected()) {
                net.NetworkService.getInstance().joinRoom(code);
                modalStage.close();
            } else {
                try { view.components.Toast.show(mainContainer, "Connect to server first", false); } catch (Throwable ignored2) {}
            }
        });

        closeBtn.setOnAction(e -> {
            service.AudioManager.playButtonClick();
            modalStage.close();
        });

        // Allow dragging the modal (since it's undecorated)
        final javafx.beans.property.DoubleProperty xOffset = new javafx.beans.property.SimpleDoubleProperty();
        final javafx.beans.property.DoubleProperty yOffset = new javafx.beans.property.SimpleDoubleProperty();

        mainContainer.setOnMousePressed(e -> {
            xOffset.set(e.getSceneX());
            yOffset.set(e.getSceneY());
        });

        mainContainer.setOnMouseDragged(e -> {
            modalStage.setX(e.getScreenX() - xOffset.get());
            modalStage.setY(e.getScreenY() - yOffset.get());
        });

        modalStage.showAndWait();
    }

    private void handleRoomUpdate(protocol.messages.RoomUpdate update) {
        if (!update.success) {
            // Show error message
            javafx.application.Platform.runLater(() -> {
                try {
                    if (primaryStage.getScene() != null && primaryStage.getScene().getRoot() != null) {
                        view.components.Toast.show(primaryStage.getScene().getRoot(), update.error, false);
                    }
                } catch (Throwable ignored) {}
            });
            return;
        }
        
        // Update existing room UI if it's open, otherwise show new room
        javafx.application.Platform.runLater(() -> {
            if (currentPlayersFlow != null && currentLogsContainer != null) {
                // Update existing room UI
                updateRoomUI(update.players, update.logs);
            } else {
                // Show new room
                showOnlineRoom(update.roomCode, update.players);
            }
        });
    }
    
    private void updateRoomUI(java.util.List<String> players, java.util.List<String> logs) {
        if (currentPlayersFlow != null) {
            // Update players
            currentPlayersFlow.getChildren().clear();
            String me = null;
            try { me = net.client.UserIdentity.getEffectiveUsername(); } catch (Throwable ignored) {}
            for (String player : players) {
                boolean isSelf = (me != null && me.equalsIgnoreCase(player));
                String labelText = isSelf ? player + " (you)" : player;
                javafx.scene.control.Label playerChip = new javafx.scene.control.Label(labelText);
                playerChip.getStyleClass().add(isSelf ? "player-chip-self" : "player-chip");
                try {
                    javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(isSelf ? (player + " · this client") : ("Player · " + player));
                    javafx.scene.control.Tooltip.install(playerChip, tip);
                } catch (Throwable ignored) {}
                currentPlayersFlow.getChildren().add(playerChip);
            }
        }
        
        if (currentLogsContainer != null && logs != null) {
            // Update logs
            currentLogsContainer.getChildren().clear();
            for (String log : logs) {
                javafx.scene.control.Label logLabel = new javafx.scene.control.Label(log);
                logLabel.getStyleClass().add("room-log-entry");
                logLabel.setWrapText(true);
                currentLogsContainer.getChildren().add(logLabel);
            }
            
            // Auto-scroll to bottom
            if (currentLogsScroll != null) {
                javafx.application.Platform.runLater(() -> currentLogsScroll.setVvalue(1.0));
            }
        }
    }

    private void showOnlineRoom(String code, java.util.List<String> players) {
        // Load the multiplayer level from config
        String levelId = "multiplayer";
        config.levels.LevelConfigLoader configLoader = new config.levels.LevelConfigLoader();
        configLoader.loadIndex("levels/levels-index.json");
        config.levels.LevelDefinition def = configLoader.findLevelById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Multiplayer level not found"));
        model.levels.Level multiplayerLevel = new config.levels.LevelFactory().createLevel(def);
        
        // Create level view using DataDrivenLevelView to properly render the multiplayer level
        view.components.levels.DataDrivenLevelView levelView = new view.components.levels.DataDrivenLevelView(multiplayerLevel, this, def);
        
        // Initialize game controller
        controller.GameController gameController = new controller.GameController(multiplayerLevel, levelView, this);
        gameController.startGame();
        
        // Set up HUD connection status for multiplayer (missing in DataDrivenLevelView)
        try {
            view.game.HUDScene hud = levelView.getHUDScene();
            if (hud != null) {
                // Initialize HUD connection status immediately
                hud.setConnectionStatus(net.NetworkService.getInstance().isConnected());
                net.NetworkService.getInstance().onConnectionChanged(connected -> {
                    hud.setConnectionStatus(connected);
                    try { 
                        view.components.Toast.show(levelView, connected ? "Connected" : "Connection lost", connected); 
                    } catch (Throwable ignored) {}
                });
            }
        } catch (Throwable ignored) {}

        // Get the game pane from level view
        javafx.scene.layout.Pane gamePane = levelView.getGamePane();
        if (gamePane != null) {
            // Build room info overlay to position over game pane only
            // Get the main layout from level view to add overlay
            javafx.scene.layout.BorderPane mainLayout = null;
            try {
                mainLayout = (javafx.scene.layout.BorderPane) levelView.getChildren().get(0);
            } catch (Exception ignored) {}
            
            // Create the room overlay
            javafx.scene.layout.VBox roomOverlay = new javafx.scene.layout.VBox();
            roomOverlay.getStyleClass().add("room-info-overlay");
            roomOverlay.setMaxWidth(280);
            roomOverlay.setPrefWidth(260);

            // Room name section
            javafx.scene.layout.HBox roomHeader = new javafx.scene.layout.HBox();
            roomHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            roomHeader.setSpacing(8);
            
            javafx.scene.control.Label roomIcon = new javafx.scene.control.Label("🏠");
            roomIcon.setStyle("-fx-font-size: 16px;");
            
            javafx.scene.control.Label roomLabel = new javafx.scene.control.Label(code);
            roomLabel.getStyleClass().add("room-info-label");
            
            // Close button
            javafx.scene.control.Button closeButton = new javafx.scene.control.Button("×");
            closeButton.getStyleClass().add("room-close-btn");
            closeButton.setOnAction(e -> {
                service.AudioManager.playButtonClick();
                roomOverlay.setVisible(false);
            });
            
            roomHeader.getChildren().addAll(roomIcon, roomLabel);
            
            // Top row with room info and close button
            javafx.scene.layout.HBox topRow = new javafx.scene.layout.HBox();
            topRow.getChildren().addAll(roomHeader, closeButton);
            topRow.setAlignment(javafx.geometry.Pos.CENTER);
            javafx.scene.layout.HBox.setHgrow(roomHeader, javafx.scene.layout.Priority.ALWAYS);

            // Players section
            javafx.scene.layout.HBox playersHeader = new javafx.scene.layout.HBox();
            playersHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            playersHeader.setSpacing(8);
            
            javafx.scene.control.Label playersIcon = new javafx.scene.control.Label("👥");
            playersIcon.setStyle("-fx-font-size: 14px;");
            
            javafx.scene.control.Label playersTitle = new javafx.scene.control.Label("Players:");
            playersTitle.getStyleClass().add("room-players-title");
            
            playersHeader.getChildren().addAll(playersIcon, playersTitle);

            // Players list with styled chips (initialize empty; server will drive updates)
            currentPlayersFlow = new javafx.scene.layout.FlowPane();
            currentPlayersFlow.setHgap(6);
            currentPlayersFlow.setVgap(4);
            updateRoomUI(players, java.util.Collections.emptyList());

            // Activity logs section
            javafx.scene.layout.HBox logsHeader = new javafx.scene.layout.HBox();
            logsHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            logsHeader.setSpacing(8);
            
            javafx.scene.control.Label logsIcon = new javafx.scene.control.Label("📋");
            logsIcon.setStyle("-fx-font-size: 14px;");
            
            javafx.scene.control.Label logsTitle = new javafx.scene.control.Label("Activity:");
            logsTitle.getStyleClass().add("room-logs-title");
            
            logsHeader.getChildren().addAll(logsIcon, logsTitle);

            // Logs list with scroll
            currentLogsContainer = new javafx.scene.layout.VBox();
            currentLogsContainer.getStyleClass().add("room-logs-container");
            currentLogsContainer.setMaxHeight(80);
            currentLogsContainer.setPrefHeight(80);
            
            currentLogsScroll = new javafx.scene.control.ScrollPane();
            currentLogsScroll.setContent(currentLogsContainer);
            currentLogsScroll.getStyleClass().add("room-logs-scroll");
            currentLogsScroll.setFitToWidth(true);
            currentLogsScroll.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            currentLogsScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            currentLogsScroll.setMaxHeight(80);

            // Lightweight UI keep-alive to ensure overlay stays responsive
            javafx.animation.Timeline refreshTl = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(5.0), ev -> {})
            );
            refreshTl.setCycleCount(javafx.animation.Animation.INDEFINITE);
            refreshTl.play();

            roomOverlay.getChildren().addAll(topRow, playersHeader, currentPlayersFlow, logsHeader, currentLogsScroll);
            roomOverlay.setVisible(true);
            
            // Add overlay directly to the level view
            levelView.getChildren().add(roomOverlay);
            javafx.scene.layout.StackPane.setAlignment(roomOverlay, javafx.geometry.Pos.TOP_RIGHT);
            javafx.scene.layout.StackPane.setMargin(roomOverlay, new javafx.geometry.Insets(100, 15, 0, 0));
            
            // Store reference for button wiring
            final javafx.scene.layout.VBox finalRoomOverlay = roomOverlay;

            // Ensure close button stops refresh timer
            try {
                closeButton.setOnAction(e -> {
                    service.AudioManager.playButtonClick();
                    try { refreshTl.stop(); } catch (Throwable ignored) {}
                    finalRoomOverlay.setVisible(false);
                });
            } catch (Throwable ignored) {}

            javafx.scene.Scene fxScene = new javafx.scene.Scene(levelView, WINDOW_WIDTH, WINDOW_HEIGHT);
            try { fxScene.getStylesheets().add(cssFile); } catch (Throwable ignored) {}
            try { service.AudioManager.stopMenuMusic(); } catch (Throwable ignored) {}
            try { service.AudioManager.playBackgroundMusic(); } catch (Throwable ignored) {}
            primaryStage.setScene(fxScene);

            // Wire HUD Room button to show overlay
            try {
                view.game.HUDScene hud = levelView.getHUDScene();
                if (hud != null && hud.getRoomButton() != null) {
                    hud.getRoomButton().setOnAction(e -> {
                        service.AudioManager.playButtonClick();
                        finalRoomOverlay.setVisible(true);
                    });
                }
            } catch (Throwable ignored) {}

            // Allow clicking on game pane to close overlay
            gamePane.setOnMouseClicked(e -> {
                if (finalRoomOverlay.isVisible()) {
                    finalRoomOverlay.setVisible(false);
                }
            });
        } else {
            // Fallback if gamePane not found
            javafx.scene.Scene fxScene = new javafx.scene.Scene(levelView, WINDOW_WIDTH, WINDOW_HEIGHT);
            try { fxScene.getStylesheets().add(cssFile); } catch (Throwable ignored) {}
            primaryStage.setScene(fxScene);
        }
    }

    /**
     * Unlock Level 2 (call this after Level 1 is completed).
     */
    public void unlockLevel2() {
        level2Unlocked = true;
    }

    /**
     * Get the current sound volume (0-100).
     */
    public double getSoundVolume() {
        return soundVolume;
    }

    // Expose LevelManager for networking (run start/finish hooks)
    public LevelManager getLevelManager() {
        return levelManager;
    }
} 
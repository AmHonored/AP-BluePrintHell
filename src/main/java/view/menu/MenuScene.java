package view.menu;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
// import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.layout.StackPane;
// import javafx.scene.layout.BorderPane;
import javafx.geometry.Insets;

/**
 * Main menu scene for Blueprint Hell. Contains buttons for navigation.
 * Follows SOLID and clean code principles. No business logic here.
 */
public class MenuScene extends StackPane {
    private final Button startGameButton;
    private final Button levelSelectButton;
    private final Button settingsButton;
    private final Button exitButton;
    private javafx.scene.control.Label connectionStatus;
    private Button connectButton;
    private Button disconnectButton;
    private Button playOfflineButton;
    private volatile boolean connecting = false;
    private Button leaderboardButton;
    private view.game.LeaderboardScene leaderboardOverlay;

    public MenuScene() {
        Text title = new Text("Blueprint Hell");
        title.getStyleClass().add("menu-title-red");

        startGameButton = new Button("Start Game");
        levelSelectButton = new Button("Level Select");
        settingsButton = new Button("Settings");
        leaderboardButton = new Button("Leaderboard");
        exitButton = new Button("Exit");

        startGameButton.setPrefWidth(220);
        levelSelectButton.setPrefWidth(220);
        settingsButton.setPrefWidth(220);
        leaderboardButton.setPrefWidth(220);
        exitButton.setPrefWidth(220);

        // Connection status label (below title)
        connectionStatus = new javafx.scene.control.Label("Disconnected");
        connectionStatus.getStyleClass().add("menu-title-red");
        connectionStatus.setStyle("-fx-font-size: 22px; -fx-text-fill: #ff4d4d; -fx-effect: dropshadow(gaussian, rgba(255,77,77,0.6), 8, 0.5, 0, 0);");

        // Main menu buttons in center (reduced spacing)
        VBox mainButtonBox = new VBox(10, startGameButton, levelSelectButton, settingsButton, leaderboardButton, exitButton);
        mainButtonBox.setAlignment(Pos.CENTER);

        // Title + connection status + buttons (reduced vertical gaps)
        VBox centerContent = new VBox(10, title, connectionStatus, mainButtonBox);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(20, 0, 0, 0)); // Reduced top margin

        // Connection controls in bottom left - smaller and distinct
        connectButton = new Button("Connect");
        disconnectButton = new Button("Disconnect");
        playOfflineButton = new Button("Play Offline");
        
        // Distinct styling for connection buttons
        String connectionButtonStyle = "-fx-background-color: rgba(0, 212, 255, 0.1);"
            + "-fx-text-fill: #00d4ff;"
            + "-fx-border-color: #00d4ff;"
            + "-fx-border-width: 1;"
            + "-fx-border-radius: 5;"
            + "-fx-background-radius: 5;"
            + "-fx-font-size: 12px;"
            + "-fx-padding: 5 15;"
            + "-fx-cursor: hand;";
        
        connectButton.setStyle(connectionButtonStyle);
        disconnectButton.setStyle(connectionButtonStyle);
        playOfflineButton.setStyle(connectionButtonStyle);
        
        connectButton.setPrefWidth(100);
        disconnectButton.setPrefWidth(100);
        playOfflineButton.setPrefWidth(100);
        
        // Hover effects
        String hoverStyle = connectionButtonStyle.replace("0.1", "0.3");
        connectButton.setOnMouseEntered(e -> connectButton.setStyle(hoverStyle));
        connectButton.setOnMouseExited(e -> connectButton.setStyle(connectionButtonStyle));
        disconnectButton.setOnMouseEntered(e -> disconnectButton.setStyle(hoverStyle));
        disconnectButton.setOnMouseExited(e -> disconnectButton.setStyle(connectionButtonStyle));
        playOfflineButton.setOnMouseEntered(e -> playOfflineButton.setStyle(hoverStyle));
        playOfflineButton.setOnMouseExited(e -> playOfflineButton.setStyle(connectionButtonStyle));

        VBox connectionControls = new VBox(8, connectButton, disconnectButton, playOfflineButton);
        connectionControls.setAlignment(Pos.TOP_LEFT);
        connectionControls.setMouseTransparent(false);

        // Use BorderPane for proper layout without overlapping
        javafx.scene.layout.BorderPane rootLayout = new javafx.scene.layout.BorderPane();
        rootLayout.setCenter(centerContent);
        
        // Add connection controls to bottom with proper padding (slightly higher)
        connectionControls.setPadding(new Insets(0, 0, 60, 40));
        rootLayout.setBottom(connectionControls);
        javafx.scene.layout.BorderPane.setAlignment(connectionControls, Pos.BOTTOM_LEFT);

        this.getChildren().add(rootLayout);
        // Overlay for leaderboard (hidden by default)
        leaderboardOverlay = new view.game.LeaderboardScene();
        leaderboardOverlay.setVisible(false);
        this.getChildren().add(leaderboardOverlay);
        this.getStyleClass().add("menu-root");

        // Leaderboard button behavior
        leaderboardButton.setOnAction(e -> {
            service.AudioManager.playButtonClick();
            try {
                if (!net.NetworkService.getInstance().isConnected()) {
                    try { view.components.Toast.show(this, "Connect to view online leaderboards", false); } catch (Throwable ignored) {}
                    return;
                }
                // Show top XP as a default view; time-per-level can be requested from level select later
                leaderboardOverlay.requestTopXp();
            } catch (Throwable ignored) {}
        });
    }

    // Hooked reflectively by VisualManager to avoid direct dependency on networking layer
    public void addConnectionControls() {
        try {
            net.NetworkService netSvc = net.NetworkService.getInstance();
            netSvc.onConnectionChanged(connected -> {
                connecting = false;
                updateStatus(connected);
                updateButtonStates(connected);
                try {
                    view.components.Toast.show(this, connected ? "Connected" : "Connection lost", connected);
                } catch (Throwable ignored) {}
            });
            // Reflect current connection state immediately on open
            updateStatus(netSvc.isConnected());
            updateButtonStates(netSvc.isConnected());
            // optimistic: attempt localhost connect when Connect clicked
            connectButton.setOnAction(e -> {
                service.AudioManager.playButtonClick();
                if (connecting || netSvc.isConnected()) return;
                setStatusConnecting();
                updateButtonStates(false);
                new Thread(() -> netSvc.connectOnline("127.0.0.1", 5050, System.getProperty("user.name", "player")), "try-connect").start();
            });
            disconnectButton.setOnAction(e -> {
                service.AudioManager.playButtonClick();
                if (connecting) return; // debounce
                netSvc.disconnect("user");
            });
            playOfflineButton.setOnAction(e -> {
                service.AudioManager.playButtonClick();
                netSvc.goOffline();
            });
            // Initialize button states
            updateButtonStates(netSvc.isConnected());
        } catch (Throwable ignored) {}
    }

    private void setStatusConnecting() {
        connecting = true;
        connectionStatus.setText("Connecting...");
        connectionStatus.setStyle("-fx-font-size: 22px; -fx-text-fill: #ffaa00; -fx-effect: dropshadow(gaussian, rgba(255,170,0,0.6), 8, 0.5, 0, 0);");
    }

    private void updateStatus(boolean connected) {
        if (connected) {
            connectionStatus.setText("Connected");
            connectionStatus.setStyle("-fx-font-size: 22px; -fx-text-fill:rgb(12, 234, 46); -fx-effect: dropshadow(gaussian, rgba(0,212,255,0.7), 10, 0.5, 0, 0);");
        } else {
            connectionStatus.setText("Disconnected");
            connectionStatus.setStyle("-fx-font-size: 22px; -fx-text-fill: #ff4d4d; -fx-effect: dropshadow(gaussian, rgba(255,77,77,0.6), 8, 0.5, 0, 0);");
        }
    }

    private void updateButtonStates(boolean isConnected) {
        if (connecting) {
            connectButton.setDisable(true);
            disconnectButton.setDisable(true);
            playOfflineButton.setDisable(false);
        } else {
            connectButton.setDisable(isConnected);
            disconnectButton.setDisable(!isConnected);
            playOfflineButton.setDisable(false);
        }
    }

    public Button getStartGameButton() {
        return startGameButton;
    }

    public Button getLevelSelectButton() {
        return levelSelectButton;
    }

    public Button getSettingsButton() {
        return settingsButton;
    }

    public Button getExitButton() {
        return exitButton;
    }
}

package view.game;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
// import javafx.scene.layout.StackPane; // not used
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import model.levels.Level;

public class HUDScene extends VBox {
    private final StatsBox wireBox;
    private final StatsBox progressBox;
    private final StatsBox lossBox;
    private final StatsBox coinsBox;
    private final StatsBox packetsBox;
    private final Label connectionLabel;
    private final javafx.scene.control.Button aergiaButton;
    private final javafx.scene.control.Button sisyphusButton;
    private final javafx.scene.control.Button eliphasButton;
    private final Button toggleHudButton;
    private final TemporalProgress temporalProgress;
    private final HBox statsContainer;
    private boolean isHudVisible = true;

    public HUDScene(Level level) {
        this.getStyleClass().add("hud-pane");
        this.setSpacing(15);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(15, 20, 15, 20));

        // Create stats boxes
        wireBox = new StatsBox("Remaining Wire", "0");
        progressBox = new StatsBox("Level Progress", "");
        lossBox = new StatsBox("Packet Loss", "0.0%");
        coinsBox = new StatsBox("Coins", "0");
        packetsBox = new StatsBox("Packets Collected", "0");

        // Create stats container (horizontal layout for stats)
        statsContainer = new HBox();
        statsContainer.setSpacing(20);
        statsContainer.setAlignment(Pos.CENTER);

        // Left section: Wire and Loss stats
        HBox leftStats = new HBox();
        leftStats.setSpacing(15);
        leftStats.setAlignment(Pos.CENTER_LEFT);
        leftStats.getChildren().addAll(wireBox, lossBox);

        // Center section: Progress bar and time controls
        VBox progressCenter = new VBox();
        progressCenter.setAlignment(Pos.CENTER);
        progressCenter.setSpacing(8);
        temporalProgress = new TemporalProgress(level);
        Label progressTitle = new Label("Level Progress");
        progressTitle.getStyleClass().addAll("stats-title", "progress-title");
        progressCenter.getChildren().addAll(progressTitle, temporalProgress);
        progressBox.getChildren().clear();
        progressBox.getChildren().add(progressCenter);

        // Right section: Coins and Packets
        HBox rightStats = new HBox();
        rightStats.setSpacing(15);
        rightStats.setAlignment(Pos.CENTER_RIGHT);
        rightStats.getChildren().addAll(coinsBox, packetsBox);

        // Add all stats to the main stats container
        statsContainer.getChildren().addAll(leftStats, progressBox, rightStats);

        // Connection status (will be added to button container)
        connectionLabel = new Label("Offline");
        connectionLabel.getStyleClass().add("connection-status");
        connectionLabel.setStyle("-fx-text-fill: #ff6666; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Create toggle HUD button with enhanced styling
        toggleHudButton = new Button("Hide HUD");
        toggleHudButton.getStyleClass().addAll("hide-hud-button", "hud-toggle-button");
        toggleHudButton.setPrefWidth(120);
        
        // Set up toggle functionality
        toggleHudButton.setOnAction(e -> toggleHudVisibility());

        // Aergia button (moved from bottom bar)
        aergiaButton = new Button("Aergia (0)");
        aergiaButton.getStyleClass().addAll("button", "aergia-button");
        aergiaButton.setPrefWidth(120);

        // Sisyphus button
        sisyphusButton = new Button("Sisyphus (0)");
        sisyphusButton.getStyleClass().addAll("button", "sisyphus-button");
        sisyphusButton.setPrefWidth(120);

        // Eliphas button
        eliphasButton = new Button("Eliphas (0)");
        eliphasButton.getStyleClass().addAll("button", "eliphas-button");
        eliphasButton.setPrefWidth(120);

        // Button container with connection status on the right
        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(10); // Reduced spacing to bring connection status closer
        buttonContainer.setPadding(new Insets(8, 0, 0, 0)); // Small top padding
        
        // Main buttons
        HBox leftButtons = new HBox();
        leftButtons.setSpacing(15);
        leftButtons.setAlignment(Pos.CENTER);
        leftButtons.getChildren().addAll(toggleHudButton, aergiaButton, sisyphusButton, eliphasButton);
        
        // Add buttons and connection status directly to container
        buttonContainer.getChildren().addAll(leftButtons, connectionLabel);

        // Add all components to the main VBox
        this.getChildren().addAll(statsContainer, buttonContainer);
    }

    /**
     * Toggle HUD visibility between showing stats and hiding them
     */
    private void toggleHudVisibility() {
        isHudVisible = !isHudVisible;
        
        if (isHudVisible) {
            // Show stats
            statsContainer.setVisible(true);
            statsContainer.setManaged(true);
            toggleHudButton.setText("Hide HUD");
            toggleHudButton.getStyleClass().remove("show-hud-button");
            toggleHudButton.getStyleClass().add("hide-hud-button");
        } else {
            // Hide stats
            statsContainer.setVisible(false);
            statsContainer.setManaged(false);
            toggleHudButton.setText("Show HUD");
            toggleHudButton.getStyleClass().remove("hide-hud-button");
            toggleHudButton.getStyleClass().add("show-hud-button");
        }
    }

    /**
     * Force show HUD (for external control)
     */
    public void showHud() {
        if (!isHudVisible) {
            toggleHudVisibility();
        }
    }

    /**
     * Force hide HUD (for external control)
     */
    public void hideHud() {
        if (isHudVisible) {
            toggleHudVisibility();
        }
    }

    /**
     * Check if HUD is currently visible
     */
    public boolean isHudVisible() {
        return isHudVisible;
    }

    // Getters for existing functionality
    public StatsBox getWireBox() { return wireBox; }
    public StatsBox getProgressBox() { return progressBox; }
    public StatsBox getLossBox() { return lossBox; }
    public StatsBox getCoinsBox() { return coinsBox; }
    public StatsBox getPacketsBox() { return packetsBox; }
    public Button getHideHudButton() { return toggleHudButton; }
    public TemporalProgress getTemporalProgress() { return temporalProgress; }
    public Button getAergiaButton() { return aergiaButton; }
    public Button getSisyphusButton() { return sisyphusButton; }
    public Button getEliphasButton() { return eliphasButton; }

    public void setConnectionStatus(boolean connected) {
        if (connected) {
            connectionLabel.setText("Online");
            connectionLabel.setStyle("-fx-text-fill: #66ff66;");
        } else {
            connectionLabel.setText("Offline");
            connectionLabel.setStyle("-fx-text-fill: #ff6666;");
        }
    }
}

package view.game;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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

        wireBox = new StatsBox("Remaining Wire", "0");
        progressBox = new StatsBox("Level Progress", "");
        lossBox = new StatsBox("Packet Loss", "0.0%");
        coinsBox = new StatsBox("Coins", "0");
        packetsBox = new StatsBox("Packets Collected", "0");

        statsContainer = new HBox();
        statsContainer.setSpacing(20);
        statsContainer.setAlignment(Pos.CENTER);

        HBox leftStats = new HBox();
        leftStats.setSpacing(15);
        leftStats.setAlignment(Pos.CENTER_LEFT);
        leftStats.getChildren().addAll(wireBox, lossBox);

        VBox progressCenter = new VBox();
        progressCenter.setAlignment(Pos.CENTER);
        progressCenter.setSpacing(8);
        temporalProgress = new TemporalProgress(level);
        Label progressTitle = new Label("Level Progress");
        progressTitle.getStyleClass().addAll("stats-title", "progress-title");
        Label timeLabel = new Label("Time: 0");
        timeLabel.getStyleClass().add("time-label");
        progressCenter.getChildren().addAll(progressTitle, temporalProgress, timeLabel);
        progressBox.getChildren().clear();
        progressBox.getChildren().add(progressCenter);

        HBox rightStats = new HBox();
        rightStats.setSpacing(15);
        rightStats.setAlignment(Pos.CENTER_RIGHT);
        rightStats.getChildren().addAll(coinsBox, packetsBox);

        statsContainer.getChildren().addAll(leftStats, progressBox, rightStats);

        toggleHudButton = new Button("Hide HUD");
        toggleHudButton.getStyleClass().addAll("hide-hud-button", "hud-toggle-button");
        toggleHudButton.setPrefWidth(120);
        
        toggleHudButton.setOnAction(e -> toggleHudVisibility());

        aergiaButton = new Button("Aergia (0)");
        aergiaButton.getStyleClass().addAll("button", "aergia-button");
        aergiaButton.setPrefWidth(120);

        sisyphusButton = new Button("Sisyphus (0)");
        sisyphusButton.getStyleClass().addAll("button", "sisyphus-button");
        sisyphusButton.setPrefWidth(120);

        eliphasButton = new Button("Eliphas (0)");
        eliphasButton.getStyleClass().addAll("button", "eliphas-button");
        eliphasButton.setPrefWidth(120);

        HBox buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(20);
        buttonContainer.getChildren().addAll(toggleHudButton, aergiaButton, sisyphusButton, eliphasButton);

        this.getChildren().addAll(statsContainer, buttonContainer);
    }

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

    public void showHud() {
        if (!isHudVisible) {
            toggleHudVisibility();
        }
    }

    public void hideHud() {
        if (isHudVisible) {
            toggleHudVisibility();
        }
    }

    public boolean isHudVisible() {
        return isHudVisible;
    }

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
}

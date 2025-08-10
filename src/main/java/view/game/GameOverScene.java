package view.game;

import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

public class GameOverScene extends VBox {
    private final VBox titleContainer;
    private final Label statusLabel;
    private final Label messageLabel;
    private final Text subtitleText;
    private final Rectangle accentBar;
    private final Button retryButton;
    private final Button menuButton;
    private final VBox statsContainer;
    private final HBox buttonContainer;

    public GameOverScene() {
        this.getStyleClass().add("game-over-overlay");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(35);
        this.setPadding(new Insets(50, 60, 50, 60));

        titleContainer = new VBox();
        titleContainer.setAlignment(Pos.CENTER);
        titleContainer.setSpacing(12);
        
        statusLabel = new Label("");
        statusLabel.getStyleClass().add("game-over-status");
        
        messageLabel = new Label("GAME OVER");
        messageLabel.getStyleClass().add("game-over-title");

        subtitleText = new Text("Time to rebuild your strategy.");
        subtitleText.getStyleClass().add("game-over-subtitle");
        
        accentBar = new Rectangle(300, 4);
        accentBar.getStyleClass().add("game-over-accent-bar");
        
        titleContainer.getChildren().addAll(statusLabel, messageLabel, subtitleText, accentBar);

        statsContainer = new VBox();
        statsContainer.getStyleClass().add("game-over-stats-container");
        statsContainer.setAlignment(Pos.CENTER);
        statsContainer.setSpacing(15);
        statsContainer.setPadding(new Insets(20, 0, 20, 0));

        retryButton = new Button("⟲ RETRY LEVEL");
        menuButton = new Button("◀ BACK TO MENU");
        
        retryButton.getStyleClass().addAll("cyberpunk-button", "cyberpunk-button-secondary");
        menuButton.getStyleClass().addAll("cyberpunk-button", "cyberpunk-button-danger");

        buttonContainer = new HBox();
        buttonContainer.setAlignment(Pos.CENTER);
        buttonContainer.setSpacing(30);
        buttonContainer.setPadding(new Insets(25, 0, 0, 0));
        buttonContainer.getChildren().addAll(retryButton, menuButton);

        this.getChildren().addAll(titleContainer, buttonContainer);
        this.setVisible(false);
    }

    public void addStatsDisplay(String title, String value, String color) {
        HBox statBox = new HBox();
        statBox.getStyleClass().add("game-over-stat-item");
        statBox.setStyle("-fx-border-color: " + color + ";");

        Label titleLabel = new Label(title + ":");
        titleLabel.getStyleClass().add("game-over-stat-title");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("game-over-stat-value");
        valueLabel.setStyle("-fx-text-fill: " + color + ";");

        statBox.getChildren().addAll(titleLabel, valueLabel);
        statsContainer.getChildren().add(statBox);
    }

    public void clearStats() {
        if (statsContainer.getChildren().size() > 1) {
            statsContainer.getChildren().removeIf(node -> node != statsContainer.getChildren().get(0));
        }
    }

    public Label getMessageLabel() { return messageLabel; }
    public Button getRetryButton() { return retryButton; }
    public Button getMenuButton() { return menuButton; }
    public VBox getStatsContainer() { return statsContainer; }
}
